/**
 * $Id: JythonManager.java,v 1.82 2007/08/17 10:51:20 jeffmc Exp $
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


package ucar.unidata.idv;



import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.Element;

import ucar.unidata.data.CacheDataSource;
import ucar.unidata.data.DataCancelException;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DerivedDataDescriptor;
import ucar.unidata.data.DescriptorDataSource;

import ucar.unidata.idv.ui.*;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlPersistable;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import visad.Data;
import visad.VisADException;

import visad.python.*;


import java.awt.*;

import java.awt.datatransfer.*;
import java.awt.event.*;

import java.io.File;
import java.io.FileOutputStream;

import java.io.InputStream;


import java.net.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;






/**
 * Manages  jython related functionality:<ul>
 * <li> The set of jtyhon interpreters used in the app.
 *
 * <li> The set of jython libraries.
 *
 * <li> The set of end user formulas. These are defined
 * using {@link ucar.unidata.data.DerivedDataDescriptor}
 * and are held  in  {@link ucar.unidata.data.DescriptorDataSource}
 *
 * </ul>
 *
 * @author IDV development team
 */


public class JythonManager extends IdvManager implements ActionListener {

    /** The path to the editor executable */
    public static final String PROP_JYTHON_EDITOR = "idv.jython.editor";

    /** any errors */
    private boolean inError = false;

    /** Hodls the temporary jython */
    private String tmpJython = "";

    /** tabbed pane */
    private JTabbedPane jythonTab;

    /** The tabbed pane that holds the editable component */
    private JTabbedPane editableTab;

    /** One text component per tab */
    private ArrayList libHolders;

    /** Shows the temp jython */
    private JTextArea tmpTextArea = new JTextArea();

    /** tmp lib */
    private LibHolder tmpHolder;


    /**
     * This holds all of the  end-user formulas, the ones
     * from the system and the ones from the users local space.
     */
    private DescriptorDataSource descriptorDataSource;

    /** formula descriptors */
    List descriptors;


    /**
     * This is the interpreter used for processing the
     * UI commands. e.g., the ones in defaultmenu.xml
     */
    private PythonInterpreter uiInterpreter = null;


    /**
     *  Used to evaluate derived data choices
     */
    private PythonInterpreter derivedDataInterpreter;


    /** The jython editor */
    private LibHolder mainHolder;


    /**
     * List of all active interpreters. We keep these around so when
     * the user changes the jython library we can reevaluate the code
     * in each interpreter.
     */
    private List interpreters = new ArrayList();


    /** The edit menu item */
    private JMenuItem editFileMenuItem;


    /** local python dir */
    private String pythonDir;

    /**
     * Create the manager and call initPython.
     *
     * @param idv The IDV
     */
    public JythonManager(IntegratedDataViewer idv) {
        super(idv);
        pythonDir = IOUtil.joinDir(getStore().getUserDirectory().toString(),
                                   "python");
        if ( !new File(pythonDir).exists()) {
            try {
                IOUtil.makeDir(pythonDir);
                //Move the old default.py to the subdir
                File oldFile =
                    new File(IOUtil.joinDir(getStore().getUserDirectory(),
                                            "default.py"));
                File newFile = new File(IOUtil.joinDir(pythonDir,
                                   "default.py"));
                if (oldFile.exists()) {
                    IOUtil.moveFile(oldFile, new File(pythonDir));
                } else {
                    IOUtil.writeFile(newFile.toString(), "");
                }
            } catch (Exception exc) {
                logException("Moving  jython lib", exc);
            }
        }
        initPython();
    }




    /**
     * Initialize the Python package in a thread.
     * We define the python cache to be in the users
     * .metapps directory. Python puts the results of its
     * parsing the jar files there.
     */
    private void initPython() {
        Misc.run(new Runnable() {
            public void run() {
                initPythonInner();
            }
        });
    }



    /**
     * Initialize the python interpreter. This gets called from initPython inside of a thread.
     */
    private void initPythonInner() {

        String cacheDir = getStore().getJythonCacheDir();
        ResourceCollection rc = getResourceManager().getResources(
                                    IdvResourceManager.RSC_JYTHONTOCOPY);
        rc.clearCache();
        try {
            for (int i = 0; i < rc.size(); i++) {
                String path      = rc.get(i).toString();
                String name      = IOUtil.getFileTail(path);
                File   localFile = new File(IOUtil.joinDir(cacheDir, name));
                //System.err.println ("Jython lib:" + localFile);

                //Do we copy all of the time? Perhaps we need to
                //check if we are running a new version?
                //if(localFile.exists()) continue;

                String contents = rc.read(i);
                if (contents == null) {
                    continue;
                }
                //              System.err.println ("Writing:" + contents);
                IOUtil.writeFile(localFile.getPath(), contents);
            }
        } catch (Exception exc) {
            logException("Writing jython lib", exc);
        }



        Properties pythonProps = new Properties();
        if (cacheDir != null) {
            pythonProps.put("python.home", cacheDir);
        }

        PythonInterpreter.initialize(System.getProperties(), pythonProps,
                                     getArgsManager().commandLineArgs);

        doMakeContents();

        makeFormulasFromLib();


        //      PySystemState sys = Py.getSystemState ();
        //      sys.add_package ("visad");
        //      sys.add_package ("visad.python");

    }


    /**
     * make formulas from the methods in the lib
     */
    private void makeFormulasFromLib() {
        List procedures = findJythonMethods(true);
        for (int i = 0; i < procedures.size(); i++) {
            PyFunction func = (PyFunction) procedures.get(i);
            String     doc  = func.__doc__.toString().trim();
            if (doc.equals("None")) {
                continue;
            }
            List      lines     = StringUtil.split(doc, "\n", true, true);
            String    formulaId = null;
            String    desc      = null;
            String    group     = null;
            Hashtable attrProps = null;
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                String line = (String) lines.get(lineIdx);
                if (line.startsWith("@formulaid")) {
                    formulaId = line.substring("@formulaid".length()).trim();
                } else if (line.startsWith("@description")) {
                    desc = line.substring("@description".length()).trim();
                } else if (line.startsWith("@group")) {
                    group = line.substring("@group".length()).trim();
                } else if (line.startsWith("@param")) {
                    line = line.substring("@param".length()).trim();
                    String[] toks = StringUtil.split(line, " ", 2);
                    if (attrProps == null) {
                        attrProps = new Hashtable();
                    }
                    attrProps.put(toks[0], "[" + toks[1] + "]");
                }
            }

            if ((formulaId != null) || (desc != null)) {
                if (formulaId == null) {
                    formulaId = func.__name__;
                }
                List categories = new ArrayList();
                if (group != null) {
                    categories.add(DataCategory.parseCategory(group, true));
                }
                DerivedDataDescriptor ddd =
                    new DerivedDataDescriptor(getIdv(), formulaId,
                        ((desc != null)
                         ? desc
                         : func.__name__), makeCallString(func, attrProps),
                                           categories);

                descriptorDataSource.addDescriptor(ddd);
            }
        }
    }


    /**
     * Create, if needed, and show the jython editor.
     */
    public void showJythonEditor() {
        super.show();
    }


    /**
     * Find the visible library
     *
     * @return lib being shown
     */
    private LibHolder findVisibleComponent() {
        for (int i = 0; i < libHolders.size(); i++) {
            LibHolder holder = (LibHolder) libHolders.get(i);
            try {
                holder.outerContents.getLocationOnScreen();
                return holder;
            } catch (Exception exc) {}
        }
        return null;
    }

    /**
     * Export selcted text of current tab to plugin
     */
    public void exportSelectedToPlugin() {
        LibHolder holder = findVisibleComponent();
        String    text   = "";
        holder.copy();
        Clipboard clipboard =
            Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
            (contents != null)
            && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                text = (String) contents.getTransferData(
                    DataFlavor.stringFlavor);
                if (holder.getText().indexOf(text) < 0) {
                    text = null;
                }
            } catch (Exception ex) {
                LogUtil.logException("Accessing clipboard", ex);
                return;
            }
        }


        if ((text == null) || (text.trim().length() == 0)) {
            LogUtil.userMessage("No text selected");
            return;
        }
        getIdv().getPluginManager().addText(text, "jython.py");
    }

    /**
     * Export  to plugin
     */
    public void exportToPlugin() {
        LibHolder holder = findVisibleComponent();
        getIdv().getPluginManager().addText(holder.getText(), "jython.py");
    }


    /**
     * Create the jython editor. We create a tabbed pane,
     * one for each valid jython library defined in the
     * IDV's resource manager.
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {

        if (contents != null) {
            return contents;
        }

        try {
            ResourceCollection resources = getResourceManager().getResources(
                                               IdvResourceManager.RSC_JYTHON);
            if (resources == null) {
                LogUtil.userMessage("No Jython resources defined");
                return null;
            }
            jythonTab   = new JTabbedPane();
            libHolders  = new ArrayList();
            editableTab = new JTabbedPane();
            jythonTab.add("Local Jython", editableTab);
            int       systemCnt = 1;
            Hashtable tabs      = new Hashtable();
            Hashtable seen      = new Hashtable();
            for (int i = 0; i < resources.size(); i++) {
                String showInEditor = resources.getProperty("showineditor",
                                          i);
                if ((showInEditor != null) && showInEditor.equals("false")) {
                    //                    System.err.println ("skipping:" + resources.get(i));
                    continue;
                }
                String path  = resources.get(i).toString();
                List   files = new ArrayList();
                File   file  = new File(path);
                if (file.exists() && file.isDirectory()) {
                    File[] libFiles =
                        file.listFiles(
                            (java.io.FileFilter) new PatternFileFilter(
                                ".*\\.py$"));
                    files.addAll(Misc.toList(libFiles));

                } else {
                    files.add(path);
                }

                for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
                    path = (String) files.get(fileIdx).toString();
                    file = new File(path);
                    String canonicalPath = StringUtil.replace(path, "\\",
                                               "/");
                    if (seen.get(canonicalPath) != null) {
                        continue;
                    }
                    seen.put(canonicalPath, canonicalPath);
                    String label = resources.getLabel(i);
                    if (label == null) {
                        label =
                            IOUtil.getFileTail(PluginManager.decode(path));
                    }

                    String text = IOUtil.readContents(path, (String) null);
                    if ((text != null) && Misc.isHtml(text)) {
                        continue;
                    }
                    String    pathDesc = PluginManager.decode(path);
                    LibHolder libHolder;
                    if (resources.isWritableResource(i)
                            || (file.exists() && file.canWrite())) {
                        libHolder = makeEditableLibHolder(label, path, text);
                    } else {
                        //Only add in the non-editable pane if there is a real resource there
                        if (new File(path).isDirectory() || (text == null)) {
                            continue;
                        }
                        JTextArea textArea = new JTextArea(text, 20, 50);
                        textArea.setEditable(false);
                        textArea.setBackground(new Color(230, 230, 230));
                        JComponent wrapper =
                            GuiUtils.center(GuiUtils.makeScrollPane(textArea,
                                400, 300));
                        JComponent tabContents =
                            GuiUtils.topCenter(
                                GuiUtils.inset(
                                    new JLabel("Non-editable"), 2), wrapper);
                        libHolder = new LibHolder(label, textArea, path,
                                wrapper, tabContents);
                        pathDesc = pathDesc + " (" + Msg.msg("non-editable")
                                   + ")";
                        libHolders.add(libHolder);
                    }


                    String category    = resources.getProperty("category", i);
                    JTabbedPane theTab = jythonTab;
                    if (libHolder.isEditable()) {
                        theTab = editableTab;
                    } else if (category != null) {
                        theTab = (JTabbedPane) tabs.get(category);
                        if (theTab == null) {
                            theTab = new JTabbedPane();
                            jythonTab.add(category, theTab);
                            tabs.put(category, theTab);
                        }
                    }
                    theTab.add(label, libHolder.outerContents);
                    theTab.setToolTipTextAt(theTab.getTabCount() - 1,
                                            pathDesc);
                }
            }
            JLabel label = new JLabel("Temporary Jython");
            JPanel tmpPanel =
                GuiUtils.center(GuiUtils.makeScrollPane(tmpTextArea, 400,
                    300));
            String tmpPath = getIdv().getStore().getTmpFile("tmp.py");
            libHolders.add(tmpHolder = new LibHolder("Temporary",
                    tmpTextArea, tmpPath, tmpPanel, tmpPanel));
            jythonTab.add("Temporary",
                          GuiUtils.topCenter(GuiUtils.inset(label, 4),
                                             tmpPanel));


            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = GuiUtils.makeDynamicMenu("File", this,
                                 "makeFileMenu");
            JMenu helpMenu = new JMenu("Help");
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            helpMenu.add(GuiUtils.makeMenuItem("Show Jython Help", this,
                    "showHelp"));
            return contents = GuiUtils.topCenter(menuBar, jythonTab);
        } catch (Throwable exc) {
            logException("Creating jython editor", exc);
            return null;
        }

    }

    /**
     * Save on exit if anything is changed
     *
     * @return continue with exit
     */
    public boolean saveOnExit() {
        List toSave = new ArrayList();
        for (int i = libHolders.size() - 1; i >= 0; i--) {
            LibHolder holder = (LibHolder) libHolders.get(i);
            if ((holder.saveBtn == null) || !holder.isEditable()) {
                continue;
            }
            if (holder.saveBtn.isEnabled()) {
                toSave.add(holder);
            }
        }
        if (toSave.size() > 0) {
            int response =
                GuiUtils.showYesNoCancelDialog(null,
                    "Do you want to save the modified Jython before exiting?",
                    "Save Jython");
            if (response == 2) {
                return false;
            }
            if (response == 0) {
                for (int i = toSave.size() - 1; i >= 0; i--) {
                    LibHolder holder = (LibHolder) toSave.get(i);
                    if ( !writeJythonLib(holder)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }



    /**
     * Make lib for editing
     *
     *
     * @param label label
     * @param path file
     * @param text text
     *
     * @return LibHolder
     *
     * @throws VisADException On badness
     */
    private LibHolder makeEditableLibHolder(String label, String path,
                                            String text)
            throws VisADException {
        final LibHolder[]   holderArray  = { null };
        final JPythonEditor jythonEditor = new JPythonEditor() {
            public void undoableEditHappened(UndoableEditEvent e) {
                if (holderArray[0] != null) {
                    holderArray[0].saveBtn.setEnabled(true);
                    holderArray[0].functions = null;
                }
                super.undoableEditHappened(e);
            }
        };

        jythonEditor.getTextComponent().addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JTextComponent comp     = jythonEditor.getTextComponent();
                    List           items    = new ArrayList();
                    String         selected = comp.getSelectedText();
                    if ((selected != null)
                            && (selected.trim().length() > 0)) {
                        selected = selected.trim();
                        List funcs = findJythonMethods(true,
                                         Misc.newList(holderArray[0]));
                        for (int i = 0; i < funcs.size(); i++) {
                            PyFunction func = (PyFunction) funcs.get(i);
                            if (func.__name__.equals(selected)) {
                                items.add(
                                    GuiUtils.makeMenuItem(
                                        "Make formula for " + selected,
                                        JythonManager.this, "makeFormula",
                                        func));
                                break;
                            }
                        }

                    }
                    items.add(GuiUtils.makeMenu("Insert Procedure Call",
                            makeProcedureMenu(jythonEditor, "insertText",
                                null)));

                    items.add(
                        GuiUtils.makeMenu(
                            "Insert Idv Action",
                            getIdv().getIdvUIManager().makeActionMenu(
                                jythonEditor, "insertText", true)));

                    JPopupMenu popup = GuiUtils.makePopupMenu(items);
                    popup.show(jythonEditor, e.getX(), e.getY());
                }
            }
        });

        jythonEditor.setPreferredSize(new Dimension(400, 300));
        JComponent wrapper     = GuiUtils.center(jythonEditor);
        JComponent tabContents = GuiUtils.center(wrapper);
        LibHolder libHolder = new LibHolder(label, jythonEditor, path,
                                            wrapper, tabContents);
        holderArray[0] = libHolder;
        libHolder.saveBtn = GuiUtils.makeButton("Save", this,
                "writeJythonLib", libHolder);
        if (mainHolder == null) {
            mainHolder = libHolder;
        }
        libHolders.add(libHolder);

        tabContents.add(BorderLayout.SOUTH, GuiUtils.wrap(libHolder.saveBtn));
        if (text == null) {
            text = "";
        }
        if (text != null) {
            jythonEditor.setText(text);
        }
        libHolder.saveBtn.setEnabled(false);
        return libHolder;
    }


    /**
     * Make a formula
     *
     * @param func function
     */
    public void makeFormula(PyFunction func) {
        String                name = func.__name__;
        DerivedDataDescriptor ddd  = null;
        for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
            DerivedDataDescriptor tmp =
                (DerivedDataDescriptor) descriptors.get(dddIdx);
            if (tmp.getId().equals(name)) {
                ddd = tmp;
                break;
            }
        }

        if (ddd != null) {
            if ( !GuiUtils.askOkCancel(
                    "Formula Exists",
                    "<html>A formula with the name: " + name
                    + " already exists.</br>Do you want to edit it?</html>")) {

                return;
            }
        }


        if (ddd == null) {
            ddd = new DerivedDataDescriptor(getIdv(), name, "",
                                            makeCallString(func, null),
                                            new ArrayList());
        }
        new FormulaDialog(getIdv(), ddd, null, new ArrayList());
    }

    /**
     * make new library
     */
    public void makeNewLibrary() {
        String name = "";
        try {
            while (true) {
                name = GuiUtils.getInput("Enter name for library", "Name: ",
                                         name);
                if (name == null) {
                    return;
                }
                String fullName = IOUtil.cleanFileName(name);
                if ( !fullName.endsWith(".py")) {
                    fullName = fullName + ".py";
                }
                fullName = IOUtil.joinDir(pythonDir, fullName);
                if (new File(fullName).exists()) {
                    LogUtil.userErrorMessage(
                        "A jython library with the given name already exists");
                    continue;
                }
                IOUtil.writeFile(fullName, "");

                LibHolder libHolder = makeEditableLibHolder(name, fullName,
                                          "");
                editableTab.add(name, libHolder.outerContents);
                editableTab.setToolTipTextAt(editableTab.getTabCount() - 1,
                                             fullName);
                editableTab.setSelectedIndex(editableTab.getTabCount() - 1);
                break;
            }
        } catch (Exception exc) {
            logException("An error occurred creating the jython library",
                         exc);
        }
    }

    /**
     * the libs
     *
     * @return the libs
     */
    public List getLibHolders() {
        return libHolders;
    }


    /**
     * remove lib
     *
     * @param holder lib
     */
    public void removeLibrary(LibHolder holder) {
        if ( !GuiUtils.askYesNo(
                "Remove Jython Library",
                "Are you sure you want to remove the library: "
                + IOUtil.getFileTail(holder.filePath))) {
            return;
        }
        try {
            libHolders.remove(holder);
            editableTab.remove(holder.outerContents);
            new File(holder.filePath).delete();
        } catch (Exception exc) {
            logException("An error occurred removing jython library", exc);
        }
    }

    /**
     * make men
     *
     * @param fileMenu menu
     */
    public void makeFileMenu(JMenu fileMenu) {
        LibHolder holder = findVisibleComponent();
        fileMenu.add(GuiUtils.makeMenuItem("New Jython Library...", this,
                                           "makeNewLibrary"));
        if ((holder != null) && holder.isEditable()
                && (holder.editProcess == null)) {
            fileMenu.add(GuiUtils.makeMenuItem("Remove  Library", this,
                    "removeLibrary", holder));
            fileMenu.add(GuiUtils.makeMenuItem("Save", this,
                    "writeJythonLib", holder));
            if (getStateManager().getPreferenceOrProperty(PROP_JYTHON_EDITOR,
                    "").trim().length() > 0) {
                fileMenu.add(editFileMenuItem =
                    GuiUtils.makeMenuItem("Edit in External Editor", this,
                                          "editInExternalEditor"));
            }
        }

        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                           "exportToPlugin"));
        fileMenu.add(GuiUtils.makeMenuItem("Export Selected to Plugin", this,
                                           "exportSelectedToPlugin"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "close"));

    }


    /**
     * Gets called when the IDV is quitting. Kills the editor process if there is one
     */
    protected void applicationClosing() {
        for (int i = libHolders.size() - 1; i >= 0; i--) {
            LibHolder holder = (LibHolder) libHolders.get(i);
            if (holder.editProcess != null) {
                try {
                    holder.editProcess.destroy();
                } catch (Exception exc) {}
            }
        }
    }

    /**
     * Edit the jython in the external editor
     */
    public void editInExternalEditor() {
        Misc.run(this, "editInExternalEditorInner", findVisibleComponent());
    }



    /**
     * Edit the jython in the external editor
     *
     * @param holder lib
     */
    public void editInExternalEditorInner(final LibHolder holder) {
        try {
            if ((holder == null) || !holder.isEditable()
                    || (holder.editProcess != null)) {
                return;
            }
            if ( !writeJythonLib(holder)) {
                return;
            }
            holder.wrapper.removeAll();
            holder.wrapper.repaint();
            editFileMenuItem.setEnabled(false);
            holder.comp.setEnabled(false);

            String filename = holder.filePath;
            File   file     = new File(filename);
            long   fileTime = file.lastModified();
            String command =
                getStateManager().getPreferenceOrProperty(PROP_JYTHON_EDITOR,
                    "").trim();
            if (command.length() == 0) {
                return;
            }

            List toks = StringUtil.split(command, " ", true, true);
            if (command.indexOf("%filename%") < 0) {
                toks.add("%filename%");
            }
            for (int i = 0; i < toks.size(); i++) {
                String tok = (String) toks.get(i);
                toks.set(i, StringUtil.replace(tok, "%filename%", filename));
            }
            //            System.err.println("toks:" + toks);
            try {
                holder.editProcess =
                    Runtime.getRuntime().exec(Misc.listToStringArray(toks));
            } catch (Exception exc) {
                holder.editProcess = null;
                logException("An error occurred editing jython library", exc);
            }
            if (holder.editProcess != null) {
                Misc.run(new Runnable() {
                    public void run() {
                        try {
                            holder.editProcess.waitFor();
                        } catch (Exception exc) {}
                        holder.editProcess = null;
                    }
                });
            }


            //This seems to hang?
            while (holder.editProcess != null) {
                Misc.sleep(1000);
                if (file.lastModified() != fileTime) {
                    fileTime = file.lastModified();
                    try {
                        String text = IOUtil.readContents(file);
                        holder.setText(text, false);
                        evaluateLibJython(false, holder);
                    } catch (Exception exc) {
                        logException(
                            "An error occurred editing jython library", exc);
                    }
                }
            }
            holder.comp.setEnabled(true);
            holder.wrapper.add(BorderLayout.CENTER, holder.comp);
            holder.wrapper.repaint();
            editFileMenuItem.setEnabled(true);
        } catch (Exception exc) {
            logException("An error occurred editing jython library", exc);
        }
    }



    /**
     * Get the window titlexxx
     *
     * @return window title
     */
    public String getWindowTitle() {
        return "Jython libraries";
    }



    /**
     * SHow help
     */
    public void showHelp() {
        getIdvUIManager().showHelp("idv.tools.jython");
    }

    /**
     * Factory method to create and interpreter. This
     * also adds the interpreter into the list of interpreters.
     *
     * @return The new interpreter
     */
    public PythonInterpreter createInterpreter() {
        PythonInterpreter interp = new PythonInterpreter();
        addInterpreter(interp);
        return interp;
    }


    /**
     * Create  a jython shell
     *
     * @return shell
     */
    public JythonShell createShell() {
        return new JythonShell(getIdv());
    }


    /**
     * Add the interpreter into the list of interpreters. Also
     * calls {@see #initInterpreter(PythonInterpreter)}
     *
     * @param interp The interpter to add and intialize
     */
    private void addInterpreter(PythonInterpreter interp) {
        interpreters.add(interp);
        initInterpreter(interp);
    }

    /**
     * Remove the interpreter from the list of interpreters.
     *
     * @param interp The interpreter to remove
     */
    public void removeInterpreter(PythonInterpreter interp) {
        interpreters.remove(interp);
    }

    /**
     * Have the given interpreter evaluate the
     * contents of each valid  Jython library defined in the given resources
     *
     * @param interp The interpreter to use
     * @param resources The set of jython library resources
     */
    private void applyJythonResources(PythonInterpreter interp,
                                      ResourceCollection resources) {
        if ((resources == null) || (interp == null)) {
            return;
        }
        for (int i = resources.size() - 1; i >= 0; i--) {
            String      resourceName = resources.get(i).toString();
            InputStream is           = null;
            try {
                is = IOUtil.getInputStream(resourceName, getClass());
            } catch (Exception exc) {}
            if (is == null) {
                continue;
            }
            try {
                interp.execfile(is, resourceName);
            } catch (PySyntaxError pse) {
                //Check for  html coming back instead of jython
                if (resources.isHttp(i)) {
                    String result = resources.read(i, false);
                    if ((result == null) || Misc.isHtml(result)) {
                        continue;
                    }
                }
                LogUtil.userErrorMessage(
                    "Syntax error in the Python library:" + resourceName
                    + "\n" + pse);
                inError = true;
            } catch (Exception exc) {
                logException("An error occurred reading jython library: "
                             + resourceName, exc);
                inError = true;
            }
        }
    }


    /**
     * Any errors
     *
     * @return in error
     */
    public boolean getInError() {
        return inError;
    }


    /**
     * Setup some basic state in the given interpreter.
     * Set the idv and datamanager variables.
     *
     * @param interpreter The interpreter to initialize
     */
    private void initBasicInterpreter(PythonInterpreter interpreter) {
        doBasicImports(interpreter);
        interpreter.set("idv", getIdv());
        interpreter.set("interpreter", interpreter);
        interpreter.set("datamanager", getDataManager());
    }

    /**
     * _more_
     *
     * @param interpreter _more_
     */
    private static void doBasicImports(PythonInterpreter interpreter) {
        interpreter.exec("import sys");
        interpreter.exec("import java");
        interpreter.exec("sys.add_package('visad')");
        interpreter.exec("sys.add_package('visad.python')");
        interpreter.exec("sys.add_package('visad.data.units')");
        interpreter.exec("from visad.python.JPythonMethods import *");
        interpreter.exec(
            "import ucar.unidata.data.grid.GridUtil as GridUtil");
        interpreter.exec("import ucar.unidata.data.DataUtil as DataUtil");
        interpreter.exec("import ucar.visad.Util as Util");
        interpreter.exec("import ucar.unidata.util.StringUtil as StringUtil");
        interpreter.exec(
            "import ucar.unidata.data.grid.DerivedGridFactory as DerivedGridFactory");
        interpreter.exec("from visad.FlatField import *");
        interpreter.exec("from visad.FieldImpl import *");
    }


    /**
     *  Initialize the given interpreter. Add in variables for "idv" and "datamanager"
     *  If initVisadLibs is true then load in the visad libs, etc.
     *  We have that check here so some interpreters that don't need the visad libs
     *  (e.g., the main ui interpreter for the idv)  don't have to suffer the
     *  overhead of loading those pkgs.
     *
     * @param interpreter The interpreter to initialize
     */
    private void initInterpreter(PythonInterpreter interpreter) {
        initBasicInterpreter(interpreter);
        if (DerivedDataDescriptor.classes != null) {
            for (int i = 0; i < DerivedDataDescriptor.classes.size(); i++) {
                String c = (String) DerivedDataDescriptor.classes.get(i);
                //                seenPaths.put (c, c);
                int    i1        = c.lastIndexOf(".");
                String pkg       = c.substring(0, i1);
                String className = c.substring(i1 + 1);
                interpreter.exec("sys.add_package('" + pkg + "')");
                interpreter.exec("from " + pkg + " import " + className);
            }
        }
        //        applyJythonResources(
        //            interpreter,
        //            getResourceManager().getResources(IdvResourceManager.RSC_JYTHON));

        for (int i = libHolders.size() - 1; i >= 0; i--) {
            LibHolder holder = (LibHolder) libHolders.get(i);
            //            if(!holder.isEditable()) continue;
            String jython = holder.getText();
            interpreter.exec(jython);
        }
    }



    /**
     * Get the end user edited text from the jython editor.
     *
     * @return The end user jython code
     */
    public String getUsersJythonText() {
        getContents();
        return mainHolder.getText();
    }


    /**
     * Append the given jython to the temp jython
     *
     * @param jython The jython from the bundle
     */
    public void appendTmpJython(String jython) {
        String oldJython = tmpHolder.getText();
        //Don't add it if we have it.
        if (oldJython.indexOf(jython) >= 0) {
            return;
        }
        String newJython = oldJython + "\n\n## Imported jython from bundle\n"
                           + jython;
        tmpHolder.setText(newJython);
        evaluateLibJython(false, tmpHolder);
    }



    /**
     * Append the given jython to that is from a bundle to the users jython
     *
     * @param jython The jython from the bundle
     */
    public void appendJythonFromBundle(String jython) {
        String oldJython = getUsersJythonText();
        //Don't add it if we have it.
        if (oldJython.indexOf(jython) >= 0) {
            return;
        }
        String newJython = oldJython + "\n\n## Imported jython from bundle\n"
                           + jython;
        mainHolder.setText(newJython);
        writeJythonLib(mainHolder);
    }

    /**
     * APpend jython to main editable lib
     *
     * @param jython jython
     */
    public void appendJython(String jython) {
        String oldJython = getUsersJythonText();
        String newJython = oldJython + "\n\n\n" + jython;
        mainHolder.setText(newJython);
        show();
        GuiUtils.showComponentInTabs(mainHolder.outerContents);
    }


    /**
     *  Have all of the interpreters evaluate the libraries
     *
     * @param forWriting Is this evaluation intended to be for when
     * we write the users jython
     * @param holderToWrite lib
     *
     * @return Was this successful
     */
    private boolean evaluateLibJython(boolean forWriting,
                                      LibHolder holderToWrite) {
        boolean ok   = false;
        String  what = "";
        try {
            if (interpreters.size() == 0) {
                getDerivedDataInterpreter();
            }
            List holders = Misc.newList(holderToWrite);
            for (int i = 0; i < holders.size(); i++) {
                LibHolder holder = (LibHolder) holders.get(i);
                if ( !holder.isEditable()) {
                    continue;
                }
                String jython = holder.getText();
                for (int interpIdx = 0; interpIdx < interpreters.size();
                        interpIdx++) {
                    ((PythonInterpreter) interpreters.get(interpIdx)).exec(
                        jython);
                }
            }
            ok = true;
        } catch (PySyntaxError pse) {
            try {
                if (forWriting) {
                    if (GuiUtils.showYesNoDialog(null,
                            "There was an error in the Python library:"
                            + pse, "Python error", "Save anyways",
                                   "Cancel")) {
                        return true;
                    }
                } else {
                    logException("There was an error in the Python library:"
                                 + pse, pse);
                }
            } catch (Throwable exc) {
                logException("Writing jython library "
                             + exc.getClass().getName(), exc);
            }
        } catch (Throwable exc) {
            logException("Writing jython library "
                         + exc.getClass().getName(), exc);
        }
        return ok;
    }


    /**
     * Save the end user jython code from the jython editor into
     * the user's .metapps area.
     *
     *
     * @param holder lib
     * @return success
     */
    public boolean writeJythonLib(LibHolder holder) {
        if (evaluateLibJython(true, holder)) {
            try {
                IOUtil.writeFile(holder.filePath, holder.getText());
                if (holder.saveBtn != null) {
                    holder.saveBtn.setEnabled(false);
                }
                return true;
            } catch (Throwable exc) {
                logException("Writing jython library "
                             + exc.getClass().getName(), exc);
            }
        }
        return false;
    }



    /**
     *  Make sure the given jython code matches the pattern (after removing whitespace):
     *  idv.procedure_name ('arg1', arg2, ..., argn)
     *  where if an arg is not in single quotes it cannot contain
     *  a procedure call.
     * <p>
     * We have this here so (hopefully) a user won't inadvertently execute
     * rogue jython code  on their machine.
     *
     * @param jython The code
     * @return Does the code  just call into idv or datamanager methods.
     */
    protected static boolean checkUntrustedJython(String jython) {
        jython = StringUtil.removeWhitespace(jython);
        String argPattern  = "([^()',]+|'[^']*')";
        String argsPattern = "((" + argPattern + ",)*" + argPattern + "?)";
        String pattern = "^((idv|datamanager).[^\\s(]+\\(" + argsPattern
                         + "\\);?)+$";
        if ( !StringUtil.stringMatch(jython, pattern)) {
            pattern = "^(idv.get[a-zA-Z]+\\(\\).[^\\s(]+\\(" + argsPattern
                      + "\\);?)+$";
            return StringUtil.stringMatch(jython, pattern);
        }
        return true;
    }



    /**
     *  Evaluate the given jython code. This code is untrusted  and has to be
     *  of the form (idv|datamanager).some_method (param1, param2, ..., paramN);
     *
     * @param jythonCode The code to execute
     */
    public void evaluateUntrusted(String jythonCode) {
        evaluateUntrusted(jythonCode, null);
    }

    /**
     *  Evaluate the given jython code. This code is untrusted  and has to be
     *  of the form (idv|datamanager).some_method (param1, param2, ..., paramN);
     *
     * @param jythonCode The code to execute
     * @param properties If non-null then populate the interpreter with the name/value pairs
     */
    public void evaluateUntrusted(String jythonCode, Hashtable properties) {
        if ( !checkUntrustedJython(jythonCode)) {
            LogUtil.userMessage("Malformed jython code:\n" + jythonCode);
            return;
        }
        evaluateTrusted(jythonCode, properties);
    }


    /**
     *  Interpret the given jython code. This code is trusted, i.e.,
     * it is not checked to make sure it is only calling idv or datamanager
     * methods.
     *
     * @param code The code toe evaluate
     */
    public void evaluateTrusted(String code) {
        evaluateTrusted(code, null);
    }



    /**
     *  Interpret the given jython code. This code is trusted, i.e.,
     * it is not checked to make sure it is only calling idv or datamanager
     * methods.
     *
     * @param code The code toe evaluate
     * @param properties If non-null then populate the interpreter with the name/value pairs
     */
    public void evaluateTrusted(String code, Hashtable properties) {
        PythonInterpreter interp = getUiInterpreter();
        if (properties != null) {
            for (Enumeration keys =
                    properties.keys(); keys.hasMoreElements(); ) {
                String param = (String) keys.nextElement();
                Object value = properties.get(param);
                interp.set(param, value);
            }
        }
        interp.exec(code);
    }



    /**
     *  Create (if needed) and initialize a Jython interpreter. The initialization is to map
     *  the variable "idv" to this instance of the idv and map "datamanager" to the DataManager
     *
     * @return The interpreter to be used for theUI
     */
    private PythonInterpreter getUiInterpreter() {
        if (uiInterpreter == null) {
            uiInterpreter = new PythonInterpreter();
            initBasicInterpreter(uiInterpreter);

        }
        return uiInterpreter;
    }



    /**
     * Update derived needs when the DataGroups change
     */
    public void dataGroupsChanged() {
        for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(dddIdx);
            ddd.updateDataGroups();
        }
    }




    /**
     * Initialize the {@link ucar.unidata.data.DerivedDataDescriptor}s
     * that are defined in the RSC_DERIVED  resource collection
     * from the given resource manager.
     *
     * @param newIrm The resource manager to get the derived resources from
     */
    protected void initUserFormulas(IdvResourceManager newIrm) {
        if (descriptorDataSource != null) {
            return;
        }
        try {
            descriptors = DerivedDataDescriptor.init(getIdv(),
                    newIrm.getXmlResources(newIrm.RSC_DERIVED));

            if (descriptors.size() == 0) {
                return;
            }

            descriptorDataSource = new DescriptorDataSource("Formulas",
                    "Formulas");
            for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
                DerivedDataDescriptor ddd =
                    (DerivedDataDescriptor) descriptors.get(dddIdx);
                // add all to the global list
                descriptorDataSource.addDescriptor(ddd);
            }
        } catch (Throwable exc) {
            logException("Initializing user formulas", exc);
        }



    }

    /**
     * Popup dialog to select formulas
     *
     * @return List of selected formulas
     */
    protected List selectFormulas() {
        Vector formulas = new Vector();
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            DataCategory cat   = ddd.getDisplayCategory();
            String       label = "";
            if (cat != null) {
                label = cat.toString(">") + ">";
            }
            label += ddd.getDescription();
            formulas.add(new TwoFacedObject(label, ddd));
        }
        JList       formulaList = new JList(formulas);
        JScrollPane scroller = GuiUtils.makeScrollPane(formulaList, 200, 300);
        JPanel contents =
            GuiUtils.topCenter(
                GuiUtils.inset(
                    GuiUtils.cLabel(
                        "Please select the formulas you would like to export"), 4), scroller);


        if ( !GuiUtils.showOkCancelDialog(null, "Export Formulas",
                                          GuiUtils.inset(contents, 5),
                                          null)) {
            return null;
        }

        Object[] items = formulaList.getSelectedValues();
        if ((items == null) || (items.length == 0)) {
            return null;
        }

        List selected = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            TwoFacedObject tfo = (TwoFacedObject) items[i];
            selected.add(tfo.getId());
        }
        return selected;
    }



    /**
     * Export selected formulas to plugin
     */
    public void exportFormulasToPlugin() {
        List selected = selectFormulas();
        if ((selected == null) || (selected.size() == 0)) {
            return;
        }
        getIdv().getPluginManager().addObject(selected);
    }

    /**
     * Export user formulas
     */
    public void exportFormulas() {
        List selected = selectFormulas();
        if ((selected == null) || (selected.size() == 0)) {
            return;
        }


        String xml      = DerivedDataDescriptor.toXml(selected);
        String filename = FileManager.getWriteFile(FILTER_XML, SUFFIX_XML);
        if (filename == null) {
            return;
        }
        try {
            IOUtil.writeFile(filename, xml);
        } catch (Exception exc) {
            logException("Writing file: " + filename, exc);
        }



    }


    /**
     * Import user formulas
     */
    public void importFormulas() {
        String filename = FileManager.getReadFile(FILTER_XML);
        if (filename == null) {
            return;
        }

        try {
            Element root = XmlUtil.getRoot(filename, getClass());
            List descriptors =
                DerivedDataDescriptor.readDescriptors(getIdv(), root, true);
            for (int i = 0; i < descriptors.size(); i++) {
                DerivedDataDescriptor ddd =
                    (DerivedDataDescriptor) descriptors.get(i);
                if ( !descriptors.contains(ddd)) {
                    ddd.setIsLocalUsers(true);
                    descriptorDataSource.addDescriptor(ddd);
                }
            }
            writeUserFormulas();
            getIdvUIManager().dataSourceChanged(descriptorDataSource);
        } catch (Exception exc) {
            logException("Importing  formulas", exc);
        }
    }


    /**
     * Save the user created  formulas.
     */
    protected void writeUserFormulas() {
        List descriptors = getLocalDescriptors();
        try {
            String filename =
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_DERIVED).getWritable();
            String           xml = DerivedDataDescriptor.toXml(descriptors);
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(xml.getBytes());
            fos.close();
        } catch (Throwable exc) {
            logException("Writing user formulas", exc);
            return;
        }
    }


    /**
     * Create the list of menu items for editing a data choice
     * that represents an end user formula.
     *
     * @param dataChoice The end user formula data choice
     * @param items List of menu items to add to
     */
    public void doMakeDataChoiceMenuItems(DataChoice dataChoice, List items) {
        JMenuItem mi;
        final DerivedDataDescriptor ddd =
            ((DerivedDataChoice) dataChoice).getDataDescriptor();
        items.add(mi = new JMenuItem("Edit Formula"));
        mi.addActionListener(new ObjectListener(dataChoice) {
            public void actionPerformed(ActionEvent ev) {
                showFormulaDialog(ddd.getIsLocalUsers()
                                  ? ddd
                                  : new DerivedDataDescriptor(ddd));
            }
        });


        items.add(mi = new JMenuItem("Copy Formula"));
        mi.addActionListener(new ObjectListener(dataChoice) {
            public void actionPerformed(ActionEvent ev) {
                showFormulaDialog(new DerivedDataDescriptor(ddd));
            }
        });


        if (ddd.getIsLocalUsers()) {
            items.add(mi = new JMenuItem("Remove Formula"));
            mi.addActionListener(new ObjectListener(dataChoice) {
                public void actionPerformed(ActionEvent ev) {
                    removeFormula((DerivedDataChoice) theObject);
                }
            });
        }

        mi = new JMenuItem("Evaluate Formula");
        mi.addActionListener(new ObjectListener(dataChoice) {
            public void actionPerformed(ActionEvent ev) {
                Misc.run(new Runnable() {
                    public void run() {
                        evaluateDataChoice((DataChoice) theObject);
                    }
                });

            }
        });
        items.add(mi);


        mi = new JMenuItem("Evaluate and Save");
        mi.addActionListener(new ObjectListener(dataChoice) {
            public void actionPerformed(ActionEvent ev) {
                Misc.run(new Runnable() {
                    public void run() {
                        getIdv().evaluateAndSave((DataChoice) theObject);
                    }
                });

            }
        });
        items.add(mi);

        items.add(GuiUtils.makeMenuItem("Export to Plugin",
                                        getIdv().getPluginManager(),
                                        "addObject", ddd));
        //items.add(GuiUtils.MENU_SEPARATOR);
    }


    /**
     * Delete the data choice if it is a user formula
     *
     * @param dataChoice The data choice to delete
     */
    public void deleteKeyPressed(DataChoice dataChoice) {
        if ((dataChoice == null)
                || !(dataChoice instanceof DerivedDataChoice)) {
            return;
        }
        DerivedDataDescriptor ddd =
            ((DerivedDataChoice) dataChoice).getDataDescriptor();


        if (ddd.getIsLocalUsers()) {
            removeFormula((DerivedDataChoice) dataChoice);
        }
    }


    /**
     * This simply clones the given data choice and calls getData
     * on it. We have this here so the user can explicitly, through the
     * GUI, evaluate a formula data choice. This way they
     *  don't have to  create a display to simply evaluate a formula.
     *
     * @param dataChoice The data chocie to evaluate
     */
    public void evaluateDataChoice(DataChoice dataChoice) {
        DataChoice clonedDataChoice = dataChoice.createClone();
        showWaitCursor();
        try {
            clonedDataChoice.getData(new DataSelection());
        } catch (DataCancelException exc) {}
        catch (Exception exc) {
            logException("Evaluating data choice", exc);
        }
        showNormalCursor();
    }




    /**
     * Remove a formula from the IDV.  You can only remove end user
     * formulas that are in the editable list.
     *
     * @param dataChoice  formula data choice
     */
    public void removeFormula(DerivedDataChoice dataChoice) {
        // we can only remove end user formulas
        removeFormula(dataChoice.getDataDescriptor());
    }

    /**
     * remove formula
     *
     * @param ddd ddd
     */
    public void removeFormula(DerivedDataDescriptor ddd) {
        if (ddd.getIsLocalUsers()) {
            descriptors.remove(ddd);
            descriptorDataSource.removeDescriptor(ddd);
            writeUserFormulas();
            getIdvUIManager().dataSourceChanged(descriptorDataSource);
        } else {
            LogUtil.userMessage("Can't remove a system formula");
        }
    }

    /**
     * Called when a formula data choice has changed (i.e.,
     * added, removed or edited.
     *
     * @param ddd descriptor for the formula.
     */
    public void descriptorChanged(DerivedDataDescriptor ddd) {
        ddd.setIsLocalUsers(true);
        if ( !descriptors.contains(ddd)) {
            descriptors.add(ddd);
            descriptorDataSource.addDescriptor(ddd);
        }
        writeUserFormulas();
        getIdvUIManager().dataSourceChanged(descriptorDataSource);
    }


    /**
     * Add a formula to the IDV.
     *
     * @param ddd  formula descriptor
     */
    public void addFormula(DerivedDataDescriptor ddd) {
        descriptorChanged(ddd);
    }


    /**
     *  Return the list of menu items to use when the user has clicked on a formula DataSource.
     *
     * @param dataSource The data source clicked on
     * @return List of menu items
     */
    public List doMakeFormulaDataSourceMenuItems(DataSource dataSource) {
        List menuItems = new ArrayList();
        menuItems.add(GuiUtils.makeMenuItem("Create Formula", this,
                                            "showFormulaDialog"));
        menuItems.add(GuiUtils.makeMenuItem("Edit Jython Library", this,
                                            "showJythonEditor"));
        menuItems.add(GuiUtils.makeMenuItem("Import Formulas", this,
                                            "importFormulas"));
        menuItems.add(GuiUtils.makeMenuItem("Export Formulas", this,
                                            "exportFormulas"));
        menuItems.add(GuiUtils.makeMenuItem("Export Formulas to Plugin",
                                            this, "exportFormulasToPlugin"));

        if (dataSource instanceof DescriptorDataSource) {
            menuItems.add(
                GuiUtils.makeMenu(
                    "Edit Formulas",
                    doMakeEditMenuItems((DescriptorDataSource) dataSource)));
        }
        return menuItems;
    }




    /**
     * make the edit menu items for the formula data source
     *
     * @return List of menu items
     */
    public List doMakeEditMenuItems() {
        return doMakeEditMenuItems(descriptorDataSource);
    }


    /**
     * make the edit menu items for the given formula data source
     *
     * @param dds The formula data source
     * @return List of menu items
     */
    public List doMakeEditMenuItems(DescriptorDataSource dds) {
        List      editMenuItems = new ArrayList();

        List      descriptors   = dds.getDescriptors();
        Hashtable catMenus      = new Hashtable();
        List      topItems      = new ArrayList();
        JMenu     derivedMenu   = null;
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            DataCategory dc       = ddd.getDisplayCategory();
            JMenu        catMenu  = null;
            String       catSoFar = "";
            JMenuItem mi = GuiUtils.makeMenuItem(
                               GuiUtils.getLocalName(
                                   ddd.getDescription(),
                                   ddd.getIsLocalUsers()), this,
                                       "showFormulaDialog", ddd);
            if (dc == null) {
                if (ddd.getIsDefault() && !ddd.getIsEndUser()) {
                    if (derivedMenu == null) {
                        derivedMenu = new JMenu("Derived Quantities");
                    }
                    derivedMenu.add(mi);
                } else {
                    topItems.add(mi);
                }
                continue;
            }
            while (dc != null) {
                String name = dc.getName();
                catSoFar = catSoFar + "-" + name;
                JMenu tmpMenu = (JMenu) catMenus.get(catSoFar);
                if (tmpMenu == null) {
                    tmpMenu = new JMenu(name);
                    catMenus.put(catSoFar, tmpMenu);
                    if (catMenu == null) {
                        editMenuItems.add(tmpMenu);
                    } else {
                        catMenu.add(tmpMenu);
                    }
                }
                catMenu = tmpMenu;
                dc      = dc.getChild();
            }
            if (catMenu == null) {
                editMenuItems.add(mi);
            } else {
                catMenu.add(mi);
            }
        }
        if (derivedMenu != null) {
            editMenuItems.add(derivedMenu);
        }
        for (int i = 0; i < topItems.size(); i++) {
            editMenuItems.add((JMenuItem) topItems.get(i));
        }
        return editMenuItems;
        //        JMenu editMenu = new JMenu("Edit");
        //        return editMenu;
    }




    /**
     * Show the formula dialog with no initial state.
     * We do this to create a new formula.
     */
    public void showFormulaDialog() {
        showFormulaDialog(null);
    }



    /**
     * get formula descriptors
     *
     * @return descriptors
     */
    public List getDescriptors() {
        return descriptors;
    }


    /**
     * Get all end user formulas
     *
     * @return end user formulas
     */
    public List getEndUserDescriptors() {
        List formulas = new ArrayList();
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            if ( !ddd.getIsEndUser()) {
                continue;
            }
            formulas.add(ddd);
        }
        return formulas;
    }

    /**
     * Get all local descriptors
     *
     * @return local descriptors
     */
    public List getLocalDescriptors() {
        List formulas = new ArrayList();
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            if (ddd.getIsLocalUsers()) {
                formulas.add(ddd);
            }
        }
        return formulas;
    }



    /**
     * Get all end user formulas
     *
     * @return end user formulas
     */
    public List getDefaultDescriptors() {
        List formulas = new ArrayList();
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            if ( !ddd.getIsDefault()) {
                continue;
            }
            formulas.add(ddd);
        }
        return formulas;
    }


    /**
     * Show formula dialog with the given initial DDD.
     *
     * @param descriptor The descriptor for the formula.
     */
    public void showFormulaDialog(DerivedDataDescriptor descriptor) {
        List                 categories = new ArrayList();
        DescriptorDataSource dds        = getDescriptorDataSource();
        if (dds != null) {
            List descriptors = dds.getDescriptors();
            for (int i = 0; i < descriptors.size(); i++) {
                DerivedDataDescriptor ddd =
                    (DerivedDataDescriptor) descriptors.get(i);
                if ( !ddd.getIsEndUser()) {
                    continue;
                }
                DataCategory cat = ddd.getDisplayCategory();
                if (cat != null) {
                    String catStr = cat.toString();
                    if ( !categories.contains(catStr)) {
                        categories.add(catStr);
                    }
                }
            }
        }

        new FormulaDialog(getIdv(), descriptor, null, categories);
    }



    /**
     * Get the descriptor data source
     *
     * @return  The descriptor data source
     */

    public DescriptorDataSource getDescriptorDataSource() {
        return descriptorDataSource;
    }





    /** Used to synchronize when creating the derivedData interpreter */
    private static Object MUTEX = new Object();



    /**
     *  We keep track of past methods that have been used so we don't have
     *  to tell the interpreter to import more than once (though perhaps
     *  the interp tracks this itself?)
     */
    private static Hashtable seenMethods = new Hashtable();



    /**
     *  We keep track of past package paths that have been used so we don't have
     *  to tell the interpreter to import more than once (though perhaps
     *  the interp tracks this itself?)
     */
    private static Hashtable seenPaths = new Hashtable();


    /**
     *  Create a (singleton) jython interpreter and initialize it with the set
     *  of classes defined in the xml
     *
     * @return The singleton Jython interpreter for derived data execution
     */
    public PythonInterpreter getDerivedDataInterpreter() {
        return getDerivedDataInterpreter(null);
    }



    /**
     *  Create a (singleton) jython interpreter and initialize it with the set
     *  of classes defined in the xml and (if needed) with the
     *  class path represented by the methodName argument (if methodName
     *  is of the form: some.package.path.SomeClass.someMethod).
     *
     *  @param methodName Used to initialize the interpreter (if non -null)
     * @return The singleton Jython interpreter for derived data execution
     */
    public PythonInterpreter getDerivedDataInterpreter(String methodName) {
        synchronized (MUTEX) {
            if (derivedDataInterpreter == null) {
                derivedDataInterpreter = createInterpreter();
            }

            if ((methodName != null)
                    && (seenMethods.get(methodName) == null)) {
                seenMethods.put(methodName, methodName);
                int i1 = methodName.lastIndexOf(".");
                int i2 = methodName.indexOf(".");
                if ((i1 >= 0) && (i2 >= 0)) {
                    String fullPath = methodName.substring(0, i1);
                    if ((i1 != i2) && (seenPaths.get(fullPath) == null)) {
                        i1 = fullPath.lastIndexOf(".");
                        String pkg       = fullPath.substring(0, i1);
                        String className = fullPath.substring(i1 + 1);
                        derivedDataInterpreter.exec("sys.add_package('" + pkg
                                + "')");
                        derivedDataInterpreter.exec("from " + pkg
                                + " import " + className);
                    }
                }
            }
        }
        return derivedDataInterpreter;
    }




    /**
     * Make menu
     *
     *
     * @param object object to call
     * @param method method to call
     * @param prefix prefic
     * @return menus
     */
    public List makeProcedureMenu(final Object object, final String method,
                                  final String prefix) {
        List menuItems = new ArrayList();
        List holders   = getLibHolders();
        for (int i = 0; i < holders.size(); i++) {
            List subItems = new ArrayList();
            final JythonManager.LibHolder libHolder =
                (JythonManager.LibHolder) holders.get(i);
            final JMenu menu = new JMenu(libHolder.getName());
            menu.addMenuListener(new MenuListener() {
                public void menuCanceled(MenuEvent e) {}

                public void menuDeselected(MenuEvent e) {}

                public void menuSelected(MenuEvent e) {
                    List funcs = libHolder.getFunctions();
                    menu.removeAll();
                    int cnt = 0;
                    for (int itemIdx = 0; itemIdx < funcs.size(); itemIdx++) {
                        Object[]     pair = (Object[]) funcs.get(itemIdx);
                        PyFunction   func = (PyFunction) pair[1];
                        StringBuffer sb   = new StringBuffer();
                        sb.append(makeCallString(func, null));
                        String s = sb.toString();
                        if ((prefix != null) && !s.startsWith(prefix)) {
                            continue;
                        }
                        JMenuItem menuItem = GuiUtils.makeMenuItem(s, object,
                                                 method, s);
                        if ( !func.__doc__.toString().equals("None")) {
                            String doc = "<html><pre>"
                                         + func.__doc__.toString().trim()
                                         + "</html>";
                            menuItem.setToolTipText(doc);
                        }

                        menu.add(menuItem);
                        cnt++;
                    }
                    if (cnt == 0) {
                        menu.add(new JMenuItem("No procedures"));
                    }
                }
            });

            menuItems.add(menu);
        }
        return menuItems;
    }

    /**
     * utility
     *
     * @param func func
     * @param props props
     *
     * @return call string
     */
    private String makeCallString(PyFunction func, Hashtable props) {
        StringBuffer sb = new StringBuffer();
        sb.append(func.__name__ + "(");
        PyTableCode tc = (PyTableCode) func.func_code;
        for (int argIdx = 0; argIdx < tc.co_argcount; argIdx++) {
            if (argIdx > 0) {
                sb.append(", ");
            }
            String param = tc.co_varnames[argIdx];
            String attrs = ((props != null)
                            ? (String) props.get(param)
                            : "");
            if (attrs == null) {
                attrs = "";
            }
            sb.append(param + attrs);
        }
        sb.append(")");
        return sb.toString();

    }


    /**
     * find methods
     *
     *
     * @param justList If true just the functions
     * @return A list of Object arrays. First element in array is the name of the lib. Second is the list of PyFunction-s
     */
    public List findJythonMethods(boolean justList) {
        return findJythonMethods(justList, getLibHolders());
    }


    /**
     * find methods
     *
     *
     * @param justList If true just the function
     * @param holders libs
     *
     * @return A list of Object arrays. First element in array is the name of the lib. Second is the list of PyFunction-s
     */
    public List findJythonMethods(boolean justList, List holders) {
        List result = new ArrayList();
        if (holders == null) {
            return result;
        }
        for (int i = 0; i < holders.size(); i++) {
            List subItems = new ArrayList();
            JythonManager.LibHolder libHolder =
                (JythonManager.LibHolder) holders.get(i);
            List funcs = libHolder.getFunctions();
            for (int itemIdx = 0; itemIdx < funcs.size(); itemIdx++) {
                Object[]   pair = (Object[]) funcs.get(itemIdx);
                PyFunction func = (PyFunction) pair[1];
                subItems.add(func);
            }
            if (justList) {
                result.addAll(subItems);
            } else {
                result.add(new Object[] { libHolder.getName(), subItems });
            }
        }
        return result;
    }






    /**
     * main
     *
     * @param args args
     *
     * @throws Exception on badness
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            PythonInterpreter interpreter = new PythonInterpreter();
            doBasicImports(interpreter);
            String mod = IOUtil.stripExtension(IOUtil.getFileTail(args[i]));
            interpreter.execfile(new java.io.FileInputStream(args[i]), mod);
            PyStringMap  seq    = (PyStringMap) interpreter.getLocals();
            PyList       keys   = seq.keys();
            PyList       items  = seq.items();
            StringBuffer sb     = new StringBuffer();
            String       modDoc = "";
            List         funcs  = new ArrayList();
            for (int itemIdx = 0; itemIdx < items.__len__(); itemIdx++) {
                PyTuple pair = (PyTuple) items.__finditem__(itemIdx);
                if ( !(pair.__finditem__(1) instanceof PyFunction)) {
                    if (pair.__finditem__(0).toString().equals("__doc__")) {
                        modDoc = pair.__finditem__(1).toString();
                    }
                    continue;
                }
                funcs.add(new Object[] { pair.__finditem__(0).toString(),
                                         pair.__finditem__(1) });
            }


            funcs = Misc.sortTuples(funcs, true);

            for (int itemIdx = 0; itemIdx < funcs.size(); itemIdx++) {
                Object[]   pair = (Object[]) funcs.get(itemIdx);
                PyFunction func = (PyFunction) pair[1];
                sb.append("<p><a name=\"" + func.__name__ + "\"></a><code class=\"command\">"
                          + func.__name__ + "(");
                PyTableCode tc = (PyTableCode) func.func_code;
                for (int argIdx = 0; argIdx < tc.co_argcount; argIdx++) {
                    if (argIdx > 0) {
                        sb.append(", ");
                    }
                    sb.append(tc.co_varnames[argIdx]);
                }
                sb.append(
                    "):</code><p style=\"padding:0;margin-left:20;margin-top:0\">\n");
                if ( !func.__doc__.toString().equals("None")) {
                    String doc = func.__doc__.toString().trim();
                    //                    doc = StringUtil.replace(doc,"\n","<br>");
                    List toks = StringUtil.split(doc, "\n", true, true);
                    sb.append(StringUtil.join("\n", toks));
                }
                sb.append("</p>");
            }
            System.out.println("<h2> Module: " + mod + "</h2>");
            if ( !modDoc.equals("None")) {
                System.out.println(modDoc);
            }
            System.out.println("<hr>");
            System.out.println(sb.toString());
            //            interpreter.exec("dir(" + mod +")");
        }
    }


    /**
     * Class LibHolder holds all things for a single lib
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class LibHolder {

        /** functions in lib */
        List functions;

        /** label */
        String label;

        /** widget */
        JComponent comp;

        /** file */
        String filePath;

        /** widget */
        JComponent wrapper;

        /** widget */
        JComponent outerContents;

        /** widget */
        JButton saveBtn;

        /** for external editing */
        Process editProcess;

        /**
         * ctor
         *
         *
         * @param label lable
         * @param comp comp
         * @param filePath lib file
         * @param wrapper wrapper
         * @param outerContents contents
         */
        public LibHolder(String label, JComponent comp, String filePath,
                         JComponent wrapper, JComponent outerContents) {
            this.label         = label;
            this.comp          = comp;
            this.filePath      = filePath;
            this.wrapper       = wrapper;
            this.outerContents = outerContents;
        }


        /**
         * name
         *
         * @return name
         */
        public String getName() {
            return label;
        }


        /**
         * Parse the functions if needed
         *
         * @return functions
         */
        public List getFunctions() {
            if (functions == null) {
                functions = new ArrayList();
                PythonInterpreter interpreter = new PythonInterpreter();
                interpreter.exec("import sys");
                interpreter.exec("import java");
                try {
                    interpreter.exec(getText());
                } catch (Exception exc) {
                    return functions;
                }

                PyStringMap seq   = (PyStringMap) interpreter.getLocals();
                PyList      items = seq.items();
                for (int itemIdx = 0; itemIdx < items.__len__(); itemIdx++) {
                    PyTuple pair = (PyTuple) items.__finditem__(itemIdx);
                    if ( !(pair.__finditem__(1) instanceof PyFunction)) {
                        continue;
                    }
                    functions.add(new Object[] {
                        pair.__finditem__(0).toString(),
                        pair.__finditem__(1) });
                }

                functions = Misc.sortTuples(functions, true);
            }
            return functions;
        }


        /**
         * Is this lib editable
         *
         * @return is editable
         */
        public boolean isEditable() {
            if (filePath == null) {
                return false;
            }
            if (comp instanceof JPythonEditor) {
                return true;
            }
            if (true) {
                return false;
            }
            JTextArea textArea = (JTextArea) comp;
            return textArea.isEditable();
        }

        /**
         * Get text
         *
         * @return text
         */
        public String getText() {
            if (comp instanceof JPythonEditor) {
                return ((JPythonEditor) comp).getText();
            } else {
                return ((JTextArea) comp).getText();
            }
        }

        /**
         * Set text
         *
         * @param text text
         */
        public void setText(String text) {
            setText(text, true);
        }

        /**
         * Set text
         *
         * @param text text
         * @param andEnable enable
         */
        public void setText(String text, boolean andEnable) {
            if (comp instanceof JPythonEditor) {
                ((JPythonEditor) comp).setText(text);
            } else {
                ((JTextArea) comp).setText(text);
            }
            if (andEnable && (saveBtn != null)) {
                saveBtn.setEnabled(true);
                functions = null;
            }
        }

        /**
         * copy
         */
        public void copy() {
            if (comp instanceof JPythonEditor) {
                ((JPythonEditor) comp).copy();
            } else {
                ((JTextArea) comp).copy();
            }
        }

    }

}


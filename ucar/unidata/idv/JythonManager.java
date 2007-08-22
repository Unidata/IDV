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

    /** One text component per tab */
    private ArrayList textComponents;

    /** Shows the temp jython */
    private JTextArea tmpTextArea = new JTextArea();


    /**
     * This holds all of the  end-user formulas, the ones
     * from the system and the ones from the users local space.
     */
    private DescriptorDataSource descriptorDataSource;

    /** _more_ */
    List descriptors;


    /**
     * This is the interpreter used for processing the
     * UI commands. e.g., the ones in defaultmenu.xm
     */
    private PythonInterpreter uiInterpreter = null;


    /**
     *  Used to evaluate derived data choices
     */
    private PythonInterpreter derivedDataInterpreter;



    /** The jython editor */
    private JPythonEditor jythonEditor;

    /** Wrapper around the editor */
    private JComponent jythonEditorHolder;

    /**
     * List of all active interpreters. We keep these around so when
     * the user changes the jython library we can reevaluate the code
     * in each interpreter.
     */
    private List interpreters = new ArrayList();


    /** The edit menu item */
    private JMenuItem editFileMenuItem;

    /** The edit process */
    private Process editProcess;



    /**
     * Create the manager and call initPython.
     *
     * @param idv The IDV
     */
    public JythonManager(IntegratedDataViewer idv) {
        super(idv);
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

        //      PySystemState sys = Py.getSystemState ();
        //      sys.add_package ("visad");
        //      sys.add_package ("visad.python");

    }


    /**
     * Create, if needed, and show the jython editor.
     */
    public void showJythonEditor() {
        super.show();
    }


    /**
     * Export selcted text of current tab to plugin
     */
    public void exportSelectedToPlugin() {
        int        index = jythonTab.getSelectedIndex();
        JComponent comp  = (JComponent) textComponents.get(index);
        String     text  = "";
        if (comp instanceof JPythonEditor) {
            ((JPythonEditor) comp).copy();
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
                    if (((JPythonEditor) comp).getText().indexOf(text) < 0) {
                        text = null;
                    }
                } catch (Exception ex) {
                    LogUtil.logException("Accessing clipboard", ex);
                    return;
                }
            }
            //      System.out.println("Clipboard contains:" + text);
        } else {
            text = ((JTextArea) comp).getSelectedText();
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
        int        index = jythonTab.getSelectedIndex();
        JComponent comp  = (JComponent) textComponents.get(index);
        String     text  = "";
        if (comp instanceof JPythonEditor) {
            text = ((JPythonEditor) comp).getText();
        } else {
            text = ((JTextArea) comp).getText();
        }
        getIdv().getPluginManager().addText(text, "jython.py");
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
            jythonTab      = new JTabbedPane();
            textComponents = new ArrayList();
            int systemCnt = 1;
            for (int i = 0; i < resources.size(); i++) {
                String showInEditor = resources.getProperty("showineditor", i);
                if(showInEditor!=null && showInEditor.equals("false")) {
                    //                    System.err.println ("skipping:" + resources.get(i));
                    continue;
                }

                //Assume that the first one in the list is the writable resources
                String text = resources.read(i);
                if ((jythonEditor == null)
                        && resources.isWritableResource(i)) {
                    jythonEditor = new JPythonEditor();
                    textComponents.add(jythonEditor);
                    jythonEditor.setPreferredSize(new Dimension(400, 300));
                    JLabel label =
                        new JLabel(
                            "<html>"
                            + Msg.msg(
                                "Path: ${param1}",
                                PluginManager.decode(
                                    resources.get(
                                        i).toString())) + "</html>");
                    jythonEditorHolder = GuiUtils.center(jythonEditor);
                    jythonTab.add(
                        PluginManager.decode(resources.getShortName(i)),
                        GuiUtils.topCenter(
                            GuiUtils.inset(label, 4), jythonEditorHolder));
                    if (text == null) {
                        text = "#\n#This is the default  editable user's jython library\n";
                    }
                    if (text != null) {
                        jythonEditor.setText(text);
                    }
                } else {
                    //Only add in the non-editable pane if there is a real resource there
                    if (text == null) {
                        continue;
                    }
                    JTextArea textArea = new JTextArea(text, 20, 50);
                    textComponents.add(textArea);
                    textArea.setEditable(false);
                    JLabel label = new JLabel(
                                       "<html>"
                                       + Msg.msg(
                                           "Path: ${param1}",
                                           StringUtil.shorten(
                                               resources.get(i).toString(),
                                               80)) + " ("
                                                   + Msg.msg("non-editable")
                                                   + ")");
                    jythonTab.add(resources.getShortName(i),
                                  GuiUtils.topCenter(GuiUtils.inset(label,
                                      4), GuiUtils.makeScrollPane(textArea,
                                          400, 300)));
                }
            }
            JLabel label = new JLabel("Temporary Jython");
            jythonTab.add(
                "Temporary",
                GuiUtils.topCenter(
                    GuiUtils.inset(label, 4),
                    GuiUtils.makeScrollPane(tmpTextArea, 400, 300)));


            JMenuBar menuBar  = new JMenuBar();
            JMenu    fileMenu = GuiUtils.makeDynamicMenu("File", this,"makeFileMenu");
            JMenu    helpMenu = new JMenu("Help");
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            helpMenu.add(GuiUtils.makeMenuItem("Show Jython Help", this,
                    "showHelp"));

            JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Save",
                                    this, "writeJythonLib"));
            return contents = GuiUtils.topCenterBottom(menuBar, jythonTab,
                    bottom);
        } catch (Throwable exc) {
            logException("Creating jython editor", exc);
            return null;
        }

    }


    public void makeFileMenu(JMenu fileMenu) {
        fileMenu.add(GuiUtils.makeMenuItem("Save", this,
                                           "writeJythonLib"));

        if (getStateManager().getPreferenceOrProperty(PROP_JYTHON_EDITOR,"").trim().length()
            > 0) {
            fileMenu.add(editFileMenuItem =
                         GuiUtils.makeMenuItem("Edit in External Editor", this,
                                               "editInExternalEditor"));
        }
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                           "exportToPlugin"));
        fileMenu.add(GuiUtils.makeMenuItem("Export Selected to Plugin",
                                           this, "exportSelectedToPlugin"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "close"));

    }


    /**
     * Gets called when the IDV is quitting. Kills the editor process if there is one
     */
    protected void applicationClosing() {
        if (editProcess != null) {
            try {
                editProcess.destroy();
            } catch (Exception exc) {}
        }
    }

    /**
     * Edit the jython in the external editor
     */
    public void editInExternalEditor() {
        if (editProcess != null) {
            return;
        }
        Misc.run(this, "editInExternalEditorInner");
    }



    /**
     * Edit the jython in the external editor
     */
    public void editInExternalEditorInner() {
        try {
            if (jythonEditor == null) {
                return;
            }
            if ( !writeJythonLib()) {
                return;
            }

            jythonEditorHolder.removeAll();
            jythonEditorHolder.repaint();
            editFileMenuItem.setEnabled(false);
            jythonEditor.setEnabled(false);

            String filename =
                getResourceManager().getResources(
                    IdvResourceManager.RSC_JYTHON).getWritable();
            File file     = new File(filename);

            long fileTime = file.lastModified();
            String command = getStateManager().getPreferenceOrProperty(PROP_JYTHON_EDITOR,
                                 "").trim();
            if (command.length() == 0) {
                return;
            }

            List toks = StringUtil.split(command, " ",true,true);
            if (command.indexOf("%filename%")< 0) {
                toks.add("%filename%");
            } 
            for(int i=0;i<toks.size();i++) {
                String tok  = (String) toks.get(i);
                toks.set(i,StringUtil.replace(tok, "%filename%",filename));
            }
            //            System.err.println("toks:" + toks);
            try {
                editProcess = Runtime.getRuntime().exec(Misc.listToStringArray(toks));
            } catch (Exception exc) {
                editProcess = null;
                logException(
                             "An error occurred editing jython library", exc);
            }
            if(editProcess!=null) {
                Misc.run(new Runnable() {
                        public void run() {
                            try {
                                editProcess.waitFor();
                            } catch (Exception exc) {}
                            editProcess = null;
                        }
                    });
            }


            //This seems to hang?
            while (editProcess != null) {
                Misc.sleep(1000);
                if (file.lastModified() != fileTime) {
                    fileTime = file.lastModified();
                    try {
                        String text = IOUtil.readContents(file);
                        jythonEditor.setText(text);
                        evaluateLibJython(false);
                    } catch (Exception exc) {
                        logException(
                            "An error occurred editing jython library", exc);
                    }
                }
            }
            jythonEditor.setEnabled(true);
            jythonEditorHolder.add(BorderLayout.CENTER, jythonEditor);
            jythonEditorHolder.repaint();
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
     * Handle actions  (SAVE, HELP, OK and CANCEL)
     * from the jython editor
     *
     * @param e The <code>ActionEvent</code>
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_SAVE)) {
            writeJythonLib();
        } else if (cmd.equals(GuiUtils.CMD_HELP)) {
            getIdvUIManager().showHelp("idv.tools.jython");
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            writeJythonLib();
            super.close();
        } else if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            super.close();
        }
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
        interpreter.exec("import sys");
        interpreter.set("idv", getIdv());
        interpreter.set("datamanager", getDataManager());
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
        applyJythonResources(
            interpreter,
            getResourceManager().getResources(IdvResourceManager.RSC_JYTHON));
        interpreter.exec(getUsersJythonText());
        interpreter.exec(tmpTextArea.getText());
    }


    /**
     * Get the end user edited text from the jython editor.
     *
     * @return The end user jython code
     */
    public String getUsersJythonText() {
        getContents();
        return jythonEditor.getText();
    }


    /**
     * Append the given jython to the temp jython
     *
     * @param jython The jython from the bundle
     */
    public void appendTmpJython(String jython) {
        String oldJython = tmpTextArea.getText();
        //Don't add it if we have it.
        if (oldJython.indexOf(jython) >= 0) {
            return;
        }
        String newJython = oldJython + "\n\n## Imported jython from bundle\n"
                           + jython;
        tmpTextArea.setText(newJython);
        evaluateLibJython(false);
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
        jythonEditor.setText(newJython);
        writeJythonLib();
    }

    /**
     *  Has all of the interpreters evaluate the libraries
     *
     * @param forWriting Is this evaluation intended to be for when
     * we write the users jython
     *
     * @return Was this successful
     */
    private boolean evaluateLibJython(boolean forWriting) {
        boolean ok   = false;
        String  what = "";
        try {
            String jython    = getUsersJythonText();
            String tmpJython = tmpTextArea.getText();
            //Add one in if there aren't any
            if (interpreters.size() == 0) {
                getDerivedDataInterpreter();
            }

            for (int i = 0; i < interpreters.size(); i++) {
                ((PythonInterpreter) interpreters.get(i)).exec(jython);
                ((PythonInterpreter) interpreters.get(i)).exec(tmpJython);
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
     * @return success
     */
    public boolean writeJythonLib() {
        if (evaluateLibJython(true)) {
            try {
                ResourceCollection rc = getResourceManager().getResources(
                                            IdvResourceManager.RSC_JYTHON);
                String jython = getUsersJythonText();
                rc.writeWritableResource(jython);
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
        JList formulaList = new JList(formulas);
        formulaList.setPreferredSize(new Dimension(200, 300));
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
    protected void evaluateDataChoice(DataChoice dataChoice) {
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
     * _more_
     *
     * @param ddd _more_
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
     * _more_
     *
     * @return _more_
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






}


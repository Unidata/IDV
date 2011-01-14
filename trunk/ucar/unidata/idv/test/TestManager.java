/*
 * $Id: TestManager.java,v 1.31 2006/06/28 16:52:33 jeffmc Exp $
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

package ucar.unidata.idv.test;



import ucar.unidata.idv.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Trace;

import ucar.unidata.util.CacheManager;

import ucar.unidata.data.*;

import java.io.*;

import java.awt.*;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.idv.ui.*;



/**
 * Provides a set of test archive creation routines.
 *
 * @author IDV development team
 */


public class TestManager extends IdvManager {

    /**
     * Create me
     *
     * @param idv The IDV
     *
     */
    public TestManager(IntegratedDataViewer idv) {
        super(idv);
    }


    /**
     * Tell each {@link ucar.unidata.idv.DisplayControl}
     * in the given list to remove itself
     *
     * @param dcs List of display controls
     */
    void removeDisplayControls(List dcs) {
        for (int i = 0; i < dcs.size(); i++) {
            try {
                ((DisplayControl) dcs.get(i)).doRemove();
            } catch (Throwable exc) {
                LogUtil.logException("testAll", exc);
            }
        }
    }


    /**
     * Evaluate all data choices in all data sources
     *
     * @throws Exception On badness
     */
    private void evaluateAllDataChoices() throws Exception {
        List dataSources = getIdv().getDataSources();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            testAll(dataSource, false);
        }
    }


    /**
     * This creates all display controls for all of the
     * data choices held by the given data source
     *
     * @param dataSource The data source to test
     * @param createDisplays And create displays
     *
     * @throws Exception On badness
     */
    public void testAll(DataSource dataSource, boolean createDisplays)
            throws Exception {
        LogUtil.setTestMode(true);
        if (DataManager.isFormulaDataSource(dataSource)) {
            return;
        }
        System.err.println("Evaluating: " + dataSource.getClass().getName()
                           + " " + dataSource);
        List          choices       = dataSource.getDataChoices();
        List descriptors = new ArrayList(getIdv().getControlDescriptors());
        List          dcs           = new ArrayList();
        List          times         = dataSource.getAllDateTimes();
        DataSelection dataSelection = null;
        if ((times != null) && (times.size() > 0)) {
            dataSelection = new DataSelection(Misc.newList(times.get(0)));
        }
        for (int cIdx = 0; cIdx < choices.size(); cIdx++) {
            if (descriptors.size() == 0) {
                break;
            }
            DataChoice dataChoice = (DataChoice) choices.get(cIdx);
            if ( !createDisplays) {
                //Just get the data
                String lbl = "\tdata:" + dataChoice + " "
                             + ((dataChoice instanceof DerivedDataChoice)
                                ? "(derived) "
                                : "");
                try {
                    Object data = dataChoice.getData(dataSelection);
                    ucar.unidata.util.CacheManager.clearCache();
                    if (data == null) {
                        System.err.println(lbl + "  **** returned null");
                    } else {
                        System.err.println(lbl + "  **** ok "
                                           + data.getClass().getName());
                    }
                } catch (DataCancelException dce) {
                    //ignore
                } catch (Throwable exc) {
                    System.err.println(lbl + "  **** exception: " + exc);
                    //                   throw exc;
                }

                continue;
            }

            List l = ControlDescriptor.getApplicableControlDescriptors(
                         dataChoice.getCategories(), descriptors);
            for (int j = 0; j < l.size(); j++) {
                if (j == 0) {
                    System.err.println("\tData: " + dataChoice.getName()
                                       + " " + dataChoice.getDescription());
                }
                ControlDescriptor cd = (ControlDescriptor) l.get(j);
                descriptors.remove(cd);
                DisplayControl dc = getIdv().doMakeControl(dataChoice, cd,
                                                           NULL_STRING);
                if (dc == null) {
                    continue;
                }
                dcs.add(dc);
                if (dcs.size() > 4) {
                    removeDisplayControls(dcs);
                    dcs = new ArrayList();
                }
            }
        }
        removeDisplayControls(dcs);
    }

    /**
     *  Helper method that prints out the buffered LogUtil logging.
     *  This is used in debugging, especially with thread/timing related bugs
     *  (where doing a println can throw off the timing).
     */
    public void printLog() {
        System.out.println(Trace.buff.toString());
        Trace.buff = new StringBuffer();
    }




    /**
     * Prompt the user for an archive name, etc.
     * and write out the test archive
     */
    public void createTestArchive() {

        String lastArchiveName =
            (String) getStateManager().getPreference(PREF_ARCHIVENAME,
                "test");
        String lastArchiveDir =
            (String) getStateManager().getPreference(PREF_ARCHIVEDIR,
                (String) null);


        JFileChooser fileChooser = ((lastArchiveDir != null)
                                    ? new JFileChooser(lastArchiveDir)
                                    : new JFileChooser());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setControlButtonsAreShown(false);
        JTextField nameFld = new JTextField();
        if (lastArchiveName != null) {
            nameFld.setText(lastArchiveName);
        }

        final JTextArea descFld = new JTextArea(7, 40);

        if ((lastArchiveName != null) && (lastArchiveDir != null)) {

            String descFile =
                IOUtil.joinDir(
                    IOUtil.joinDir(new File(lastArchiveDir), lastArchiveName),
                    lastArchiveName + ".txt");
            System.err.println("checking last desc:" + descFile);

            String lastDescription = IOUtil.readContents(descFile,
                                         getIdv().getClass(), (String) null);
            if (lastDescription != null) {
                descFld.setText(lastDescription);
            }
        }
        JCheckBox islCbx = new JCheckBox("Generate ISL", false);
        JScrollPane descScroller = GuiUtils.makeScrollPane(descFld, 100, 40);
        JPanel contents = GuiUtils.doLayout(new Component[]{
                              GuiUtils.rLabel(" Archive name: "),
                              GuiUtils.inset(nameFld, 6),
                              GuiUtils.top(GuiUtils.rLabel(" Description: ")),
                              GuiUtils.inset(descScroller, 6),
                              islCbx,GuiUtils.filler() }, 2,
                                  GuiUtils.WT_NY, GuiUtils.WT_N);

        JPanel filePanel =
            GuiUtils.topCenter(
                new JLabel("Directory to write archive directory to:"),
                fileChooser);
        contents = GuiUtils.inset(
            GuiUtils.vbox(contents, new JLabel(" "), filePanel), 6);
        if ( !GuiUtils.showOkCancelDialog(null, "Test archive", contents,
                                          null, null)) {
            return;
        }
        final String archiveName     = nameFld.getText().trim();
        File         chosenDirectory = fileChooser.getSelectedFile();
        if (chosenDirectory == null) {
            chosenDirectory = fileChooser.getCurrentDirectory();
        }
        if (chosenDirectory == null) {
            return;
        }
        chosenDirectory = new File(IOUtil.joinDir(chosenDirectory,
                                                  archiveName));

        IOUtil.makeDir(chosenDirectory);
        File archiveFile = new File(IOUtil.joinDir(chosenDirectory,
                                                   archiveName + (islCbx.isSelected()?".isl":".jnlp")));
        if (archiveFile.exists()) {
            if ( !GuiUtils.showYesNoDialog(
                    null,
                    "Directory: " + archiveName
                    + " exists. Do you want to delete everything in that directory and  create a new  archive?", "Test archive exists")) {
                return;
            }
            File[] files = chosenDirectory.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].toString().endsWith(".jpg")) {
                    files[i].delete();
                }
            }
            IOUtil.makeDir(chosenDirectory);
        }

        final File directory = chosenDirectory;
        String     desc      = descFld.getText().trim();
        getStateManager().putPreference(PREF_ARCHIVENAME, archiveName);
        getStateManager().writePreference(PREF_ARCHIVEDIR,
                                          directory.getParent().toString());

        final boolean doIsl  = islCbx.isSelected();
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                boolean ok = true;
                try {
                    ok = writeTestArchive(archiveName, directory, true,doIsl);
                    String descText = descFld.getText().trim();
                    if (ok && (descText.length() > 0)) {
                        IOUtil.writeFile(IOUtil.joinDir(directory, archiveName
                                                        + ".txt"), descText);
                    }
                } catch (Exception exc) {
                    ok = false;
                    logException("Writing archive", exc);
                }
                showNormalCursor();
                if (ok) {
                    LogUtil.userMessage("Test archive " + archiveName
                                        + " written to: " + directory);
                }
            }
        });
    }



    /**
     * Write the test archive.
     * Write out the set of screen captures from all of the displays
     * and view managers active in the IDV
     *
     * @param archiveName Name of the archive
     * @param directory The directory to put things in
     * @param writeBundle Should the IDV bundle also be writted
     * @return  Was this successful
     */
    protected boolean writeTestArchive(String archiveName, File directory,
                                       boolean writeBundle, boolean doIsl) {
        try {
            File imagePath = null;
            if (directory != null) {
                imagePath = new File(IOUtil.joinDir(directory, archiveName));
                archiveName = IOUtil.joinDir(directory, archiveName);
            }

            if (writeBundle) {
                String bundleName = archiveName + (doIsl?".isl":SUFFIX_JNLP);
                if ( !getPersistenceManager().doSave(bundleName)) {
                    return false;
                }
            }
            if(doIsl) return true;
            /**
             * List mainWindows = IdvWindow.getMainWindows();
             * for (int i = 0; i < mainWindows.size(); i++) {
             *   String      path   = archiveName + "mainwindow_" + (i + 1)+".jpg";
             *   IdvWindow window = (IdvWindow) mainWindows.get(i);
             *   window.setLocation(50, 50);
             *   window.show();
             *   window.toFront();
             *   Misc.sleep(100);
             *   GuiUtils.writeImage(window, path);
             * }
             */

            List displayControls = getIdv().getDisplayControls();
            List viewManagers = getVMManager().getViewManagers();
            for (int i = 0; i < viewManagers.size(); i++) {
                ViewManager viewManager = (ViewManager) viewManagers.get(i);
                if(viewManager.getDisplayWindow()!=null) {
                    viewManager.getDisplayWindow().setVisible(false);
                }
            }
            List goodDisplayControls = new ArrayList();
            for (int i = 0; i < displayControls.size(); i++) {
                DisplayControl dc = (DisplayControl) displayControls.get(i);
                JFrame frame = dc.getWindow();
                if ((frame == null) || !frame.isShowing()) {
                    continue;
                }
                goodDisplayControls.add(dc);
                frame.setVisible(false);
            }

            for (int i = 0; i < viewManagers.size(); i++) {
                String      vmArchive   = archiveName + "_view_" + (i + 1);
                ViewManager viewManager = (ViewManager) viewManagers.get(i);
                if(viewManager.getDisplayWindow()!=null) {
                    viewManager.getDisplayWindow().setLocation(50, 50);
                    viewManager.getDisplayWindow().setVisible(true);
                    Misc.sleep(100);
                    viewManager.writeTestArchive(vmArchive);
                    viewManager.getDisplayWindow().setVisible(false);
                }
            }

            int  displayCnt      = 0;
            for (int i = 0; i < goodDisplayControls.size(); i++) {
                DisplayControl dc = (DisplayControl) goodDisplayControls.get(i);
                JFrame frame = dc.getWindow();
                if(frame==null) continue;
                frame.setLocation(50, 50);
                frame.setVisible(true);
                Misc.sleep(100);
                displayCnt++;
                String displayArchiveName = archiveName + "_control_"
                                            + (displayCnt);
                dc.writeTestArchive(displayArchiveName);
                frame.setVisible(false);
            }

            if(!LogUtil.getTestMode()) {
                for (int i = 0; i < viewManagers.size(); i++) {
                    ViewManager viewManager = (ViewManager) viewManagers.get(i);
                    if(viewManager.getDisplayWindow()!=null) {
                        viewManager.getDisplayWindow().setVisible(true);
                    }
                }
            }


            /**
             *  TODO access these editros from the idv
             * if (getIdv().isColorTableEditorShowing()) {
             *   GuiUtils.writeImage(colorTableEditor.getFrame(),
             *                       archiveName + "_colortablegui.jpg");
             * }
             *
             * if (getIdv().isParamDefaultsEditorShowing()) {
             *   GuiUtils.writeImage(getIdv().getParamDefaultsEditor().getFrame(),
             *                       archiveName + "_paramdefaultsgui.jpg");
             * }
             * if (getIdv().isStationModelEditorShowing()) {
             *   GuiUtils.writeImage(
             *   getIdv().getStationModelManager().getFrame(),
             *       archiveName + "_stationmodelgui.jpg");
             * }
             */

        } catch (Exception exc) {
            logException("Writing archive", exc);
            return false;
        }

        return true;
    }



    private boolean didIsl = false;
    /**
     *  Run the tests specified on the command line and exit
     */
    public void doTest() {
        LogUtil.setTestMode(true);
        try {
            for (int i = 0; i < getArgsManager().argXidvFiles.size(); i++) {
                getPersistenceManager().decodeXmlFile(
                    (String) getArgsManager().argXidvFiles.get(i), false);
            }
            for (int i = 0; i < getArgsManager().b64Bundles.size(); i++) {
                getPersistenceManager().loadB64Bundle(
                    (String) getArgsManager().b64Bundles.get(i));
            }
            for (int i = 0; i < getArgsManager().scriptingFiles.size(); i++) {
                String scriptFile = (String)getArgsManager().scriptingFiles.get(i);
                getIdv().getImageGenerator().processScriptFile(scriptFile);
                didIsl = true;
            }
        } catch (Throwable exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
            System.exit(1);
        }
        Misc.run(this, "waitAndTest");
    }


    /**
     * Wait until displays are done and then run the test
     */
    public void waitAndTest() {
        //        System.err.println ("WaitAndTest");
        if(!didIsl) {
            waitUntilDisplaysAreDone(getIdvUIManager());
        }
        doTestInner();
        System.exit(0);
    }






    /**
     *  Actually runs the test bundles when in test mode. If testImage (which specified an image file name)
     *  is non-null will also find the first ViewManager and print out its image
     */
    private void doTestInner() {
        try {
            String testArchive = getArgsManager().testArchive;
            String testDir     = getArgsManager().testDir;
            if (getArgsManager().testEval) {
                evaluateAllDataChoices();
            }
            if (testArchive != null) {
                writeTestArchive(testArchive, new File(testDir), false, false);
            }
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
            System.exit(1);
        }
        if (LogUtil.anyErrors()) {
            System.exit(1);
        }
    }




    /**
     * Call the GC enough to collect most any dangling memory
     */
    public static void gc() {
        Thread t = Thread.currentThread();
        for (int i = 0; i < 5; i++) {
            Runtime.getRuntime().gc();
            try {
                t.sleep(400);
            } catch (Exception exc) {}
        }
    }


    /** Preference ids for trace */
    public static final String PREF_TEST_ONLYTHESE = "idv.test.onlythese";

    /** Preference ids for trace */
    public static final String PREF_TEST_NOTTHESE = "idv.test.notthese";

    /**
     * Start execution tracing
     */
    public void startTrace() {
        Trace.setFilters((String) getStore().get(PREF_TEST_NOTTHESE),
                         (String) getStore().get(PREF_TEST_ONLYTHESE));
        Trace.startTrace();
        System.out.println("****** START ****** ");
    }

    /**
     * Change the filters
     */
    public void changeFilters() {
        String[] filters = Trace.changeFilters();
        if (filters == null) {
            return;
        }
        getStore().put(PREF_TEST_NOTTHESE, filters[0]);
        getStore().put(PREF_TEST_ONLYTHESE, filters[1]);
    }

    /**
     * Stop execution tracing
     */
    public void stopTrace() {
        Trace.stopTrace();
    }

    /**
     * Start debug logging
     */
    public void startDebug() {
        LogUtil.setDebugMode(true);
        System.out.println("****** START DEBUG ****** ");
    }

    /**
     * Stop debug logging
     */
    public void stopDebug() {
        LogUtil.setDebugMode(false);
        System.out.println("****** END DEBUG ****** ");
    }

    /**
     * Print out the execution trace
     */
    public void printTrace() {
        System.out.println("****** TRACE ****** ");
        Trace.printMsgs();
    }



    /**
     * Print the data cache statistics
     */
    public void printCacheStats() {
        CacheManager.printStats();
    }





    /**
     * This method tests  if the display controls all have valid help.
     */

    public void testDisplayControlHelp() {
        List controlDescriptors = getIdv().getControlDescriptors();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd =
                (ControlDescriptor) controlDescriptors.get(i);
            //The cd prints the warning
            //TODO            cd.isControlHelpValid ();
        }

    }

    /**
     * Dump out the threads
     */
    public void dumpThreads() {
        ThreadGroup root =
            Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        // Visit each thread group
        visitThreads(root, 0);
    }

    // This method recursively visits all thread groups under `group'.

    /**
     * Walk the threads
     *
     * @param group Thread group
     * @param level level
     */
    public static void visitThreads(ThreadGroup group, int level) {
        // Get threads in `group'
        int      numThreads = group.activeCount();
        Thread[] threads    = new Thread[numThreads * 2];
        numThreads = group.enumerate(threads, false);

        // Enumerate each thread in `group'
        for (int i = 0; i < numThreads; i++) {
            // Get thread
            Thread thread = threads[i];
            System.err.println(
                "\n************************************************\n"
                + thread);
            thread.dumpStack();
        }

        // Get thread subgroups of `group'
        int           numGroups = group.activeGroupCount();
        ThreadGroup[] groups    = new ThreadGroup[numGroups * 2];
        numGroups = group.enumerate(groups, false);

        // Recursively visit each subgroup
        for (int i = 0; i < numGroups; i++) {
            visitThreads(groups[i], level + 1);
        }
    }



}









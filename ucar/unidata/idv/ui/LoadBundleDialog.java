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

package ucar.unidata.idv.ui;


import ucar.unidata.data.DataSource;


import ucar.unidata.idv.*;


import ucar.unidata.ui.RovingProgress;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;





/**
 * Class LoadDialog provides the dialog that shows the progress
 * of loading a bundle
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.20 $
 */
public class LoadBundleDialog {

    /** Action command for the cancel and remove button */
    private static final String CMD_CANCELANDREMOVE = "Cancel & Remove";

    /** Shows the message */
    private JLabel msgLabel1;


    /** Shows the message */
    private JLabel msgLabel2;

    /** The dialog */
    private JDialog dialog;


    /** gui contents */
    private JPanel contents;

    /** Has the user pressed cancel */
    private boolean okToRun = true;

    /** The progress bar */
    private RovingProgress progressBar;

    /** The persistence manager */
    private IdvPersistenceManager persistenceManager;

    /** List of data sources that wer decoded */
    private List dataSources = new ArrayList();

    /** List of displays that wer decoded */
    private List displayControls = new ArrayList();

    /** Did the user press cancel and remove */
    private boolean removeItems = false;

    /** The title */
    private String dialogTitle;

    /**
     * Create me
     *
     * @param persistenceManager A reference to the persistence manager in case we need it
     * @param label The label to use in the dialog title
     */
    public LoadBundleDialog(IdvPersistenceManager persistenceManager,
                            String label) {
        dialogTitle             = label;
        label                   = null;
        this.persistenceManager = persistenceManager;
        msgLabel1               = new JLabel(" ");
        msgLabel1.setMinimumSize(new Dimension(250, 20));
        msgLabel1.setPreferredSize(new Dimension(250, 20));
        msgLabel2 = new JLabel(" ");
        msgLabel2.setMinimumSize(new Dimension(250, 20));
        msgLabel2.setPreferredSize(new Dimension(250, 20));

        progressBar = new RovingProgress();
        progressBar.start();
        progressBar.setBorder(BorderFactory.createLineBorder(Color.gray));
        JLabel         waitLbl        = new JLabel(IdvWindow.getWaitIcon());

        ActionListener buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                removeItems = true;
                //removeItems = ae.getActionCommand().equals(CMD_CANCELANDREMOVE);
                okToRun = false;
                setMessage("Cancelling load. Please wait...");
                Misc.runInABit(2000, new Runnable() {
                    public void run() {
                        dispose();
                    }
                });
            }
        };
        //String[] cmds = { CMD_CANCELANDREMOVE, GuiUtils.CMD_CANCEL };
        String[] cmds = { GuiUtils.CMD_CANCEL };
        //            String[] tts = { "Press to cancel and remove any loaded items",
        //                             "Press to cancel" };
        String[] tts = { "Press to cancel and remove loaded items" };
        JPanel buttonPanel = GuiUtils.makeButtons(buttonListener, cmds, cmds,
                                 tts, null);


        GuiUtils.tmpInsets = GuiUtils.INSETS_2;
        JComponent labelComp = GuiUtils.doLayout(new Component[] {
            new JLabel("Status:"), msgLabel1, waitLbl, GuiUtils.filler(),
            msgLabel2, GuiUtils.filler()
        }, 3, GuiUtils.WT_NYN, GuiUtils.WT_N);

        contents = GuiUtils.inset(labelComp, 5);
        if (label != null) {
            contents = GuiUtils.topCenter(GuiUtils.cLabel("Loading: "
                    + label), contents);
        }
        //            contents = GuiUtils.vbox(contents,
        //                                     GuiUtils.inset(progressBar, 5),
        //                                     buttonPanel);
        contents = GuiUtils.vbox(contents, buttonPanel);
    }

    /**
     * Clear the list of data sources and displays.
     * We do this because there can be a retained reference to this dialog from Java
     */
    public void clear() {
        dataSources     = null;
        displayControls = null;
    }


    /**
     * Overwrite dispose to stop the progress bar
     */
    public void dispose() {
        progressBar.stop();
        if (dialog != null) {
            dialog.dispose();
        }
    }

    /**
     * Set the message label text
     *
     * @param msg The message
     */
    public void setMessage(String msg) {
        msgLabel1.setText(msg + " ");
        msgLabel2.setText(" ");
    }

    /**
     * Set the message label text
     *
     * @param msg The message
     */
    public void setMessage1(String msg) {
        msgLabel1.setText(msg + " ");
    }

    /**
     * Set the message label text
     *
     * @param msg The message
     */
    public void setMessage2(String msg) {
        msgLabel2.setText(msg + " ");
    }


    /**
     * Appen to  the message label text
     *
     * @param msg The text to append
     */
    public void appendMessage(String msg) {
        msgLabel1.setText(msgLabel1.getText() + msg);
    }

    /**
     * Has the user pressed cancel yet
     *
     * @return Ok to keep loading
     */
    public boolean okToRun() {
        return okToRun;
    }


    /**
     * Create and show the gui
     */
    public void showDialog() {
        if (dialog == null) {
            JFrame parentFrame =
                persistenceManager.getIdv().getIdvUIManager().getFrame();
            dialog = new JDialog(parentFrame, "Loading Bundle");
            if (dialogTitle != null) {
                dialog.setTitle("Loading Bundle: " + dialogTitle);
            }
            dialog.getContentPane().add(contents);
        }
        dialog.pack();
        Point center = GuiUtils.getLocation(null);
        if (persistenceManager.getIdv().okToShowWindows()) {
            dialog.setLocation(20, 20);
            dialog.setVisible(true);
        }
    }

    /**
     * Add the decoded date source to the list
     *
     * @param dataSource The new data source
     */
    public void addDataSource(DataSource dataSource) {
        dataSources.add(dataSource);
    }

    /**
     * Add the decoded display to the list
     *
     * @param displayControl The new display
     */
    public void addDisplayControl(DisplayControl displayControl) {
        displayControls.add(displayControl);
    }


    /**
     * Get the data sources that have  been loaded up till now
     *
     * @return List of data sources
     */
    public List getDataSources() {
        return dataSources;
    }

    /**
     * Get the displays that have  been loaded up till now
     *
     * @return List of display controls
     */
    public List getDisplayControls() {
        return displayControls;
    }

    /**
     * Was the laod cancelled
     *
     * @return Should the loaded items be removed
     */
    public boolean getShouldRemoveItems() {
        return removeItems;
    }

}

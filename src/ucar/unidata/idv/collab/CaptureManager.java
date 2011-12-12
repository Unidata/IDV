/*
 * $Id: CaptureManager.java,v 1.16 2005/10/20 20:46:39 jeffmc Exp $
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

package ucar.unidata.idv.collab;


import ucar.unidata.idv.*;

import ucar.unidata.collab.*;

import ucar.unidata.xml.*;

import ucar.visad.display.AnimationWidget;


import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.ObjectListener;


import java.rmi.RemoteException;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * This class manages both the IDV collaboration mechanism
 * as well as the event capture mechanism.
 *
 * @author IDV development team
 * @version $Revision: 1.16 $Date: 2005/10/20 20:46:39 $
 */


public class CaptureManager {

    /**
     *  Reference to the  IntegratedDataViewer
     */
    private IntegratedDataViewer idv;

    /** The associated CollabManager */
    private CollabManager collabManager;


    /** The window for showing the capture dialog */
    private JFrame captureWindow;

    /** GUI contents of the capture window */
    private Component captureContents;

    /** Icon for the capture gui */
    private static ImageIcon startIcon;

    /** Icon for the capture gui */
    private static ImageIcon stopIcon;

    /** Icon for the capture gui */
    private static ImageIcon rewindIcon;

    /** Icon for the capture gui */
    private static ImageIcon stepIcon;


    /**
     * List that holds the change messages that have originated
     * in this processfor when we are doing a capture.
     */
    private List captureList = new ArrayList();

    /** Are we currently capturing events */
    private boolean doingCapture = false;

    /** Turn on/off event capture */
    private JButton captureBtn = new JButton("Start event capture");

    /** Write capture button */
    private JButton writeCaptureBtn;

    /** Clear capture button */
    private JButton clearCaptureBtn;


    /** label */
    private JLabel captureLabel;

    /** Select the event capture replay speed */
    private JRadioButton oneXBtn;

    /** Select the event capture replay speed */
    private JRadioButton twoXBtn;

    /** Select the event capture replay speed */
    private JRadioButton fiveXBtn;

    /** Select the event capture replay speed */
    private JRadioButton tenXBtn;

    /**
     * When checked the replay skips inter event delays that are
     * longer than one second
     */
    private JCheckBox skipLongDelaysCbx;

    /** Keep looping */
    private JCheckBox playForeverCbx;

    /** Allows the user to load in a new event capture file */
    private JButton loadCaptureBtn;

    /** Start replaying */
    private JButton playReplayBtn;

    /** GO back to the beginning of the replay */
    private JButton rewindReplayBtn;

    /** Step the next event in the replay */
    private JButton stepReplayBtn;

    /** This displays the current time stamp when replaying */
    private JLabel replayTimeLabel;

    /** This shows the event number */
    private JLabel replayEventLabel;

    /** Are we playing back a event capture */
    private boolean playingCapture = false;

    /**
     * This is the count of how may times the user has started a replay
     * It allows the thread that is running the replay to, when it comes out
     * of its sleep, know if it should be stopped or not.
     */
    private int currentReplayId = 0;

    /** The list of replay events */
    private List replayList;

    /** Index into the replay list */
    private int replayIndex = 0;


    /**
     *  We have this client around when we are doing a replay
     */
    private CollabClient replayClient;


    /**
     * Create the capture manager
     *
     * @param idv The idv
     * @param collabManager The Collaboration manager.
     */
    public CaptureManager(IntegratedDataViewer idv,
                          CollabManager collabManager) {
        this.idv           = idv;
        this.collabManager = collabManager;
        replayClient       = new CollabClient();
        replayClient.setValid(true);
    }


    /**
     * Are we currently capturing events.
     *
     * @return Are we capturing events
     */
    protected boolean doingCapture() {
        return doingCapture;
    }

    /**
     * Add the given event to the captureList
     *
     * @param event The event
     */
    protected void addEvent(CaptureEvent event) {
        captureList.add(event);
        captureLabel.setText(StringUtil.padRight("Events: "
                                                 + (captureList.size()
                                                    + 1), 20));
    }


    /**
     * Have the user choose a capture file and then load it.
     */
    private void loadCapture() {
        String filename = FileManager.getReadFile(IdvConstants.FILTER_CPT);
        if (filename == null) {
            return;
        }
        loadCaptureFile(filename);
    }

    /**
     * Load in the capture events held by the given file
     *
     * @param filename The file to load
     * @return Was load successful
     */
    public boolean loadCaptureFile(String filename) {
        stopCapture();
        showCaptureWindow();
        String xml = IOUtil.readContents(filename, getClass(), null);
        if (xml == null) {
            LogUtil.userMessage("Could not read the given file: " + filename);
            return false;
        }
        List tmpList;
        try {
            tmpList = (List) idv.decodeObject(xml);
        } catch (Exception exc) {
            collabManager.logException("Failed to create capture list", exc);
            return false;
        }
        replayList  = tmpList;
        replayIndex = 0;
        checkReplayButtons();
        return true;
    }


    /**
     * Load the given capture file and then start running
     *
     * @param filename The file  to load
     */
    public void runCaptureFile(String filename) {
        if ( !loadCaptureFile(filename)) {
            return;
        }
        startReplay();
    }


    /**
     * Stop the replay and update the gui
     */
    private void stopReplay() {
        playingCapture = false;
        checkReplayButtons();
    }


    /**
     * Start the replay and update the gui
     */
    private void startReplay() {
        currentReplayId++;
        Misc.run(new Runnable() {
            public void run() {
                startReplayInner(currentReplayId);
            }
        });
    }


    /**
     * Get the replay speed  factor
     * @return The speed
     */
    private int getSpeed() {
        if (oneXBtn.isSelected()) {
            return 1;
        }
        if (twoXBtn.isSelected()) {
            return 2;
        }
        if (fiveXBtn.isSelected()) {
            return 5;
        }
        if (tenXBtn.isSelected()) {
            return 10;
        }
        return 1;
    }

    /**
     * This gets called from within a thread to actually do the replay.
     *
     *
     * @param replayId Used to determine if the user has pressed stop and then
     * start again while this replay thread is sleeping. If they do then the
     * given replayId will not be equals to the currentReplayId and we exit
     */
    private void startReplayInner(int replayId) {
        if (playingCapture) {
            return;
        }
        playingCapture = true;
        checkReplayButtons();
        long lastTimestamp = 0;
        checkReplayLabel();
        int cnt = 0;
        while (true) {
            for (; replayIndex < replayList.size(); replayIndex++) {
                CaptureEvent event =
                    (CaptureEvent) replayList.get(replayIndex);
                long timestamp = event.getTimestamp();
                if (cnt != 0) {
                    try {
                        long sleep = (timestamp - lastTimestamp) / getSpeed();
                        if (skipLongDelaysCbx.isSelected()
                                && (sleep > 1000)) {
                            sleep = 1000;
                        }
                        Thread.currentThread().sleep(sleep);
                    } catch (Exception exc) {}
                }
                cnt++;
                if (currentReplayId != replayId) {
                    return;
                }
                if ( !playingCapture) {
                    return;
                }
                if (replayIndex >= replayList.size()) {
                    return;
                }
                checkReplayLabel();
                lastTimestamp = timestamp;
                //When we are doing replay we pass in false to tell the handler not
                //to check if we have seen this message before.
                collabManager.handleMessage(replayClient, event.getMessage(),
                                            false);
            }
            if ( !playForeverCbx.isSelected()) {
                break;
            }
            replayIndex = 0;
        }
        playingCapture = false;
        checkReplayButtons();
    }

    /**
     * Single step the replay
     */
    private void stepReplay() {
        if ((replayList == null) || (replayIndex >= replayList.size())) {
            return;
        }
        collabManager.handleMessage(
            replayClient,
            ((CaptureEvent) replayList.get(replayIndex)).getMessage(), false);
        replayIndex++;
        checkReplayButtons();
    }

    /**
     * Update the GUI labels
     */
    private void checkReplayLabel() {
        if ((replayList == null) || (replayList.size() == 0)) {
            replayTimeLabel.setText(StringUtil.padRight("", 40));
            replayEventLabel.setText(StringUtil.padRight("", 20));
            rewindReplayBtn.setEnabled(false);
        } else {
            if (replayIndex == 0) {
                rewindReplayBtn.setEnabled(false);
            } else {
                rewindReplayBtn.setEnabled(true);
            }
            CaptureEvent event1 = (CaptureEvent) replayList.get(0);
            CaptureEvent eventn =
                (CaptureEvent) replayList.get(replayList.size() - 1);
            String timeMsg;
            String totalTime = getTimeString(event1, eventn);
            if ((replayIndex > 0) && (replayIndex < replayList.size())) {
                timeMsg =
                    getTimeString(event1, (CaptureEvent) replayList
                        .get(replayIndex)) + "/" + totalTime;
            } else {
                timeMsg = totalTime;
            }
            replayTimeLabel.setText(StringUtil.padRight("Time: " + timeMsg,
                                                        40));
            replayEventLabel.setText(
                StringUtil.padLeft(
                    "Event: " + (replayIndex) + "/" + replayList.size(), 20));
        }
    }

    /**
     * Get the string that represents the time between the two events
     *
     * @param event1 Event 1
     * @param event2 Event 2
     * @return The time string
     */
    private String getTimeString(CaptureEvent event1, CaptureEvent event2) {
        double timeDiff =
            ((double) event2.getTimestamp() - event1.getTimestamp()) / 1000.0;
        int    minutes = (int) (timeDiff / 60.0);
        int    seconds = (int) (timeDiff % 60.0);
        String timeMsg;
        if (minutes == 0) {
            timeMsg = seconds + " seconds";
        } else {
            timeMsg = minutes + ":" + ((seconds <= 9)
                                       ? "0"
                                       : "") + seconds;
        }
        return timeMsg;
    }

    /**
     * Update the GUI to reflect the current replay state
     */
    private void checkReplayButtons() {
        if (doingCapture) {
            playReplayBtn.setEnabled(false);
            stepReplayBtn.setEnabled(false);
        } else {
            captureBtn.setEnabled( !playingCapture);
            writeCaptureBtn.setEnabled( !playingCapture);
            clearCaptureBtn.setEnabled( !playingCapture);
            loadCaptureBtn.setEnabled( !playingCapture);
            if (playingCapture) {
                playReplayBtn.setIcon(stopIcon);

            } else {
                playReplayBtn.setIcon(startIcon);
            }

            if ((replayList == null) || (replayList.size() == 0)) {
                playReplayBtn.setEnabled(false);
                stepReplayBtn.setEnabled(false);
            } else {
                playReplayBtn.setEnabled(replayIndex < replayList.size());
                stepReplayBtn.setEnabled( !playingCapture
                                          && (replayIndex
                                              < replayList.size()));
            }
        }
        checkReplayLabel();

    }


    /**
     * Stop capturing events
     */
    private void stopCapture() {
        doingCapture = false;
        captureBtn.setText("Start Capture");
        checkReplayButtons();
    }


    /**
     * Write out the capture list
     */
    private void writeCapture() {
        String filename = FileManager.getWriteFile(IdvConstants.FILTER_CPT,
                                                   IdvConstants.SUFFIX_CPT);
        if (filename != null) {
            String xml = idv.encodeObject(captureList, true);
            try {
                IOUtil.writeFile(filename, xml);
            } catch (Exception exc) {
                collabManager.logException("Writing file:" + filename, exc);
                return;
            }
        }
    }


    /**
     * Clear the capture list
     */
    private void clearCapture() {
        captureList = new ArrayList();
        captureLabel.setText(StringUtil.padRight("Events: 0", 20));
    }


    /**
     * Start capturing events
     */
    private void startCapture() {
        captureBtn.setText("Stop Capture");
        doingCapture = true;
        checkReplayButtons();
    }



    /**
     * Make the GUI
     *
     * @return The GUI
     */
    private Component doMakeCaptureContents() {

        if (captureContents != null) {
            return captureContents;
        }

        if (startIcon == null) {
            String imgp = "/auxdata/ui/icons/";
            startIcon  = GuiUtils.getImageIcon(imgp + "Play24.gif");
            stopIcon   = GuiUtils.getImageIcon(imgp + "Stop24.gif");
            rewindIcon = GuiUtils.getImageIcon(imgp + "Rewind24.gif");
            stepIcon   = GuiUtils.getImageIcon(imgp + "StepForward24.gif");

        }

        captureBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (doingCapture()) {
                    stopCapture();
                } else {
                    startCapture();
                }
            }
        });

        loadCaptureBtn = new JButton("Load Captured Events");
        loadCaptureBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                loadCapture();
            }
        });


        captureLabel    = new JLabel(StringUtil.padRight("Events: 0", 20));
        writeCaptureBtn = new JButton("Write Captured Events");
        writeCaptureBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                writeCapture();
            }
        });
        clearCaptureBtn = new JButton("Clear Captured Events");
        clearCaptureBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                clearCapture();
            }
        });


        JPanel capturePanel =
            GuiUtils.vbox(
                GuiUtils.left(
                    GuiUtils.hflow(
                        Misc.newList(
                            captureBtn, writeCaptureBtn, clearCaptureBtn), 4, 4)), GuiUtils
                                .left(captureLabel));

        capturePanel.setBorder(
            new TitledBorder(
                new EtchedBorder(EtchedBorder.LOWERED), "Capture"));

        skipLongDelaysCbx = new JCheckBox("Skip long delays");

        ButtonGroup speedGroup = new ButtonGroup();
        oneXBtn = new JRadioButton("1X", true);
        speedGroup.add(oneXBtn);
        twoXBtn = new JRadioButton("2X");
        speedGroup.add(twoXBtn);
        fiveXBtn = new JRadioButton("5X");
        speedGroup.add(fiveXBtn);
        tenXBtn = new JRadioButton("10X");
        speedGroup.add(tenXBtn);
        JPanel speedPanel =
            GuiUtils.left(GuiUtils.hflow(Misc.newList(new JLabel("Speed:"),
                oneXBtn, twoXBtn, fiveXBtn, tenXBtn)));


        replayTimeLabel  = new JLabel("                ");
        replayEventLabel = new JLabel("                ");

        playReplayBtn    = GuiUtils.getImageButton(startIcon);
        playReplayBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (playingCapture) {
                    stopReplay();
                } else {
                    startReplay();
                }
            }
        });

        rewindReplayBtn = GuiUtils.getImageButton(rewindIcon);
        rewindReplayBtn.setToolTipText("Reset to the beginning");
        rewindReplayBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                replayIndex = 0;
                checkReplayButtons();
            }
        });

        stepReplayBtn = GuiUtils.getImageButton(stepIcon);
        stepReplayBtn.setToolTipText("Step forward one event");
        stepReplayBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                stepReplay();
            }
        });



        playForeverCbx = new JCheckBox("Keep playing       ");

        //        JPanel labelPanel = GuiUtils.leftRight(replayTimeLabel,
        //                                               replayEventLabel);
        JPanel labelPanel = GuiUtils.left(replayTimeLabel);
        checkReplayButtons();
        JPanel buttonsPanel = GuiUtils.hflow(Misc.newList(rewindReplayBtn,
                                  playReplayBtn, stepReplayBtn,
                                  playForeverCbx));
        JPanel replayPanel = GuiUtils.leftCenter(buttonsPanel, labelPanel);



        JPanel playPanel = GuiUtils.vbox(GuiUtils.left(loadCaptureBtn),
                                         GuiUtils.left(replayPanel),
                                         GuiUtils.left(skipLongDelaysCbx),
                                         GuiUtils.left(speedPanel));
        playPanel.setBorder(
            new TitledBorder(
                new EtchedBorder(EtchedBorder.LOWERED), "Replay"));
        GuiUtils.tmpInsets = new Insets(8, 0, 8, 0);
        captureContents = GuiUtils.top(GuiUtils.doLayout(new Component[]{
            capturePanel,
            playPanel }, 1, GuiUtils.WT_Y, GuiUtils.WT_N));
        return captureContents;
    }

    /**
     * Popup the window.
     */
    public void showCaptureWindow() {
        if (captureWindow == null) {
            if (captureContents == null) {
                captureContents = doMakeCaptureContents();
            }
            captureWindow = new JFrame("Capture Window");
            GuiUtils.packWindow(captureWindow, captureContents, true);
        }
        captureWindow.show();
        captureWindow.toFront();
    }



}










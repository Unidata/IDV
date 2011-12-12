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

package ucar.visad.display;


import ucar.unidata.collab.*;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.*;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;




/**
 * A widget to control animation in a VisAD display. Allows user to
 * set loop position at first frame, step backward one step,
 * step forward one step,
 * set loop position at last frame, toggle looping on and off, and invoke
 * an AnimationPropertiesDialogWidget.
 * Uses several image icons in "auxdata.jar, making a kind of
 * GUI wrapper for a set of ucar.visad.Animation "displayables"; uses methods of
 * ucar.visad.display Animation
 * which invoke VisAD Animation class methods.
 *
 * @author IDV Development Team
 * @version $Revision: 1.115 $
 */
public class AnimationWidget extends SharableImpl implements ActionListener {

    /** count */
    static int cnt = 0;

    /** instance count */
    int mycnt = cnt++;

    /** Do we show the big icon */

    public static boolean bigIcon = false;


    /** The dialog to use */
    private AnimationPropertiesDialog propertiesDialog;

    /** Listener for property changes from the Animation */
    private PropertyChangeListener animationListener;

    /** The box panel */
    private AnimationBoxPanel boxPanel;

    /** Is the box panel visible */
    private boolean boxPanelVisible = true;

    /** Used for auto update of animation time */
    private int timestamp = 0;

    /** Is the play button being shown */
    private boolean showingPlayBtn = false;

    /** stop icon */
    private static Icon stopIcon;


    /** start icon */
    private static Icon startIcon;

    /** Flag for changing the INDEX */
    public static final String CMD_INDEX = "CMD_INDEX";

    /** property for setting the widget to the first frame */
    public static final String CMD_BEGINNING = "CMD_BEGINNING";

    /** property for setting the widget to the loop in reverse */
    public static final String CMD_BACKWARD = "CMD_BACKWARD";

    /** property for setting the widget to the start or stop */
    public static final String CMD_STARTSTOP = "CMD_STARTSTOP";

    /** property for setting the widget to the loop forward */
    public static final String CMD_FORWARD = "CMD_FORWARD";

    /** property for setting the widget to the last frame */
    public static final String CMD_END = "CMD_END";

    /** property for properties */
    public static final String CMD_PROPS = "CMD_PROPS";

    /** The property for sharing the animation value */
    public static final String SHARE_VALUE = "SHARE_VALUE";

    /** The property for sharing the animation index */
    public static final String SHARE_INDEX = "SHARE_INDEX";

    /** shortcut to the static XmlUtil class */
    static final XmlUtil XU = null;


    /** The start/stop button */
    AbstractButton startStopBtn;

    /** the animation */
    private Animation anime;

    /** The UI contents */
    private JComponent contents;


    /** Keep around the last shared value */
    private Real lastSharedValue;

    /** flag for whether it's in the process of starting or stopping */
    private boolean settingStartStop = false;

    /** Should we ignore the set change event */
    private boolean ignoreAnimationSetChange = false;

    /** Animation info for this widget */
    private AnimationInfo animationInfo;

    /** Indicator (currently a JComboBox) */
    private JComboBox timesCbx = null;

    /** is the times checkbox visible? */
    private boolean timesCbxVisible = true;

    /** mutex  for accessing the timesCbx */
    private Object timesCbxMutex;

    /** Times array from current animation set */
    private DateTime[] timesArray;

    /** flag for whether to ingnore timesCbx events or not */
    private boolean ignoreTimesCbxEvents = false;

    /**
     * Set to true after we have made the gui contents. Use this to not set the items in the combobox until
     *   we have made the gui to fix the random huge size problem
     */
    private boolean madeContents = false;




    /**
     * Default Constructor
     *
     */
    public AnimationWidget() {
        this(null, null, null);
    }

    /**
     * Contruct an AnimationWidget using the info supplied.
     *
     * @param info default state to use.
     */

    public AnimationWidget(AnimationInfo info) {
        this(null, null, info);
    }

    /**
     * Construct an AnimationWidget using the parent supplied.
     *
     * @param parentf   the parent JFrame
     */
    public AnimationWidget(JFrame parentf) {
        this(parentf, null);
    }


    /**
     * Construct an AnimationWidget using the parent, the
     * Animation
     *
     * @param parentf    the parent JFrame
     * @param anim       a ucar.visad.display.Animation object to manage
     */
    public AnimationWidget(JFrame parentf, Animation anim) {
        this(parentf, anim, null);
    }


    /**
     * Contruct a new AnimationWidget.
     *
     * @param parentf    the parent JFrame
     * @param anim       a ucar.visad.display.Animation object to manage
     * @param info       Default values for the AnimationInfo
     */
    public AnimationWidget(JFrame parentf, Animation anim,
                           AnimationInfo info) {

        // Initialize sharing to true
        super("AnimationWidget", true);
        timesCbx = new JComboBox() {
            public String getToolTipText(MouseEvent event) {
                if (boxPanel != null) {
                    return boxPanel.getToolTipText();
                }
                return " ";
            }
        };
        timesCbx.setToolTipText("");
        timesCbxMutex = timesCbx.getTreeLock();
        timesCbx.setFont(new Font("Dialog", Font.PLAIN, 9));
        timesCbx.setLightWeightPopupEnabled(false);
        // set to non-visible until items are added
        timesCbx.setVisible(false);
        timesCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreTimesCbxEvents && (anime != null)) {
                    debug("got timesCbx event");
                    setTimeFromUser((Real) timesCbx.getSelectedItem());
                    if (boxPanel != null) {
                        boxPanel.setOnIndex(timesCbx.getSelectedIndex());
                    }
                }
            }
        });

        animationInfo = new AnimationInfo();
        if (anim != null) {
            setAnimation(anim);
        }
        if (anime != null) {
            animationInfo.set(anime.getAnimationInfo());
        }
        if (info != null) {
            setProperties(info);
            animationInfo.setRunning(info.getRunning());
        }

        boxPanel = new AnimationBoxPanel(this);
        if (timesArray != null) {
            updateBoxPanel(timesArray);
        }
    }


    /**
     * Show the date box
     *
     * @param v  true to show
     */
    public void showDateBox(boolean v) {
        timesCbxVisible = v;
        if (timesCbx != null) {
            timesCbx.setVisible(v);
        }
    }

    /**
     * Set the times that should be used. If this is set we don't go to the displaymaster to get the times.
     *
     * @param times List of times
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setBaseTimes(Set times)
            throws VisADException, RemoteException {
        getAnimationSetInfo().setBaseTimes(times);
        if (times != null) {
            if (anime != null) {
                if (getAnimationSetInfo().getActive()) {
                    Set newSet = getAnimationSetInfo().makeTimeSet(null);
                    anime.setSet(newSet);
                } else {
                    anime.setSet(times);
                }
                updateIndicator(anime.getSet());
            }
        } else {
            updateIndicator(null);
        }
    }


    /**
     * Get the Java Component which is the set of controls.
     *
     * @return a Java Component
     */
    public JComponent getContents() {
        return getContents(false);
    }


    /**
     * get the Java Component which is the set of controls.
     *
     *
     * @param floatToolBar  true if the toolbar should be floatable
     * @return a Java Component
     */
    public JComponent getContents(boolean floatToolBar) {
        if (contents == null) {
            initSharable();
            contents = doMakeContents(floatToolBar);
            if (animationInfo.getRunning()) {
                setRunning(true);
            }
        }
        return contents;
    }

    /**
     * Get the component used to display the time step value.
     *
     * @return timesCbx component
     */
    public Component getIndicatorComponent() {
        return timesCbx;
    }




    /**
     * Get the correect icon name based on whether we are in big icon mode
     *
     * @param name base name
     *
     * @return Full path to icon
     */
    private String getIcon(String name) {
        /*
        if(name.equals("Pause"))
            return "/auxdata/ui/icons/control_pause_blue.png";
        if(name.equals("Play"))
            return "/auxdata/ui/icons/control_play_blue.png";
        if(name.equals("Rewind"))
            return "/auxdata/ui/icons/control_rewind_blue.png";
        if(name.equals("StepBack"))
            return "/auxdata/ui/icons/control_start_blue.png";
        if(name.equals("StepForward"))
            return "/auxdata/ui/icons/control_end_blue.png";
        if(name.equals("FastForward"))
            return "/auxdata/ui/icons/control_fastforward_blue.png";
        */
        if (name.equals("Information") && !bigIcon) {
            return "/auxdata/ui/icons/information.png";
        }
        return "/auxdata/ui/icons/" + name + (bigIcon
                ? "24"
                : "16") + ".gif";
    }



    /**
     * Make the UI for this widget.
     *
     * @param floatToolBar  true if the toolbar should be floatable
     * @return  UI as a Component
     */
    private JComponent doMakeContents(boolean floatToolBar) {

        String      imgp     = "/auxdata/ui/icons/";
        KeyListener listener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getSource() instanceof JComboBox)) {
                    return;
                }
                int  code = e.getKeyCode();
                char c    = e.getKeyChar();
                if ((code == KeyEvent.VK_RIGHT)
                        || (code == KeyEvent.VK_KP_RIGHT)) {
                    if (e.isShiftDown()) {
                        gotoIndex(anime.getNumSteps() - 1);
                    } else {
                        actionPerformed(CMD_FORWARD);
                    }
                } else if ((code == KeyEvent.VK_LEFT)
                           || (code == KeyEvent.VK_KP_LEFT)) {
                    if (e.isShiftDown()) {
                        gotoIndex(0);
                    } else {
                        actionPerformed(CMD_BACKWARD);
                    }
                } else if (code == KeyEvent.VK_ENTER) {
                    actionPerformed(CMD_STARTSTOP);
                } else if ((code == KeyEvent.VK_P) && e.isControlDown()) {
                    actionPerformed(CMD_PROPS);
                } else if (Character.isDigit(c)) {
                    int step = new Integer("" + c).intValue() - 1;
                    if (step < 0) {
                        step = 0;
                    }
                    if (step >= anime.getNumSteps()) {
                        step = anime.getNumSteps() - 1;
                    }
                    gotoIndex(step);
                }
            }
        };

        List buttonList = new ArrayList();
        buttonList.add(timesCbx);
        //Update the list of times
        setTimesInTimesBox();


        Dimension preferredSize = timesCbx.getPreferredSize();
        if (preferredSize != null) {
            int height = preferredSize.height;
            if (height < 50) {
                JComponent filler = GuiUtils.filler(3, height);
                buttonList.add(filler);
            }
        }

        String[][] buttonInfo = {
            { "Go to first frame", CMD_BEGINNING, getIcon("Rewind") },
            { "One frame back", CMD_BACKWARD, getIcon("StepBack") },
            { "Run/Stop", CMD_STARTSTOP, getIcon("Play") },
            { "One frame forward", CMD_FORWARD, getIcon("StepForward") },
            { "Go to last frame", CMD_END, getIcon("FastForward") },
            { "Properties", CMD_PROPS, getIcon("Information") }
        };

        for (int i = 0; i < buttonInfo.length; i++) {
            JButton btn = GuiUtils.getScaledImageButton(buttonInfo[i][2],
                              getClass(), 2, 2);
            btn.setToolTipText(buttonInfo[i][0]);
            btn.setActionCommand(buttonInfo[i][1]);
            btn.addActionListener(this);
            btn.addKeyListener(listener);
            //            JComponent wrapper = GuiUtils.center(btn);
            //            wrapper.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            btn.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            buttonList.add(btn);
            //            buttonList.add(wrapper);
            if (i == 2) {
                startStopBtn = btn;
            }
        }




        JComponent contents = GuiUtils.hflow(buttonList, 1, 0);
        if (boxPanel == null) {
            boxPanel = new AnimationBoxPanel(this);
            if (timesArray != null) {
                updateBoxPanel(timesArray);
            }
        }
        boxPanel.addKeyListener(listener);
        if ( !getBoxPanelVisible()) {
            boxPanel.setVisible(false);
        }
        contents = GuiUtils.doLayout(new Component[] { boxPanel, contents },
                                     1, GuiUtils.WT_Y, GuiUtils.WT_N);
        //      GuiUtils.addKeyListenerRecurse(listener,contents);
        if (floatToolBar) {
            JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
            toolbar.setFloatable(true);
            contents = GuiUtils.left(contents);
            toolbar.add(contents);
            contents = toolbar;
        }

        updateRunButton();
        madeContents = true;
        return contents;
    }



    /**
     * Debug
     *
     * @param msg the message
     */
    private void debug(String msg) {
        //        System.err.println("anim:" + mycnt + " " +Thread.currentThread() +" " + msg);
    }

    /**
     * Apply the info from the dialog
     *
     * @param info The info
     * @param andShare Share the state with other widgets
     */
    protected void applyProperties(AnimationInfo info, boolean andShare) {
        if (boxPanel != null) {
            boxPanel.applyProperties(propertiesDialog.boxPanel);
        }
        setProperties(info);
        if (andShare) {
            doShare(CMD_PROPS, info);
        }
    }


    /**
     * Make and show an AnimationPropertiesDialog;
     * if that returns animationInfo ok,
     * set the new animationInfo data into the the Animations.
     */
    protected void showPropertiesDialog() {
        if (propertiesDialog == null) {
            AnimationBoxPanel propertiesBoxPanel =
                new AnimationBoxPanel(null, boxPanel.getStepsOk());
            propertiesDialog = new AnimationPropertiesDialog(this,
                    GuiUtils.getFrame(getContents()), propertiesBoxPanel);
            animationInfo.shared       = getSharing();
            animationInfo.boxesVisible = getBoxPanelVisible();
            propertiesDialog.setInfo(animationInfo);
        }
        propertiesDialog.boxPanel.applyProperties(boxPanel);
        propertiesDialog.show();
    }


    /**
     * Called when the box panel has changed through a user drag or click
     *
     * @param boxPanel The changed box panel
     */
    protected void boxPanelChanged(AnimationBoxPanel boxPanel) {
        //If we have the properties dialog up then tell its boxpanel to change
        if ((propertiesDialog != null) && (this.boxPanel == boxPanel)) {
            propertiesDialog.boxPanel.applyProperties(boxPanel);
        }
    }


    /**
     * Force the existing animation properties, held in the animation widget's
     * "animationInfo" member data, into this widget's set of
     * Animation objects.
     */
    public void resetProperties() {
        setProperties(animationInfo);
    }

    /**
     * From the "animationInfo" set of animation properties, set all these
     * values into all the Animation objects held as memeber data.
     *
     * @param transfer  AnimationInfo to get properties from
     */
    public void setProperties(AnimationInfo transfer) {
        setBoxPanelVisible(transfer.getBoxesVisible());
        animationInfo.set(transfer);
        setSharing(animationInfo.shared);
        if (animationInfo.getAnimationGroup() != null) {
            setShareGroup(animationInfo.getAnimationGroup());
        }
        if (propertiesDialog != null) {
            propertiesDialog.setInfo(animationInfo);
        }

        try {
            if (anime != null) {
                anime.setAnimationInfo(animationInfo);
                DisplayMaster displayMaster = anime.getDisplayMaster();
                if (displayMaster != null) {
                    displayMaster.dataChange();
                } else {
                    if (getAnimationSetInfo().getActive()) {
                        anime.setSet(getAnimationSetInfo().makeTimeSet(null));
                    } else {
                        anime.setSet(getAnimationSetInfo().getBaseTimes());
                    }
                }
            }
        } catch (Exception exp) {
            LogUtil.logException("Error setting properties", exp);
        }
        updateRunButton();
        checkAutoUpdate();
    }



    /**
     * Start/stop autoupdating if needed
     */
    private void checkAutoUpdate() {
        //bumping up the timestamp will stop any previous threads
        timestamp++;
        if (animationInfo.getAnimationSetInfo().usingCurrentTime()) {
            Misc.run(new Runnable() {
                public void run() {
                    updateAnimationSet(++timestamp);
                }
            });
        }
    }

    /**
     * Get the display master that the animation is in
     *
     * @return The display master or null
     */
    protected DisplayMaster getDisplayMaster() {
        if (anime != null) {
            return anime.getDisplayMaster();
        }
        return null;
    }

    /**
     * autoupdate the set of synthetic times
     *
     * @param myTimestamp used to only have on thread active
     */
    private void updateAnimationSet(int myTimestamp) {
        try {
            while (true) {
                long seconds =
                    (long) (animationInfo.getAnimationSetInfo()
                        .getPollMinutes() * 60);
                Misc.sleepSeconds(seconds);
                DisplayMaster displayMaster = getDisplayMaster();
                if ((displayMaster == null) || (anime == null)
                        || (myTimestamp != timestamp)) {
                    break;
                }
                displayMaster.dataChange();
            }
        } catch (Exception exc) {
            LogUtil.logException("Error updating animation set", exc);
        }
    }




    /**
     * Public by implementing ActionListener.
     *
     * @param e  ActionEvent to check
     */
    public void actionPerformed(ActionEvent e) {
        actionPerformed(e.getActionCommand());
    }


    /**
     * Handle the action
     *
     * @param cmd The action
     */
    public void actionPerformed(String cmd) {
        if (cmd.equals(CMD_STARTSTOP)) {
            setRunning( !isRunning());
        } else if (cmd.equals(CMD_FORWARD)) {
            stepForward();
        } else if (cmd.equals(CMD_BACKWARD)) {
            stepBackward();
        } else if (cmd.equals(CMD_BEGINNING)) {
            gotoBeginning();
        } else if (cmd.equals(CMD_END)) {
            gotoEnd();
        } else if (cmd.equals(CMD_PROPS)) {
            showPropertiesDialog();
        }
    }

    /**
     * We got the tiem from another animation widget.
     *
     * @param time The time.
     */
    protected void handleSharedTime(Real time) {
        if (anime != null) {
            anime.setAniValue(time);
        }
    }

    /**
     * THis allows external code to set the time. It sets the time on
     * the animation and it does a doShare.
     *
     * @param time The time
     */
    public void setTimeFromUser(Real time) {
        anime.setAniValue(time);
        //        shareValue(time);
        shareValue();
    }


    /**
     * Method called when sharing is turned on.
     *
     * @param from    source of shareable information
     * @param dataId  ID for the data
     * @param data    the shareable data
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {

        if (dataId.equals(SHARE_INDEX)) {
            if (anime != null) {
                anime.setCurrent(((Integer) data[0]).intValue());
            }
        } else if (dataId.equals(SHARE_VALUE)) {
            Real sharedValue = (Real) data[0];
            debug("receiveShareData " + sharedValue);
            handleSharedTime(sharedValue);
        } else if (dataId.equals(CMD_STARTSTOP)) {
            setRunning(((Boolean) data[0]).booleanValue());
        } else if (dataId.equals(CMD_FORWARD)) {
            stepForward();
        } else if (dataId.equals(CMD_BACKWARD)) {
            stepBackward();
        } else if (dataId.equals(CMD_BEGINNING)) {
            gotoBeginning();
        } else if (dataId.equals(CMD_END)) {
            gotoEnd();
        } else if (dataId.equals(CMD_PROPS)) {
            AnimationInfo newInfo = (AnimationInfo) data[0];
            if (propertiesDialog != null) {
                newInfo.shared = getSharing();
                propertiesDialog.setInfo(newInfo);
            }
            setProperties(newInfo);
        } else {
            super.receiveShareData(from, dataId, data);
        }
    }

    /**
     * Update the icon in the run button
     */
    private void updateRunButton() {
        if (stopIcon == null) {
            stopIcon  = Resource.getIcon(getIcon("Pause"), true);
            startIcon = Resource.getIcon(getIcon("Play"), true);
        }
        if (startStopBtn != null) {
            boolean running = isRunning() && haveTimes();
            if (running) {
                startStopBtn.setIcon(stopIcon);
                startStopBtn.setToolTipText("Stop animation");
            } else {
                startStopBtn.setIcon(startIcon);
                startStopBtn.setToolTipText("Start animation");
            }
        }
    }

    /**
     * Set the animation state and change the start/stop widget
     *
     * @param state  true to start animating
     */
    public void setRunning(boolean state) {
        //Check to make sure don't infinitely loop
        if (settingStartStop) {
            return;
        }
        settingStartStop = true;
        animationInfo.setRunning(state);
        if (anime != null) {
            anime.setAnimating(state);
        }
        if ( !state) {
            doShare(CMD_STARTSTOP, new Boolean(state));
        }
        shareValue();
        updateRunButton();
        settingStartStop = false;
    }

    /**
     * Are we running
     *
     * @return Is running
     */
    public boolean isRunning() {
        if (anime != null) {
            return anime.isAnimating();
        }
        return false;
    }


    /**
     * Get the {@link AnimationInfo} associated with this widget.
     * @return AnimationInfo used by this widget.
     */
    public AnimationInfo getAnimationInfo() {
        return animationInfo;
    }

    /**
     * Holds the synthetic animation set info
     *
     * @return Animation set info
     */
    public AnimationSetInfo getAnimationSetInfo() {
        return animationInfo.getAnimationSetInfo();
    }

    /**
     * Share the index of the animation step.
     */
    protected void shareIndex() {
        doShare(SHARE_INDEX, new Integer(anime.getCurrent()));
    }


    /**
     * Share the value of the animation step.
     */
    protected void shareValue() {
        Animation     myAnimation     = anime;
        AnimationInfo myAnimationInfo = animationInfo;
        if ((myAnimation != null) && (myAnimationInfo != null)) {
            if (myAnimation.getNumSteps() > 0) {
                if (animationInfo.getShareIndex()) {
                    shareIndex();
                } else {
                    shareValue(myAnimation.getAniValue());
                }
            }
        }
    }



    /**
     * Share the time value
     *
     * @param time The value to share
     */
    protected void shareValue(Real time) {
        lastSharedValue = time;
        debug("shareValue:" + time);
        //        Misc.printStack(mycnt+ " share",4,null);
        doShare(SHARE_VALUE, time);
    }


    /**
     * Take one step forward in the animation sequence.
     */
    public void stepForward() {
        if (anime != null) {
            anime.takeStepForward();
        }
        //shareIndex ();
        shareValue();
    }

    /**
     * Take one step backward in the animation sequence.
     */
    protected void stepBackward() {
        if (anime != null) {
            anime.takeStepBackward();
        }
        //shareIndex ();
        shareValue();
    }

    /**
     * Set the current frame to the index supplied.
     * Turn off animation
     * This ignores any frames the user may have turned off
     *
     * @param index   index into the animation set
     */
    public void gotoIndex(int index) {
        if (anime != null) {
            setRunning(false);
            anime.setCurrent(index, false);
        }
        shareValue();
    }

    /**
     * Go to the beginning of the animation sequence.
     */
    public void gotoBeginning() {
        if (anime != null) {
            anime.setCurrent(0);
        }
        setRunning(false);
        //shareIndex ();
        shareValue();
    }

    /**
     * Get the array of times
     *
     * @return times
     */
    public DateTime[] getTimes() {
        return anime.getTimes();
    }


    /**
     * Go to the end of the animation sequence.
     */
    public void gotoEnd() {
        if (anime != null) {
            visad.Set aset = anime.getSet();
            if (aset != null) {
                try {
                    anime.setCurrent(aset.getLength() - 1);
                } catch (VisADException ve) {
                    ;
                }
            }
        }

        setRunning(false);
        //shareIndex ();
        shareValue();
    }



    /**
     * The user has clicked on the box. Pass this through to the animation
     *
     * @param stepsOk What time steps are ok
     */
    public void stepsOkChanged(boolean[] stepsOk) {
        try {
            anime.setStepsOk(stepsOk);
        } catch (Exception exp) {
            LogUtil.logException("Error setting steps ok", exp);
        }
    }



    /**
     * Sets the <CODE>ucar.visad.display.Animation</CODE>
     * controlled by this widget.  Removes
     * any other <CODE>ucar.visad.display.Animation</CODE>
     * from the control of this widget.
     * @param newAnimation ucar.visad.display.Animation to control
     */
    public void setAnimation(Animation newAnimation) {
        if (newAnimation == null) {
            throw new NullPointerException("Animation can't be null");
        }
        removeAnimationListener();
        anime = newAnimation;
        animationInfo.set(anime.getAnimationInfo());
        updateIndicator(anime.getSet());
        animationListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                handleAnimationPropertyChange(evt);
            }
        };
        anime.addPropertyChangeListener(animationListener);
    }



    /**
     * The animation changed. Handle the change.
     *
     * @param evt The event
     */
    private void handleAnimationPropertyChange(PropertyChangeEvent evt) {
        //        System.err.println ("Handlechange:" +evt.getPropertyName());
        if (evt.getPropertyName().equals(Animation.ANI_VALUE)) {
            debug("handleAnimationPropertyChange value :"
                  + evt.getPropertyName());
            Real eventValue = (Real) evt.getNewValue();
            // if there's nothing to do, return;
            if ((eventValue == null) || eventValue.isMissing()) {
                return;
            }

            /** The Animation associated with this widget */
            DateTime time = null;
            try {
                time = new DateTime(eventValue);
            } catch (VisADException ve) {
                ;
            }
            final DateTime theDateTime = time;
            final int      theIndex    = ((anime != null)
                                          ? anime.getCurrent()
                                          : -1);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    boolean oldValue = ignoreTimesCbxEvents;
                    try {
                        ignoreTimesCbxEvents = true;
                        //                        synchronized (timesCbxMutex) {
                        xcnt++;

                        timesCbx.setSelectedItem(theDateTime);
                        //                        }
                        if ((boxPanel != null) && (theIndex >= 0)) {
                            boxPanel.setOnIndex(theIndex);
                        }
                        timesCbx.repaint();
                    } finally {
                        ignoreTimesCbxEvents = oldValue;
                    }
                }
            });
            shareValue();
        } else if (evt.getPropertyName().equals(Animation.ANI_SET)) {
            if (ignoreAnimationSetChange) {
                return;
            }
            updateIndicatorInner((Set) evt.getNewValue(), true);
        }
    }



    /**
     * Remove the listener from the Animation.  Called when object
     * is destroyed.
     * @see #destroy
     */
    private void removeAnimationListener() {
        if ((anime != null) && (animationListener != null)) {
            anime.removePropertyChangeListener(animationListener);
            animationListener = null;
            anime             = null;
        }
    }

    /**
     * Method called when destroying this object.
     */
    public void destroy() {
        removeSharable();
        removeAnimationListener();
        timestamp++;
    }


    /**
     * Adds an <CODE>ucar.visad.display.Animation</CODE>
     * to be controlled by this widget.
     * @param anim ucar.visad.display.Animation to control  (must not be null)
     * @deprecated  use setAnimation();
     */
    public void addAnimation(Animation anim) {
        setAnimation(anim);
    }

    /**
     * Update the indicator with the list of times
     *
     * @param timeSet  set of times
     */
    public void updateIndicator(final Set timeSet) {
        //Call the updateIndicatorInner insode a thread to
        //prevent possible deadlock around the Swing component tree
        //        Misc.run(new Runnable() {
        //            public void run() {
        updateIndicatorInner(timeSet, false);
        //            }
        //        });
    }






    /**
     * Get the time at the given index. May return null.
     *
     * @param index Index
     *
     * @return Time
     */
    public DateTime getTimeAtIndex(int index) {
        if (anime == null) {
            return null;
        }
        if (timesArray == null) {
            timesArray = Animation.getDateTimeArray(anime.getSet());
        }
        if ((timesArray == null) || (index < 0)
                || (index >= timesArray.length)) {
            return null;
        }
        return timesArray[index];
    }

    /**
     * Do we have any times
     *
     * @return have any times
     */
    private boolean haveTimes() {
        return (timesArray != null) && (timesArray.length > 0);
    }

    /**
     * Actually does the work of updating the indicator.
     * This will also turn off animation if there are no times
     * @param timeSet  animation set
     * @param timeSetChange Was this do to just a change in the time set.
     * If so then we will automatically stop running if there are no times
     * We have this check here so we can disambiguate when this is called
     * from when we just have a new Animation at initialization time (timeSetChange=false)
     * from when the times have changed in the display.
     */
    int xcnt = 0;

    /**
     * _more_
     *
     * @param timeSet _more_
     * @param timeSetChange _more_
     */
    private void updateIndicatorInner(Set timeSet, boolean timeSetChange) {
        //      timeSet  = checkAnimationSet(timeSet);
        timesArray = Animation.getDateTimeArray(timeSet);

        //Stop running if there are no times
        if ((timesArray.length == 0) && timeSetChange) {
            setRunning(false);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Only set the list data if we have created the gui contents 
                if (madeContents) {
                    setTimesInTimesBox();
                    updateRunButton();
                }
            }
        });
        updateBoxPanel(timesArray);
    }


    /**
     * _more_
     */
    private void setTimesInTimesBox() {
        DateTime[] theTimesArray = this.timesArray;
        if (theTimesArray == null) {
            return;
        }
        boolean oldValue = ignoreTimesCbxEvents;
        try {
            ignoreTimesCbxEvents = true;
            GuiUtils.setListData(timesCbx, theTimesArray);
            timesCbx.setVisible(timesCbxVisible && (theTimesArray != null)
                                && (timesCbx.getItemCount() > 0));

        } finally {
            ignoreTimesCbxEvents = oldValue;
        }
    }



    /**
     * Update the state of the box panel with the animation set
     *
     * @param timesArray Array of times in set
     */
    private void updateBoxPanel(DateTime[] timesArray) {
        if (boxPanel != null) {
            boxPanel.setNumTimes(timesArray.length);
            if (anime != null) {
                boxPanel.setOnIndex(anime.getCurrent());
            }
            if (propertiesDialog != null) {
                propertiesDialog.boxPanel.applyProperties(this.boxPanel);
            }
        }
    }

    /**
     * Set the BoxPanelVisible property.
     *
     * @param value The new value for BoxPanelVisible
     */
    public void setBoxPanelVisible(boolean value) {
        boxPanelVisible = value;
        if (boxPanel != null) {
            if (boxPanel.isVisible() != boxPanelVisible) {
                boxPanel.setVisible(boxPanelVisible);
                if (boxPanel.getParent() != null) {
                    boxPanel.getParent().doLayout();
                }
            }
        }
    }

    /**
     * Get the BoxPanelVisible property.
     *
     * @return The BoxPanelVisible
     */
    public boolean getBoxPanelVisible() {
        return boxPanelVisible;
    }




    /**
     * Main method for testing
     *
     * @param args  arguments
     */
    public static void main(String[] args) {

        JFrame frame = new JFrame();

        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        AnimationWidget widget = new AnimationWidget(frame);

        frame.getContentPane().add(widget.getContents());
        frame.pack();
        frame.setVisible(true);
    }
}

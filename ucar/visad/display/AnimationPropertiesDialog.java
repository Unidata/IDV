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


import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.ui.TimeLengthField;
import ucar.unidata.ui.Timeline;

import ucar.unidata.util.DatedObject;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import visad.CommonUnit;
import visad.DateTime;
import visad.Real;
import visad.Set;
import visad.VisADException;


import java.*;

import java.awt.*;

import java.awt.event.*;

import java.beans.*;

import java.io.*;

import java.lang.*;

import java.net.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.border.*;

import javax.swing.event.*;


/**
 * A widget to get properties for animation from the user.
 * A popup widget to set animation forward looping time intervals (typically
 * 0.5 sec),  backward looping time intervals (typically 0.5 sec),
 * looping direction (forward or backward),
 * first dwell time (time viewing first frame, typically 0.5 second),
 * last dwell time (time viewing last frame, typically 1.5 second).
 * In future plan to provide a graphic list of frames
 * to allow user to toggle some off or "out of loop".
 * <p>
 * Invoked by an AnimationWidget object.
 * @author  IDV Development Team
 * @version $Revision: 1.83 $
 */
public class AnimationPropertiesDialog extends JDialog implements ActionListener,
        ChangeListener {

    /** List of reset TFOs */
    private List resetList;

    /** true = forward loop */
    private boolean ok;

    /** forward increment scrollbar */
    private JSlider fwdSlider;

    /** back increment scrollbar */
    private JSlider backSlider;

    /** scollbar for selecting dwell for first step */
    private JSlider startSlider;

    /** scollbar for selecting dwell for last step */
    private JSlider endSlider;

    /** label for fwdScrollBar */
    private JTextField fwdField;

    /** label for backScrollBar */
    private JTextField backField;

    /** label for startScrollBar */
    private JTextField startField;

    /** label for endScrollBar */
    private JTextField endField;

    /** share none button */
    private JRadioButton shareNoneBtn;

    /** checkbox for selecting shared state */
    private JRadioButton shareAbsBtn;

    /** checkbox for selecting share index state */
    private JRadioButton shareRelBtn;

    /** Show the boxes */
    private JCheckBox boxesVisibleButton;

    /** predefined button */
    private JButton predefinedBtn;

    /** Direction widget */
    private JComboBox dirCbx;

    /** should we save this animationinfo as a prototype */
    private JCheckBox saveAsPrototypeCbx;

    /** Combobox for setting the reset policy */
    private JComboBox resetCbx;

    /** AnimationInfo for this widget */
    private AnimationInfo myInfo = new AnimationInfo();

    /** The animation widget */
    private AnimationWidget animationWidget;

    /** The box panel */
    AnimationBoxPanel boxPanel;


    /** gui widgets */
    private JComponent setContents;

    /** gui widgets */
    private TimeLengthField intervalField;

    /** gui widgets */
    private TimeLengthField startOffsetField;

    /** gui widgets */
    private TimeLengthField endOffsetField;

    /** gui widgets */
    private TimeLengthField roundToField;

    /** Holds the details */
    private GuiUtils.CardLayoutPanel startExtra;

    /** Holds the details */
    private GuiUtils.CardLayoutPanel endExtra;

    /** details widget */
    private JLabel startDataModeLabel;

    /** details widget */
    private JLabel endDataModeLabel;

    /** details widget */
    private JLabel startCurrentModeLabel;

    /** details widget */
    private JLabel endCurrentModeLabel;

    /** details widget */
    private JLabel startRelativeModeLabel;

    /** details widget */
    private JLabel endRelativeModeLabel;

    /** gui widgets */
    private JTextField pollIntervalFld;

    /** gui widgets */
    private JRadioButton animationSetNotActiveBtn;

    /** gui widgets */
    private JRadioButton animationSetActiveBtn;

    /** gui widgets */
    private JComboBox startTimeBox;

    /** gui widgets */
    private JComboBox endTimeBox;


    /** gui widgets */
    private DateTimePicker startTimePicker;


    /** gui widgets */
    private DateTimePicker endTimePicker;

    /** gui widgets */
    List baseTimeList = new ArrayList();

    /** gui widgets */
    List pollList = new ArrayList();

    /** gui widgets */
    List intervalList = new ArrayList();

    /** used for when we have a failed string->double conversion */
    private String intervalText = "";

    /** flag for updating labels */
    private boolean okToUpdate = false;

    /** timeline */
    private Timeline timeline;


    /**
     * Constuctor with input string naming for what data
     *
     * @param animationWidget the AnimationWidget
     * @param parent the original parent JFrame
     * @param boxPanel The box panel
     */
    public AnimationPropertiesDialog(AnimationWidget animationWidget,
                                     JFrame parent,
                                     AnimationBoxPanel boxPanel) {

        super(parent, "Time Animation Properties", false);

        JTabbedPane tabbedPane = new JTabbedPane();
        this.boxPanel        = boxPanel;
        this.animationWidget = animationWidget;

        dirCbx = new JComboBox(new Vector(Misc.newList("Forward", "Backward",
                "Rocking")));
        dirCbx.setToolTipText("<html>Set loop direction</html>");

        boxesVisibleButton = new JCheckBox("Boxes Visible",
                                           myInfo.getBoxesVisible());

        resetList = Misc.newList(
            new TwoFacedObject("Start Time", AnimationInfo.RESET_BEGINNING),
            new TwoFacedObject("No Change", AnimationInfo.RESET_CURRENT),
            new TwoFacedObject("End Time", AnimationInfo.RESET_END));

        resetCbx = new JComboBox(new Vector(resetList));
        resetCbx.setToolTipText(
            "When there are new times in the display what should happen?");

        boolean shared     = myInfo.getShared();
        boolean shareIndex = myInfo.getShareIndex();
        // make check box for toggling sharing
        shareNoneBtn = new JRadioButton("None", !shared);
        shareNoneBtn.setToolTipText("Don't share times");
        shareAbsBtn = new JRadioButton("Absolute", shared && !shareIndex);
        shareAbsBtn.setToolTipText("Share absolute time step");
        shareRelBtn = new JRadioButton("Relative", shared && shareIndex);
        shareRelBtn.setToolTipText("Share relative time step");
        GuiUtils.buttonGroup(shareNoneBtn, shareAbsBtn).add(shareRelBtn);

        JComponent sharingComponent = GuiUtils.hbox(shareNoneBtn,
                                          shareAbsBtn, shareRelBtn);


        startField         = makeField();
        endField           = makeField();
        fwdField           = makeField();
        backField          = makeField();



        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        //        GuiUtils.tmpInsets = new Insets(1,1,1,1);
        JPanel dwellPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Forward:"),
            GuiUtils.left(GuiUtils.hbox(fwdField, makeSlider(fwdField))),
            GuiUtils.rLabel("First:"),
            GuiUtils.left(GuiUtils.hbox(startField, makeSlider(startField))),
            GuiUtils.rLabel("Reverse:"),
            GuiUtils.left(GuiUtils.hbox(backField, makeSlider(backField))),
            GuiUtils.rLabel("Last:"),
            GuiUtils.left(GuiUtils.hbox(endField, makeSlider(endField)))
        }, 4, GuiUtils.WT_NN, GuiUtils.WT_N);


        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        timeline           = new Timeline(new ArrayList(), 300);
        timeline.setIsCapableOfSelection(false);

        JComponent top = GuiUtils.formLayout(new Component[] {
            GuiUtils.rLabel("Dwell:"), GuiUtils.left(dwellPanel),
            GuiUtils.rLabel("Direction:"), GuiUtils.left(dirCbx),
            GuiUtils.rLabel("Share Times:"), GuiUtils.left(sharingComponent),
            GuiUtils.filler(),
            GuiUtils.left(animationWidget.getSharablePropertiesComponent()),
            GuiUtils.rLabel("Skip Steps:"),
            GuiUtils.left(GuiUtils.hbox(boxesVisibleButton, boxPanel)),
            //GuiUtils.rLabel("Timeline:"),
            //timeline.getContents(false),
            GuiUtils.rLabel("Reset To:"),
            GuiUtils.left(
                GuiUtils.hbox(
                    resetCbx,
                    GuiUtils.lLabel("  When the time set has changed")))
        });
        top = GuiUtils.top(top);


        JPanel buttons = GuiUtils.makeApplyOkCancelButtons(this);
        saveAsPrototypeCbx = new JCheckBox("Save As Default", false);
        if (Misc.getPrototypeManager() != null) {
            buttons = GuiUtils.wrap(GuiUtils.hbox(buttons,
                    saveAsPrototypeCbx));
        }


        JPanel mainContents = GuiUtils.inset(top, 5);

        JPanel contents = GuiUtils.centerBottom(GuiUtils.inset(tabbedPane,
                              5), buttons);

        tabbedPane.add("Settings", mainContents);
        tabbedPane.add(
            "Define Animation Times",
            GuiUtils.inset(GuiUtils.top(getAnimationSetComponent()), 5));

        setInfo(myInfo);

        getContentPane().add("Center", contents);
        //        setSize(400, 100);
        pack();
        setLocation(300, 100);
        okToUpdate = true;
    }  // end cstr


    /**
     * Get the animation times
     *
     * @return array of times
     *
     * @throws RemoteException Java RMI exception
     * @throws VisADException  problem creating the DateTime objects
     */
    private DateTime[] getAnimationTimes()
            throws VisADException, RemoteException {
        Set timeSet = animationWidget.getAnimationSetInfo().getBaseTimes();
        if ((timeSet == null)
                && (animationWidget.getDisplayMaster() != null)) {
            timeSet =
                animationWidget.getDisplayMaster()
                    .getAnimationSetFromDisplayables();
        }
        DateTime[] timesArray = Animation.getDateTimeArray(timeSet);
        return timesArray;
    }


    /**
     * Update the labels
     */
    private void updateLabels() {
        try {
            if ( !okToUpdate) {
                return;
            }
            updateTimeline();
            DateTime[] timesArray  = getAnimationTimes();

            double     startOffset = startOffsetField.getTime() * 60;
            double     endOffset   = endOffsetField.getTime() * 60;
            DateTime   min         = null;
            DateTime   max         = null;
            if (timesArray.length > 0) {
                min = timesArray[0];
                max = timesArray[timesArray.length - 1];
                double minSeconds =
                    min.getValue(CommonUnit.secondsSinceTheEpoch);
                min = new DateTime(
                    AnimationSetInfo.roundTo(
                        roundToField.getTime(), minSeconds) + startOffset);
                double maxSeconds =
                    max.getValue(CommonUnit.secondsSinceTheEpoch);
                max = new DateTime(
                    AnimationSetInfo.roundTo(
                        roundToField.getTime(), maxSeconds) + endOffset);
            }



            String startDataText = ((min != null)
                                    ? min.toString()
                                    : "");
            String endDataText   = ((max != null)
                                    ? max.toString()
                                    : "");
            startDataModeLabel.setText(startDataText);
            endDataModeLabel.setText(endDataText);
            double now = System.currentTimeMillis() / 1000;
            now = AnimationSetInfo.roundTo(roundToField.getTime(), now);
            startCurrentModeLabel.setText(new visad.DateTime(now
                    + startOffset).toString());
            endCurrentModeLabel.setText(new visad.DateTime(now
                    + endOffset).toString());
        } catch (Exception exc) {
            LogUtil.logException("Error initializing dialog", exc);
        }
    }


    /**
     * Make the value slider  that sets the value of the field
     *
     * @param field The field
     *
     * @return The slider button
     */
    private JComponent makeSlider(final JTextField field) {
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slide = (JSlider) e.getSource();
                if (slide.getValueIsAdjusting()) {
                    //                      return;
                }
                setFieldFromSlider(slide, field);
            }
        };
        JComponent[] comps = GuiUtils.makeSliderPopup(5, 1000, 50, listener);
        ((JSlider) comps[1]).setMinorTickSpacing(5);
        if (field == fwdField) {
            fwdSlider = (JSlider) comps[1];
        }
        if (field == backField) {
            backSlider = (JSlider) comps[1];
        }
        if (field == startField) {
            startSlider = (JSlider) comps[1];
        }
        if (field == endField) {
            endSlider = (JSlider) comps[1];
        }
        return comps[0];
    }




    /**
     * Utility to add the component to the list of comps
     *
     * @param comp comp to add
     * @param comps list to add to
     *
     * @return The component
     */
    private JComponent addTo(JComponent comp, List comps) {
        comps.add(comp);
        return comp;
    }

    /**
     * Create the animation set component for the gui
     *
     * @return The component
     */
    private JComponent getAnimationSetComponent() {

        predefinedBtn = GuiUtils.makeButton("Predefined", this,
                                            "popupPredefinedMenu");

        pollIntervalFld  = new JTextField("", 4);

        intervalField    = new TimeLengthField("Interval", true);
        startOffsetField = new TimeLengthField("Start Offset") {
            //pick up any changes
            public void timeChanged() {
                super.timeChanged();
                updateLabels();
            }
        };
        endOffsetField = new TimeLengthField("End Offset") {
            //pick up any changes
            public void timeChanged() {
                super.timeChanged();
                updateLabels();
            }
        };
        roundToField = new TimeLengthField("Round To Value", true) {
            //pick up any changes
            public void timeChanged() {
                super.timeChanged();
                updateLabels();
            }
        };

        animationSetNotActiveBtn =
            new JRadioButton("Use all times from data");
        animationSetActiveBtn =
            new JRadioButton("Define your own list of times");
        GuiUtils.buttonGroup(animationSetNotActiveBtn, animationSetActiveBtn);
        animationSetActiveBtn.addChangeListener(this);
        startTimeBox =
            GuiUtils.makeComboBox(AnimationSetInfo.TIMEMODES,
                                  AnimationSetInfo.STARTMODELABELS,
                                  AnimationSetInfo.TIMEMODE_DATA);

        ActionListener boxListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkEnabled();
                updateLabels();
            }
        };
        startTimeBox.addActionListener(boxListener);
        endTimeBox = GuiUtils.makeComboBox(AnimationSetInfo.TIMEMODES,
                                           AnimationSetInfo.ENDMODELABELS,
                                           AnimationSetInfo.TIMEMODE_DATA);
        endTimeBox.addActionListener(boxListener);

        Date startDate = null;
        Date endDate   = null;
        try {
            //            DateTime[] times = animationWidget.getTimes();
            //            startDate = (times!=null&&times.length>0?ucar.visad.Util.makeDate(times[0]):null);
            //            endDate = (times!=null&&times.length>0?ucar.visad.Util.makeDate(times[times.length-1]):null);
        } catch (Exception exc) {
            //noop
        }
        startTimePicker = new DateTimePicker(startDate);
        endTimePicker   = new DateTimePicker(endDate);


        List comps = new ArrayList();

        JPanel startOffsetPanel = GuiUtils.hbox(new JLabel("  Offset: "),
                                      startOffsetField.getContents());
        JPanel endOffsetPanel = GuiUtils.hbox(new JLabel("  Offset: "),
                                    endOffsetField.getContents());

        JPanel startDetails = GuiUtils.hbox(startTimeBox, startOffsetPanel);
        JPanel endDetails   = GuiUtils.hbox(endTimeBox, endOffsetPanel);

        startExtra = new GuiUtils.CardLayoutPanel();
        endExtra   = new GuiUtils.CardLayoutPanel();


        JPanel startPickerPanel =
            GuiUtils.inset(GuiUtils.left(startTimePicker),
                           new Insets(5, 0, 0, 0));
        JPanel endPickerPanel = GuiUtils.inset(GuiUtils.left(endTimePicker),
                                    new Insets(5, 0, 0, 0));


        startDataModeLabel     = new JLabel(" ");
        endDataModeLabel       = new JLabel(" ");

        startRelativeModeLabel = new JLabel(" ");
        endRelativeModeLabel   = new JLabel(" ");

        startCurrentModeLabel  = new JLabel(" ");
        endCurrentModeLabel    = new JLabel(" ");


        startExtra.add("" + AnimationSetInfo.TIMEMODE_DATA,
                       startDataModeLabel);
        endExtra.add("" + AnimationSetInfo.TIMEMODE_DATA, endDataModeLabel);

        startExtra.add("" + AnimationSetInfo.TIMEMODE_CURRENT,
                       startCurrentModeLabel);
        endExtra.add("" + AnimationSetInfo.TIMEMODE_CURRENT,
                     endCurrentModeLabel);

        startExtra.add("" + AnimationSetInfo.TIMEMODE_RELATIVE,
                       startRelativeModeLabel);
        endExtra.add("" + AnimationSetInfo.TIMEMODE_RELATIVE,
                     endRelativeModeLabel);

        startExtra.add("" + AnimationSetInfo.TIMEMODE_FIXED,
                       startPickerPanel);
        endExtra.add("" + AnimationSetInfo.TIMEMODE_FIXED, endPickerPanel);

        comps.add(GuiUtils.rLabel("Start Time:"));
        comps.add(GuiUtils.left(GuiUtils.vbox(startDetails, startExtra)));
        comps.add(GuiUtils.rLabel("End Time:"));
        comps.add(GuiUtils.left(GuiUtils.vbox(endDetails, endExtra)));


        comps.add(addTo(GuiUtils.rLabel("Interval:"), intervalList));
        comps.add(GuiUtils.left(addTo(intervalField.getContents(),
                                      intervalList)));

        comps.add(GuiUtils.rLabel("Round To:"));
        comps.add(GuiUtils.left(roundToField.getContents()));


        JComponent pollIntervalPanel = GuiUtils.hbox(new Component[] {
                                           pollIntervalFld,
                                           GuiUtils.lLabel(" minutes") });
        comps.add(addTo(GuiUtils.rLabel("Refresh Rate:"), pollList));
        comps.add(GuiUtils.left(addTo(pollIntervalPanel, pollList)));
        GuiUtils.tmpInsets = new Insets(10, 5, 5, 5);
        setContents = GuiUtils.top(GuiUtils.doLayout(comps, 2,
                GuiUtils.WT_NN, GuiUtils.WT_N));
        return GuiUtils.vbox(
            GuiUtils.left(animationSetNotActiveBtn), GuiUtils.left(
                GuiUtils.hbox(
                    animationSetActiveBtn, predefinedBtn)), GuiUtils.inset(
                        GuiUtils.filler(), 5), setContents);

    }

    /** predefined methods */
    String[] PREDEFINED_METHODS = { "predefinedDataRange",
                                    "predefinedRealTime" };

    /** predefined labels */
    String[] PREDEFINED_LABELS = { "Uniform Across Data Range", "Real Time" };


    /**
     * Popup predefined menu
     */
    public void popupPredefinedMenu() {
        List items = new ArrayList();
        for (int i = 0; i < PREDEFINED_METHODS.length; i++) {
            items.add(GuiUtils.makeMenuItem(PREDEFINED_LABELS[i], this,
                                            PREDEFINED_METHODS[i]));
        }
        GuiUtils.showPopupMenu(items, predefinedBtn);
    }



    /** predefined interval field */
    TimeLengthField predefinedIntervalField;

    /**
     * Get the predefined interval component
     *
     * @return  the interval component
     */
    private JComponent getPredefinedInterval() {
        if (predefinedIntervalField == null) {
            predefinedIntervalField = new TimeLengthField("Interval", true,
                    false);
        }
        predefinedIntervalField.setTime(this.intervalField.getTime());
        return predefinedIntervalField.getFieldsComponent(true);
    }

    /**
     * Apply the predefined interval
     *
     * @return true if successful
     */
    private boolean applyPredefinedInterval() {
        if ( !predefinedIntervalField.applyFields(true)) {
            return false;
        }
        this.intervalField.setTime(predefinedIntervalField.getTime());
        return true;
    }


    /**
     * Set up the predefined data range
     */
    public void predefinedDataRange() {

        String label =
            "Configure the animation set to run from start time of data to end time with a uniform time step";
        List comps = new ArrayList();
        comps.add(GuiUtils.rLabel("Time Step Every:"));
        comps.add(getPredefinedInterval());
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
        while (true) {
            if ( !showPredefined(contents, label, "Uniform Data Range")) {
                return;
            }
            if (applyPredefinedInterval()) {
                break;
            }
        }
        startTimeBox.setSelectedIndex(AnimationSetInfo.TIMEMODE_DATA);
        endTimeBox.setSelectedIndex(AnimationSetInfo.TIMEMODE_DATA);
    }



    /**
     * Set up the predefined real time
     */
    public void predefinedRealTime() {
        TimeLengthField widthField = new TimeLengthField("Interval", true,
                                         false);
        widthField.setTime(startOffsetField.getTime());

        String label =
            "Configure the animation set to run from current time (now) back an amount of time";

        List comps = new ArrayList();
        JTextField pollIntervalFld =
            new JTextField(this.pollIntervalFld.getText(), 4);
        JComponent pollIntervalPanel = GuiUtils.hbox(new Component[] {
                                           pollIntervalFld,
                                           GuiUtils.lLabel(" minutes") });

        comps.add(GuiUtils.rLabel("Time Length:"));
        comps.add(GuiUtils.left(widthField.getFieldsComponent(true)));
        comps.add(GuiUtils.rLabel("Time Step Every:"));
        comps.add(getPredefinedInterval());
        comps.add(GuiUtils.rLabel("Update Every:"));
        comps.add(GuiUtils.left(pollIntervalPanel));

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);

        boolean ok = false;
        while ( !ok) {
            ok = true;
            if ( !showPredefined(contents, label, "Real Time")) {
                return;
            }
            if ( !applyPredefinedInterval()) {
                ok = false;
            }
            if ( !widthField.applyFields(true)) {
                ok = false;
            }
        }

        this.startOffsetField.setTime(-widthField.getTime());
        this.pollIntervalFld.setText(pollIntervalFld.getText());
        startTimeBox.setSelectedIndex(AnimationSetInfo.TIMEMODE_RELATIVE);
        endTimeBox.setSelectedIndex(AnimationSetInfo.TIMEMODE_CURRENT);
    }


    /** predefined dialog */
    JDialog predefinedDialog;

    /**
     * Show the predefined dialog
     *
     * @param contents  the contents
     * @param label  the label
     * @param title  the dialog title
     *
     * @return  true if successful
     */
    private boolean showPredefined(JComponent contents, String label,
                                   String title) {
        predefinedDialog = new JDialog(this, title, true);
        final boolean[] ok    = { false };
        JButton         okBtn = new JButton("OK");
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                ok[0] = true;
                predefinedDialog.dispose();
            }
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                predefinedDialog.dispose();
            }
        });
        JPanel buttons = GuiUtils.hbox(okBtn, cancelBtn, 5);

        JLabel lbl     = new JLabel(label);
        contents =
            GuiUtils.topCenterBottom(GuiUtils.inset(lbl, 10),
                                     GuiUtils.inset(contents, 10),
                                     GuiUtils.inset(GuiUtils.wrap(buttons),
                                         10));
        predefinedDialog.getContentPane().add(contents);
        predefinedDialog.setLocation(predefinedBtn.getLocationOnScreen());
        predefinedDialog.pack();
        predefinedDialog.show();
        return ok[0];
    }


    /**
     * Utility to convert string to double
     *
     * @param t string
     *
     * @return double
     */
    private double getNum(String t) {
        intervalText = t = t.trim();
        if (t.length() == 0) {
            return 0.0;
        }
        return new Double(t).doubleValue();
    }




    /**
     * Apply the properties
     *
     * @return success
     */
    protected boolean applyAnimationSetProperties() {
        AnimationSetInfo aniSet = myInfo.getAnimationSetInfo();
        aniSet.setActive(animationSetActiveBtn.isSelected());
        int startMode = GuiUtils.getValueFromBox(startTimeBox);
        int endMode   = GuiUtils.getValueFromBox(endTimeBox);
        if ((startMode == endMode)
                && (startMode == AnimationSetInfo.TIMEMODE_RELATIVE)) {
            LogUtil.userErrorMessage(
                "You cannot set both times to be relative");
            return false;
        }

        try {
            double startOffset = startOffsetField.getTime();
            double endOffset   = endOffsetField.getTime();

            if (startMode == AnimationSetInfo.TIMEMODE_RELATIVE) {
                while (startOffset > 0) {
                    String msg =
                        "<html>The start offset is postitive which will result in a start time after the end time<br>Do you want to change the value to:</html>";
                    startOffsetField.setTime(-startOffset);
                    if ( !startOffsetField.showDialog(msg)) {
                        startOffsetField.setTime(startOffset);
                        return false;
                    }
                    startOffset = startOffsetField.getTime();
                }
            }
            if (endMode == AnimationSetInfo.TIMEMODE_RELATIVE) {
                while (endOffset < 0) {
                    String msg =
                        "<html>The end offset is negative which will result in an end before the start time.<br>Do you want to change the value to:</html>";
                    endOffsetField.setTime(-endOffset);
                    if ( !endOffsetField.showDialog(msg)) {
                        endOffsetField.setTime(endOffset);
                        return false;
                    }
                    endOffset = endOffsetField.getTime();
                }
            }
            aniSet.setPollMinutes(getNum(pollIntervalFld.getText()));
            aniSet.setIntervalMinutes(intervalField.getTime());
            aniSet.setStartOffsetMinutes(startOffset);
            aniSet.setEndOffsetMinutes(endOffset);
            aniSet.setRoundTo(roundToField.getTime());
            aniSet.setStartMode(startMode);
            aniSet.setEndMode(endMode);
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad value:" + intervalText);
            return false;
        }


        try {
            if (aniSet.getStartMode() == AnimationSetInfo.TIMEMODE_FIXED) {
                aniSet.setStartFixedTime(startTimePicker.getDate());
            }
            if (aniSet.getEndMode() == AnimationSetInfo.TIMEMODE_FIXED) {
                aniSet.setEndFixedTime(endTimePicker.getDate());
            }
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad value:" + nfe);
            return false;
        }


        return true;
    }

    /**
     * Show the properties dialog
     */
    public void show() {
        updateTimeline();
        super.show();
    }



    /**
     * Enable/disable the gui
     */
    protected void checkEnabled() {
        startExtra.show("" + startTimeBox.getSelectedIndex());
        endExtra.show("" + endTimeBox.getSelectedIndex());


        int startMode = GuiUtils.getValueFromBox(startTimeBox);
        int endMode   = GuiUtils.getValueFromBox(endTimeBox);
        if (animationSetActiveBtn.isSelected()) {
            GuiUtils.enableTree(setContents, true);
            predefinedBtn.setEnabled(true);
            GuiUtils.enableComponents(pollList, (startMode
                    == AnimationSetInfo.TIMEMODE_CURRENT) || (endMode
                        == AnimationSetInfo.TIMEMODE_CURRENT));
            //            GuiUtils.enableTree(startTimePicker,
            //                                startMode == AnimationSetInfo.TIMEMODE_FIXED);
            //            GuiUtils.enableTree(endTimePicker,
            //                                endMode == AnimationSetInfo.TIMEMODE_FIXED);
        } else {
            GuiUtils.enableTree(setContents, false);
            predefinedBtn.setEnabled(false);
        }

    }

    /**
     * Update the gui
     */
    protected void updateAnimationSetProperties() {
        AnimationSetInfo aniSet = myInfo.getAnimationSetInfo();
        animationSetActiveBtn.setSelected(aniSet.getActive());
        animationSetNotActiveBtn.setSelected( !aniSet.getActive());
        GuiUtils.setValueOfBox(startTimeBox, aniSet.getStartMode(),
                               AnimationSetInfo.TIMEMODES,
                               AnimationSetInfo.STARTMODELABELS);
        GuiUtils.setValueOfBox(endTimeBox, aniSet.getEndMode(),
                               AnimationSetInfo.TIMEMODES,
                               AnimationSetInfo.ENDMODELABELS);

        intervalField.setTime(aniSet.getIntervalMinutes());
        startOffsetField.setTime(aniSet.getStartOffsetMinutes());
        endOffsetField.setTime(aniSet.getEndOffsetMinutes());
        roundToField.setTime(aniSet.getRoundTo());
        pollIntervalFld.setText("" + aniSet.getPollMinutes());
        startTimePicker.setDate(aniSet.getStartFixedDate());
        endTimePicker.setDate(aniSet.getEndFixedDate());
        checkEnabled();
    }




    /**
     * Make a text field for the dwell rates
     *
     * @return text field
     */
    private JTextField makeField() {
        JTextField textField = new JTextField("     ", 5);
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setSlidersFromFields();
                //                setScrollersFromFields();
            }
        });
        return textField;
    }



    /**
     * Something changed
     *
     * @param e event
     */
    public void stateChanged(ChangeEvent e) {
        checkEnabled();
    }

    /**
     * Public due to implementing of ActionListener
     *
     * @param evt  action event
     */
    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            if ( !applyAnimationSetProperties()) {
                return;
            }
            myInfo.setResetPolicy(
                TwoFacedObject.getIdString(resetCbx.getSelectedItem()));

            try {
                myInfo.setFwdSpeed(
                    (float) (Misc.parseValue(fwdField.getText().trim())));
                myInfo.setBackSpeed(
                    (float) (Misc.parseValue(backField.getText().trim())));
                myInfo.setStartDwell(
                    (float) (Misc.parseValue(startField.getText().trim())));
                myInfo.setEndDwell(
                    (float) (Misc.parseValue(endField.getText().trim())));
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Bad number format:" + nfe);
                return;
            }


            int dirIdx = dirCbx.getSelectedIndex();
            myInfo.setRocking(dirIdx == 2);
            myInfo.setDirection(dirIdx == 0);


            myInfo.setShared(shareAbsBtn.isSelected()
                             || shareRelBtn.isSelected());
            myInfo.setShareIndex(shareRelBtn.isSelected());
            myInfo.setBoxesVisible(boxesVisibleButton.isSelected());
            animationWidget.applySharableProperties();
            myInfo.setAnimationGroup(
                (String) animationWidget.getShareGroup());

            if (saveAsPrototypeCbx.isSelected()
                    && (Misc.getPrototypeManager() != null)) {
                Misc.getPrototypeManager().writePrototype(myInfo);
            }
            animationWidget.applyProperties(myInfo, false);
        }

        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_CANCEL)) {
            setVisible(false);
        }
    }




    /**
     * Set the position of the scrollers
     */
    private void setSlidersFromFields() {
        setSliderFromField(fwdField, fwdSlider);
        setSliderFromField(backField, backSlider);
        setSliderFromField(startField, startSlider);
        setSliderFromField(endField, endSlider);
    }



    /**
     * Set the positiojn of the scroller from the value of the field
     *
     * @param field The field
     * @param bar The scroller
     */
    private void setSliderFromField(JTextField field, JSlider bar) {
        try {
            double v = Math.max(0.01,
                                Misc.parseValue(field.getText().trim()));
            bar.setValue((int) (v * 100.0f));
            field.setText("" + v);
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad number format:"
                                     + field.getText().trim());
        }
    }





    /**
     * Set the value of the field from the scroller
     *
     * @param bar The scroller
     * @param field The field
     */
    private void setFieldFromSlider(JSlider bar, JTextField field) {
        // round off to nearest .05
        int rawValue = bar.getValue();
        rawValue = rawValue - rawValue % 5;
        field.setText((float) (rawValue / 100.0f) + "");
    }




    /**
     * set the gui widgets value.
     *
     *
     * @param transfer The info to use
     */
    protected void setInfo(AnimationInfo transfer) {
        myInfo.set(transfer);

        fwdField.setText("" + myInfo.getFwdSpeed());
        backField.setText("" + myInfo.getBackSpeed());
        startField.setText("" + myInfo.getStartDwell());
        endField.setText("" + myInfo.getEndDwell());
        setSlidersFromFields();
        //        setScrollersFromFields();



        if (myInfo.getRocking()) {
            dirCbx.setSelectedIndex(2);
        } else if (myInfo.getDirection()) {
            dirCbx.setSelectedIndex(0);
        } else {
            dirCbx.setSelectedIndex(1);
        }




        boolean shared     = myInfo.getShared();
        boolean shareIndex = myInfo.getShareIndex();

        shareAbsBtn.setSelected(shared && !shareIndex);
        shareRelBtn.setSelected(shared && shareIndex);
        shareNoneBtn.setSelected( !shared);
        boxesVisibleButton.setSelected(myInfo.getBoxesVisible());
        int idx =
            resetList.indexOf(new TwoFacedObject(myInfo.getResetPolicy()));
        if (idx >= 0) {
            resetCbx.setSelectedIndex(idx);
        }

        okToUpdate = false;
        updateAnimationSetProperties();
        okToUpdate = true;
        updateLabels();
    }


    /**
     * Update the timeline
     */
    private void updateTimeline() {
        try {
            List datedThings = DatedObject.wrap(
                                   ucar.visad.Util.makeDates(
                                       getAnimationTimes()));
            timeline.setDatedThings(datedThings, true);
        } catch (Exception exc) {
            LogUtil.logException("Error initializing dialog", exc);
        }

    }

    /**
     * Get the box panel
     *
     * @return The box panel
     */
    public AnimationBoxPanel getBoxPanel() {
        return boxPanel;
    }


}

/*
 * $Id: DataTimeRange.java,v 1.6 2007/05/09 21:59:26 dmurray Exp $
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

package ucar.unidata.data;


import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.ui.TimeLengthField;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class DataTimeRange specifies a time range
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.6 $
 */
public class DataTimeRange {


    /** Time range mode */
    public static final int MODE_DATA = 0;

    /** Time range mode */
    public static final int MODE_ANIMATION = 1;

    /** Time range mode */
    public static final int MODE_FIXED = 2;

    /** Time range mode */
    public static final int MODE_RELATIVE = 3;

    /** Time range mode */
    public static int[] MODES = { MODE_DATA, MODE_ANIMATION, MODE_RELATIVE,
                                  MODE_FIXED };

    /** Time range mode */
    public static String[] STARTMODELABELS = { "Use First Time from Data",
            "From Animation Time", "Relative to End Time", "Fixed:" };


    /** Time range mode */
    public static String[] ENDMODELABELS = { "Use Last Time from Data",
                                             "From Animation Time",
                                             "Relative to Start Time",
                                             "Fixed:" };

    /** dialog */
    private JDialog timeDialog;


    /** Start fixed time minutes */
    private long startFixedTime = Long.MAX_VALUE;

    /** End fixed time minutes */
    private long endFixedTime = Long.MAX_VALUE;


    /** offset */
    private double startOffsetMinutes = 0.0;

    /** offset */
    private double endOffsetMinutes = 0.0;


    /** How do we determine the start time */
    private int startMode = MODE_DATA;

    /** How do we determine the end time */
    private int endMode = MODE_DATA;


    /** widget */
    private JComboBox startTimeBox;

    /** widget */
    private JComboBox endTimeBox;


    /** widget */
    private DateTimePicker startTimePicker;

    /** widget */
    private DateTimePicker endTimePicker;

    /** widget */
    private TimeLengthField startOffsetField;

    /** widget */
    private TimeLengthField endOffsetField;

    /** real type to use */
    private RealType dataTimeRealType;


    /** widget */
    private JLabel timeModeLabel;

    /** is the label one line */
    private boolean oneLineLabel = false;

    /** Was ok pressed */
    private boolean dialogOk = false;

    /**
     * ctor
     */
    public DataTimeRange() {}


    /**
     * calculate time range
     *
     * @param dataStartTime data start time
     * @param dataEndTime data end time
     * @param aniValue animation time
     *
     * @return The time range
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Real[] getTimeRange(Real dataStartTime, Real dataEndTime,
                               Real aniValue)
            throws VisADException, RemoteException {
        double startDate =
            (double) dataStartTime.getValue(CommonUnit.secondsSinceTheEpoch);
        double endDate =
            (double) dataEndTime.getValue(CommonUnit.secondsSinceTheEpoch);

        double startOffsetTime = (double) (startOffsetMinutes * 60);
        double endOffsetTime   = (double) (endOffsetMinutes * 60);


        double animationTime   = endDate;
        if ((aniValue != null) && !aniValue.isMissing()) {
            animationTime =
                (double) aniValue.getValue(CommonUnit.secondsSinceTheEpoch);
        }




        if (startMode == MODE_DATA) {
            startDate = startDate + startOffsetTime;
        } else if (startMode == MODE_ANIMATION) {
            startDate = animationTime + startOffsetTime;
        } else if (startMode == MODE_FIXED) {
            startDate = startFixedTime / 1000 + startOffsetTime;
        }
        if (endMode == MODE_DATA) {
            endDate = endDate + endOffsetTime;
        } else if (endMode == MODE_ANIMATION) {
            endDate = animationTime + endOffsetTime;
        } else if (endMode == MODE_FIXED) {
            endDate = endFixedTime / 1000 + endOffsetTime;
        }


        if (startMode == MODE_RELATIVE) {
            startDate = endDate + startOffsetTime;
        }
        if (endMode == MODE_RELATIVE) {
            endDate = startDate + endOffsetTime;
        }


        return new Real[] {
            new Real(RealType.Time, startDate,
                     CommonUnit.secondsSinceTheEpoch),
            new Real(RealType.Time, endDate,
                     CommonUnit.secondsSinceTheEpoch) };
    }


    /**
     * get the time model label
     *
     * @return time model label
     */
    public JLabel getTimeModeLabel() {
        if (timeModeLabel == null) {
            timeModeLabel = new JLabel("");
        }
        setTimeModeLabel();
        return timeModeLabel;
    }

    /**
     * Set time mode text
     */
    private void setTimeModeLabel() {
        timeModeLabel.setText(getLabelText());
    }




    /**
     * Show the dialog
     *
     * @return success
     */
    public boolean showDialog() {
        timeDialog = new JDialog((Frame) null, "Time Settings", true);
        List           comps       = new ArrayList();
        ActionListener boxListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkTimeEnabled();
            }
        };
        startTimeBox = GuiUtils.makeComboBox(MODES, STARTMODELABELS,
                                             MODE_DATA);

        GuiUtils.setValueOfBox(startTimeBox, getStartMode(), MODES,
                               STARTMODELABELS);




        startTimeBox.addActionListener(boxListener);
        endTimeBox = GuiUtils.makeComboBox(MODES, ENDMODELABELS, MODE_DATA);
        GuiUtils.setValueOfBox(endTimeBox, getEndMode(), MODES,
                               ENDMODELABELS);
        endTimeBox.addActionListener(boxListener);

        startOffsetField = new TimeLengthField("Start Offset");
        endOffsetField   = new TimeLengthField("End Offset");

        startTimePicker  = new DateTimePicker();
        endTimePicker    = new DateTimePicker();

        JPanel startOffsetPanel = GuiUtils.hbox(new JLabel(" Offset: "),
                                      startOffsetField.getContents());
        JPanel endOffsetPanel = GuiUtils.hbox(new JLabel(" Offset: "),
                                    endOffsetField.getContents());


        JPanel startDetails = GuiUtils.hbox(startTimeBox, startOffsetPanel);
        JPanel endDetails = GuiUtils.hbox(endTimeBox, endOffsetPanel);


        JPanel startPickerPanel =
            GuiUtils.inset(GuiUtils.left(startTimePicker),
                           new Insets(5, 0, 0, 0));
        JPanel endPickerPanel = GuiUtils.inset(GuiUtils.left(endTimePicker),
                                    new Insets(5, 0, 0, 0));

        comps.add(GuiUtils.top(GuiUtils.rLabel("Start Time:")));
        comps.add(GuiUtils.left(GuiUtils.vbox(startDetails,
                startPickerPanel)));
        comps.add(GuiUtils.top(GuiUtils.rLabel("End Time:")));
        comps.add(GuiUtils.left(GuiUtils.vbox(endDetails, endPickerPanel)));





        GuiUtils.tmpInsets = new Insets(10, 5, 10, 5);
        JPanel contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                            GuiUtils.WT_N);


        dialogOk = false;
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    if ( !applyTimeProperties()) {
                        return;
                    }
                    dialogOk = true;
                    setTimeModeLabel();
                }
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    timeDialog.setVisible(false);
                    timeDialog.dispose();
                }

            }
        };


        startOffsetField.setTime(getStartOffsetMinutes());
        endOffsetField.setTime(getEndOffsetMinutes());
        startTimePicker.setDate(getStartFixedDate());
        endTimePicker.setDate(getEndFixedDate());

        timeDialog.getContentPane().add(
            GuiUtils.inset(
                GuiUtils.centerBottom(
                    contents,
                    GuiUtils.makeApplyOkCancelButtons(listener)), 5));

        checkTimeEnabled();
        timeDialog.pack();
        timeDialog.setVisible(true);

        return dialogOk;
    }




    /**
     * apply the properties
     *
     * @return success
     */
    public boolean applyTimeProperties() {
        double startOffset = startOffsetField.getTime();
        double endOffset   = endOffsetField.getTime();

        int    startMode   = GuiUtils.getValueFromBox(startTimeBox);
        int    endMode     = GuiUtils.getValueFromBox(endTimeBox);

        if ((startMode == endMode) && (startMode == MODE_RELATIVE)) {
            LogUtil.userErrorMessage(
                "You cannot set both times to be relative");
            return false;
        }




        if ((startMode == MODE_RELATIVE) && (startOffset >= 0)) {
            LogUtil.userErrorMessage("The start offset is "
                                     + ((startOffset == 0)
                                        ? "0"
                                        : "negative") + " which will result in\na start time "
                                        + ((startOffset == 0)
                                           ? "equal to"
                                           : "after") + " the end time.");
            return false;
        }
        if ((endMode == MODE_RELATIVE) && (endOffset <= 0)) {
            LogUtil.userErrorMessage("The end offset is " + ((endOffset == 0)
                    ? "0"
                    : "negative") + " which will result in\nan end time "
                                  + ((endOffset == 0)
                                     ? "equal to"
                                     : "before") + " the start time.");
            return false;
        }


        setStartFixedTime(startTimePicker.getDate());
        setEndFixedTime(endTimePicker.getDate());

        setStartMode(startMode);
        setEndMode(endMode);

        setStartOffsetMinutes(startOffset);
        setEndOffsetMinutes(endOffset);

        return true;
    }


    /**
     * Enable/disable gui components
     */
    private void checkTimeEnabled() {
        int startMode = GuiUtils.getValueFromBox(startTimeBox);
        int endMode   = GuiUtils.getValueFromBox(endTimeBox);
        GuiUtils.enableTree(startTimePicker, startMode == MODE_FIXED);
        GuiUtils.enableTree(endTimePicker, endMode == MODE_FIXED);
    }



    /**
     * set onelinelabel
     *
     * @param v one line label
     */
    public void setOneLineLabel(boolean v) {
        oneLineLabel = v;
    }

    /**
     * Label that describes the range
     *
     * @return Label
     */
    public String getLabelText() {
        String lbl = "<html>Start: ";
        if (startMode == MODE_DATA) {
            lbl += "from data ";
        } else if (startMode == MODE_ANIMATION) {
            lbl += "from animation ";
        } else if (startMode == MODE_FIXED) {
            lbl += "" + getStartFixedDate();
        } else {
            lbl += Math.abs(startOffsetMinutes) + " minutes before end";
        }
        if ((startOffsetMinutes != 0) && (startMode != MODE_RELATIVE)) {
            lbl += ((startOffsetMinutes > 0)
                    ? "+"
                    : "") + startOffsetMinutes + " minutes ";
        }


        if ( !oneLineLabel) {
            lbl += "<br>";
        } else {
            lbl += "&nbsp;&nbsp;&nbsp;";
        }

        lbl += " End: ";
        if (endMode == MODE_DATA) {
            lbl += "from data ";
        } else if (endMode == MODE_ANIMATION) {
            lbl += "from animation ";
        } else if (endMode == MODE_FIXED) {
            lbl += "" + getEndFixedDate();
        } else {
            lbl += endOffsetMinutes + " minutes after start";
        }
        if ((endOffsetMinutes != 0) && (endMode != MODE_RELATIVE)) {
            lbl += ((endOffsetMinutes > 0)
                    ? "+"
                    : "") + endOffsetMinutes + " minutes ";
        }

        lbl += "</html>";
        return lbl;
    }

    /**
     * Get a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        return getLabelText();

    }

    /**
     * Set the StartMode property.
     *
     * @param value The new value for StartMode
     */
    public void setStartMode(int value) {
        startMode = value;
    }

    /**
     * Get the StartMode property.
     *
     * @return The StartMode
     */
    public int getStartMode() {
        return startMode;
    }

    /**
     * Set the EndMode property.
     *
     * @param value The new value for EndMode
     */
    public void setEndMode(int value) {
        endMode = value;
    }

    /**
     * Get the EndMode property.
     *
     * @return The EndMode
     */
    public int getEndMode() {
        return endMode;
    }


    /**
     * Set the StartOffsetMinutes property.
     *
     * @param value The new value for StartOffsetMinutes
     */
    public void setStartOffsetMinutes(double value) {
        startOffsetMinutes = value;
    }

    /**
     * Get the StartOffsetMinutes property.
     *
     * @return The StartOffsetMinutes
     */
    public double getStartOffsetMinutes() {
        return startOffsetMinutes;
    }

    /**
     * Set the EndOffsetMinutes property.
     *
     * @param value The new value for EndOffsetMinutes
     */
    public void setEndOffsetMinutes(double value) {
        endOffsetMinutes = value;
    }

    /**
     * Get the EndOffsetMinutes property.
     *
     * @return The EndOffsetMinutes
     */
    public double getEndOffsetMinutes() {
        return endOffsetMinutes;
    }

    /**
     *  Set the StartFixedTime property.
     *
     *  @param value The new value for StartFixedTime
     */
    public void setStartFixedTime(long value) {
        startFixedTime = value;
    }

    /**
     * set the property
     *
     * @param d value
     */
    public void setStartFixedTime(Date d) {
        startFixedTime = d.getTime();
    }


    /**
     * set the property
     *
     * @param d value
     */
    public void setEndFixedTime(Date d) {
        endFixedTime = d.getTime();
    }

    /**
     * get the property
     *
     * @return the property
     */
    public Date getStartFixedDate() {
        return new Date(getStartFixedTime());
    }


    /**
     * get the property
     *
     * @return  the property
     */
    public Date getEndFixedDate() {
        return new Date(getEndFixedTime());
    }

    /**
     *  Get the StartFixedTime property.
     *
     *  @return The StartFixedTime
     */
    public long getStartFixedTime() {
        if (startFixedTime == Long.MAX_VALUE) {
            startFixedTime = System.currentTimeMillis();
        }
        return startFixedTime;
    }

    /**
     *  Set the EndFixedTime property.
     *
     *  @param value The new value for EndFixedTime
     */
    public void setEndFixedTime(long value) {
        endFixedTime = value;
    }

    /**
     *  Get the EndFixedTime property.
     *
     *  @return The EndFixedTime
     */
    public long getEndFixedTime() {
        if (endFixedTime == Long.MAX_VALUE) {
            endFixedTime = System.currentTimeMillis();
        }
        return endFixedTime;
    }



}


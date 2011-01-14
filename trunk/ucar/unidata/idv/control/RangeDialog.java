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

package ucar.unidata.idv.control;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Range;
import ucar.unidata.util.Removable;


import ucar.visad.display.*;

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.List;

import javax.swing.*;

import javax.swing.event.*;



/**
 * A JFrame widget to get range info from the user.
 *
 * The code to handle button events and actions is
 * in the event Listeners appearing in the constructor.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.10 $
 */
public class RangeDialog implements RangeWidget, Removable {


    /** The display */
    private DisplayControlImpl displayControl;

    /** String for default range */
    private final String CMD_RANGE_DEFAULT = "cmd.range.default";

    /** String for data range */
    private final String CMD_RANGE_DATA = "cmd.range.data";

    /** String for color table range */
    private final String CMD_RANGE_COLORTABLE = "cmd.range.colortable";

    /** for change range dialog */
    private JTextField rangeMinField;

    /** for change range dialog */
    private JTextField rangeMaxField;

    /** for change range dialog */
    private JButton rangePopupBtn;

    /** title for dialog */
    private String dialogTitle;

    /** title for dialog */
    private String setRangeMethod;

    /** Range for this */
    private Range range;

    /** component to position near */
    private Component launcher;

    /** dialog */
    private JDialog dialog;

    /** _more_          */
    private JComponent contents;


    /**
     * Construct the widget.
     *
     *
     * @param displayControl The display
     * @param r  the initial range
     * @param title  title for frame
     * @param setRangeMethod  the method to call on the displaycontrol for change
     */
    public RangeDialog(DisplayControlImpl displayControl, Range r,
                       String title, String setRangeMethod) {
        this(displayControl, r, title, setRangeMethod,
             displayControl.getWindow());
    }

    /**
     * Construct the widget.
     *
     *
     * @param displayControl The display
     * @param r  the initial range
     * @param title  title for frame
     * @param setRangeMethod  the method to call on the displaycontrol for change
     * @param launcher  a component to place this near (can be null);
     */
    public RangeDialog(DisplayControlImpl displayControl, Range r,
                       String title, String setRangeMethod,
                       Component launcher) {
        this.displayControl = displayControl;
        this.range          = r;
        this.dialogTitle    = title;
        this.setRangeMethod = setRangeMethod;
        this.launcher       = launcher;
        doMakeDialog();
    }



    /**
     * Change the range
     */
    public void doMakeDialog() {
        rangePopupBtn = GuiUtils.makeButton("Use Predefined", this,
                                            "rangePopup");
        rangeMinField = new JTextField(format(range.getMin()), 6);
        rangeMaxField = new JTextField(format(range.getMax()), 6);

        List comps = Misc.newList(new JLabel("New Range    From: "),
                                  rangeMinField, new JLabel("To: "),
                                  rangeMaxField,
                                  GuiUtils.inset(rangePopupBtn,
                                      new Insets(0, 5, 0, 0)));
        dialog = new JDialog((Frame) null, dialogTitle, true);
        JComponent     mainContents = GuiUtils.inset(GuiUtils.hflow(comps),
                                          5);

        ActionListener listener     = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    try {
                        Range newRange =
                            new Range(
                                Misc.parseNumber(rangeMinField.getText()),
                                Misc.parseNumber(rangeMaxField.getText()));
                        handleNewRange(newRange);
                    } catch (NumberFormatException pe) {
                        LogUtil.userMessage("Incorrect numeric format ");
                        return;
                    }
                }
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        };

        rangeMinField.addActionListener(listener);
        rangeMaxField.addActionListener(listener);
        rangeMinField.setActionCommand(GuiUtils.CMD_OK);
        rangeMaxField.setActionCommand(GuiUtils.CMD_OK);
        dialog.getContentPane().add(
            contents = GuiUtils.centerBottom(
                mainContents, GuiUtils.makeApplyOkCancelButtons(listener)));

        dialog.pack();
    }



    /**
     * Dispose of the dialog
     */
    public void doRemove() {
        if (contents != null) {
            GuiUtils.empty(contents, true);
        }
        if (dialog != null) {
            dialog.dispose();
        }
        dialog         = null;
        displayControl = null;
    }




    /**
     * Show the dialog
     */
    public void showDialog() {
        GuiUtils.showDialogNearSrc(launcher, dialog);
    }

    /**
     * Set the range in the dialog
     *
     * @param cmd Which one
     */
    public void setRangeFromPopup(String cmd) {
        Range r = null;
        if (cmd.equals(CMD_RANGE_COLORTABLE)) {
            ColorTable originalCT =
                displayControl.getOldColorTableOrInitialColorTable();
            if (originalCT != null) {
                r = originalCT.getRange();
            }
        } else if (cmd.equals(CMD_RANGE_DATA)) {
            r = displayControl.getColorRangeFromData();
        } else if (cmd.equals(CMD_RANGE_DEFAULT)) {
            try {
                r = displayControl.getColorRangeFromData();
                // r = displayControl.getInitialRange();
            } catch (Exception e) {}
        }
        setRangeDialog(r);
    }

    /**
     * Set the value in the min max fields in the range dialog
     *
     * @param r The range
     */
    public void setRangeDialog(Range r) {
        if (r != null) {
            rangeMinField.setText(format(r.getMin()));
            rangeMaxField.setText(format(r.getMax()));
        }
    }

    /**
     * Show the change range dialog
     */
    public void rangePopup() {
        try {
            Range ctRange = null;
            ColorTable originalCT =
                displayControl.getOldColorTableOrInitialColorTable();
            if (originalCT != null) {
                ctRange = originalCT.getRange();
            }
            List items = new ArrayList();
            items.add(GuiUtils.makeMenuItem("Default Range", this,
                                            "setRangeFromPopup",
                                            CMD_RANGE_DEFAULT));
            items.add(GuiUtils.makeMenuItem("From All Data", this,
                                            "setRangeFromPopup",
                                            CMD_RANGE_DATA));
            if (ctRange != null) {
                items.add(GuiUtils.makeMenuItem("From Color Table", this,
                        "setRangeFromPopup", CMD_RANGE_COLORTABLE));
            }
            displayControl.addToRangeMenu((RangeWidget) this, items);

            JPopupMenu popup = GuiUtils.makePopupMenu(items);
            Dimension  d     = rangePopupBtn.getSize();
            popup.show(rangePopupBtn, 0, d.height);
        } catch (Exception exc) {
            logException("Range popup", exc);
        }
    }

    /**
     * Get the display conventions from the contol
     *
     * @param value  the value to format
     * @return the formatted string
     */
    private String format(double value) {
        return displayControl.getDisplayConventions().format(value);
    }

    /**
     * Utility method to log an exception.
     *
     * @param message The message
     * @param exc The exception
     */
    public void logException(String message, Exception exc) {
        displayControl.logException(message, exc);
    }



    /**
     * Handle a new range
     *
     * @param newRange  new range
     */
    private void handleNewRange(Range newRange) {
        if (Misc.equals(range, newRange)) {
            return;
        }
        if (setRangeMethod != null) {
            try {
                Method theMethod = Misc.findMethod(displayControl.getClass(),
                                       setRangeMethod,
                                       new Class[] { newRange.getClass() });
                theMethod.invoke(displayControl, new Object[] { newRange });
            } catch (Exception exc2) {
                logException("propertyChange", exc2);
            }
        }
    }

}

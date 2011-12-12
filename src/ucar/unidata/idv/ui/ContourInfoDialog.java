/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import visad.Unit;

import visad.util.HersheyFont;

import java.awt.*;
import java.awt.event.*;

import java.util.Vector;

import javax.swing.*;


/**
 * A JFrame widget to get contouring info from the user.
 *
 * The code to handle button events and actions is
 * in the event Listeners appearing in the constructor.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.21 $
 */
public class ContourInfoDialog implements ActionListener {


    /** max allowable levels */
    private static final int MAX_LEVELS = 1500;


    /** Was the setting successful */
    private boolean ok;


    /** The dialog we show in */
    private JDialog dialog;

    /** contents */
    private JComponent contents;

    /** Holds the state */
    private ContourInfo myInfo;

    /** input field for the interval */
    private JTextField intervalFld;

    /** input field for min */
    private JTextField minFld;

    /** input field for max */
    private JTextField maxFld;

    /** input field for baseFld */
    private JTextField baseFld;

    /** checkbox for labeling */
    private JCheckBox toggleBtn;

    /** checkbox for dashing */
    private JCheckBox dashBtn;


    /** combobox for line width */
    private JComboBox widthBox;

    /** combobox for dash style */
    private JComboBox styleBox;

    /** font selector */
    private JComboBox fontBox;

    /** font size selector */
    private JComboBox fontSizeBox;

    /** font size selector */
    private JComboBox alignBox;

    /** label selectors */
    private JPanel labelPanel;

    /** title */
    private String title;

    /** contour alignments */
    private TwoFacedObject[] aligns = { new TwoFacedObject("Along Contours",
                                          new Boolean(true)),
                                        new TwoFacedObject("Horizontal",
                                            new Boolean(false)) };

    /**
     * Construct the widget.
     * with interval, min, max entry boxes
     * and ok and cancel buttons.
     *
     * @param title  title for frame
     * @param showApplyBtn Should we show the apply button
     */
    public ContourInfoDialog(String title, boolean showApplyBtn) {
        this(title, showApplyBtn, null);
    }


    /**
     * Construct the widget.
     * with interval, min, max entry boxes
     * and ok and cancel buttons.
     *
     * @param title  title for frame
     * @param showApplyBtn Should we show the apply button
     * @param unit Unit to show. May be null.
     */
    public ContourInfoDialog(String title, boolean showApplyBtn, Unit unit) {
        this(title, showApplyBtn, unit, true);
    }

    /**
     * ctor
     *
     * @param title  title for frame
     * @param showApplyBtn Should we show the apply button
     * @param unit Unit to show. May be null.
     * @param doDialog show as dialog
     */
    public ContourInfoDialog(String title, boolean showApplyBtn, Unit unit,
                             boolean doDialog) {
        this.title = title;
        doMakeContents(showApplyBtn, unit, doDialog);
    }



    /**
     * Utility to create a text field
     *
     * @return The field
     */
    private JTextField makeField() {
        JTextField fld = new JTextField("", 6);
        fld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (doApply()) {
                    dialog.setVisible(false);
                    ok = true;
                }
            }
        });

        return fld;
    }

    /**
     * Initialize the contents
     *
     * @param showApplyBtn Should we show the apply button
     * @param unit The unit to show as a label.
     * @param doDialog show as dialog
     */
    private void doMakeContents(boolean showApplyBtn, Unit unit,
                                boolean doDialog) {
        String labelString = "";
        if (unit != null) {
            labelString = " " + unit.toString() + " ";
        }


        dashBtn = new JCheckBox("Dash:");
        dashBtn.setToolTipText("Dash contour lines if less than base");

        styleBox = new JComboBox(new String[] { "_ _ _", ".....", "_._._" });
        styleBox.setMaximumSize(new Dimension(30, 16));
        styleBox.setToolTipText("Set the line style");
        Font f = Font.decode("monospaced-BOLD");
        if (f != null) {
            styleBox.setFont(f);
        }

        dashBtn = new JCheckBox("Dash:");
        dashBtn.setToolTipText("Dash contour lines if less than base");
        dashBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                styleBox.setEnabled(((JCheckBox) e.getSource()).isSelected());
            }
        });
        Vector   fonts  = GuiUtils.getFontList();
        String[] hFonts = visad.util.TextControlWidget.getHersheyFontNames();
        for (int i = 0; i < hFonts.length; i++) {
            fonts.add(makeTwoFacedFont(new HersheyFont(hFonts[i])));
        }
        fonts.insertElementAt(makeTwoFacedFont(null), 0);
        fontBox = new JComboBox(fonts);
        fontBox.setToolTipText("Set the contour label font");
        fontSizeBox = GuiUtils.doMakeFontSizeBox(12);
        fontSizeBox.setToolTipText("Set the contour label size");
        alignBox = new JComboBox(aligns);
        alignBox.setToolTipText("Set the contour label alignment");

        Component[] labelcomps = new Component[] {
            GuiUtils.rLabel("Font:"), GuiUtils.left(fontBox),
            GuiUtils.rLabel("Size:"), GuiUtils.left(fontSizeBox),
            GuiUtils.rLabel("Align:"), GuiUtils.left(alignBox)
        };
        labelPanel = GuiUtils.doLayout(labelcomps, 2, GuiUtils.WT_NY,
                                       GuiUtils.WT_N);

        toggleBtn = new JCheckBox("Labels:", true);
        toggleBtn.setToolTipText("Toggle contour labels");
        toggleBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                GuiUtils.enableTree(labelPanel,
                                    ((JCheckBox) e.getSource()).isSelected());
            }
        });

        Component[] comps = new Component[] {
            GuiUtils.rLabel("Contour Interval:"),
            GuiUtils.centerRight(intervalFld = makeField(),
                                 new JLabel(labelString)),
            GuiUtils.rLabel("Base Contour:"),
            GuiUtils.centerRight(baseFld = makeField(),
                                 new JLabel(labelString)),
            GuiUtils.rLabel("Minimum Value:"),
            GuiUtils.centerRight(minFld = makeField(),
                                 new JLabel(labelString)),
            GuiUtils.rLabel("Maximum Value:"),
            GuiUtils.centerRight(maxFld = makeField(),
                                 new JLabel(labelString)),
            GuiUtils.rLabel("Line Width:"),
            widthBox = GuiUtils.createValueBox(this, "lineWidth", 1,
                Misc.createIntervalList(1, 5, 1), true),
            GuiUtils.right(dashBtn), styleBox, GuiUtils.right(toggleBtn),
            labelPanel
        };

        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel p1 = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                      GuiUtils.WT_N);
        contents = p1;


        /*
        contents = GuiUtils.vbox(p1,
                                 GuiUtils.hflow(Misc.newList(toggleBtn,
                                     dashBtn, styleBox)));
        */
        JPanel buttons = (showApplyBtn
                          ? GuiUtils.makeApplyOkCancelButtons(this)
                          : GuiUtils.makeOkCancelButtons(this));
        if (doDialog) {
            this.dialog = GuiUtils.createDialog(title, true);
            dialog.getContentPane().add(GuiUtils.centerBottom(contents,
                    buttons));
            GuiUtils.packInCenter(dialog);
        }
    }


    /**
     * get the gui contents
     *
     * @return gui contents
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * get the label widget contents
     *
     * @return label widget contents
     */
    public JComponent getLabelPanel() {
        return labelPanel;
    }

    /**
     * Apply the state to the display
     *
     * @return Was this successful
     */
    public boolean doApply() {
        try {
            String intoStr = intervalFld.getText();
            float  into    = Float.NaN;
            // check to see if they typed in an interval or range
            if ( !ContourInfo.isIrregularInterval(intoStr)) {
                into    = (float) Misc.parseNumber(intoStr);
                intoStr = null;
            }

            float baso = (float) Misc.parseNumber(baseFld.getText());
            float mino = (float) Misc.parseNumber(minFld.getText());
            float maxo = (float) Misc.parseNumber(maxFld.getText());

            // trap bad values and attempt to fix them
            if ( !Float.isNaN(into) && (into < 0.0)) {
                into *= -1.0;
            }

            if ( !Float.isNaN(into) && (mino > maxo)) {
                maxo = mino + into;
            }

            // permit mino == maxo the case of one contour line
            // now set the data of the ContourInfo
            myInfo.setInterval(into);
            myInfo.setLevelsString(
                ContourInfo.cleanupUserLevelString(intoStr));
            myInfo.setBase(baso);
            myInfo.setMin(mino);
            myInfo.setMax(maxo);
            myInfo.setDashOn(dashBtn.isSelected());
            myInfo.setIsLabeled(toggleBtn.isSelected());
            myInfo.setLineWidth(
                new Integer(
                    widthBox.getSelectedItem().toString()).intValue());
            myInfo.setDashedStyle(styleBox.getSelectedIndex() + 1);
            // sanity check
            float[] levels = myInfo.getContourLevels();
            if (levels.length > MAX_LEVELS) {
                LogUtil.userErrorMessage(
                    "Contour interval too small for range");
                return false;
            }
            myInfo.setFont(
                ((TwoFacedObject) fontBox.getSelectedItem()).getId());
            myInfo.setLabelSize(
                ((Integer) fontSizeBox.getSelectedItem()).intValue());
            myInfo.setAlignLabels(((Boolean) ((TwoFacedObject) alignBox
                .getSelectedItem()).getId()).booleanValue());
            return true;
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Incorrect number format");
            return false;
        } catch (Exception exc) {
            LogUtil.logException("Setting contours", exc);
            return false;
        }
    }

    /**
     * Show the dialog box and wait for results and deal with them
     * (ok or cancel).
     * @param evt ActionEvent
     */
    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            if ( !doApply()) {
                return;
            }
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            ok = false;
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            ok = true;
        }

        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            dialog.setVisible(false);
        }
    }

    /**
     * Show the dialog box and wait for results and deal with them.
     * @param transfer a ContourInfo data object to transfer all values
     * @return boolean if the user enterd data ok.
     */
    public boolean showDialog(ContourInfo transfer) {
        ok = true;
        setState(transfer);
        dialog.setVisible(true);
        return ok;
    }

    /**
     * Show the dialog box and wait for results and deal with them.
     * @param transfer a ContourInfo data object to transfer all values
     */
    public void setState(ContourInfo transfer) {
        myInfo = transfer;
        intervalFld.setText(transfer.getIntervalString());
        baseFld.setText(Misc.format(transfer.getBase()));
        minFld.setText(Misc.format(transfer.getMin()));
        maxFld.setText(Misc.format(transfer.getMax()));
        toggleBtn.setSelected(myInfo.getIsLabeled());
        //toggleBtn.setEnabled( !myInfo.getIsFilled());
        dashBtn.setSelected(myInfo.getDashOn());
        dashBtn.setEnabled( !myInfo.getIsFilled());
        widthBox.setSelectedItem(new Integer(myInfo.getLineWidth()));
        styleBox.setSelectedIndex(myInfo.getDashedStyle() - 1);
        styleBox.setEnabled(myInfo.getDashOn());
        fontBox.setSelectedItem(makeTwoFacedFont(myInfo.getFont()));
        fontSizeBox.setSelectedItem(new Integer(myInfo.getLabelSize()));
        alignBox.setSelectedItem((myInfo.getAlignLabels())
                                 ? aligns[0]
                                 : aligns[1]);
    }

    /**
     * Make a consistent TwoFacedFont object
     *
     * @param font  the font object (null, Font or HersheyFont)
     *
     * @return the corresponding TFO
     */
    private TwoFacedObject makeTwoFacedFont(Object font) {
        if (font == null) {
            return new TwoFacedObject("Default", font);
        } else if (font instanceof Font) {
            return GuiUtils.makeTwoFacedFont((Font) font);
        } else if (font instanceof HersheyFont) {
            return new TwoFacedObject(font.toString(), font);
        }
        return null;
    }

    /**
     * Get the contour font (Font or HersheyFont or null)
     *
     * @param fontSpec  string or Font/HersheyFont
     *
     * @return  the font (Font, HersheyFont or null)
     */
    public static Object getContourFont(Object fontSpec) {
        Object font = fontSpec;
        if ((fontSpec != null)
                && !((fontSpec instanceof Font)
                     || (fontSpec instanceof HersheyFont))) {
            String fontName = fontSpec.toString();
            try {
                if (fontName.startsWith("HersheyFont:")) {
                    font = new HersheyFont(
                        fontName.substring(fontName.indexOf(" ") + 1));
                } else {
                    // Default list has point size of 1
                    fontName = fontName + "-1";
                    font     = Font.decode(fontName);
                }
            } catch (Exception e) {
                font = null;
            }
        }
        return font;
    }




    /**
     * Get the info
     *
     * @return The info
     */
    public ContourInfo getInfo() {
        return myInfo;
    }

}

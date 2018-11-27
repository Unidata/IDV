/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

import visad.ContourControl;
import visad.Unit;

import visad.util.HersheyFont;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;


/**
 * A JFrame widget to get contouring info from the user.
 *
 * The code to handle button events and actions is
 * in the event Listeners appearing in the constructor.
 *
 * @author Unidata Development Team
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

    /** label frequency */
    private JSlider labelFreqSlider;

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

    /** current action command */
    private String current_action_command = null;

    /** input spinner for contour label line skip count */
    private JSpinner js = null;

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

        labelFreqSlider = new JSlider(JSlider.HORIZONTAL,
                                      ContourControl.LABEL_FREQ_LO,
                                      ContourControl.LABEL_FREQ_HI, 2);
        labelFreqSlider.setMajorTickSpacing(2);
        labelFreqSlider.setPaintTicks(true);
        // Create the label table
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer,
                                                    JLabel>();
        labelTable.put(new Integer(ContourControl.LABEL_FREQ_LO),
                       new JLabel("Lo"));
        labelTable.put(new Integer(ContourControl.LABEL_FREQ_MED),
                       new JLabel("Med"));
        labelTable.put(new Integer(ContourControl.LABEL_FREQ_HI),
                       new JLabel("Hi"));
        labelFreqSlider.setLabelTable(labelTable);
        labelFreqSlider.setPaintLabels(true);
        labelFreqSlider.setSnapToTicks(true);
        // initialize to min value
        labelFreqSlider.setValue(ContourControl.LABEL_FREQ_LO);

        // TJJ Oct 2013
        // Use a spinner here, best choice for the UI since 
        //  - No error checking needed
        //  - Almost always, user will want a small integer value

        Integer defaultInterval =
            new Integer(ContourControl.EVERY_NTH_DEFAULT);
        Integer minInterval  = new Integer(1);
        Integer maxInterval  = new Integer(ContourControl.EVERY_NTH_MAX);
        Integer intervalStep = new Integer(1);
        SpinnerNumberModel snm = new SpinnerNumberModel(defaultInterval,
                                     minInterval, maxInterval, intervalStep);
        js = new JSpinner(snm);
        // grab the underlying text field temporarily so we can disable hand-editing
        JFormattedTextField jftfLineSkip = getTextField(js);
        if (jftfLineSkip != null) {
            jftfLineSkip.setColumns(3);
            jftfLineSkip.setEditable(false);
        }

        toggleBtn = new JCheckBox("Labels:", true);
        toggleBtn.setToolTipText("Toggle contour labels");

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
            GuiUtils.right(dashBtn), styleBox,
            GuiUtils.right(GuiUtils.right(toggleBtn)), new JLabel(""),
            GuiUtils.rLabel("Font:"), GuiUtils.center(fontBox),
            GuiUtils.rLabel("Size:"), GuiUtils.center(fontSizeBox),
            GuiUtils.rLabel("Align:"), GuiUtils.center(alignBox),
            GuiUtils.rLabel("Frequency:"), GuiUtils.center(labelFreqSlider),
            GuiUtils.rLabel("Label Every Nth Line:"), GuiUtils.center(js)
        };

        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel p1 = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                      GuiUtils.WT_N);
        contents = p1;

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

            boolean any_changed      = false;
            float   cur_interval     = myInfo.getInterval();
            String  cur_lev_string   = myInfo.getLevelsString();
            float   cur_base         = myInfo.getBase();
            float   cur_min          = myInfo.getMin();
            float   cur_max          = myInfo.getMax();
            boolean cur_dash_on      = myInfo.getDashOn();
            boolean cur_isLabelled   = myInfo.getIsLabeled();
            int     cur_line_width   = myInfo.getLineWidth();
            int     cur_dash_style   = myInfo.getDashedStyle();
            Object  cur_font         = myInfo.getFont();
            int     cur_font_size    = myInfo.getLabelSize();
            int     cur_label_freq   = myInfo.getLabelFreq();
            int     cur_line_skip    = myInfo.getLabelLineSkip();
            boolean cur_align_labels = myInfo.getAlignLabels();

            String new_lev_string =
                ContourInfo.cleanupUserLevelString(intoStr);
            boolean new_dash_on    = dashBtn.isSelected();
            boolean new_isLabelled = toggleBtn.isSelected();
            int new_line_width =
                new Integer(widthBox.getSelectedItem().toString()).intValue();
            int new_dash_style = styleBox.getSelectedIndex() + 1;
            Object new_font =
                ((TwoFacedObject) fontBox.getSelectedItem()).getId();
            int new_font_size =
                ((Integer) fontSizeBox.getSelectedItem()).intValue();
            boolean new_align_labels =
                ((Boolean) ((TwoFacedObject) alignBox.getSelectedItem())
                    .getId()).booleanValue();
            int new_label_freq = labelFreqSlider.getValue();
            int new_line_skip  = ((Number) js.getValue()).intValue();

            // permit mino == maxo the case of one contour line
            // now set the data of the ContourInfo
            if (0 != Float.compare(cur_interval, into)) {
                myInfo.setInterval(into);
                any_changed = true;
            }
            /*myInfo.setLevelsString(
                ContourInfo.cleanupUserLevelString(intoStr));*/
            if ( !Misc.equals(cur_lev_string, new_lev_string)) {
                myInfo.setLevelsString(new_lev_string);
                any_changed = true;
            }
            if (0 != Float.compare(cur_base, baso)) {
                myInfo.setBase(baso);
                any_changed = true;
            }
            if (0 != Float.compare(cur_min, mino)) {
                myInfo.setMin(mino);
                any_changed = true;
            }
            if (0 != Float.compare(cur_max, maxo)) {
                myInfo.setMax(maxo);
                any_changed = true;
            }
            //myInfo.setDashOn(dashBtn.isSelected());
            if (cur_dash_on != new_dash_on) {
                myInfo.setDashOn(new_dash_on);
                any_changed = true;
            }
            //myInfo.setIsLabeled(toggleBtn.isSelected());
            if (cur_isLabelled != new_isLabelled) {
                myInfo.setIsLabeled(new_isLabelled);
                any_changed = true;
            }
            //myInfo.setLineWidth(new Integer(widthBox.getSelectedItem().toString()).intValue());
            if (cur_line_width != new_line_width) {
                myInfo.setLineWidth(new_line_width);
                any_changed = true;
            }
            //myInfo.setDashedStyle(styleBox.getSelectedIndex() + 1);
            if (cur_dash_style != new_dash_style) {
                myInfo.setDashedStyle(new_dash_style);
                any_changed = true;
            }
            // sanity check
            float[] levels = myInfo.getContourLevels();
            if (levels.length > MAX_LEVELS) {
                LogUtil.userErrorMessage(
                    "Contour interval too small for range");
                return false;
            }
            if ( !Misc.equals(cur_font, new_font)) {
                myInfo.setFont(new_font);
                any_changed = true;
            }
            if (cur_font_size != new_font_size) {
                myInfo.setLabelSize(new_font_size);
                any_changed = true;
            }
            if (cur_align_labels != new_align_labels) {
                myInfo.setAlignLabels(new_align_labels);
                any_changed = true;
            }
            if (cur_label_freq != new_label_freq) {
                myInfo.setLabelFreq(new_label_freq);
                any_changed = true;
            }
            if (cur_line_skip != new_line_skip) {
                myInfo.setLabelLineSkip(new_line_skip);
                any_changed = true;
            }
            /*myInfo.setFont(
                ((TwoFacedObject) fontBox.getSelectedItem()).getId());
            myInfo.setLabelSize(
                ((Integer) fontSizeBox.getSelectedItem()).intValue());
            myInfo.setLabelFreq(labelFreqSlider.getValue());
            myInfo.setAlignLabels(((Boolean) ((TwoFacedObject) alignBox
                .getSelectedItem()).getId()).booleanValue());
            return true;*/
            if ((current_action_command != null) && (dialog != null)
                    && current_action_command.equals(GuiUtils.CMD_OK)) {
                dialog.setVisible(false);
            }

            return any_changed;
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
        current_action_command = cmd;
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
     * Return the formatted text field used by the editor, or null if the editor
     * doesn't descend from JSpinner.DefaultEditor.
     *
     * @param spinner the spinner
     * @return the text field
     */
    public JFormattedTextField getTextField(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor) editor).getTextField();
        } else {
            LogUtil.message("Unexpected editor type: "
                            + spinner.getEditor().getClass()
                            + " isn't a descendant of DefaultEditor");
            return null;
        }
    }

    /**
     * Show the dialog box and wait for results and deal with them.
     * @param transfer a ContourInfo data object to transfer all values
     * @return boolean if the user entered data ok.
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
        labelFreqSlider.setValue(transfer.getLabelFreq());
        js.setValue(transfer.getLabelLineSkip());
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

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


import ucar.unidata.ui.FontSelector;


import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.display.ColorScale;
import ucar.visad.display.ColorScaleInfo;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.swing.*;

import javax.swing.event.*;


/**
 * A JFrame widget to get color scale info from the user.
 *
 * The code to handle button events and actions is
 * in the event Listeners appearing in the constructor.
 *
 * @author Unidata Development Team
 */
public class ColorScaleDialog implements ActionListener {

    /** Was the setting successful */
    private boolean ok;

    /** the UI contents */
    private JPanel contents;

    /** The dialog we show in */
    private JDialog dialog;

    /** Holds the state */
    private ColorScaleInfo myInfo;

    /** combobox for the orientation TODO: implement this */
    private JComboBox orientationBox;

    /** combobox for the placement */
    private JComboBox placementBox;

    /** combobox for the label color */
    private GuiUtils.ColorSwatch colorSwatch;

    /** checkbox for visibility */
    private JCheckBox visibilityCbx;

    /** checkbox for label visibility */
    private JCheckBox labelVisibilityCbx;

    /** checkbox for including transparency */
    private JCheckBox alphaCbx;

    /** The display */
    private DisplayControlImpl displayControl;

    /** The Font selector */
    private FontSelector fontSelector = null;

    /** list of orienations */
    private final static String[] positions = new String[] {
                                                  ColorScaleInfo.TOP,
            ColorScaleInfo.BOTTOM, ColorScaleInfo.LEFT,
            ColorScaleInfo.RIGHT };

    /**
     * Construct the widget.
     * with interval, min, max entry boxes
     * and ok and cancel buttons.
     *
     * @param displayControl The display
     * @param title  title for frame
     * @param info   the color scale info
     * @param showDialog  true to show the dialog
     */
    public ColorScaleDialog(DisplayControlImpl displayControl, String title,
                            ColorScaleInfo info, boolean showDialog) {
        ok                  = false;
        this.displayControl = displayControl;


        myInfo              = new ColorScaleInfo(info);

        if (showDialog) {
            dialog = GuiUtils.createDialog(((displayControl != null)
                                            ? displayControl.getWindow()
                                            : null), title, true);
        }

        doMakeContents(showDialog);
        String place = myInfo.getPlacement();
        // account for old bundles
        if (place != null) {
            placementBox.setSelectedItem(place);
        }
        //orientationBox.setSelectedItem(myInfo.getOrientation());
        visibilityCbx.setSelected(myInfo.getIsVisible());
        labelVisibilityCbx.setSelected(myInfo.getLabelVisible());
        alphaCbx.setSelected(myInfo.getUseAlpha());

        if (showDialog) {
            dialog.setVisible(true);
        }


    }

    /**
     * Get the main contents of the dialog
     *
     * @return  the contents
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * Dispose of the dialog
     */
    protected void dispose() {
        displayControl = null;
        if (dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }

    /**
     * Apply the state to the display
     *
     * @return Was this successful
     */
    protected boolean doApply() {
        //myInfo.setOrientation(GuiUtils.getValueFromBox(orientationBox));
        String place = (String) placementBox.getSelectedItem();
        if (place != null) {
            myInfo.setPlacement(place);
        }
        myInfo.setLabelColor(colorSwatch.getSwatchColor());
        myInfo.setIsVisible(visibilityCbx.isSelected());
        myInfo.setLabelVisible(labelVisibilityCbx.isSelected());
        myInfo.setUseAlpha(alphaCbx.isSelected());
        myInfo.setLabelFont(fontSelector.getFont());
        try {
            if (displayControl != null) {
                displayControl.setColorScaleInfo(
                    new ColorScaleInfo(getInfo()));
            }
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Setting color scale info", exc);
            return false;
        }
    }

    /**
     * Initialize the contents
     *
     * @param showDialog  true to show the dialog
     */
    private void doMakeContents(boolean showDialog) {
        placementBox = new JComboBox(positions);
        colorSwatch = new GuiUtils.ColorSwatch(myInfo.getLabelColor(),
                "Color Scale Label Color");
        final JComponent colorComp = colorSwatch.getSetPanel();
        visibilityCbx = new JCheckBox("", myInfo.getIsVisible());
        alphaCbx = new JCheckBox("", myInfo.getUseAlpha());
        fontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false,
                                        false);
        fontSelector.setFont(myInfo.getLabelFont());

        labelVisibilityCbx = new JCheckBox("Visible",
                                           myInfo.getLabelVisible());
        labelVisibilityCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean showLabel = ((JCheckBox) e.getSource()).isSelected();
                GuiUtils.enableTree(fontSelector.getComponent(), showLabel);
                GuiUtils.enableTree(colorComp, showLabel);
            }
        });
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        contents           = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Visible: "), visibilityCbx,
            //GuiUtils.leftRight(visibilityCbx, GuiUtils.flow( new Component[] {GuiUtils.rLabel("  Show Transparency: "), alphaCbx})),
            GuiUtils.rLabel("Position: "),
            GuiUtils.leftRight(placementBox, GuiUtils.filler()),
            GuiUtils.rLabel("Labels: "),
            GuiUtils.leftRight(labelVisibilityCbx, GuiUtils.filler()),
            GuiUtils.filler(),
            GuiUtils.leftRight(GuiUtils.rLabel("Font: "),
                               fontSelector.getComponent()),
            GuiUtils.filler(),
            GuiUtils.leftRight(GuiUtils.rLabel("Color: "),
                               GuiUtils.leftRight(colorComp,
                                   GuiUtils.filler())),
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        contents = GuiUtils.leftRight(contents, GuiUtils.filler());
        if (showDialog) {
            JPanel buttons;
            if (displayControl != null) {
                buttons = GuiUtils.makeApplyOkCancelButtons(this);
            } else {
                buttons = GuiUtils.makeOkCancelButtons(this);
            }
            dialog.getContentPane().add(GuiUtils.centerBottom(contents,
                    buttons));
            GuiUtils.packInCenter(dialog);
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
     * Get the info
     *
     * @return The info
     */
    public ColorScaleInfo getInfo() {
        return myInfo;
    }

    /**
     * get the display list font
     *
     * @return the font or null
     */
    private Font getDisplayListFont() {
        if (displayControl == null) {
            return null;
        }
        Font f    = displayControl.getViewManager().getDisplayListFont();
        int  size = (f == null)
                    ? 12
                    : f.getSize();
        if ((f != null) && f.getName().equals(FontSelector.DEFAULT_NAME)) {
            f = null;
        }
        return f;
    }

    /**
     * Was ok pressed
     *
     * @return was ok pressed
     */
    public boolean getOk() {
        return ok;
    }


}

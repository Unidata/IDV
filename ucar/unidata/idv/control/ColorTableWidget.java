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
import ucar.unidata.ui.colortable.ColorTableCanvas;
import ucar.unidata.ui.colortable.ColorTableEditor;


import ucar.unidata.ui.colortable.ColorTableManager;


import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.Removable;
import ucar.unidata.util.TwoFacedObject;

import visad.VisADException;

import visad.util.BaseRGBMap;

import visad.util.ColorPreview;

import java.awt.*;
import java.awt.event.*;



import java.awt.geom.Rectangle2D;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * A {@link ControlWidget} for the color table information in the
 * control window
 *
 * @author  Jeff McWhirter
 * @version $Revision: 1.105 $
 */
public class ColorTableWidget extends ControlWidget implements PropertyChangeListener,
        RangeWidget, Removable {


    /** This defines which color table we are editing */
    private String whichColorTable = "default";

    /** color table editor */
    private ColorTableEditor myEditor;

    /** range for colors */
    Range range;

    /** list of color previews */
    List colorPreviews = new ArrayList();

    /** color table */
    ColorTable colorTable;

    /** old (original) color table */
    ColorTable oldTable;

    /** old (original) range */
    Range oldRange;

    /** popup button */
    JButton popupBtn;


    /** for change range dialog_ */
    private JTextField rangeMinField;

    /** for change range dialog_ */
    private JTextField rangeMaxField;

    /** for change range dialog_ */
    private JButton rangePopupBtn;



    /** String for default range */
    private final String CMD_RANGE_DEFAULT = "cmd.range.default";

    /** String for data range */
    private final String CMD_RANGE_DATA = "cmd.range.data";

    /** String for color table range */
    private final String CMD_RANGE_COLORTABLE = "cmd.range.colortable";

    /** The color table manager */
    private ColorTableManager colorTableManager;


    /**
     * Construct a ColorTableWidget
     *
     * @param theDisplayControl   the associated DisplayControl
     * @param colorTableManager   the color table manager
     * @param ct                  the color table
     * @param r                   range for the table
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public ColorTableWidget(DisplayControlImpl theDisplayControl,
                            ColorTableManager colorTableManager,
                            ColorTable ct, Range r)
            throws RemoteException, VisADException {
        super(theDisplayControl);
        this.colorTableManager = colorTableManager;
        init(theDisplayControl, ct, r);
    }



    /**
     * Construct a ColorTableWidget
     *
     * @param theDisplayControl   the associated DisplayControl
     * @param ct                  the color table
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public ColorTableWidget(DisplayControlImpl theDisplayControl,
                            ColorTable ct)
            throws RemoteException, VisADException {
        super(theDisplayControl);
        init(theDisplayControl, ct, null);
    }


    /**
     * Initialize the class
     *
     * @param theDisplayControl   the associated DisplayControl
     * @param ct                  the color table
     * @param r                   range for the table
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void init(DisplayControlImpl theDisplayControl, ColorTable ct,
                      Range r)
            throws RemoteException, VisADException {
        this.colorTable = new ColorTable(ct);
        if (colorTable == null) {
            colorTable = colorTableManager.getDefaultColorTable();
        }

        //Set the range here after we have created the labels so we set the text on them
        setRange(r);

        popupBtn = new JButton(colorTable.getName());
        popupBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showMenu(popupBtn);
            }
        });
    }

    /**
     * Create the preview for the color table
     *
     * @param forMain         true if for the main display
     * @param legendType      type of legend
     * @see DisplayControl#BOTTOM_LEGEND
     * @see DisplayControl#SIDE_LEGEND
     *
     * @return The preview component
     */
    private JComponent doMakePreview(boolean forMain, int legendType) {
        try {
            MyColorPreview preview =
                new MyColorPreview(this,
                                   (List<Color>) colorTable.getColorList(),
                                   forMain, legendType);
            colorPreviews.add(preview);
            preview.setRange(range, displayControl.getColorUnit());
            return preview.doMakeContents();
        } catch (Exception exc) {
            logException("Creating preview panel", exc);
        }
        return new JPanel();
    }

    /**
     * Class MyColorPreview
     */
    private static class MyColorPreview extends RangeColorPreview implements KeyListener {

        /** the color table widget */
        private ColorTableWidget ctw;

        /** parent widget */
        private ColorTableWidget parentWidget = null;

        /** Is the mouse in the component */
        private boolean mouseIn = false;


        /**
         * Create a new color table preview
         *
         * @param ctw   color table widget
         * @param colors _more_
         * @param forMain For main control window or for legend
         * @param legendType Side or bottom legend if for a legend
         *
         */
        public MyColorPreview(ColorTableWidget ctw, List<Color> colors,
                              boolean forMain, int legendType) {
            super(colors, ctw.getDisplayConventions(), legendType, forMain);
            this.ctw = ctw;
            setToolTipText(
                "<html>Click to focus<br>Right click to show menu<br>Control-r: Revert to default range<br>Right arrow: shift range up<br>Left arrow: shift range down<br>Up arrow: expand range <br>Down arrow: shrink range");
            addKeyListener(this);
        }


        /**
         * get tool tip. We override this to set the focus
         *
         * @return tooltip
         */
        public String getToolTipText() {
            //Only request focus if the mouse is been in the component
            if (mouseIn) {
                //                requestFocus();
            }
            return super.getToolTipText();
        }


        /**
         * Mouse entered event handler
         *
         * @param event  the MouseEvent
         */
        public void mouseEntered(MouseEvent event) {
            mouseIn = true;
            super.mouseEntered(event);
        }

        /**
         * mouse exited
         *
         * @param event event
         */
        public void mouseExited(MouseEvent event) {
            mouseIn = false;
            super.mouseExited(event);
        }



        /**
         * Noop
         *
         * @param e The event
         */
        public void keyPressed(KeyEvent e) {
            double delta = 0.01 * range.getSpan();
            //Round it to the nearest whole number if the span is "large"
            if (delta > 2.0) {
                delta = (int) delta;
            }

            Range newRange = null;
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                newRange = new Range(range.getMin() - delta,
                                     range.getMax() + delta);
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                newRange = new Range(range.getMin() + delta,
                                     range.getMax() - delta);
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                newRange = new Range(range.getMin() - delta,
                                     range.getMax() - delta);
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                newRange = new Range(range.getMin() + delta,
                                     range.getMax() + delta);
            }
            if (newRange != null) {
                ctw.handleNewRange(newRange);
                repaint();
            }

        }


        /**
         * Noop
         *
         * @param e The event
         */
        public void keyReleased(KeyEvent e) {}

        /**
         * Noop
         *
         * @param e The event
         */
        public void keyTyped(KeyEvent e) {}


        /**
         * Mouse clicked event handler
         *
         * @param event  the MouseEvent
         */
        public void mouseClicked(MouseEvent event) {
            requestFocus();
            if (SwingUtilities.isRightMouseButton(event)) {
                if (parentWidget == null) {
                    ctw.showMenu(this);
                } else {
                    parentWidget.showMenu(this);
                }

                return;
            }
            //For now let's not bring up the editor on a click
            if (true) {
                return;
            }
            try {
                if (parentWidget == null) {
                    ctw.doEdit();
                } else {
                    parentWidget.doEdit();
                }
            } catch (Exception exc) {
                ctw.logException("Edit color table", exc);
            }
        }

        /**
         * Mouse pressed event handler
         *
         * @param event  the MouseEvent
         */
        public void mousePressed(MouseEvent event) {
            if ( !SwingUtilities.isRightMouseButton(event)) {
                return;
            }
            if (parentWidget == null) {
                ctw.showMenu(this);
            }
        }
    }


    /**
     * Called to remove this from the display.
     */
    public void doRemove() {
        super.doRemove();
        if (myEditor != null) {
            myEditor.doClose();
            myEditor = null;
        }
    }



    /**
     * Get the component for the legend panel.
     *
     * @param legendType      type of legend
     * @see ucar.unidata.idv.DisplayControl#BOTTOM_LEGEND
     * @see ucar.unidata.idv.DisplayControl#SIDE_LEGEND
     * @return  component for legend panel
     */
    public JComponent getLegendPanel(int legendType) {
        return doMakePreview(false, legendType);
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
        setRange(newRange);
        try {
            displayControl.setRange(whichColorTable, range);
        } catch (Exception exc2) {
            logException("propertyChange", exc2);
        }
    }




    /**
     * Set the range in the dialog
     *
     * @param cmd Which one
     */
    public void setRangeFromPopup(String cmd) {
        Range r = null;
        if (cmd.equals(CMD_RANGE_COLORTABLE)) {
            Range ctRange = null;
            ColorTable originalCT =
                colorTableManager.getColorTable(colorTable.getName());
            if (originalCT != null) {
                r = originalCT.getRange();
            }
        } else if (cmd.equals(CMD_RANGE_DATA)) {
            r = getDisplayControl().getColorRangeFromData();
        } else if (cmd.equals(CMD_RANGE_DEFAULT)) {
            try {
                r = getDisplayControl().getInitialRange();
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
            rangeMinField.setText(getDisplayConventions().format(r.getMin()));
            rangeMaxField.setText(getDisplayConventions().format(r.getMax()));
        }
    }



    /**
     * Show the change range dialog
     */
    public void rangePopup() {
        try {
            Range ctRange = null;
            ColorTable originalCT =
                colorTableManager.getColorTable(colorTable.getName());
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
            getDisplayControl().addToRangeMenu((RangeWidget) this, items);

            JPopupMenu popup = GuiUtils.makePopupMenu(items);
            Dimension  d     = rangePopupBtn.getSize();
            popup.show(rangePopupBtn, 0, d.height);
        } catch (Exception exc) {
            logException("Range popup", exc);
        }
    }


    /**
     * Change the range
     */
    public void showChangeRangeDialog() {
        rangePopupBtn = GuiUtils.makeButton("Use Predefined", this,
                                            "rangePopup");
        rangeMinField = new JTextField(
            "" + getDisplayConventions().format(range.getMin()), 6);
        rangeMaxField = new JTextField(
            "" + getDisplayConventions().format(range.getMax()), 6);
        List comps = Misc.newList(new JLabel("New Range    From: "),
                                  rangeMinField, new JLabel("To: "),
                                  rangeMaxField,
                                  GuiUtils.inset(rangePopupBtn,
                                      new Insets(0, 5, 0, 0)));

        final JDialog dialog = new JDialog((Frame) null, "Change Range",
                                           true);
        JComponent     contents = GuiUtils.inset(GuiUtils.hflow(comps), 5);

        ActionListener listener = new ActionListener() {
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
        dialog.getContentPane().add(GuiUtils.centerBottom(contents,
                GuiUtils.makeApplyOkCancelButtons(listener)));

        dialog.pack();
        GuiUtils.showDialogNearSrc(popupBtn, dialog);
    }


    /**
     * Called to edit color table
     */
    public void doEdit() {
        colorTable.setRange(range);
        oldTable = new ColorTable(colorTable);
        oldRange = new Range(range);

        if (myEditor != null) {
            myEditor.show();
        } else {
            myEditor = colorTableManager.edit(colorTable,
                    (PropertyChangeListener) this);
        }

    }


    /**
     * Public implemenation for property changes
     *
     * @param e   event for property change.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(ColorTableManager.PROP_COLORTABLE)) {
            ColorTable newTable = (ColorTable) e.getNewValue();
            try {
                displayControl.setColorTable(whichColorTable, newTable);
            } catch (Exception exc) {
                logException("actionPerformed", exc);
            }
        } else if (e.getPropertyName().equals(ColorTableManager.PROP_RANGE)) {
            handleNewRange((Range) e.getNewValue());
        } else if (e.getPropertyName().equals(ColorTableManager.PROP_CLOSE)) {
            myEditor = null;
        } else if (e.getPropertyName().equals(
                ColorTableManager.PROP_CANCEL)) {

            /**
             *  FOr now lets ignore the undoing of the color table on a CANCEL
             * try {
             *   myEditor = null;
             *   ColorTable newTable = (ColorTable) e.getNewValue();
             *   displayControl.setRange(whichColorTable, range);
             *   setColorTable(newTable);
             *   displayControl.setColorTable(whichColorTable, newTable);
             * } catch (Exception exc2) {
             *   logException("propertyChange", exc2);
             * }
             */
        }

    }

    /**
     * Called if a color table is selected
     *
     * @param ct   color table that was selected
     */
    private void colorTableSelected(ColorTable ct) {
        colorTable = ct;
        if (colorTable.getRange() != null) {
            //For now don't do this            setRange (colorTable.getRange ());
        }
        popupBtn.setText(colorTable.getName());
        if (myEditor != null) {
            myEditor.setColorTable(colorTable);
        }


        try {
            displayControl.setColorTable(whichColorTable, colorTable);
        } catch (Exception e) {
            logException("actionPerformed", e);
        }
    }


    /**
     * Show menu anchored at the component location
     *
     * @param from   component for anchoring
     */
    public void showMenu(Component from) {
        JPopupMenu popup = GuiUtils.makePopupMenu(makeMenuItems());
        Dimension  d     = from.getSize();
        popup.show(from, 0, d.height);
    }

    /**
     * Make the menu for the color table.
     * @return  menu of menu items.
     * @see #makeMenuItems
     */
    public JMenu makeMenu() {
        return GuiUtils.makeMenu("Color Table", makeMenuItems());
    }


    /**
     * use the default colortable
     */
    public void doUseDefault() {
        displayControl.revertToDefaultColorTable(whichColorTable);
        if ((myEditor != null) && (colorTable != null)) {
            myEditor.setColorTable(colorTable);
        }
    }


    /**
     * Set the transparency
     *
     * @param v Integer, 0-100, of the inverse transparency
     */
    public void setTransparency(Integer v) {
        try {
            float trans = (100 - v.intValue()) / 100.0f;
            colorTable = new ColorTable(colorTable);
            colorTable.setTransparency(trans);
            displayControl.setColorTable(whichColorTable, colorTable);
            if ((myEditor != null) && (colorTable != null)) {
                myEditor.setColorTable(colorTable);
            }
        } catch (Exception exc) {
            logException("Setting transparency on color table", exc);
        }
    }


    /**
     * Make the menu items for the widget.
     *
     * @return  the list of menus
     */
    public ArrayList makeMenuItems() {
        ArrayList l = new ArrayList();
        l.add(GuiUtils.makeMenuItem("Edit Color Table", this, "doEdit"));
        l.add(GuiUtils.makeMenuItem("Change Range...", this,
                                    "showChangeRangeDialog"));
        l.add(GuiUtils.makeMenuItem("Use Default", this, "doUseDefault"));
        l.add(GuiUtils.MENU_SEPARATOR);


        JMenu transMenu = new JMenu("Transparency");
        l.add(transMenu);
        int[] values = {
            0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
        };
        for (int i = 0; i < values.length; i++) {
            transMenu.add(GuiUtils.makeMenuItem(values[i] + "%", this,
                    "setTransparency", new Integer(values[i])));
        }


        if (displayControl.getColorDimness() >= 0.1f) {
            l.add(GuiUtils.makeMenuItem("Dimmer", displayControl,
                                        "setColorDimmer"));
        }


        if (displayControl.getColorDimness() < 1.0f) {
            l.add(GuiUtils.makeMenuItem("Brighter", displayControl,
                                        "setColorBrighter"));
        }
        l.add(GuiUtils.MENU_SEPARATOR);


        colorTableManager.makeColorTableMenu(new ObjectListener(null) {
            public void actionPerformed(ActionEvent ae, Object data) {
                colorTableSelected((ColorTable) data);
            }
        }, l);

        return l;
    }

    /**
     * Revert to the default range of the color table (control dependent)
     */
    protected void revertToDefaultRange() {
        displayControl.revertToDefaultRange(whichColorTable);
    }


    /**
     * Get the range for the data
     *
     * @return  the range
     */
    public Range getRange() {
        return range;
    }

    /**
     * Set the range for the color table.
     *
     * @param r   range of table
     */
    public void setRange(Range r) {
        if (Misc.equals(r, range)) {
            return;
        }
        this.range = ((r != null)
                      ? new Range(r)
                      : null);
        for (int i = 0; i < colorPreviews.size(); i++) {
            ((MyColorPreview) colorPreviews.get(i)).setRange(range,
                    displayControl.getColorUnit());
        }
        if (myEditor != null) {
            myEditor.setRange(r);
        }
    }


    /**
     * Set the color palette on the previews.
     *
     * @param palette   new palette
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setColorPalette(float[][] palette)
            throws RemoteException, VisADException {
        ColorTable ct = new ColorTable("", "", palette);

        for (int i = 0; i < colorPreviews.size(); i++) {
            MyColorPreview colorPreview =
                (MyColorPreview) colorPreviews.get(i);
            colorPreview.setColors(ct.getColorList());
        }
    }


    /**
     * Set the color table.
     *
     * @param colorTable   new color table
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setColorTable(ColorTable colorTable)
            throws RemoteException, VisADException {
        this.colorTable = colorTable;
        for (int i = 0; i < colorPreviews.size(); i++) {
            MyColorPreview colorPreview =
                (MyColorPreview) colorPreviews.get(i);
            colorPreview.setColors(colorTable.getColorList());
        }
        popupBtn.setText(colorTable.getName());
    }
    /**
     * Get the color table.
     *
     * @return  the color table
     */

    public ColorTable getColorTable(){
        return  this.colorTable;
    }
    /**
     * Fill the list of components and set them in a certain number of
     * columns.
     *
     * @param l            List of components
     * @param columns      number of columns
     */
    public void fillList(List l, int columns) {
        JLabel label   = GuiUtils.rLabel("Color Table:");
        int    keyCode = GuiUtils.charToKeyCode("T");
        if (keyCode != -1) {
            label.setDisplayedMnemonic(keyCode);
        }
        label.setLabelFor(popupBtn);
        l.add(label);
        GuiUtils.tmpFill = GridBagConstraints.HORIZONTAL;
        JPanel p = GuiUtils.doLayout(new Component[] { popupBtn,
                new Label(" "),
                GuiUtils.left(
                    doMakePreview(
                        true, DisplayControlImpl.BOTTOM_LEGEND)) }, 3,
                            GuiUtils.WT_NNY, GuiUtils.WT_N);
        l.add(p);
    }

}

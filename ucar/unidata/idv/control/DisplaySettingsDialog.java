/**
 * $Id: DisplaySettingsDialog.java,v 1.13 2007/08/21 15:31:01 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.ControlDescriptor;

import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.ContourInfoDialog;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PropertyValue;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;

import ucar.visad.display.ColorScaleInfo;



import visad.*;

import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;
import java.util.Vector;

import javax.swing.*;



/**
 * Shows the display settings dialog
 * @author IDV development team
 * @version $Revision: 1.13 $
 */
public class DisplaySettingsDialog {

    /** _more_ */
    IntegratedDataViewer idv;

    /** The dialog */
    JDialog dialog;

    /** The display */
    private DisplayControlImpl display;

    /** _more_ */
    private List propertyValues = new ArrayList();

    /** _more_ */
    private JButton applyBtn;

    /** _more_ */
    List displayWrappers;

    /** _more_ */
    List displays;

    /** _more_ */
    private JPanel propertiesHolder;

    /** _more_ */
    private JScrollPane leftSP;

    /** _more_ */
    private JComponent contents;


    /**
     * _more_
     *
     * @param idv _more_
     * @param display _more_
     */
    public DisplaySettingsDialog(IntegratedDataViewer idv,
                                 DisplayControlImpl display) {
        this(idv, display, true);
    }

    /**
     * _more_
     *
     * @param idv _more_
     * @param display _more_
     * @param showDialog _more_
     */
    public DisplaySettingsDialog(IntegratedDataViewer idv,
                                 DisplayControlImpl display,
                                 boolean showDialog) {
        this.idv = idv;
        displays = idv.getDisplayControls();
        if ((display == null) && (displays.size() > 0)) {
            display = (DisplayControlImpl) displays.get(0);
        }
        contents = doMakeContents();
        if (display != null) {
            setDisplay(display);
        }
        if (showDialog) {
            showDialog();
        }
    }



    /**
     * _more_
     *
     * @param idv _more_
     */
    public DisplaySettingsDialog(IntegratedDataViewer idv) {
        this(idv, null);
    }




    /**
     * _more_
     *
     * @param display _more_
     */
    public DisplaySettingsDialog(DisplayControlImpl display) {
        this(display.getIdv(), display);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getPropertyValues() {
        List props = new ArrayList();
        for (int i = 0; i < propertyValues.size(); i++) {
            PropertyValueWrapper prop =
                (PropertyValueWrapper) propertyValues.get(i);
            props.add(prop.propertyValue);
        }
        return props;
    }


    private static Font FONT_NORMAL;
    private static Font FONT_SELECTED;

    /**
     * _more_
     *
     * @param display _more_
     */
    public void setDisplay(DisplayControlImpl display) {
        this.display   = display;
        propertyValues = new ArrayList();
        display.addDisplaySettings(this);
        if (dialog != null) {
            dialog.setTitle("Display Settings -- " + display.getTitle());
        }
        updatePropertiesComponent();
        for (int i = 0; i < displays.size(); i++) {
            DisplayWrapper dw = (DisplayWrapper) displayWrappers.get(i);
            if(FONT_NORMAL ==null) {
                FONT_NORMAL = dw.cbx.getFont();
                FONT_SELECTED = FONT_NORMAL.deriveFont(Font.ITALIC);
            }
            if (dw.dci == display) {
                dw.cbx.setForeground(Color.blue);
                dw.cbx.setSelected(true);
                dw.cbx.setFont(FONT_SELECTED);
            } else {
                dw.cbx.setForeground(Color.black);
                dw.cbx.setSelected(false);
                dw.cbx.setFont(FONT_NORMAL);
            }
        }
    }


    /**
     * _more_
     *
     * @param object _more_
     * @param propName _more_
     * @param label _more_
     * @param category _more_
     */
    protected void addPropertyValue(Object object, String propName,
                                    String label, String category) {
        propertyValues.add(
            new PropertyValueWrapper(
                new PropertyValue(propName, label, object, category)));

    }


    /**
     * Show the dialog
     *
     */
    private void showDialog() {
        Window f = GuiUtils.getWindow(display.getContents());
        dialog = GuiUtils.createDialog(f, "", true);
        LogUtil.registerWindow(dialog);
        if (display != null) {
            dialog.setTitle("Display Settings -- " + display.getTitle());
        }
        dialog.getContentPane().add(contents);
        dialog.pack();
        if (f != null) {
            GuiUtils.showDialogNearSrc(f, dialog);
        } else {
            dialog.setLocation(new Point(200, 200));
            dialog.show();
        }
    }



    /**
     * Class DisplayWrapper _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.13 $
     */
    private class DisplayWrapper {

        /** _more_ */
        JCheckBox cbx;

        /** _more_ */
        DisplayControlImpl dci;

        /**
         * _more_
         *
         * @param dci _more_
         *
         * @param display _more_
         */
        public DisplayWrapper(DisplayControlImpl display) {
            this.dci = display;
            cbx      = new JCheckBox(dci.getTitle());
            cbx.setToolTipText("<html>Right click to show popup menu</html>");
            cbx.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if ( !SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    List items = new ArrayList();
                    items.add(
                        GuiUtils.makeMenuItem(
                            "Use properties from this display",
                            DisplaySettingsDialog.this, "setDisplay", dci));
                    JPopupMenu popup = GuiUtils.makePopupMenu(items);
                    popup.show(cbx, e.getX(), e.getY());
                }
            });
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent doMakeContents() {
        displayWrappers = new ArrayList();
        List      viewLabels = new ArrayList();
        Hashtable viewMap    = new Hashtable();
        int       viewCnt    = 0;
        for (int i = 0; i < displays.size(); i++) {
            DisplayControlImpl dci = (DisplayControlImpl) displays.get(i);
            DisplayWrapper     dw  = new DisplayWrapper(dci);
            displayWrappers.add(dw);
            ViewManager vm = dci.getDefaultViewManager();
            String      label;
            if (vm == null) {
                label = "No View";
            } else {
                label = (String) viewMap.get(vm);
                if (label == null) {
                    viewCnt++;
                    label = vm.getName();
                }
                if ((label == null) || (label.trim().length() == 0)) {
                    label = "View " + viewCnt;
                }
                viewMap.put(vm, label);
            }
            List comps = (List) viewMap.get(label);
            if (comps == null) {
                viewLabels.add(label);
                viewMap.put(label, comps = new ArrayList());
            }
            comps.add(dw.cbx);
        }
        List   displayComps = new ArrayList();
        Insets compInsets   = new Insets(0, 15, 0, 0);
        Insets labelInsets  = new Insets(5, 5, 0, 0);
        for (int i = 0; i < viewLabels.size(); i++) {
            String label = (String) viewLabels.get(i);
            List   comps = (List) viewMap.get(label);
            displayComps.add(GuiUtils.inset(new JLabel(label), labelInsets));
            for (int compIdx = 0; compIdx < comps.size(); compIdx++) {
                displayComps.add(
                    GuiUtils.inset(
                        (JComponent) comps.get(compIdx), compInsets));
            }
        }

        JComponent    displaysComp = GuiUtils.vbox(displayComps);


        final JButton selectBtn    = new JButton("Display Groups");
        selectBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                popupDisplayGroupMenu(selectBtn);
            }
        });

        applyBtn = GuiUtils.makeButton("Apply", this, "doApply");
        JButton   okBtn     = GuiUtils.makeButton("OK", this, "doOk");
        JButton   cancelBtn = GuiUtils.makeButton("Cancel", this, "doCancel");
        JButton   saveBtn   = GuiUtils.makeButton("Save", this, "doSave");

        JButton[] holder    = { null };
        holder[0] = GuiUtils.makeButton("Saved Settings", this,
                                        "popupDisplaySettingsMenu", holder);
        propertiesHolder = new JPanel(new BorderLayout());

        JComponent groupApplyComp = GuiUtils.inset(propertiesHolder, 5);
        leftSP = GuiUtils.makeScrollPane(GuiUtils.top(groupApplyComp), 350,
                                         300);
        JScrollPane rightSP =
            GuiUtils.makeScrollPane(GuiUtils.top(displaysComp), 350, 300);
        leftSP.setPreferredSize(new Dimension(350, 300));
        rightSP.setPreferredSize(new Dimension(300, 300));
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JComponent buttons = GuiUtils.doLayout(new Component[] { saveBtn,
                applyBtn, okBtn, cancelBtn }, 4, GuiUtils.WT_N,
                    GuiUtils.WT_N);

        JComponent applyContents =
            GuiUtils.doLayout(new Component[] {
                GuiUtils.inset(GuiUtils.leftRight(new JLabel("Properties"),
                    holder[0]), 2),
                GuiUtils.inset(GuiUtils.leftRight(new JLabel("Displays"),
                    selectBtn), 2),
                GuiUtils.inset(leftSP, 2), GuiUtils.inset(rightSP, 2), }, 2,
                    GuiUtils.WT_YY, GuiUtils.WT_NY);

        applyContents = GuiUtils.centerBottom(applyContents,
                GuiUtils.wrap(buttons));
        applyContents = GuiUtils.inset(applyContents, 5);
        return applyContents;
    }

    /**
     * _more_
     *
     * @param comp _more_
     */
    private void popupDisplayGroupMenu(JComponent comp) {
        final List items = new ArrayList();
        ControlDescriptor cd =
            idv.getControlDescriptor(display.getDisplayId());

        List   dataSources    = display.getDataSources();
        String dataSourceName = "";
        String dataSourceKey  = null;
        if ((dataSources != null) && (dataSources.size() > 0)) {
            dataSourceName = DataSourceImpl.getNameForDataSource(
                (DataSource) dataSources.get(0), 20, true);
            dataSourceKey = DisplayControlBase.FIND_WITHTHISDATA;
        }


        String[] keys = {
            DisplayControlBase.FIND_THIS, DisplayControlBase.FIND_ALL,
            DisplayControlBase.FIND_CLASS + display.getClass().getName(),
            DisplayControlBase.FIND_CATEGORY + display.getDisplayCategory(),
            dataSourceKey, ((display.getShortParamName() != null)
                            ? DisplayControlBase.FIND_WITHTHISFIELD
                            : null), DisplayControlBase.FIND_WITHDATA,
            DisplayControlBase.FIND_WITHTHISVIEW,
            DisplayControlBase.FIND_SPECIAL
        };

        String[] labels = {
            "This display", "All displays",
            "Displays of type: " + cd.getDescription(),
            "Displays with category: " + display.getDisplayCategory(),
            "Displays with data source: " + dataSourceName,
            "Displays with this field: " + display.getShortParamName(),
            "Displays with any data", "Displays in view", "Special displays",
        };


        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            if (key == null) {
                continue;
            }
            JMenuItem mi = new JMenuItem(labels[i]);
            items.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    List selected = display.findDisplays(key, displays);
                    for (int i = 0; i < displays.size(); i++) {
                        DisplayWrapper dw =
                            (DisplayWrapper) displayWrappers.get(i);
                        dw.cbx.setSelected(selected.contains(dw.dci));
                    }
                }
            });
        }

        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        popup.show(comp, 0, comp.getBounds().height);


    }


    /**
     * _more_
     */
    private void updatePropertiesComponent() {
        List      cats   = new ArrayList();
        Hashtable catMap = new Hashtable();
        for (int i = 0; i < propertyValues.size(); i++) {
            PropertyValueWrapper prop =
                (PropertyValueWrapper) propertyValues.get(i);
            List catComps = (List) catMap.get(prop.getCategory());
            if (catComps == null) {
                catComps = new ArrayList();
                catMap.put(prop.getCategory(), catComps);
                cats.add(prop.getCategory());
            }
            catComps.add(GuiUtils.leftRight(prop.getCheckbox(),
                                            prop.changeBtn));
        }

        List comps = new ArrayList();
        for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
            String cat      = (String) cats.get(catIdx);
            List   catComps = (List) catMap.get(cat);
            comps.add(new JLabel(cat));
            Insets inset = new Insets(0, 10, 0, 0);
            for (int compIdx = 0; compIdx < catComps.size(); compIdx++) {
                JComponent comp = (JComponent) catComps.get(compIdx);
                comps.add(GuiUtils.inset(comp, inset));
            }
        }

        propertiesHolder.removeAll();
        propertiesHolder.add(BorderLayout.CENTER, GuiUtils.vbox(comps));
        propertiesHolder.validate();
        propertiesHolder.repaint();
        leftSP.validate();
        leftSP.getViewport().scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }


    /**
     * _more_
     *
     * @param holder _more_
     */
    public void popupDisplaySettingsMenu(JButton[] holder) {
        List displaySettings = idv.getResourceManager().getDisplaySettings();
        List applyItems = makeDisplaySettingsMenuItems(displaySettings, this,
                              "applyDisplaySetting", "");
        if (applyItems.size() == 0) {
            return;
        }
        GuiUtils.showPopupMenu(applyItems, holder[0]);
    }



    /**
     * _more_
     */
    public void doSave() {
        List propList = new ArrayList();
        for (int i = 0; i < propertyValues.size(); i++) {
            PropertyValueWrapper prop =
                (PropertyValueWrapper) propertyValues.get(i);
            if ( !prop.getCheckbox().isSelected()) {
                continue;
            }
            //Make a copy
            propList.add(new PropertyValue(prop.propertyValue));
        }

        DisplaySetting.doSave(idv, dialog, propList, display);
    }


    /**
     * _more_
     *
     * @param displaySetting _more_
     */
    public void applyDisplaySetting(DisplaySetting displaySetting) {
        List newProps = new ArrayList(displaySetting.getPropertyValues());
        for (int propIdx = 0; propIdx < propertyValues.size(); propIdx++) {
            PropertyValueWrapper oldProp =
                (PropertyValueWrapper) propertyValues.get(propIdx);
            boolean gotOne = false;
            for (int newPropIdx = 0; newPropIdx < newProps.size();
                    newPropIdx++) {
                PropertyValue newProp =
                    (PropertyValue) newProps.get(newPropIdx);
                if (Misc.equals(newProp.getName(), oldProp.getName())) {
                    gotOne = true;
                    oldProp.propertyValue.setValue(newProp.getValue());
                    oldProp.setCheckboxLabel();
                    newProps.remove(newPropIdx);
                    break;
                }
            }
            oldProp.getCheckbox().setSelected(gotOne);
        }

        //Now add in any we didnt have
        if (newProps.size() > 0) {
            for (int i = 0; i < newProps.size(); i++) {
                PropertyValue newProp =
                    new PropertyValue((PropertyValue) newProps.get(i));
                propertyValues.add(new PropertyValueWrapper(newProp, true));
            }
            updatePropertiesComponent();
        }


    }




    /**
     * _more_
     *
     *
     * @param displaySettings _more_
     * @param object _more_
     * @param method _more_
     * @param labelPrefix _more_
     *
     * @return _more_
     */
    public static List makeDisplaySettingsMenuItems(List displaySettings,
            Object object, String method, String labelPrefix) {
        List      items    = new ArrayList();
        Hashtable catMenus = new Hashtable();
        for (int i = 0; i < displaySettings.size(); i++) {
            DisplaySetting displaySetting =
                (DisplaySetting) displaySettings.get(i);
            String label      = displaySetting.getName();
            List   toks       = StringUtil.split(label, ">", true, true);
            String catSoFar   = "";
            JMenu  parentMenu = null;
            for (int tokIdx = 0; tokIdx < toks.size() - 1; tokIdx++) {
                String cat = (String) toks.get(tokIdx);
                catSoFar = catSoFar + "-" + cat;
                JMenu catMenu = (JMenu) catMenus.get(catSoFar);
                if (catMenu == null) {
                    catMenu = new JMenu(cat);
                    catMenus.put(catSoFar, catMenu);
                    if (parentMenu == null) {
                        items.add(catMenu);
                    } else {
                        parentMenu.add(catMenu);
                    }
                }
                parentMenu = catMenu;
            }
            if (toks.size() > 0) {
                label = (String) toks.get(toks.size() - 1);
            }
            JMenuItem mi = GuiUtils.makeMenuItem(labelPrefix + label, object,
                               method, displaySetting);
            if (parentMenu == null) {
                items.add(mi);
            } else {
                parentMenu.add(mi);
            }
        }
        return items;
    }




    /**
     * _more_
     */
    public void doCancel() {
        dialog.dispose();
    }

    /**
     * _more_
     */
    public void doOk() {
        doApply();
        doCancel();
    }

    /**
     * _more_
     */
    public void doApply() {
        final List selectedDisplays = new ArrayList();
        for (int i = 0; i < displayWrappers.size(); i++) {
            DisplayWrapper dw = (DisplayWrapper) displayWrappers.get(i);
            if (dw.cbx.isSelected()) {
                selectedDisplays.add(dw.dci);
            }
        }
        final List propList = new ArrayList();
        for (int i = 0; i < propertyValues.size(); i++) {
            PropertyValueWrapper prop =
                (PropertyValueWrapper) propertyValues.get(i);
            if ( !prop.getCheckbox().isSelected()) {
                continue;
            }
            propList.add(prop.propertyValue);
        }
        applyBtn.setEnabled(false);
        display.showWaitCursor();
        Misc.run(new Runnable() {
            public void run() {
                applyPropertyValues(selectedDisplays, propList);
                display.showNormalCursor();
                applyBtn.setEnabled(true);
            }
        });
    }


    /**
     * _more_
     *
     * @param selectedDisplays _more_
     * @param props _more_
     */
    private static void applyPropertyValues(List selectedDisplays,
                                            List props) {
        try {
            for (int displayIdx = 0; displayIdx < selectedDisplays.size();
                    displayIdx++) {
                DisplayControlImpl display =
                    (DisplayControlImpl) selectedDisplays.get(displayIdx);
                display.applyPropertyValues(props);
            }
        } catch (Exception exc) {
            LogUtil.logException("Applying properties", exc);
            return;
        }
    }




    /**
     * Class PropertyValueWrapper _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.13 $
     */
    public class PropertyValueWrapper {

        /** _more_ */
        PropertyValue propertyValue;

        /** _more_ */
        private JCheckBox cbx;

        /** _more_ */
        private JComponent changeBtn;

        /**
         * _more_
         *
         * @param propertyValue _more_
         */
        public PropertyValueWrapper(PropertyValue propertyValue) {
            this(propertyValue, false);
        }

        /**
         * _more_
         *
         * @param propertyValue _more_
         * @param cbxValue _more_
         */
        public PropertyValueWrapper(PropertyValue propertyValue,
                                    boolean cbxValue) {
            this.propertyValue = propertyValue;
            getCheckbox().setSelected(cbxValue);
        }

        /**
         * _more_
         *
         * @param v _more_
         *
         * @return _more_
         */
        private boolean canChange(Object v) {
            return (v instanceof String) || (v instanceof Double)
                   || (v instanceof Integer) || (v instanceof Float)
                   || (v instanceof Range) || (v instanceof ContourInfo)
                   || (v instanceof Color) || (v instanceof ColorScaleInfo)
                   || (v instanceof Unit) || (v instanceof Real)
                   || (v instanceof ColorTable);

        }


        /**
         * _more_
         */
        public void changeProperty() {
            Object v = propertyValue.getValue();
            if (v instanceof Boolean) {
                Boolean b = (Boolean) v;
                if (b.booleanValue()) {
                    changeValueTo(new Boolean(false));
                } else {
                    changeValueTo(new Boolean(true));
                }
                cbx.setSelected(true);
                return;
            }

            if (v instanceof ColorTable) {
                List items = new ArrayList();
                idv.getColorTableManager().makeColorTableMenu(
                    new ObjectListener(null) {
                    public void actionPerformed(ActionEvent ae, Object data) {
                        changeValueTo(data);
                        cbx.setSelected(true);
                    }
                }, items);
                GuiUtils.showPopupMenu(items, changeBtn);
            }


            if (canChange(v)) {
                changeValue();
                return;
            }


        }



        /**
         * _more_
         *
         * @return _more_
         */
        public JCheckBox getCheckbox() {
            if (cbx == null) {
                cbx = new JCheckBox("", false);
                setCheckboxLabel();
                String icon = null;
                Object v    = propertyValue.getValue();
                if (propertyValue.getValue() instanceof Boolean) {
                    icon = "/auxdata/ui/icons/Refresh16.gif";
                } else if (canChange(v)) {
                    icon = "/auxdata/ui/icons/Settings16.png";
                }
                if (icon == null) {
                    changeBtn = new JPanel();
                } else {
                    changeBtn = GuiUtils.makeImageButton(icon, this,
                            "changeProperty", null);
                    changeBtn.setToolTipText("Change value");
                }
            }
            return cbx;
        }


        /**
         * _more_
         */
        public void changeValue() {

            Object value = propertyValue.getValue();
            if (value instanceof Range) {
                Range      r             = (Range) value;
                JTextField rangeMinField = new JTextField(r.getMin() + "",
                                               10);
                JTextField rangeMaxField = new JTextField(r.getMax() + "",
                                               10);
                List comps = Misc.newList(new JLabel("From: "),
                                          rangeMinField, new JLabel("To: "),
                                          rangeMaxField);
                JComponent contents = GuiUtils.inset(GuiUtils.hflow(comps),
                                          5);
                if ( !GuiUtils.showOkCancelDialog(dialog,
                        "Change " + propertyValue.getLabel(),
                        GuiUtils.inset(contents, 5), null)) {
                    return;
                }
                Range newRange =
                    new Range(Misc.parseNumber(rangeMinField.getText()),
                              Misc.parseNumber(rangeMaxField.getText()));
                propertyValue.setValue(newRange);
            } else if (value instanceof ColorScaleInfo) {
                ColorScaleInfo csi =
                    new ColorScaleInfo((ColorScaleInfo) value);
                ColorScaleDialog csd = new ColorScaleDialog(null,
                                           "Color Scale Properties", csi,
                                           true);
                if ( !csd.getOk()) {
                    return;
                }
                propertyValue.setValue(new ColorScaleInfo(csd.getInfo()));


            } else if (value instanceof Real) {
                try {
                    Real   r = (Real) propertyValue.getValue();
                    String s = "" + r.getValue();
                    while (true) {
                        try {
                            s = GuiUtils.getInput("Enter new value for "
                                    + propertyValue.getLabel(), "Value: ", s,
                                        r.getUnit() + "");
                            if (s == null) {
                                return;
                            }
                            double d = Misc.parseDouble(s);
                            propertyValue.setValue(r.cloneButValue(d));
                            break;
                        } catch (NumberFormatException nfe) {
                            LogUtil.userErrorMessage("Bad number format: "
                                    + s);
                        }
                    }
                } catch (Exception exc) {
                    LogUtil.logException("Setting value", exc);
                    return;
                }
            } else if (value instanceof ContourInfo) {
                ContourInfoDialog cid =
                    new ContourInfoDialog("Change "
                                          + propertyValue.getLabel(), false);
                ContourInfo ci =
                    new ContourInfo((ContourInfo) propertyValue.getValue());
                if ( !cid.showDialog(ci)) {
                    return;
                }
                propertyValue.setValue(ci);
            } else if ((value instanceof String) || (value instanceof Double)
                       || (value instanceof Integer)
                       || (value instanceof Float)) {
                String newString = GuiUtils.getInput("Enter new value for "
                                       + propertyValue.getLabel(), "Value: ",
                                           value.toString());
                if (newString == null) {
                    return;
                }
                Object newValue = newString.trim();
                if (value instanceof Double) {
                    newValue = new Double(newString);
                } else if (value instanceof Float) {
                    newValue = new Float(newString);
                } else if (value instanceof Integer) {
                    newValue = new Integer(newString);
                }
                propertyValue.setValue(newValue);
            } else if (value instanceof Unit) {
                Unit newUnit =
                    idv.getDisplayConventions().selectUnit((Unit) value,
                        null);
                if (newUnit == null) {
                    return;
                }
                propertyValue.setValue(newUnit);
            } else if (value instanceof Color) {
                Color c = JColorChooser.showDialog(changeBtn, "Select Color",
                              (Color) value);
                if (c == null) {
                    return;
                }
                propertyValue.setValue(c);
            } else {
                return;
            }
            cbx.setSelected(true);
            setCheckboxLabel();
            GuiUtils.showWidget(cbx);

        }


        /**
         * _more_
         *
         * @param o _more_
         */
        public void changeValueTo(Object o) {
            propertyValue.setValue(o);
            setCheckboxLabel();
        }


        /**
         * _more_
         */
        public void setCheckboxLabel() {
            Object value  = propertyValue.getValue();
            String svalue = getValueLabel(value);
            if (svalue.length() > 20) {
                svalue = svalue.substring(0, 19) + "...";
            }

            cbx.setText(propertyValue.getLabel() + " (" + svalue + ")");
            cbx.setToolTipText("<html>Select this to apply the value:<br><i>"
                               + value
                               + "</i><br>Right click to edit.</html>");

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getCategory() {
            return propertyValue.getCategory();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getName() {
            return propertyValue.getName();
        }

    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String getValueLabel(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Color) {
            Color c = (Color) v;
            return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        }

        if (v instanceof ContourInfo) {
            ContourInfo ci = (ContourInfo) v;
            return ci.getInterval() + "/" + ci.getBase() + "/" + ci.getMin()
                   + "/" + ci.getMax();
        }
        if (v instanceof ColorScaleInfo) {
            ColorScaleInfo csi = (ColorScaleInfo) v;
            return (csi.getIsVisible()
                    ? "visible"
                    : "not visible") + " " + csi.getPlacement();
        }

        return v.toString();
    }




}


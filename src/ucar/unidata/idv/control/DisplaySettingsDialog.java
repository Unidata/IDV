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
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;



/**
 * Shows the display settings dialog
 * @author IDV development team
 * @version $Revision: 1.13 $
 */
public class DisplaySettingsDialog {

    /** the idv */
    IntegratedDataViewer idv;

    /** The dialog */
    JDialog dialog;

    /** The display */
    private DisplayControlImpl display;

    /** The property values */
    private List<PropertyValueWrapper> propertyValues =
        new ArrayList<PropertyValueWrapper>();

    /** apply button */
    private JButton applyBtn;

    /** ??? */
    List<DisplayWrapper> displayWrappers;

    /** The displays */
    List displays;

    /** gui component */
    private JPanel propertiesHolder;

    /** gui component */
    private JScrollPane propertiesSP;

    /** gui component */
    private JComponent contents;

    /** _more_          */
    private JMenuBar menuBar;

    /** list of dipslays */
    private JList displaysList;

    /** list of saved settings */
    private JList displaySettingsList;

    /**
     * ctor
     *
     * @param idv the idv
     * @param display The initial display to use
     */
    public DisplaySettingsDialog(IntegratedDataViewer idv,
                                 DisplayControlImpl display) {
        this(idv, display, true);
    }

    /**
     * The ctor
     *
     * @param idv the idv
     * @param display Initial display to use
     * @param showDialog Should we show the dialog
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
     * ctor
     *
     * @param idv the idv
     */
    public DisplaySettingsDialog(IntegratedDataViewer idv) {
        this(idv, null);
    }




    /**
     * ctor
     *
     * @param display The initial display
     */
    public DisplaySettingsDialog(DisplayControlImpl display) {
        this(display.getIdv(), display);
    }

    /**
     * Get the property values
     *
     * @return property values
     */
    public List<PropertyValue> getPropertyValues() {
        List<PropertyValue> props = new ArrayList<PropertyValue>();
        for (int i = 0; i < propertyValues.size(); i++) {
            PropertyValueWrapper prop =
                (PropertyValueWrapper) propertyValues.get(i);
            props.add(prop.propertyValue);
        }
        return props;
    }


    /** The font to use */
    private static Font FONT_NORMAL;

    /** The selected font */
    private static Font FONT_SELECTED;

    /**
     * Set the display to use
     *
     * @param display The display
     */
    public void setDisplay(DisplayControlImpl display) {
        this.display   = display;
        propertyValues = new ArrayList<PropertyValueWrapper>();
        display.addDisplaySettings(this);
        if (dialog != null) {
            dialog.setTitle("Display Settings -- " + display.getTitle());
        }
        if ((display != null) && (displaysList != null)) {
            displaysList.setSelectedValue(display, true);
        }

        updatePropertiesComponent();
    }


    /** not used */
    private static HashSet logSeen = new HashSet();

    /**
     * Add a property value
     *
     * @param object value of the property
     * @param propName its name
     * @param label label to use
     * @param category The category of the property
     */
    protected void addPropertyValue(Object object, String propName,
                                    String label, String category) {

        PropertyValue propertyValue = new PropertyValue(propName, label,
                                          object, category);

        propertyValues.add(new PropertyValueWrapper(propertyValue));

        if (false && !logSeen.contains(propName)) {
            logSeen.add(propName);
            Object value        = propertyValue.getValue();
            String exampleValue = "";
            if (value != null) {
                if (value instanceof ColorTable) {
                    propName     = "colorTableName";
                    exampleValue = ((ColorTable) value).getName();
                } else if (value instanceof Range) {
                    exampleValue = "min:max";
                } else if (value instanceof Real) {
                    Real r = (Real) value;
                    exampleValue = "Real value, e.g., " + r.getValue() + "["
                                   + r.getUnit() + "]";
                } else if (value instanceof ColorScaleInfo) {
                    //                    propName = "colorScaleVisible";
                    String fmt = ColorScaleInfo.getParamStringFormat();
                    fmt = fmt.replace(";", ";<br>");
                    exampleValue = "semi-colon delimited string:<br>&quot;"
                                   + fmt + "&quot";
                } else if (value instanceof ContourInfo) {
                    exampleValue =
                        "semi-colon delimited string:<br>&quot;interval=&lt;interval&gt;;<br> "
                        + "min=&lt;min&gt;;<br> " + "max=&lt;max&gt;;<br> "
                        + "base=&lt;base&gt;;<br> "
                        + "dashed=true/false;<br> "
                        + "labels=true/false;&quot;";
                } else if (value instanceof Boolean) {
                    exampleValue = "true|false";
                } else if (value instanceof String) {
                    exampleValue = "String";
                } else if (value instanceof Double) {
                    exampleValue = "double";
                } else if (value instanceof Integer) {
                    exampleValue = "integer";
                } else if (value instanceof Unit) {
                    exampleValue = "unit, e.g.," + value.toString();
                } else {
                    exampleValue = "Unknown type: "
                                   + value.getClass().getName();
                }
            }
            System.out.print("<tr valign=top><td>" + label + "</td><td><i>"
                             + propName + "</i></td>");
            System.out.print("<td>" + exampleValue + "</td>");
            System.out.print("<td><i>&lt;property name=&quot;" + propName
                             + "&quot; value=&quot;&quot;/&gt;</i></td>");
            System.out.println("</tr>");
        }

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
     * Class DisplayWrapper Holds a display and the gui state shown in the dialog
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.13 $
     */
    private class DisplayWrapper {

        /** The checkbox */
        JCheckBox cbx;

        /** The display */
        DisplayControlImpl dci;

        /**
         * ctor
         *
         *
         * @param display the display
         */
        public DisplayWrapper(DisplayControlImpl display) {
            this.dci = display;
            cbx      = new JCheckBox(dci.getTitle());
        }

        /**
         * to string
         *
         * @return to string
         */
        public String toString() {
            return dci.getLabel();
        }
    }


    /**
     * Make the gui
     *
     * @return the gui
     */
    private JComponent doMakeContents() {

        displayWrappers = new ArrayList<DisplayWrapper>();
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

        applyBtn = GuiUtils.makeButton("Apply>>", this, "doApply");
        JButton okBtn     = GuiUtils.makeButton("OK", this, "doOk");
        JButton cancelBtn = GuiUtils.makeButton("Close", this, "doCancel");
        JButton saveBtn   = GuiUtils.makeButton("Save", this, "doSave");

        propertiesHolder = new JPanel(new BorderLayout());


        int listHeight = 250;
        int listWidth  = 200;
        displaysList = new JList(new Vector(displays));
        displaysList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displaysList.setBackground(null);
        JComponent displaysSP =
            GuiUtils.makeScrollPane(GuiUtils.top(displaysList), listWidth,
                                    listHeight);

        displaysSP.setPreferredSize(new Dimension(listWidth, listHeight));
        if (display != null) {
            displaysList.setSelectedValue(display, true);
        }
        displaysList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                DisplayControlImpl display =
                    (DisplayControlImpl) displaysList.getSelectedValue();
                if (display != null) {
                    displaySettingsList.clearSelection();
                    setDisplay(display);
                }
            }
        });


        displaySettingsList = new JList(
            new Vector(idv.getResourceManager().getDisplaySettings()));
        displaySettingsList.setBackground(null);
        displaySettingsList.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        JComponent displaySettingsSP =
            GuiUtils.makeScrollPane(GuiUtils.top(displaySettingsList),
                                    listWidth, listHeight);
        displaySettingsSP.setPreferredSize(new Dimension(listWidth,
                listHeight));
        displaySettingsList.addListSelectionListener(
            new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                DisplaySetting displaySetting =
                    (DisplaySetting) displaySettingsList.getSelectedValue();
                if (displaySetting != null) {
                    displaysList.clearSelection();
                    applyDisplaySetting(displaySetting);

                }
            }
        });




        JComponent propertiesComp = GuiUtils.inset(propertiesHolder, 5);
        propertiesSP = GuiUtils.makeScrollPane(GuiUtils.top(propertiesComp),
                300, 300);
        JScrollPane rightSP =
            GuiUtils.makeScrollPane(GuiUtils.top(displaysComp), 300, 300);
        propertiesSP.setPreferredSize(new Dimension(300, 300));
        rightSP.setPreferredSize(new Dimension(300, 300));
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        /*        JComponent buttons = GuiUtils.doLayout(new Component[] { saveBtn,
                                                                 cancelBtn }, 4, GuiUtils.WT_N,
                                               GuiUtils.WT_N);
        */
        JComponent buttons = cancelBtn;

        JComponent sourceComp = GuiUtils.topCenter(new JLabel("Source"),
                                    GuiUtils.doLayout(new Component[] {
                                        new JLabel("Displays"),
                                        displaysSP,
                                        new JLabel("Saved Settings"),
                                        displaySettingsSP }, 1,
                                            GuiUtils.WT_Y, GuiUtils.WT_NYNY));

        JTabbedPane sourcePane = new JTabbedPane();
        sourcePane.addTab("Displays", displaysSP);
        sourcePane.addTab("Saved Settings", displaySettingsSP);


        sourceComp = GuiUtils.topCenter(new JLabel("Source"), sourcePane);
        JComponent propComp = GuiUtils.topCenter(new JLabel("Properties"),
                                  propertiesSP);

        JComponent targetComp =
            GuiUtils.topCenter(new JLabel("Target Displays"), rightSP);

        JComponent applyContents = GuiUtils.doLayout(new Component[] {
                                       sourceComp,
                                       propComp, GuiUtils.wrap(applyBtn),
                                       targetComp }, 4, new double[] { 1,
                1.25, 0, 1 }, GuiUtils.WT_Y);

        applyContents = GuiUtils.centerBottom(applyContents,
                GuiUtils.wrap(buttons));
        applyContents = GuiUtils.inset(applyContents, 5);

        List  menus    = new ArrayList();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = GuiUtils.makeDynamicMenu("Select", this,
                             "showSelectMenu");
        menus.add(fileMenu);
        menus.add(editMenu);
        fileMenu.add(GuiUtils.makeMenuItem("Save Selected Properties", this,
                                           "doSave"));
        menuBar       = GuiUtils.makeMenuBar(menus);
        applyContents = GuiUtils.topCenter(menuBar, applyContents);
        return applyContents;

    }

    /**
     * Add items to the Select menu
     *
     * @param menu select menu
     */
    public void showSelectMenu(JMenu menu) {
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
            menu.add(mi);
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
    }


    /**
     * Show the display selection group menu
     *
     * @param comp The component to show near
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
     * Update gui
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
        propertiesSP.validate();
        propertiesSP.getViewport().scrollRectToVisible(new Rectangle(0, 0, 1,
                1));
    }





    /**
     * Save the settings
     */
    public void doSave() {
        List<PropertyValue> propList = new ArrayList<PropertyValue>();
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
        Object selected = displaySettingsList.getSelectedValue();
        displaySettingsList.setListData(
            new Vector(idv.getResourceManager().getDisplaySettings()));
        if (selected != null) {
            displaySettingsList.setSelectedValue(selected, true);

        }
    }


    /**
     * Apply the display settings
     *
     * @param displaySetting The display setting
     */
    public void applyDisplaySetting(DisplaySetting displaySetting) {
        if (dialog != null) {
            dialog.setTitle("Display Settings -- " + displaySetting);
        }
        List<PropertyValue> newProps =
            new ArrayList<PropertyValue>(displaySetting.getPropertyValues());
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
     * make menu items
     *
     *
     * @param displaySettings List of display settings
     * @param object ???
     * @param method Method to call
     * @param labelPrefix Prefix
     *
     * @return Items
     */
    public static List makeDisplaySettingsMenuItems(
            List<DisplaySetting> displaySettings, Object object,
            String method, String labelPrefix) {
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
     * cancel dialog
     */
    public void doCancel() {
        dialog.dispose();
    }

    /**
     * do ok of dialog
     */
    public void doOk() {
        doApply();
        doCancel();
    }

    /**
     * apply dialog
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
     * apply property values to the displays
     *
     * @param selectedDisplays displays
     * @param props properties
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
     * Class PropertyValueWrapper utility class
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.13 $
     */
    public class PropertyValueWrapper {

        /** The value */
        PropertyValue propertyValue;

        /** gui component */
        private JCheckBox cbx;

        /** gui component */
        private JComponent changeBtn;

        /**
         * ctor
         *
         * @param propertyValue the value
         */
        public PropertyValueWrapper(PropertyValue propertyValue) {
            this(propertyValue, false);
        }

        /**
         * ctor
         *
         * @param propertyValue the value
         * @param cbxValue is cbx on
         */
        public PropertyValueWrapper(PropertyValue propertyValue,
                                    boolean cbxValue) {
            this.propertyValue = propertyValue;
            getCheckbox().setSelected(cbxValue);
        }

        /**
         * can the property value be changed in the gui
         *
         * @param v property value
         *
         * @return can we change this
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
         * Change the property value
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
         * Get the cbx
         *
         * @return the cbx
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
         * Change the value
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
         * Change the value
         *
         * @param o new value
         */
        public void changeValueTo(Object o) {
            propertyValue.setValue(o);
            setCheckboxLabel();
        }


        /**
         * set label
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
         * get the category of this property value
         *
         * @return get the category of this property value
         */
        public String getCategory() {
            return propertyValue.getCategory();
        }

        /**
         * Get the name
         *
         * @return the name
         */
        public String getName() {
            return propertyValue.getName();
        }

    }


    /**
     * Get label to show for the given value
     *
     * @param v value
     *
     * @return label
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

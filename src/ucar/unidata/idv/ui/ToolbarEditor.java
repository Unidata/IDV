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

package ucar.unidata.idv.ui;


import org.w3c.dom.Document;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import ucar.unidata.idv.*;
import ucar.unidata.ui.TwoListPanel;
import ucar.unidata.ui.XmlUi;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;



/**
 * This supports editing the toolbar.xml resource.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.18 $
 */
public class ToolbarEditor implements ActionListener {

    /** Action command for moving the selected entry up */
    private static final String CMD_UP = "Up";

    /** Action command for moving the selected entry down */
    private static final String CMD_DOWN = "Down";

    /** Add a &quot;space&quot; entry */
    private static final String CMD_ADDSPACE = "Add Space";

    /** Action command for inserting the action into the toolbar list */
    private static final String CMD_INSERT = "Insert";

    /** Action command for removing the selected toolbar entry */
    private static final String CMD_REMOVE = "Remove";

    /** Action command for reloading the toolbar list with the original items */
    private static final String CMD_RELOAD = "Reload Original";

    /** Action command for reloading the toolbar list with the original items */
    private static final String CMD_REMOVEWRITABLE = "Reload System";

    /** action command */
    private static final String CMD_EXPORTPLUGIN =
        "Export Selected to Plugin";

    /** action command */
    private static final String CMD_EXPORTMENUPLUGIN =
        "Export Selected to Menu Plugin";

    /** For adding a space */
    private static final String SPACE = "-space-";

    /** Gives us unique ids for the sapce objects */
    private int spaceCnt = 0;

    /** The ui manager */
    private IdvUIManager uiManager;


    /** The gui contents */
    private JComponent contents;

    /** Does the real work */
    private TwoListPanel twoListPanel;

    /** The toolbar xml resources */
    XmlResourceCollection resources;


    /** used to export toolbars to plugin */
    private JTextField menuNameFld;

    /** used to export toolbars to plugin */
    private JComboBox menuIdBox;

    /** used to export toolbars to plugin */
    private JCheckBox menuOverwriteCbx;




    /**
     * The ctor
     *
     * @param uiManager The ui manager
     */
    public ToolbarEditor(IdvUIManager uiManager) {
        this.uiManager = uiManager;
        resources = uiManager.getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_TOOLBAR);
        init();
    }


    /**
     * Does the given TFO represent a -space-
     *
     * @param tfo The TFO
     *
     * @return Is it a space
     */
    private boolean isSpace(TwoFacedObject tfo) {
        return tfo.toString().equals(SPACE);
    }

    /**
     * Get the initial list of icons
     *
     * @return List of  TwoFacedObjects
     */
    private Vector getCurrentList() {
        Vector  icons = new Vector();
        Element root  = uiManager.getToolbarRoot();
        if (root != null) {
            NodeList children = XmlUtil.getElements(root);
            for (int i = 0; i < children.getLength(); i++) {
                Element node = (Element) children.item(i);
                if (node.getTagName().equals(XmlUi.TAG_BUTTON)) {
                    String action = XmlUtil.getAttribute(node,
                                        IdvUIManager.ATTR_ACTION);
                    if ( !uiManager.isAction(action)) {
                        continue;
                    }
                    action = uiManager.stripAction(action);
                    String label = uiManager.getActionDescription(action);
                    if (label == null) {
                        label = action;
                    }
                    icons.add(new TwoFacedObject(label, action));
                } else if (node.getTagName().equals(XmlUi.TAG_FILLER)) {
                    icons.add(new TwoFacedObject(SPACE,
                            SPACE + (spaceCnt++)));
                } else {
                    System.err.println("Unknown toobar tag:"
                                       + XmlUtil.toString(node));
                }
            }
        }
        return icons;
    }

    /**
     * Get all the possible icon actions
     *
     * @return All the actions as TwoFacedObject-s
     */
    private Vector getAllActions() {
        List   allActions = uiManager.getActions();
        Vector actions    = new Vector();
        for (int i = 0; i < allActions.size(); i++) {
            String action = (String) allActions.get(i);
            action = uiManager.stripAction(action);
            String label = uiManager.getActionDescription(action);
            if (label == null) {
                label = action;
            }
            actions.add(new TwoFacedObject(label, action));
        }
        return actions;
    }

    /**
     * Get the GUI contents
     *
     * @return The GUI contents
     */
    public JComponent getContents() {
        return contents;
    }


    /**
     * Init me.
     */
    private void init() {
        Vector  currentIcons   = getCurrentList();
        Vector  actions        = getAllActions();

        JButton addSpaceButton = new JButton("Add space");
        addSpaceButton.setActionCommand(CMD_ADDSPACE);
        addSpaceButton.addActionListener(this);

        JButton reloadButton = new JButton(CMD_RELOAD);
        reloadButton.setActionCommand(CMD_RELOAD);
        reloadButton.addActionListener(this);

        JButton removeButton = new JButton(CMD_REMOVEWRITABLE);
        removeButton.setActionCommand(CMD_REMOVEWRITABLE);
        removeButton.addActionListener(this);

        JButton export1Button = new JButton(CMD_EXPORTPLUGIN);
        export1Button.setToolTipText(
            "Export the selected items to the plugin");
        export1Button.setActionCommand(CMD_EXPORTPLUGIN);
        export1Button.addActionListener(this);

        JButton export2Button = new JButton(CMD_EXPORTMENUPLUGIN);
        export2Button.setToolTipText(
            "Export the selected items as a menu to the plugin");
        export2Button.setActionCommand(CMD_EXPORTMENUPLUGIN);
        export2Button.addActionListener(this);

        List buttons = Misc.newList(new JLabel(" "), addSpaceButton,
                                    reloadButton, removeButton);
        buttons.addAll(Misc.newList(new JLabel(" "), export1Button,
                                    export2Button));
        JPanel extra = GuiUtils.vbox(buttons);


        twoListPanel = new TwoListPanel(actions, "Actions", currentIcons,
                                        "Toolbar", extra);


        JLabel notice =
            GuiUtils.cLabel(
                "Note: Toolbar changes will take effect with new windows");
        contents = GuiUtils.centerBottom(twoListPanel, notice);
    }




    /**
     * Export the selected actions as a menu to the plugin manager
     *
     * @param tfos selected actions
     */
    private void doExportToMenu(Object[] tfos) {
        if (menuNameFld == null) {
            menuNameFld = new JTextField("", 10);
            Hashtable menuIds     = uiManager.getMenuIds();
            Vector    menuIdItems = new Vector();
            menuIdItems.add(new TwoFacedObject("None", null));
            for (Enumeration keys =
                    menuIds.keys(); keys.hasMoreElements(); ) {
                String menuId = (String) keys.nextElement();
                if ( !(menuIds.get(menuId) instanceof JMenu)) {
                    continue;
                }
                JMenu  menu  = (JMenu) menuIds.get(menuId);
                String label = menu.getText();
                menuIdItems.add(new TwoFacedObject(label, menuId));
            }
            menuIdBox        = new JComboBox(menuIdItems);
            menuOverwriteCbx = new JCheckBox("Overwrite", false);
            menuOverwriteCbx.setToolTipText(
                "Select this if you want to replace the selected menu with the new menu");
        }

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent dialogContents = GuiUtils.doLayout(new Component[] {
                                        GuiUtils.rLabel("Menu Name:"),
                                        menuNameFld,
                                        GuiUtils.rLabel("Add to Menu:"),
                                        GuiUtils.left(
                                            GuiUtils.hbox(
                                                menuIdBox,
                                                menuOverwriteCbx)) }, 2,
                                                    GuiUtils.WT_NY,
                                                    GuiUtils.WT_N);
        while (true) {
            if ( !GuiUtils.askOkCancel("Export to Menu Plugin",
                                       dialogContents)) {
                return;
            }

            String menuName = menuNameFld.getText().trim();
            if (menuName.length() == 0) {
                LogUtil.userMessage("Please enter a menu name");
                continue;
            }

            StringBuffer xml = new StringBuffer();
            xml.append(XmlUtil.XML_HEADER);
            String idXml = "";
            TwoFacedObject menuIdTfo =
                (TwoFacedObject) menuIdBox.getSelectedItem();
            if (menuIdTfo.getId() != null) {
                idXml = XmlUtil.attr("id", menuIdTfo.getId().toString());
                if (menuOverwriteCbx.isSelected()) {
                    idXml = idXml + XmlUtil.attr("replace", "true");
                }
            }

            xml.append("<menus>\n");
            xml.append("<menu label=\"" + menuName + "\" " + idXml + ">\n");
            for (int i = 0; i < tfos.length; i++) {
                TwoFacedObject tfo = (TwoFacedObject) tfos[i];
                if (tfo.toString().equals(SPACE)) {
                    xml.append("<separator/>\n");
                } else {
                    xml.append(
                        XmlUtil.tag(
                            "menuitem",
                            XmlUtil.attrs(
                                "label", tfo.toString(), "action",
                                "action:" + tfo.getId().toString())));
                }
            }
            xml.append("</menu></menus>\n");
            uiManager.getIdv().getPluginManager().addText(xml.toString(),
                    "menubar.xml");
            return;
        }
    }


    /**
     * Export the actions
     *
     * @param tfos the actions
     */
    private void doExport(Object[] tfos) {
        StringBuffer content = new StringBuffer();
        for (int i = 0; i < tfos.length; i++) {
            TwoFacedObject tfo = (TwoFacedObject) tfos[i];
            if (tfo.toString().equals(SPACE)) {
                content.append("<filler/>\n");
            } else {
                content.append(
                    XmlUtil.tag(
                        "button",
                        XmlUtil.attr(
                            "action", "action:" + tfo.getId().toString())));
            }
        }
        StringBuffer xml = new StringBuffer();
        xml.append(XmlUtil.XML_HEADER);
        xml.append(
            XmlUtil.tag(
                "panel",
                XmlUtil.attrs("layout", "flow", "margin", "4", "vspace", "0")
                + XmlUtil.attrs(
                    "hspace", "2", "i:space", "2", "i:width",
                    "5"), content.toString()));
        LogUtil.userMessage(
            "Note, if a user has changed their toolbar the plugin toolbar will be ignored");
        uiManager.getIdv().getPluginManager().addText(xml.toString(),
                "toolbar.xml");
    }




    /**
     *     Handle the action
     *
     *     @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_EXPORTMENUPLUGIN)
                || cmd.equals(CMD_EXPORTPLUGIN)) {
            Object[] tfos = twoListPanel.getToList().getSelectedValues();
            if (tfos.length == 0) {
                LogUtil.userMessage(
                    "Please select entries in the Toolbar list");
                return;
            }
            if (cmd.equals(CMD_EXPORTMENUPLUGIN)) {
                doExportToMenu(tfos);
            } else {
                doExport(tfos);
            }
        }


        if (cmd.equals(CMD_REMOVEWRITABLE)) {
            if ( !GuiUtils.showYesNoDialog(
                    null,
                    "Are you sure you want to remove any custom toolbar and revert to the system tolbar?",
                    "Delete confirmation")) {
                return;
            }
            resources.removeWritable();
            twoListPanel.reinitialize(getAllActions(), getCurrentList());
        } else if (cmd.equals(CMD_RELOAD)) {
            twoListPanel.reload();
        } else if (cmd.equals(CMD_ADDSPACE)) {
            twoListPanel.insertEntry(new TwoFacedObject(SPACE,
                    SPACE + (spaceCnt++)));
        }
    }

    /**
     * Were there any changes
     *
     * @return Any changes
     */
    public boolean anyChanges() {
        return twoListPanel.getChanged();
    }


    /**
     * Write out the toolbar xml.
     */
    public void doApply() {
        Document doc  = resources.getWritableDocument("<panel/>");
        Element  root = resources.getWritableRoot("<panel/>");
        root.setAttribute(XmlUi.ATTR_LAYOUT, XmlUi.LAYOUT_FLOW);
        root.setAttribute(XmlUi.ATTR_MARGIN, "4");
        root.setAttribute(XmlUi.ATTR_VSPACE, "0");
        root.setAttribute(XmlUi.ATTR_HSPACE, "2");
        root.setAttribute(XmlUi.inheritName(XmlUi.ATTR_SPACE), "2");
        root.setAttribute(XmlUi.inheritName(XmlUi.ATTR_WIDTH), "5");

        XmlUtil.removeChildren(root);
        List icons = twoListPanel.getCurrentEntries();
        for (int i = 0; i < icons.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject) icons.get(i);
            Element        element;
            if (isSpace(tfo)) {
                element = doc.createElement(XmlUi.TAG_FILLER);
                element.setAttribute(XmlUi.ATTR_WIDTH, "5");
            } else {
                element = doc.createElement(XmlUi.TAG_BUTTON);
                element.setAttribute(XmlUi.ATTR_ACTION,
                                     "action:" + tfo.getId().toString());
            }
            root.appendChild(element);
        }
        try {
            resources.writeWritable();
        } catch (Exception exc) {
            LogUtil.logException("Writing toolbar", exc);
        }
    }


}

/**
 * $Id: ,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository.client;
import ucar.unidata.repository.*;


import org.w3c.dom.Document;
import org.w3c.dom.Element;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;



import java.util.ArrayList;

import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class InteractiveRepositoryClient extends RepositoryClient {

    /** _more_ */
    JTree groupTree;

    /** _more_ */
    DefaultTreeModel treeModel;

    /** _more_ */
    GroupNode treeRoot;

    /** _more_ */
    String initialGroup;

    /** _more_ */
    String initialGroupName = "Top";

    /**
     * _more_
     */
    public InteractiveRepositoryClient() {}


    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     *
     * @throws Exception _more_
     */
    public InteractiveRepositoryClient(String hostname, int port, String base)
            throws Exception {
        super(hostname, port, base);
    }


    /**
     * _more_
     *
     * @param error _more_
     * @param exc _more_
     */
    public void handleError(String error, Exception exc) {
        if (exc != null) {
            LogUtil.logException(error, exc);
        } else {
            LogUtil.userErrorMessage(error);
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void handleMessage(String message) {
        LogUtil.userMessage(message);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getSelectedGroup() {
        if (getDefaultGroupId() != null) {
            return getDefaultGroupId();
        }
        return getSelectedGroupFromTree();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSelectedGroupFromTree() {
        TreePath[] paths = groupTree.getSelectionModel().getSelectionPaths();
        if (paths == null) {
            return null;
        }
        for (int i = 0; i < paths.length; i++) {
            Object last = paths[i].getLastPathComponent();
            if (last == null) {
                continue;
            }
            if ( !(last instanceof GroupNode)) {
                continue;
            }
            return ((GroupNode) last).id;
        }
        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getTreeComponent() {
        if (groupTree == null) {
            doMakeGroupTree();
        }
        Dimension defaultDimension = new Dimension(200, 150);
        JScrollPane scroller = GuiUtils.makeScrollPane(groupTree,
                                   (int) defaultDimension.getWidth(),
                                   (int) defaultDimension.getHeight());
        scroller.setPreferredSize(defaultDimension);
        return scroller;
    }

    /**
     * _more_
     *
     * @param node _more_
     */
    public void refreshTreeNode(GroupNode node) {
        node.removeAllChildren();
        node.haveLoaded = false;
        node.checkExpansion();
    }

    /**
     * _more_
     */
    public void refreshTree() {
        refreshTreeNode(treeRoot);
    }

    /**
     * _more_
     */
    private void doMakeGroupTree() {
        treeRoot  = new GroupNode(initialGroupName, ((initialGroup != null)
                ? initialGroup
                : ""), true, false);
        treeModel = new DefaultTreeModel(treeRoot);
        groupTree = new GroupTree(treeModel);
        groupTree.setToolTipText(
            "<html>Right-click to show menu<br>Groups you can add to shown in<b>bold</b><br>Anonymous upload shown in <i>italics</i></html>");
        groupTree.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == e.VK_R) && e.isControlDown()) {
                    refreshTree();
                }
            }
        });
        groupTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                treeClick(event);
            }
        });
        ToolTipManager.sharedInstance().registerComponent(groupTree);
        groupTree.setShowsRootHandles(true);
        final ImageIcon iconOpen =
            GuiUtils.getImageIcon(
                "/ucar/unidata/repository/htdocs/icons/folderopen.png",
                getClass());
        final ImageIcon iconClosed =
            GuiUtils.getImageIcon(
                "/ucar/unidata/repository/htdocs/icons/folderclosed.png",
                getClass());

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            private Font newFont;
            private Font uploadFont;
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if (value instanceof GroupNode) {
                    GroupNode node = (GroupNode) value;
                    if (node.canDoNew || node.canDoUpload) {
                        setForeground(Color.black);
                    } else {
                        setForeground(Color.gray);
                    }

                    if (node.canDoNew) {
                        if (newFont == null) {
                            newFont = getFont().deriveFont(Font.BOLD);
                        }
                        setFont(newFont);
                    } else if (node.canDoUpload) {
                        if (uploadFont == null) {
                            uploadFont = getFont().deriveFont(Font.ITALIC);
                        }
                        setFont(uploadFont);
                    }
                }
                if (expanded || leaf) {
                    setIcon(iconOpen);
                } else {
                    setIcon(iconClosed);
                }
                return this;
            }
        };
        groupTree.setCellRenderer(renderer);
        treeRoot.checkExpansion();
    }

    /**
     * _more_
     *
     * @param event _more_
     */
    public void treeClick(MouseEvent event) {
        TreePath path = groupTree.getPathForLocation(event.getX(),
                            event.getY());
        if (path == null) {
            return;
        }
        Object last = path.getLastPathComponent();
        if (last == null) {
            return;
        }
        if ( !(last instanceof GroupNode)) {
            return;
        }
        GroupNode groupNode = (GroupNode) last;
        if (SwingUtilities.isRightMouseButton(event)) {
            JPopupMenu popup = new JPopupMenu();
            if (groupNode.canDoNew) {
                popup.add(GuiUtils.makeMenuItem("Create New Group", this,
                        "newGroup", groupNode));
            }
            popup.add(GuiUtils.makeMenuItem("Refresh", this,
                                            "refreshTreeNode", groupNode));
            popup.show((Component) event.getSource(), event.getX(),
                       event.getY());
        }

    }

    /**
     * _more_
     *
     * @param groupTreeNode _more_
     */
    public void newGroup(GroupNode groupTreeNode) {
        String parentId = groupTreeNode.id;
        String name = GuiUtils.getInput("Enter a group name to create",
                                        "Name: ", "");
        if (super.newGroup(parentId, name)) {
            groupTreeNode.removeAllChildren();
            groupTreeNode.haveLoaded = false;
            groupTreeNode.checkExpansion();
        }
    }



    /**
     * Class GroupNode _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class GroupNode extends DefaultMutableTreeNode {

        /** Have I loaded */
        private boolean haveLoaded = false;

        /** _more_ */
        private String id;

        /** _more_ */
        private boolean canDoNew = false;

        /** _more_ */
        private boolean canDoUpload = false;

        /**
         * _more_
         *
         * @param name _more_
         * @param id _more_
         * @param canDoNew _more_
         * @param canDoUpload _more_
         */
        public GroupNode(String name, String id, boolean canDoNew,
                         boolean canDoUpload) {
            super(name);
            this.id          = id;
            this.canDoNew    = canDoNew;
            this.canDoUpload = canDoUpload;
        }

        /**
         * Expand if needed
         */
        public void checkExpansion() {
            if (haveLoaded) {
                return;
            }
            haveLoaded = true;
            //Run it in a thread
            Misc.run(this, "checkExpansionInner");
        }


        /**
         * expand if needed. This gets called in a thread from the above method.
         */
        public void checkExpansionInner() {
            try {
                GuiUtils.setCursor(groupTree, GuiUtils.waitCursor);
                boolean  haveId = id.length() > 0;
                String[] args;
                if (haveId) {
                    args = new String[] {
                        ARG_ENTRYID, id, ARG_OUTPUT, "xml.xml", ARG_SESSIONID,
                        getSessionId()
                    };
                } else {
                    args = new String[] { ARG_OUTPUT, "xml.xml",
                                          ARG_SESSIONID, getSessionId() };
                }
                String url = HtmlUtil.url(URL_ENTRY_SHOW.getFullUrl(), args);
                String xml = IOUtil.readContents(url, getClass());
                removeAllChildren();
                //                System.err.println ("URL:" + id);
                //                System.err.println ("XML:" + xml);
                Element root = XmlUtil.getRoot(xml);
                for (Element child : (List<Element>) XmlUtil.findChildren(
                        root, TAG_ENTRY)) {
                    if (XmlUtil.getAttribute(child, ATTR_TYPE, "").equals(
                            TYPE_GROUP) || XmlUtil.getAttribute(
                            child, ATTR_ISGROUP, false)) {
                        GroupNode childNode =
                            new GroupNode(XmlUtil.getAttribute(child,
                                ATTR_NAME), XmlUtil.getAttribute(child,
                                    ATTR_ID), XmlUtil.getAttribute(child,
                                        ATTR_CANDONEW,
                                        false), XmlUtil.getAttribute(child,
                                            ATTR_CANDOUPLOAD, false));

                        childNode.add(
                            new DefaultMutableTreeNode("Please wait..."));
                        this.add(childNode);
                    }
                }
                treeModel.nodeStructureChanged(this);
            } catch (Exception exc) {
                removeAllChildren();
                LogUtil.logException("Error loading group tree", exc);
            } finally {
                GuiUtils.setCursor(groupTree, GuiUtils.normalCursor);
            }
        }
    }


    /**
     * Class GroupTree _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class GroupTree extends JTree {

        /**
         * _more_
         *
         * @param model _more_
         */
        public GroupTree(TreeModel model) {
            super(model);
        }

        /**
         * _more_
         *
         * @param event _more_
         *
         * @return _more_
         */
        public String getToolTipText(MouseEvent event) {
            return "<html>Right-click to show menu<br>Groups you can add to shown in<b>bold</b><br>Anonymous upload shown in <i>italics</i></html>";
        }

        /**
         * _more_
         *
         * @param treePath _more_
         *
         * @throws ExpandVetoException _more_
         */
        public void fireTreeWillExpand(TreePath treePath)
                throws ExpandVetoException {
            Object[] path = treePath.getPath();
            if ((path.length > 0)
                    && (path[path.length - 1] instanceof GroupNode)) {
                ((GroupNode) path[path.length - 1]).checkExpansion();
            }
            super.fireTreeWillExpand(treePath);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean showConfigDialog() {
        JTextField nameFld     = new JTextField(getName(), 30);
        JTextField serverFld   = new JTextField(getHostname(), 30);
        JTextField pathFld     = new JTextField(getUrlBase(), 30);
        JTextField portFld     = new JTextField("" + getPort());
        JTextField passwordFld = new JPasswordField(getPassword());
        JTextField userFld     = new JTextField(getUser(), 30);
        List       comps       = new ArrayList();
        comps.add(GuiUtils.rLabel("Name:"));
        comps.add(GuiUtils.inset(nameFld, 4));
        comps.add(GuiUtils.rLabel("Server:"));
        comps.add(GuiUtils.inset(serverFld, 4));
        comps.add(GuiUtils.rLabel("Port:"));
        comps.add(GuiUtils.inset(portFld, 4));
        comps.add(GuiUtils.rLabel("Base Path:"));
        comps.add(GuiUtils.inset(pathFld, 4));
        comps.add(GuiUtils.rLabel("User Name:"));
        comps.add(GuiUtils.inset(GuiUtils.hbox(userFld,
                new JLabel(" Leave blank for anonyous")), 4));
        comps.add(GuiUtils.rLabel("Password:"));
        comps.add(GuiUtils.inset(passwordFld, 4));
        JPanel contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_Y,
                                            GuiUtils.WT_NNY);
        contents = GuiUtils.topCenter(
            GuiUtils.cLabel("Please provide the following information"),
            contents);

        while (true) {
            if ( !GuiUtils.askOkCancel("Configure access to RAMADDA",
                                       contents)) {
                return false;
            }
            setName(nameFld.getText());
            setHostname(serverFld.getText().trim());
            setPort(new Integer(portFld.getText().trim()).intValue());
            setUrlBase(pathFld.getText().trim());
            setUser(userFld.getText().trim());
            setPassword(passwordFld.getText().trim());
            String[] msg = { "" };
            if (isValidSession(true, msg)) {
                LogUtil.userMessage("Configuration succeeded");
                break;
            }
            LogUtil.userMessage(msg[0]);
        }
        return true;
    }



    /**
     *     _more_
     *
     *     @return _more_
     */
    public boolean doConnect() {
        String[] msg = new String[] { "" };
        if ( !isValidSession(true, msg)) {
            if (getUser().length() != 0) {
                JLabel lbl =
                    new JLabel(
                        "<html>Could not connect to RAMADDA:<blockquote>"
                        + msg[0]
                        + "</blockquote>Do you want to configure the connection?</html>");
                if ( !GuiUtils.showOkCancelDialog(null,
                        "RAMADDA Connection Error", GuiUtils.inset(lbl, 5),
                        null)) {
                    return false;
                }
            }
            return showConfigDialog();
        }
        return true;
    }

    /**
     *  Set the InitialGroup property.
     *
     *  @param value The new value for InitialGroup
     */
    public void setInitialGroup(String value) {
        this.initialGroup = value;
    }

    /**
     *  Get the InitialGroup property.
     *
     *  @return The InitialGroup
     */
    public String getInitialGroup() {
        return this.initialGroup;
    }


    /**
     *  Set the InitialGroupName property.
     *
     *  @param value The new value for InitialGroupName
     */
    public void setInitialGroupName(String value) {
        this.initialGroupName = value;
    }

    /**
     *  Get the InitialGroupName property.
     *
     *  @return The InitialGroupName
     */
    public String getInitialGroupName() {
        return this.initialGroupName;
    }



}


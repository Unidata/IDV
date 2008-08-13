/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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




package ucar.unidata.repository;


import org.w3c.dom.Document;


import org.w3c.dom.Element;

import ucar.unidata.ui.HttpFormEntry;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
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
public class RepositoryClient extends RepositoryBase {


    /** _more_          */
    JTree groupTree;

    /** _more_          */
    DefaultTreeModel treeModel;

    /** _more_          */
    GroupNode treeRoot;

    /** _more_ */
    private String sessionId;

    /** _more_ */
    private String user = "";

    /** _more_ */
    private String password = "";

    /** _more_ */
    private String name = "RAMADDA Client";


    /**
     * _more_
     */
    public RepositoryClient() {}


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
        Dimension defaultDimension = new Dimension(200, 100);
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
        treeRoot  = new GroupNode("Top", "0");
        treeModel = new DefaultTreeModel(treeRoot);
        groupTree = new GroupTree(treeModel);
        groupTree.setToolTipText("Right-click to show menu");
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
            GuiUtils.getImageIcon("/ucar/unidata/repository/htdocs" +ICON_FOLDER_OPEN,
                                  getClass());
        final ImageIcon iconClosed =  
            GuiUtils.getImageIcon("/ucar/unidata/repository/htdocs" +ICON_FOLDER_CLOSED,
                                  getClass());

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if(expanded || leaf)
                    setIcon(iconOpen);
                else
                    setIcon(iconClosed);
                return this;
            }};
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
            popup.add(GuiUtils.makeMenuItem("Create New Group", this,
                                            "newGroup", groupNode));
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
        try {
            String parentId = groupTreeNode.id;
            String name = GuiUtils.getInput("Enter a group name to create",
                                            "Name: ", "");
            if (name == null) {
                return;
            }
            Document doc = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
            Element groupNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                    new String[] {
                ATTR_ID, "1234", ATTR_TYPE, TYPE_GROUP, ATTR_PARENT, parentId,
                ATTR_NAME, name
            });

            String xml     = XmlUtil.toString(root);
            List   entries = new ArrayList();
            addUrlArgs(entries);
            entries.add(new HttpFormEntry(ARG_FILE, "entries.xml",
                                          xml.getBytes()));
            String[] result = HttpFormEntry.doPost(entries,
                                  URL_ENTRY_XMLCREATE.getFullUrl());

            if (result[0] != null) {
                LogUtil.userErrorMessage("Error creating group:\n"
                                         + result[0]);
                return;
            }
            Element response = XmlUtil.getRoot(result[1]);
            if (responseOk(response)) {
                LogUtil.userMessage("Group created");
                groupTreeNode.removeAllChildren();
                groupTreeNode.haveLoaded = false;
                groupTreeNode.checkExpansion();
                return;
            }
            String body = XmlUtil.getChildText(response).trim();
            LogUtil.userErrorMessage("Error creating group:" + body);
        } catch (Exception exc) {
            LogUtil.logException("Error creating group", exc);
        }
    }

    /**
     * _more_
     *
     * @param entries _more_
     */
    public void addUrlArgs(List entries) {
        entries.add(HttpFormEntry.hidden(ARG_SESSIONID, getSessionId()));
        entries.add(HttpFormEntry.hidden(ARG_OUTPUT, "xml"));
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

        /** _more_          */
        private String id;

        /**
         * _more_
         *
         * @param name _more_
         * @param id _more_
         */
        public GroupNode(String name, String id) {
            super(name);
            this.id = id;
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
                removeAllChildren();
                String url = HtmlUtil.url(URL_ENTRY_SHOW.getFullUrl(),
                                          new String[] { ARG_ID,
                        id, ARG_OUTPUT, "xml.xml" });
                String  xml  = IOUtil.readContents(url, getClass());
                Element root = XmlUtil.getRoot(xml);
                for (Element child : (List<Element>) XmlUtil.findChildren(
                        root, TAG_GROUP)) {
                    GroupNode childNode =
                        new GroupNode(XmlUtil.getAttribute(child, ATTR_NAME),
                                      XmlUtil.getAttribute(child, ATTR_ID));
                    childNode.add(
                        new DefaultMutableTreeNode("Please wait..."));
                    this.add(childNode);
                }
                treeModel.nodeStructureChanged(this);
            } catch (Exception exc) {
                LogUtil.logException("Error loading group tree", exc);
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
            return "Right-click to show context menu";
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
     *     _more_
     *    
     *     @return _more_
     */
    public boolean doConnect() {
        String[] msg = new String[] { "" };

        if ( !isValidSession(true, msg)) {
            if (user.length() != 0) {
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
     * _more_
     *
     * @return _more_
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean showConfigDialog() {
        JTextField nameFld     = new JTextField(name, 30);
        JTextField serverFld   = new JTextField(getHostname(), 30);
        JTextField pathFld     = new JTextField(getUrlBase(), 30);
        JTextField portFld     = new JTextField("" + getPort());
        JTextField passwordFld = new JPasswordField(password);
        JTextField userFld     = new JTextField(user, 30);
        List       comps       = new ArrayList();
        comps.add(GuiUtils.rLabel("Name:"));
        comps.add(GuiUtils.inset(nameFld, 4));
        comps.add(GuiUtils.rLabel("Server:"));
        comps.add(GuiUtils.inset(serverFld, 4));
        comps.add(GuiUtils.rLabel("Port:"));
        comps.add(GuiUtils.inset(portFld, 4));
        comps.add(GuiUtils.rLabel("Base Path:"));
        comps.add(GuiUtils.inset(pathFld, 4));
        comps.add(GuiUtils.rLabel("User name:"));
        comps.add(GuiUtils.inset(userFld, 4));
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
            user     = userFld.getText().trim();
            password = passwordFld.getText().trim();
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
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(String hostname, int port, String base)
            throws Exception {
        super(hostname, port);
        setUrlBase(base);
    }


    /**
     * _more_
     *
     * @param doLogin _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public boolean isValidSession(boolean doLogin, String[] msg) {
        if ( !isValidSession(msg)) {
            if (doLogin) {
                return doLogin(msg);
            }
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     *
     * @param msg _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isValidSession(String[] msg) {
        if (sessionId == null) {
            msg[0] = "No session id";
            return false;
        }
        try {
            String url = HtmlUtil.url(URL_USER_HOME.getFullUrl(),
                                      new String[] { ARG_OUTPUT,
                    "xml", ARG_SESSIONID, sessionId });
            String  contents = IOUtil.readContents(url, getClass());
            Element root     = XmlUtil.getRoot(contents);
            if (responseOk(root)) {
                return true;
            } else {
                msg[0] = XmlUtil.getChildText(root).trim();
                return false;
            }
        } catch (Exception exc) {
            msg[0] = "Could not connect to server:" + getHostname();
            return false;
        }
    }

    /**
     * _more_
     *
     * @param root _more_
     *
     * @return _more_
     */
    public boolean responseOk(Element root) {
        return XmlUtil.getAttribute(root, ATTR_CODE).equals("ok");
    }



    /**
     * _more_
     *
     *
     * @param msg _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean doLogin(String[] msg) {
        try {
            String url = HtmlUtil.url(URL_USER_LOGIN.getFullUrl(),
                                      new String[] {
                ARG_OUTPUT, "xml", ARG_USER_PASSWORD, getPassword(),
                ARG_USER_ID, getUser()
            });
            String  contents = IOUtil.readContents(url, getClass());
            Element root     = XmlUtil.getRoot(contents);
            String  body     = XmlUtil.getChildText(root).trim();
            if (responseOk(root)) {
                sessionId = body;
                return true;
            } else {
                msg[0] = body;
                return false;
            }
        } catch (Exception exc) {
            msg[0] = "Could not connect to server:" + getHostname();
        }
        return false;
    }

    /**
     *  Set the Password property.
     *
     *  @param value The new value for Password
     */
    private void setPassword(String value) {
        password = value;
    }

    /**
     *  Get the Password property.
     *
     *  @return The Password
     */
    private String getPassword() {
        return password;
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @param value The new value
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(XmlUtil.decodeBase64(new String(value)));
        }
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @return The Password
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }
        return XmlUtil.encodeBase64(password.getBytes()).getBytes();
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUser() {
        return user;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }



}


/*
 * $Id: DisplayTree.java,v 1.11 2007/04/16 21:32:37 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control.multi;


import org.w3c.dom.Document;

import org.w3c.dom.Element;


import ucar.unidata.idv.*;


import ucar.unidata.ui.DndTree;



import ucar.unidata.util.FileManager;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;


/**
 * Class DisplayTree Gives a tree gui for editing bundles
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.11 $
 */
public class DisplayTree extends DndTree implements ActionListener {


    /** The window */
    private JFrame frame;

    /** The root of the tree */
    private MyTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;

    /** A mapping from tree node to component */
    private Hashtable nodeToData;

    /** The display control */
    private MultiDisplayHolder displayControl;

    /** Icon to use for categories */
    private ImageIcon groupIcon;

    /** Icon to use for leafs */
    private ImageIcon leafIcon;






    /**
     * Create the tree with the given bundle type
     *
     *
     * @param displayControl The display control
     */
    public DisplayTree(MultiDisplayHolder displayControl) {

        this.displayControl = displayControl;
        groupIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/Folder.gif",
                                          getClass());
        leafIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/File.gif",
                                         getClass());


        setToolTipText(
            "<html>Right click to show popup menu.<br>Drag to move groups or harts</html>");

        treeRoot = new MyTreeNode();
        setRootVisible(false);
        setShowsRootHandles(true);
        treeModel = new DefaultTreeModel(treeRoot);
        setModel(treeModel);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if ((nodeToData == null) || (value == null)) {
                    return this;
                }
                Object data = nodeToData.get(value);
                if (data == null) {
                    setIcon(groupIcon);
                    return this;
                }
                setToolTipText(
                    "<html>Right click to show component menu.<br>Drag to move</html>");

                if ( !(data instanceof DisplayGroup)) {
                    setIcon(leafIcon);
                } else {
                    setIcon(groupIcon);
                }
                return this;
            }
        };
        setCellRenderer(renderer);



        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
		if (GuiUtils.isDeleteEvent(e)) {
                    deleteSelected();
                }
            }
        });

        getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        //            TreeSelectionModel.SINGLE_TREE_SELECTION);


        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                TreePath path = getPathForLocation(event.getX(),
                                    event.getY());
                DisplayComponent comp = findCompAtPath(path);
                if (comp == null) {
                    return;
                }
                getDisplayControl().showDisplayComponent(comp);
                frame.toFront();
                if (event.getClickCount() > 1) {
                    comp.showProperties();
                    return;
                }

                if ( !SwingUtilities.isRightMouseButton(event)) {
                    return;
                }

                clearSelection();
                addSelectionPath(path);
                comp.showPopup((JComponent) event.getSource(), event.getX(),
                               event.getY());
            }
        });
        loadComponents();

        String    title            = displayControl.toString();
        Dimension defaultDimension = new Dimension(300, 400);
        JScrollPane sp = GuiUtils.makeScrollPane(this,
                             (int) defaultDimension.getWidth(),
                             (int) defaultDimension.getHeight());
        sp.setPreferredSize(defaultDimension);
        JPanel buttons = GuiUtils.makeButtons(this,
                             new String[] { GuiUtils.CMD_CLOSE });

        JPanel contents = GuiUtils.centerBottom(sp, buttons);
        frame = GuiUtils.createFrame(title);
        frame.getContentPane().add(contents);
        frame.pack();
    }


    /**
     * Get the display control
     *
     * @return the display control
     */
    public MultiDisplayHolder getDisplayControl() {
        return displayControl;
    }

    /**
     * Close the window
     */
    public void close() {
        if (frame != null) {
            frame.dispose();
        }
    }

    /**
     * Handle the action
     *
     * @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            close();
        }
    }

    /**
     * Show the window
     *
     * @param src The component to show me near
     */
    public void show(Component src) {
        loadComponents();
        GuiUtils.showDialogNearSrc(src, frame);
        //        frame.show();
    }


    /**
     * Ok to drag the node
     *
     * @param sourceNode The node to drag
     *
     * @return Ok to drag
     */
    protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
        return sourceNode.getParent() != null;
    }


    /**
     * Class MyTreeNode is used in the tree
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.11 $
     */
    private static class MyTreeNode extends DefaultMutableTreeNode {

        /** The component I represent */
        DisplayComponent comp;

        /**
         * ctor
         *
         * @param comp the component
         */
        public MyTreeNode(DisplayComponent comp) {
            super(comp.getName());
            this.comp = comp;
        }

        /**
         * ctor
         */
        public MyTreeNode() {
            super("");
        }

        /**
         * Do I represent a DisplayGroup
         *
         * @return represent a DisplayGroup
         */
        public boolean isGroup() {
            return (comp != null) && (comp instanceof DisplayGroup);
        }

        /**
         * DO I have a non-null component
         *
         * @return is a component
         */
        public boolean isComponent() {
            return comp != null;
        }


    }


    /**
     * Ok to drop the node
     *
     *
     * @param sourceNode The dragged node
     * @param destNode Where to drop
     * @param isOn Is the drop right on a node
     *
     * @return Ok to drop
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode,
                               boolean isOn) {

        MyTreeNode source = (MyTreeNode) sourceNode;
        MyTreeNode dest   = (MyTreeNode) destNode;

        //        if ( !dest.isGroup()) {
        //            return false;
        //        }
        if (destNode == sourceNode.getParent()) {
            //            return false;
        }
        while (dest != null) {
            if (dest == sourceNode) {
                return false;
            }
            dest = (MyTreeNode) dest.getParent();
        }
        return true;
    }



    /**
     * Handle the DND drop
     *
     *
     * @param sourceNode The dragged node
     * @param destNode Where to drop
     * @param isOn Is the drop right on a node
     */
    protected void doDrop(DefaultMutableTreeNode sourceNode,
                          DefaultMutableTreeNode destNode, boolean isOn) {
        try {
            MyTreeNode source = (MyTreeNode) sourceNode;
            MyTreeNode dest   = (MyTreeNode) destNode;
            //      source.comp.setDisplayGroup((DisplayGroup)dest.comp);
            DisplayGroup oldGroup = source.comp.getDisplayGroup();
            if (oldGroup != null) {
                oldGroup.removeDisplayComponent(source.comp);
            } else {
                if (source.comp instanceof DisplayGroup) {
                    displayControl.removeDisplayGroup(
                        (DisplayGroup) source.comp);
                }
            }

            DisplayGroup target = null;
            int          index  = -1;
            if ( !dest.isGroup() || !isOn) {
                target = dest.comp.getDisplayGroup();
                if (target == null) {
                    index  = 0;
                    target = (DisplayGroup) dest.comp;
                } else {
                    index = target.indexOf(dest.comp);
                    index++;
                }
                target.addDisplayComponent(source.comp, index);
            } else {
                if (isOn) {
                    target = (DisplayGroup) dest.comp;
                    target.addDisplayComponent(source.comp);
                } else {
                    target = dest.comp.getDisplayGroup();
                    if (target == null) {
                        //                        dest.comp.addDisplayComponent(source.comp);
                    } else {}

                }
            }


            loadComponents();
            displayControl.showDisplayComponent(dest.comp);
        } catch (Exception exc) {
            LogUtil.logException("An error has occurred", exc);
        }
    }





    /**
     * Delete the selected item in the tree
     */
    public void deleteSelected() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return;
        }
        DisplayComponent comp = findCompAtPath(paths[0]);
        if (comp == null) {
            return;
        }
        displayControl.removeDisplayComponent(comp);
    }



    /**
     * Create the tree recursively
     *
     * @param displayGroup The group to recurse on
     * @param treeNode The corresponding tree node
     */
    protected void recurse(DisplayGroup displayGroup, MyTreeNode treeNode) {
        List displayComponents = displayGroup.getDisplayComponents();
        for (int i = 0; i < displayComponents.size(); i++) {
            DisplayComponent displayComponent =
                (DisplayComponent) displayComponents.get(i);
            MyTreeNode childNode = new MyTreeNode(displayComponent);
            nodeToData.put(childNode, displayComponent);
            treeNode.add(childNode);
            if (displayComponent instanceof DisplayGroup) {
                recurse((DisplayGroup) displayComponent, childNode);
            }
        }

    }


    /**
     * Load in the bundles into the tree
     */
    protected void loadComponents() {

        Enumeration paths =
            getExpandedDescendants(new TreePath(treeRoot.getPath()));
        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);
        nodeToData = new Hashtable();
        treeRoot.removeAllChildren();
        treeModel.nodeStructureChanged(treeRoot);

        List displayGroups = displayControl.getDisplayGroups();
        for (int groupIdx = 0; groupIdx < displayGroups.size(); groupIdx++) {
            DisplayGroup displayGroup =
                (DisplayGroup) displayGroups.get(groupIdx);
            MyTreeNode treeNode = new MyTreeNode(displayGroup);
            nodeToData.put(treeNode, displayGroup);
            treeRoot.add(treeNode);
            recurse(displayGroup, treeNode);
        }


        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }




    /**
     * Find the data associated with the given  tree path
     *
     * @param path The path
     *
     * @return The data
     */

    private DisplayComponent findCompAtPath(TreePath path) {
        if (path == null) {
            return null;
        }
        MyTreeNode last = (MyTreeNode) path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        return last.comp;
    }

}


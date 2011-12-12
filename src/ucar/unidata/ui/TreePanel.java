/*
 * $Id: TreePanel.java,v 1.6 2007/07/30 19:37:18 jeffmc Exp $
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




package ucar.unidata.ui;


import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;



import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;

import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.*;



/**
 *  This class shows a tree on the left and a card panel on the right
 */

public class TreePanel extends JPanel implements TreeSelectionListener {

    /** _more_          */
    public static final String CATEGORY_DELIMITER = ">";

    /** the tree */
    private JTree tree;

    /** The scroller */
    private JScrollPane treeView;

    /** _more_ */
    private boolean useSplitPane = true;

    /** _more_ */
    private int treeWidth = -1;


    /** The root */
    private DefaultMutableTreeNode root;

    /** the model */
    private DefaultTreeModel treeModel;

    /** The panel */
    private GuiUtils.CardLayoutPanel panel;

    /** _more_ */
    private JPanel emptyPanel;

    /** _more_ */
    private Hashtable<Object,JComponent> catComponents = new Hashtable<Object,JComponent>();

    private Hashtable<Object,List<JComponent>> buttonsForCategory = new Hashtable<Object,List<JComponent>>();

    /** Maps categories to tree node */
    private Hashtable catToNode = new Hashtable();

    /** Maps components to tree node */
    private Hashtable compToNode = new Hashtable();

    /** ok to respond to selection changes */
    private boolean okToUpdateTree = true;

    /**
     * Class MyTreeNode _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.6 $
     */
    public static class MyTreeNode extends DefaultMutableTreeNode {

        /** icon */
        public ImageIcon icon;

        /** _more_          */
        public Object object;

        /**
         * ctor
         *
         * @param o object
         * @param icon icon
         * @param object _more_
         */
        public MyTreeNode(Object o, ImageIcon icon, Object object) {
            super(o);
            this.icon   = icon;
            this.object = object;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Object getObject() {
            return object;
        }

    }


    /**
     * ctor
     */
    public TreePanel() {
        this(true, -1);
    }

    /**
     * _more_
     *
     * @param useSplitPane _more_
     * @param treeWidth _more_
     */
    public TreePanel(boolean useSplitPane, int treeWidth) {
        this.useSplitPane = useSplitPane;
        this.treeWidth    = treeWidth;

        setLayout(new BorderLayout());
        root      = new DefaultMutableTreeNode("");
        treeModel = new DefaultTreeModel(root);
        tree      = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = doMakeTreeCellRenderer();
        renderer.setIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        tree.setCellRenderer(renderer);


        panel = new GuiUtils.CardLayoutPanel() {
            public void show(Component comp) {
                super.show(comp);
                showPath(panel.getVisibleComponent());
            }
        };
        catToNode = new Hashtable();
        treeView  = new JScrollPane(tree);
        //        tree.setBackground(getBackground());
        if (treeWidth > 0) {
            treeView.setPreferredSize(new Dimension(treeWidth, 100));
        }


        JComponent center;
        if (useSplitPane) {
            double weight = 0.2;
            JSplitPane splitPane = ((treeWidth > 0)
                                    ? GuiUtils.hsplit(treeView, panel,
                                                      treeWidth,weight)
                                    : GuiUtils.hsplit(treeView, panel, 150,weight));
            center = splitPane;
            splitPane.setOneTouchExpandable(true);
        } else {
            center = GuiUtils.leftCenter(treeView, panel);
        }
        this.add(BorderLayout.CENTER, center);

        //        this.add(BorderLayout.WEST, treeView);
        //        this.add(BorderLayout.CENTER, panel);
        tree.addTreeSelectionListener(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DefaultTreeCellRenderer doMakeTreeCellRenderer() {
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if ( !(value instanceof MyTreeNode)) {
                    return this;
                }
                MyTreeNode node = (MyTreeNode) value;
                if (node.icon != null) {
                    setIcon(node.icon);
                } else {
                    setIcon(null);
                }
                return this;
            }
        };
        return renderer;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public Component getVisibleComponent() {
        return panel.getVisibleComponent();
    }

    /**
     * Handle tree selection changed
     *
     * @param e event
     */
    public void valueChanged(TreeSelectionEvent e) {
        if ( !okToUpdateTree) {
            return;
        }


        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            TwoFacedObject tfo = (TwoFacedObject) node.getUserObject();
            panel.show((Component) tfo.getId());
        } else {
            if (emptyPanel == null) {
                if (emptyPanel == null) {
                    panel.addCard(emptyPanel =
                        new JPanel(new BorderLayout()));
                }
            }
            emptyPanel.removeAll();
            if (node.getUserObject() instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) node.getUserObject();
                JComponent interior = catComponents.get(tfo.getId());
                if (interior != null) {
                    if ( !panel.contains(interior)) {
                        panel.addCard(interior);
                    }
                    panel.show(interior);
                    return;
                }
                List<JComponent> buttons = buttonsForCategory.get(tfo.getId());
                if(buttons!=null) {
                    emptyPanel.add(GuiUtils.top(GuiUtils.doLayout(new Component[]{GuiUtils.filler(50,5),
                                                              GuiUtils.vbox(buttons),
                                                              GuiUtils.filler(50,5)},
                                                                  3, GuiUtils.WT_Y,GuiUtils.WT_N)));
                    emptyPanel.invalidate();
                    emptyPanel.validate();
                    emptyPanel.repaint();
                }
            }
            panel.show(emptyPanel);
        }
    }


    /**
     * _more_
     *
     * @param comp _more_
     * @param icon _more_
     */
    public void setIcon(Component comp, ImageIcon icon) {
        MyTreeNode node = (MyTreeNode) compToNode.get(comp);
        if (node != null) {
            node.icon = icon;
            tree.repaint();
        }
    }


    /**
     * Add the component to the panel
     *
     * @param component component
     * @param category tree category. May be null.
     * @param label Tree node label
     * @param icon Node icon. May be null.
     */
    public void addComponent(JComponent component, String category,
                             String label, ImageIcon icon) {
        addComponent(component, category, label, icon, "");
    }


    /**
     * _more_
     *
     * @param component _more_
     * @param category _more_
     * @param label _more_
     * @param icon _more_
     * @param object _more_
     */
    public void addComponent(JComponent component, String category,
                             String label, ImageIcon icon, Object object) {
        TwoFacedObject         tfo = new TwoFacedObject(label, component);
        DefaultMutableTreeNode panelNode = new MyTreeNode(tfo, icon, object);
        compToNode.put(component, panelNode);
        if (category == null) {
            root.add(panelNode);
        } else {
            List<String> toks = StringUtil.split(category, CATEGORY_DELIMITER, true,
                                         true);
            String                 catSoFar = "";
            DefaultMutableTreeNode catNode  = root;
            for (int i = 0; i < toks.size(); i++) {
                String cat = (String) toks.get(i);
                catSoFar = catSoFar + CATEGORY_DELIMITER + cat;
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) catToNode.get(catSoFar);
                if (node == null) {
                    TwoFacedObject catTfo = new TwoFacedObject(cat, catSoFar);
                    node = new DefaultMutableTreeNode(catTfo);
                    catToNode.put(catSoFar, node);
                    catNode.add(node);
                }
                catNode = node;
            }
            catNode.add(panelNode);
            List<JComponent> comps = buttonsForCategory.get(catSoFar);
            if(comps==null) {
                comps = new ArrayList<JComponent>();
                buttonsForCategory.put(catSoFar,comps);
            }
            if(toks.size()>0) {
                comps.add(GuiUtils.makeButton(label,
                                              this,"show", component));
            }
        }
        panel.addCard(component);
        treeChanged();
    }


    /**
     * _more_
     */
    private void treeChanged() {
        Hashtable stuff = GuiUtils.initializeExpandedPathsBeforeChange(tree,
                              root);
        treeModel.nodeStructureChanged(root);
        GuiUtils.expandPathsAfterChange(tree, stuff, root);
    }

    /**
     * _more_
     *
     * @param cat _more_
     * @param comp _more_
     */
    public void addCategoryComponent(String cat, JComponent comp) {
        catComponents.put(CATEGORY_DELIMITER + cat, comp);
    }


    /**
     * _more_
     *
     * @param component _more_
     */
    public void removeComponent(JComponent component) {
        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) compToNode.get(component);
        if (node == null) {
            return;
        }
        compToNode.remove(component);
        if (node.getParent() != null) {
            node.removeFromParent();
        }
        panel.remove(component);
        treeChanged();

    }


    /**
     * _more_
     *
     * @param component _more_
     */
    public void show(Component component) {
        panel.show(component);
    }


    /**
     * Show the tree node that corresponds to the component
     *
     * @param component comp
     */
    public void showPath(Component component) {
        if (component != null) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) compToNode.get(component);
            if (node != null) {
                TreePath path = new TreePath(treeModel.getPathToRoot(node));
                okToUpdateTree = false;
                tree.setSelectionPath(path);
                tree.expandPath(path);
                okToUpdateTree = true;
            }
        }
    }


    /**
     * Open all paths
     */
    public void openAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandPath(tree.getPathForRow(i));
        }
        showPath(panel.getVisibleComponent());
    }


    /**
     * Close all paths
     */
    public void closeAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.collapsePath(tree.getPathForRow(i));
        }
        showPath(panel.getVisibleComponent());
    }


}


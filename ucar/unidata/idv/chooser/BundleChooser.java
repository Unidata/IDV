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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;



import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceResults;



import ucar.unidata.idv.*;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;




import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;






/**
 * A chooser that shows the SavedBundles from the bundles.xml
 *
 * @author IDV development team
 */


public class BundleChooser extends IdvChooser {

    /** The JTree */
    private JTree tree;


    /**
     * Create the BundleChooser, passing in the manager and the xml element
     * from choosers.xml
     *
     * @param mgr The manager
     * @param root The xml root
     *
     */
    public BundleChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Create the GUI for this chooser
     *
     * @return The gui of this chooser
     */
    protected JComponent doMakeContents() {
        tree = new JTree();
        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                treeClick(event);
            }
        });
        doUpdate();

        Dimension defaultDimension = new Dimension(300, 400);
        JScrollPane scroller = GuiUtils.makeScrollPane(tree,
                                   (int) defaultDimension.getWidth(),
                                   (int) defaultDimension.getHeight());
        scroller.setPreferredSize(defaultDimension);
        JComponent bottomButtons = getDefaultButtons();
        return GuiUtils.centerBottom(scroller, bottomButtons);
    }


    /**
     *  Reload  the jtree
     */
    public void doUpdate() {
        DefaultMutableTreeNode treeRoot =
            new DefaultMutableTreeNode("Bundles");

        DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
        Hashtable        catNodes  = new Hashtable();
        List bundles = getIdv().getPersistenceManager().getBundles(
                           IdvPersistenceManager.BUNDLES_FAVORITES);
        for (int i = 0; i < bundles.size(); i++) {
            SavedBundle            bundle     = (SavedBundle) bundles.get(i);
            List                   categories = bundle.getCategories();
            DefaultMutableTreeNode catNode    = treeRoot;
            String                 fullCat    = "";
            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                String cat = (String) categories.get(catIdx);
                fullCat = fullCat + "---" + cat;
                DefaultMutableTreeNode tmpNode =
                    (DefaultMutableTreeNode) catNodes.get(fullCat);
                if (tmpNode == null) {
                    tmpNode = new DefaultMutableTreeNode(cat);
                    catNode.add(tmpNode);
                    catNodes.put(fullCat, tmpNode);
                }
                catNode = tmpNode;
            }
            DefaultMutableTreeNode bundleNode =
                new DefaultMutableTreeNode(bundle);
            catNode.add(bundleNode);
        }
        tree.setModel(treeModel);
    }



    /**
     * If a bundle is selected then load it in and close the chooser.
     */
    public void doLoadInThread() {
        TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return;
        }
        loadBundle(getBundleAtPath(paths[0]));
    }


    /**
     * Load in the given bundle and, if successful, close the chooser
     *
     * @param bundle The bundle to load
     */
    public void loadBundle(SavedBundle bundle) {
        if (bundle == null) {
            return;
        }
        showWaitCursor();
        if (getIdv().handleAction(bundle.getUrl(), null)) {
            closeChooser();
        }
        showNormalCursor();
    }

    /**
     * Find the SavedBundle associated with the given  tree path
     *
     * @param path The path
     *
     * @return The saved bundle object or null.
     */
    protected SavedBundle getBundleAtPath(TreePath path) {
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode last =
            (DefaultMutableTreeNode) path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        Object userData = last.getUserObject();
        if ((userData == null) || !(userData instanceof SavedBundle)) {
            return null;
        }
        return (SavedBundle) userData;
    }

    /**
     * Handle the tree click event
     *
     * @param event The event
     */
    protected void treeClick(MouseEvent event) {
        setHaveData(false);
        SavedBundle bundle =
            getBundleAtPath(tree.getPathForLocation(event.getX(),
                event.getY()));
        if (bundle == null) {
            return;
        }
        setHaveData(true);
        if (event.getClickCount() > 1) {
            loadBundle(bundle);
        }


    }


    /**
     * Overwrite base class method to return a different name for the load button
     *
     * @return Load button name
     */
    public String getLoadCommandName() {
        return "Load bundle";
    }

}

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


import ucar.unidata.data.DerivedDataDescriptor;


import ucar.unidata.geoloc.*;

import ucar.unidata.idv.*;
import ucar.unidata.idv.control.DisplaySetting;

import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;





/**
 *
 * @author IDV development team
 */
public class ResourceViewer extends IdvManager {

    /** _more_ */
    private JDialog dialog;

    /** _more_ */
    private JTabbedPane tabbedPane;

    /** _more_          */
    private JCheckBox localOnlyCbx;

    /**
     * _more_
     *
     * @param idv _more_
     */
    public ResourceViewer(IntegratedDataViewer idv) {
        super(idv);
        init();
    }


    /**
     * _more_
     */
    private void init() {
        tabbedPane   = new JTabbedPane();
        localOnlyCbx = new JCheckBox("Local Only", false);
        localOnlyCbx.setToolTipText("Only show local resources");
        localOnlyCbx.addActionListener(GuiUtils.makeActionListener(this,
                "updateTrees", null));
        JButton listResources = GuiUtils.makeButton("List Sources",
                                    getIdv().getResourceManager(),
                                    "showHtmlView", null);

        JComponent buttons = GuiUtils.leftCenter(
                                 localOnlyCbx,
                                 GuiUtils.wrap(
                                     GuiUtils.hbox(
                                         GuiUtils.makeButton(
                                             "Update", this,
                                             "updateTrees"), listResources,
                                                 5)));
        contents = GuiUtils.inset(GuiUtils.centerBottom(tabbedPane, buttons),
                                  3);
        updateTrees();
    }

    /**
     * _more_
     */
    public void updateTrees() {
        int tabIdx = tabbedPane.getSelectedIndex();
        tabbedPane.removeAll();
        List<ResourceTree> trees = new ArrayList<ResourceTree>();
        trees.add(
            makeTree(
                "Favorite Bundles",
                getPersistenceManager().getBundles(
                    IdvPersistenceManager.BUNDLES_FAVORITES)));

        trees.add(makeTree("Formulas",
                           getIdv().getJythonManager().getDescriptors()));

        trees.add(makeTree("Color Tables",
                           getIdv().getColorTableManager().getColorTables()));

        trees.add(
            makeTree(
                "Layout Models",
                getIdv().getStationModelManager().getStationModels()));

        trees.add(makeTree("Param Defaults",
                           getIdv().getParamDefaultsEditor().getResources()));

        /*
          aliases are not supported yet
        trees.add(
            makeTree(
                "Param Aliases",
                getIdv().getAliasEditor().getResources()));
        */

        trees.add(
            makeTree(
                "Projections",
                getIdv().getIdvProjectionManager().getProjections()));

        trees.add(
            makeTree(
                "Display Settings",
                getIdv().getResourceManager().getDisplaySettings()));


        for (ResourceTree tree : trees) {
            tabbedPane.addTab(tree.label, tree.getContents());
        }
        if (tabIdx >= 0) {
            tabbedPane.setSelectedIndex(tabIdx);
        }
        tabbedPane.invalidate();
        tabbedPane.repaint();
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param objects _more_
     *
     * @return _more_
     */
    private ResourceTree makeTree(String label, List objects) {
        List<CategorizedThing> things    = new ArrayList<CategorizedThing>();
        boolean                localOnly = localOnlyCbx.isSelected();
        for (Object o : objects) {
            CategorizedThing thing = makeThing(o);
            if (localOnly && (thing.state == thing.STATE_SYSTEM)) {
                continue;
            }
            if (thing != null) {
                things.add(thing);
            }
        }
        return new ResourceTree(label, things);
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Jan 12, '10
     * @author         Enter your name here...    
     */
    public static class ResourceWrapper {

        /** _more_          */
        boolean local;

        /** _more_          */
        List categories;

        /** _more_          */
        String name;

        /** _more_          */
        Object object;

        /**
         * _more_
         *
         * @param o _more_
         * @param name _more_
         * @param category _more_
         * @param local _more_
         */
        public ResourceWrapper(Object o, String name, String category,
                               boolean local) {
            this.categories = ((category != null)
                               ? StringUtil.split(category, ">", true, true)
                               : null);
            this.local      = local;
            this.name       = name;
            this.object     = o;
        }
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    private CategorizedThing makeThing(Object o) {
        if (o instanceof ResourceWrapper) {
            ResourceWrapper rw = (ResourceWrapper) o;
            return new CategorizedThing(o, rw.name, rw.categories, rw.local);
        }

        if (o instanceof SavedBundle) {
            SavedBundle b = (SavedBundle) o;
            return new CategorizedThing(o, b.getName(), b.getCategories(),
                                        b.getLocal());
        }

        if (o instanceof DisplaySetting) {
            DisplaySetting ds  = (DisplaySetting) o;
            String         cat = ds.getCategory();
            return new CategorizedThing(o, ds.getNameWithoutCategory(),
                                        ((cat != null)
                                         ? StringUtil.split(cat, ">", true,
                                         true)
                                         : null), ds.getIsLocal());
        }

        if (o instanceof StationModel) {
            StationModel sm   = (StationModel) o;
            boolean isLocal   = getIdv().getStationModelManager().isUsers(sm);
            List toks = StringUtil.split(sm.getName(), ">", true, true);
            String       name = (String) toks.get(toks.size() - 1);
            toks.remove(toks.size() - 1);
            return new CategorizedThing(o, name, toks, isLocal);
        }

        if (o instanceof ColorTable) {
            ColorTable ct    = (ColorTable) o;
            boolean    local = getIdv().getColorTableManager().isUsers(ct);
            return new CategorizedThing(o, ct.getName(),
                                        StringUtil.split(ct.getCategory(),
                                            ">", true, true), local);
        }
        if (o instanceof DerivedDataDescriptor) {
            DerivedDataDescriptor dds = (DerivedDataDescriptor) o;
            String                cat = null;
            if (dds.getDisplayCategory() != null) {
                cat = "" + dds.getDisplayCategory();
            }
            return new CategorizedThing(o, dds.getDescription(),
                                        ((cat != null)
                                         ? Misc.newList(cat)
                                         : null), dds.getIsLocalUsers());
        }

        if (o instanceof ProjectionImpl) {
            ProjectionImpl p    = (ProjectionImpl) o;
            List names = StringUtil.split(p.getName(), ">", true, true);
            List           cats = null;
            String         name = "";
            if (names.size() > 0) {
                name = (String) names.get(names.size() - 1);
                names.remove(names.size() - 1);
                if (names.size() > 0) {
                    cats = names;
                }
            }
            return new CategorizedThing(o, name, cats);
        }




        return new CategorizedThing(o, o.toString(), null);
    }


    /**
     * Class CategorizedThing _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class CategorizedThing {

        /** _more_          */
        public static final int STATE_LOCAL = 0;

        /** _more_          */
        public static final int STATE_SYSTEM = 1;

        /** _more_          */
        public static final int STATE_UNKNOWN = 2;


        /** _more_ */
        String name;

        /** _more_ */
        List<String> categories;

        /** _more_ */
        Object thing;

        /** _more_ */
        int state = STATE_UNKNOWN;

        /**
         * _more_
         *
         * @param thing _more_
         * @param name _more_
         * @param categories _more_
         */
        public CategorizedThing(Object thing, String name,
                                List<String> categories) {
            this(thing, name, categories, STATE_UNKNOWN);
        }

        /**
         * _more_
         *
         * @param thing _more_
         * @param name _more_
         * @param categories _more_
         * @param local _more_
         */
        public CategorizedThing(Object thing, String name,
                                List<String> categories, boolean local) {
            this(thing, name, categories, (local
                                           ? STATE_LOCAL
                                           : STATE_SYSTEM));
        }

        /**
         * _more_
         *
         * @param thing _more_
         * @param name _more_
         * @param categories _more_
         * @param state _more_
         */
        public CategorizedThing(Object thing, String name,
                                List<String> categories, int state) {
            this.thing      = thing;
            this.name       = name;
            this.categories = categories;
            this.state      = state;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isLocal() {
            return state == STATE_LOCAL;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean hasCategory() {
            return (categories != null) && (categories.size() > 0);
        }
    }


    /**
     * Class ResourceTree _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class ResourceTree {

        /** _more_ */
        String label;

        /** _more_ */
        JTree tree;

        /** _more_ */
        List<CategorizedThing> resources;

        /** _more_ */
        JComponent contents;

        /** _more_ */
        DefaultTreeModel treeModel;

        /** _more_ */
        DefaultMutableTreeNode treeRoot;

        /**
         * _more_
         *
         * @param label _more_
         * @param resources _more_
         */
        public ResourceTree(String label, List<CategorizedThing> resources) {
            this.label     = label;
            this.resources = resources;
            treeRoot       = new DefaultMutableTreeNode("");
            treeModel      = new DefaultTreeModel(treeRoot);
            tree           = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            tree.setShowsRootHandles(true);
            JScrollPane sp = new JScrollPane(tree);
            //contents = GuiUtils.topCenter(GuiUtils.left(new JLabel(label)), sp);
            JButton button =
                GuiUtils.makeImageButton("/auxdata/ui/icons/Plus.gif", this,
                                         "addObjectsToPluginCreator");
            button.setToolTipText("Add Selected to Plugin Creator");
            JComponent buttons = GuiUtils.wrap(button);
            contents =
                GuiUtils.leftCenter(GuiUtils.inset(GuiUtils.top(buttons), 3),
                                    sp);
            updateTree();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean hasResources() {
            return resources.size() > 0;

        }

        /**
         * _more_
         */
        public void updateTree() {
            treeRoot.removeAllChildren();
            Hashtable nodes = new Hashtable();
            if (resources.size() == 0) {
                treeRoot.add(new DefaultMutableTreeNode("None"));
            }
            for (CategorizedThing thing : resources) {
                DefaultMutableTreeNode parent = treeRoot;
                if (thing.hasCategory()) {
                    String catString = "";
                    for (String cat : thing.categories) {
                        catString = catString + ">" + cat;
                        DefaultMutableTreeNode tmp =
                            (DefaultMutableTreeNode) nodes.get(catString);
                        if (tmp == null) {
                            tmp = new DefaultMutableTreeNode(cat);
                            nodes.put(catString, tmp);
                            //Insert the category tree node before any leaf nodes
                            int idx = 0;
                            for (idx = 0; idx < parent.getChildCount();
                                    idx++) {
                                DefaultMutableTreeNode child =
                                    (DefaultMutableTreeNode) parent
                                        .getChildAt(idx);
                                if (child.getUserObject()
                                        instanceof TwoFacedObject) {
                                    break;
                                }
                            }
                            parent.insert(tmp, idx);
                        }
                        parent = tmp;
                    }
                }
                String label = GuiUtils.getLocalName(thing.name,
                                   thing.isLocal(), true);
                parent.add(
                    new DefaultMutableTreeNode(
                        new TwoFacedObject(label, thing)));
            }
            treeModel.nodeStructureChanged(treeRoot);
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public JComponent getContents() {
            return contents;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List getSelectedObjects() {
            TreePath[] paths   = tree.getSelectionModel().getSelectionPaths();
            List       objects = new ArrayList();
            if (paths == null) {
                return objects;
            }
            for (int i = 0; i < paths.length; i++) {
                Object last = paths[i].getLastPathComponent();
                if (last == null) {
                    continue;
                }
                Object object =
                    ((DefaultMutableTreeNode) last).getUserObject();
                if ((object == null) || !(object instanceof TwoFacedObject)) {
                    continue;
                }
                CategorizedThing thing =
                    (CategorizedThing) ((TwoFacedObject) object).getId();
                objects.add(thing.thing);
            }
            return objects;
        }


        /**
         * _more_
         */
        public void addObjectsToPluginCreator() {
            getIdv().getPluginManager().addObjects(getSelectedObjects());
        }


    }

    ;





    /**
     * Get the title to use for the iwindow
     *
     * @return window title
     */
    public String getWindowTitle() {
        return GuiUtils.getApplicationTitle() + "Resource Viewer";
    }

}

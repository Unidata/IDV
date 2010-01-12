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


import ucar.unidata.data.*;


import ucar.unidata.idv.*;




import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;







/**
 * Class AutoDisplayEditor  manages the auto-display creation feature.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.16 $
 */
public class AutoDisplayEditor extends IdvManager {

    /** Xml tag name */
    private static final String TAG_AUTODISPLAY = "autodisplay";

    /** Xml tag name */
    private static final String TAG_AUTODISPLAYS = "autodisplays";


    /** Xml attribute name */
    private static final String ATTR_DATASOURCE = "datasource";

    /** Xml attribute name */
    private static final String ATTR_PARAMETER = "parameter";

    /** Xml attribute name */
    private static final String ATTR_DISPLAY = "display";

    /** Xml attribute name */
    private static final String ATTR_ENABLED = "enabled";



    /** The tree root */
    private DefaultMutableTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;

    /** The tree */
    private JTree tree;

    /** Only load in the jtree when we show the window */
    private boolean needToLoadTree = true;

    /** Holds the autodisplay info we get from the xml */
    private Hashtable autoDisplayMap;


    /**
     * Create the editor
     *
     * @param idv The IDV
     */
    public AutoDisplayEditor(IntegratedDataViewer idv) {
        super(idv);
        JPanel buttons = GuiUtils.makeButtons(this,
                             new String[] { GuiUtils.CMD_CLOSE });
        contents = GuiUtils.centerBottom(doMakeContents(), buttons);
    }


    /**
     * Get the title to use for the iwindow
     *
     * @return window title
     */
    protected String getWindowTitle() {
        return "Auto-Displays Editor";
    }

    /**
     * Show the frame
     */
    public void show() {
        if (needToLoadTree) {
            loadTree();
        }
        super.show();
    }



    /** Ordinal names for images */
    public static final String[] ordinalNames = {
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
        "Eight", "Ninth", "Tenth"
    };



    /**
     * Load the tree
     */
    private void loadTree() {
        Hashtable oldPaths =
            GuiUtils.initializeExpandedPathsBeforeChange(tree, treeRoot);
        needToLoadTree = false;
        treeRoot.removeAllChildren();
        initAutoDisplays();
        for (Enumeration keys =
                autoDisplayMap.keys(); keys.hasMoreElements(); ) {
            final DataSourceDescriptor dsd =
                getDataManager().getDescriptor((String) keys.nextElement());
            if (dsd == null) {
                continue;
            }
            List paramDisplays = (List) autoDisplayMap.get(dsd.getId());
            if (paramDisplays.size() == 0) {
                continue;
            }
            String dsdLabel = dsd.getLabel();
            if ((dsdLabel == null) || (dsdLabel.trim().length() == 0)) {
                dsdLabel = dsd.getId();
            }
            DefaultMutableTreeNode dsdNode =
                new DefaultMutableTreeNode(dsdLabel);
            treeRoot.add(dsdNode);
            for (int i = 0; i < paramDisplays.size(); i++) {
                ParamDisplay pd         = (ParamDisplay) paramDisplays.get(i);
                String       paramLabel = pd.paramId;
                if (pd.paramId.startsWith("#")) {
                    int nth = new Integer(pd.paramId.substring(1)).intValue();
                    if (nth < ordinalNames.length) {
                        paramLabel = ordinalNames[nth] + " parameter";
                    } else {
                        paramLabel = nth + "th parameter";
                    }
                }

                TwoFacedObject tfo = new TwoFacedObject(paramLabel + " --> "
                                         + pd.cd, new Object[] { dsd,
                        pd });
                DefaultMutableTreeNode displayNode =
                    new DefaultMutableTreeNode(tfo);
                dsdNode.add(displayNode);
            }
        }
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(tree, oldPaths, treeRoot);
    }



    /**
     * Make the GUI
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        treeRoot  = new DefaultMutableTreeNode("");
        treeModel = new DefaultTreeModel(treeRoot);
        tree      = new JTree(treeModel);
        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if ( !SwingUtilities.isRightMouseButton(event)) {
                    return;
                }

                TreePath path = tree.getPathForLocation(event.getX(),
                                    event.getY());
                if (path == null) {
                    return;
                }
                DefaultMutableTreeNode last =
                    (DefaultMutableTreeNode) path.getLastPathComponent();
                Object data = last.getUserObject();
                if ((data == null) || !(data instanceof TwoFacedObject)) {
                    return;
                }
                final TwoFacedObject tfo   = (TwoFacedObject) data;
                JPopupMenu           popup = new JPopupMenu();
                JMenuItem            mi;
                popup.add(mi = new JMenuItem("Remove"));
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        removeAutoDisplay(tfo);
                    }
                });
                for (int i = 0; i < 5; i++) {
                    final int index = i;
                    popup.add(mi = new JMenuItem("Change parameter to the "
                            + ordinalNames[i] + " parameter"));
                    mi.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            setParameter(tfo, "#" + index);
                        }
                    });
                }

                popup.show((Component) event.getSource(), event.getX(),
                           event.getY());
            }
        });

        tree.setToolTipText(
            "<html>Use the 'Delete' key to remove the selected auto-displays.<br>Right click to show popup menu.</html>");
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (GuiUtils.isDeleteEvent(event)) {
                    TreePath[] paths =
                        tree.getSelectionModel().getSelectionPaths();
                    if ((paths == null) || (paths.length == 0)) {
                        return;
                    }
                    DefaultMutableTreeNode last =
                        (DefaultMutableTreeNode) paths[0]
                            .getLastPathComponent();
                    if (last == null) {
                        return;
                    }
                    Object object = last.getUserObject();
                    if ( !(object instanceof TwoFacedObject)) {
                        return;
                    }
                    TwoFacedObject tfo = (TwoFacedObject) object;
                    removeAutoDisplay(tfo);
                }
            }
        });
        Dimension defaultDimension = new Dimension(300, 400);
        JScrollPane sp = GuiUtils.makeScrollPane(tree,
                             (int) defaultDimension.getWidth(),
                             (int) defaultDimension.getHeight());
        sp.setPreferredSize(defaultDimension);
        return sp;
    }


    /**
     * Extract the ParamDisplay object from the given tfo
     *
     * @param tfo The tfo
     *
     * @return The param display
     */
    private ParamDisplay getParamDisplay(TwoFacedObject tfo) {
        return (ParamDisplay) ((Object[]) tfo.getId())[1];
    }

    /**
     * Extract the DSD from the given TFO
     *
     * @param tfo The TFO
     *
     * @return The DSD
     */
    private DataSourceDescriptor getDataSourceDescriptor(TwoFacedObject tfo) {
        return (DataSourceDescriptor) ((Object[]) tfo.getId())[0];
    }

    /**
     * Set the param id for the ParamDisplay held by the TFO. Write out the xml and reload
     * the tree.
     *
     * @param tfo The TFO
     * @param paramId The param id to set
     */
    private void setParameter(TwoFacedObject tfo, String paramId) {
        getParamDisplay(tfo).paramId = paramId;
        writeXml();
        loadTree();
    }


    /**
     * Remove the ParamDisplay held by the given TFO
     *
     * @param tfo The TFO
     */
    private void removeAutoDisplay(TwoFacedObject tfo) {
        ParamDisplay pd = getParamDisplay(tfo);
        removeAutoDisplay(getDataSourceDescriptor(tfo), pd.paramId, pd.cd);
    }



    /**
     * Remove from the autodisplays collection the given datasource/parameter/display
     *
     *
     * @param dsd The data source to remove
     * @param paramId The paramId
     * @param cd The display
     */
    private void removeAutoDisplay(DataSourceDescriptor dsd, String paramId,
                                   ControlDescriptor cd) {

        List paramDisplays = (List) autoDisplayMap.get(dsd.getId());
        if (paramDisplays == null) {
            return;
        }

        for (int i = 0; i < paramDisplays.size(); i++) {
            ParamDisplay pd = (ParamDisplay) paramDisplays.get(i);
            if (Misc.equals(paramId, pd.paramId) && Misc.equals(cd, pd.cd)) {
                paramDisplays.remove(i);
                break;
            }
        }
        writeXml();
        loadTree();
    }




    /**
     * Add into the autodisplays collection the given data choice and control
     *
     * @param dataChoice The dat choice
     * @param cd The control descriptor
     */
    public void addDisplayForDataSource(DataChoice dataChoice,
                                        ControlDescriptor cd) {
        List dataSources = new ArrayList();
        dataChoice.getDataSources(dataSources);
        if (dataSources.size() == 0) {
            return;
        }
        String paramId       = dataChoice.getName();
        String dataSourceId  =
            ((DataSource) dataSources.get(0)).getTypeName();

        List   paramDisplays = (List) autoDisplayMap.get(dataSourceId);
        if (paramDisplays == null) {
            autoDisplayMap.put(dataSourceId, paramDisplays = new ArrayList());
        }
        for (int i = 0; i < paramDisplays.size(); i++) {
            ParamDisplay pd = (ParamDisplay) paramDisplays.get(i);
            if (Misc.equals(paramId, pd.paramId) && Misc.equals(cd, pd.cd)) {
                return;
            }
        }
        paramDisplays.add(new ParamDisplay(paramId, cd, true));
        writeXml();
        loadTree();
    }


    /**
     * Write the xml resource
     */
    private void writeXml() {
        XmlResourceCollection resources =
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_AUTODISPLAYS);
        Document displaysDoc =
            resources.getWritableDocument("<autodisplays></autodisplays>");
        Element root =
            resources.getWritableRoot("<autodisplays></autodisplays>");

        XmlUtil.removeChildren(root);
        initAutoDisplays();
        for (Enumeration keys =
                autoDisplayMap.keys(); keys.hasMoreElements(); ) {
            DataSourceDescriptor dsd =
                getDataManager().getDescriptor((String) keys.nextElement());
            if (dsd == null) {
                continue;
            }
            List paramDisplays = (List) autoDisplayMap.get(dsd.getId());
            for (int i = 0; i < paramDisplays.size(); i++) {
                ParamDisplay pd = (ParamDisplay) paramDisplays.get(i);
                Element element = displaysDoc.createElement(TAG_AUTODISPLAY);
                element.setAttribute(ATTR_DATASOURCE, dsd.getId());
                element.setAttribute(ATTR_PARAMETER, pd.paramId);
                element.setAttribute(ATTR_DISPLAY, pd.cd.getControlId());
                element.setAttribute(ATTR_ENABLED, pd.enabled + "");
                root.appendChild(element);
            }
        }

        try {
            resources.writeWritable();
        } catch (Throwable exc) {
            logException("Adding autodisplay", exc);
        }

    }


    /**
     * Find the ControlDescriptor-s in the given list of paramDisplays
     * that match the given param id
     *
     * @param paramId Param id to match
     * @param paramDisplays List of ParamDisplay-s
     *
     * @return Listof ControlDescriptor-s
     */
    private List findDisplays(String paramId, List paramDisplays) {
        List descriptors = null;
        for (int i = 0; i < paramDisplays.size(); i++) {
            ParamDisplay pd = (ParamDisplay) paramDisplays.get(i);
            if (Misc.equals(paramId, pd.paramId)) {
                if (descriptors == null) {
                    descriptors = new ArrayList();
                }
                descriptors.add(pd.cd);
            }
        }
        return descriptors;
    }



    /**
     * Return a list that holds datachoice,ControlDescriptor for
     * the autodisplays that match the given data source
     *
     * @param dataSource Data source to look at
     *
     * @return List of data choices/control descriptors
     */
    public List getDisplaysForDataSource(DataSource dataSource) {
        List pairs       = new ArrayList();
        List dataChoices = dataSource.getDataChoices();
        initAutoDisplays();
        String typeName = dataSource.getTypeName();
        if (typeName == null) {
            return new ArrayList();
        }

        List paramDisplays = (List) autoDisplayMap.get(typeName);
        if (paramDisplays == null) {
            return pairs;
        }

        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dc   = (DataChoice) dataChoices.get(i);
            List       dcs1 = findDisplays(dc.getName(), paramDisplays);
            List       dcs2 = findDisplays("#" + i, paramDisplays);
            if (dcs1 != null) {
                for (int j = 0; j < dcs1.size(); j++) {
                    pairs.add(dc);
                    pairs.add(dcs1.get(j));
                }
            }
            if (dcs2 != null) {
                for (int j = 0; j < dcs2.size(); j++) {
                    pairs.add(dc);
                    pairs.add(dcs2.get(j));
                }
            }

        }
        return pairs;
    }







    /**
     * Load in the xml and create the autoDisplayMap
     */
    private void initAutoDisplays() {
        if (autoDisplayMap != null) {
            return;
        }
        autoDisplayMap = new Hashtable();
        XmlResourceCollection resources =
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_AUTODISPLAYS);
        for (int i = 0; i < resources.size(); i++) {
            Element root = resources.getRoot(i);
            if (root == null) {
                continue;
            }
            List nodes = XmlUtil.findChildren(root, TAG_AUTODISPLAY);
            for (int nodeIdx = 0; nodeIdx < nodes.size(); nodeIdx++) {
                Element autoDisplayNode = (Element) nodes.get(nodeIdx);
                String dataSourceId = XmlUtil.getAttribute(autoDisplayNode,
                                          ATTR_DATASOURCE);
                String paramId = XmlUtil.getAttribute(autoDisplayNode,
                                     ATTR_PARAMETER);
                String displayId = XmlUtil.getAttribute(autoDisplayNode,
                                       ATTR_DISPLAY);
                boolean enabled = XmlUtil.getAttribute(autoDisplayNode,
                                      ATTR_ENABLED, true);
                ControlDescriptor cd =
                    getIdv().getControlDescriptor(displayId);
                if (cd == null) {
                    continue;
                }
                List paramDisplays = (List) autoDisplayMap.get(dataSourceId);
                if (paramDisplays == null) {
                    paramDisplays = new ArrayList();
                    autoDisplayMap.put(dataSourceId, paramDisplays);
                }
                paramDisplays.add(new ParamDisplay(paramId, cd, enabled));
            }
        }
    }


    /**
     * Class ParamDisplay Holds a param id and a control descriptor
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.16 $
     */
    public static class ParamDisplay {

        /** Is this enabled */
        boolean enabled = true;

        /** THe param id */
        String paramId;

        /** The control descriptor */
        ControlDescriptor cd;

        /**
         * CTOR
         *
         * @param paramId Param id
         * @param cd ControlDescriptor
         * @param enabled Is it enabled
         */
        public ParamDisplay(String paramId, ControlDescriptor cd,
                            boolean enabled) {
            this.paramId = paramId;
            this.cd      = cd;
            this.enabled = enabled;
        }

    }










}

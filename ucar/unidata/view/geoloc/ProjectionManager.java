/*
 * $Id: ProjectionManager.java,v 1.57 2007/05/04 14:17:36 dmurray Exp $
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

package ucar.unidata.view.geoloc;


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;


import ucar.unidata.gis.maps.MapData;
import ucar.unidata.ui.PersistentDataDialog;
import ucar.unidata.ui.PersistentDataManager;
import ucar.unidata.ui.PersistentObject;

import ucar.unidata.util.FileManager;

import ucar.unidata.util.Format;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.ListenerManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.view.NPController;


import ucar.unidata.xml.XmlEncoder;



import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.io.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 *  Manages a modal dialogue box that allows the user to define
 *  projections using subclasses of ucar.unidata.gis.ProjectionImpl.
 * <pre>
 *    1) It uses bean introspection on ProjectionImpl subclasses to dynamically
 *       configure edit fields for the projection's parameters. The subclass should
 *       define getXXX() and setXXX() methods for each parameter.
 *    3) when the user selects a projection, a NewProjectionEvent is sent to any listeners
 *    4) currently the list of possible projection classes is maintained as a
 *       hard-wired list of class names. This should be rethunk.
 * </pre>
 * @author John Caron
 * @version $Revision: 1.57 $
 */

public class ProjectionManager implements ActionListener {

    /** property change identifier */
    public static final String PROPERTY_CHANGE = "ProjectionImpl";


    /**
     * the current list of projection classes: be nice to do this dynamically
     */
    private static List defaultProjections = new ArrayList();


    /** List of maps to use in navigated panel */
    private List maps;

    /** List of default maps to use in navigated panel if maps is null */
    private List defaultMaps;

    /**
     * Add the given class to the list of default projections
     *
     * @param projectionClass The class
     */
    public static void addDefaultProjection(String projectionClass) {
        getDefaultProjections().add(projectionClass);
    }

    /**
     * Create the list of default projection classes
     *
     * @return list of default projection classes
     */
    public static List getDefaultProjections() {
        if (defaultProjections.size() == 0) {
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.LatLonProjection");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.LambertConformal");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.TransverseMercator");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.Stereographic");
            defaultProjections.add("ucar.unidata.geoloc.projection.Mercator");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.AlbersEqualArea");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.LambertAzimuthalEqualArea");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.Orthographic");
            defaultProjections.add(
                "ucar.unidata.geoloc.projection.VerticalPerspectiveView");
        }
        return defaultProjections;
    }



    /** current projection */
    private ProjectionImpl current;

    /** manage NewProjection events */
    private ListenerManager lm;

    /** parent container */
    private RootPaneContainer parent;

    /** flag for event response */
    private boolean eventsOK = false;

    // GUI stuff that needs class scope

    /** projection table */
    private JTableProjection projTable;

    /** navigated panel controller */
    private NPController npViewControl;

    /** map panel */
    private NavigatedPanel mapViewPanel;

    /** view dialog */
    private JDialog viewDialog;

    /** _more_ */
    private JLabel mapLabel;

    /** _more_ */
    private JComponent contents;

    /** new projection dialog */
    private NewProjectionDialog newDialog = null;



    /** Name for dialogs id */
    private final static String DEFAULT_NAME = "Projection";

    /** help id */
    private String helpId = DEFAULT_NAME;



    /** array of classes for reflection */
    private final static Class[] VOIDCLASSARG = new Class[] {};

    /** array of objects for reflection */
    private final static Object[] VOIDOBJECTARG = new Object[] {};



    /** debug flags */
    private static boolean
        debug      = false,
        debugBeans = false;

    /** Default constructor */
    public ProjectionManager() {
        this(null, null, true);
    }

    //TODO: For now put in this dummy ctor for the clients 
    //that  try to pass in a store

    /**
     * Create a new ProjectionManager.
     *
     * @param parent   parent container
     * @param dummy    dummy argument for clients that try to pass in
     *                 a store
     *
     */
    public ProjectionManager(RootPaneContainer parent, Object dummy) {
        this(parent, null);
    }

    /**
     * Dummy method for stores
     */
    public void storePersistentData() {
        //Another dummy fow now
    }


    /**
     * Create a new ProjectionManager
     *
     * @param parent  parent container
     */
    public ProjectionManager(RootPaneContainer parent) {
        this(parent, null);
    }


    /**
     * Create a new ProjectionManager.
     *
     * @param parent   parent container
     * @param makeDialog   true to make this a dialog
     *
     */
    public ProjectionManager(RootPaneContainer parent, boolean makeDialog) {
        this(parent, null, makeDialog);
    }

    /**
     * Create a new ProjectionManager.
     *
     * @param parent      JFrame (application) or JApplet (applet)
     * @param projections  list of initial projections
     */
    public ProjectionManager(RootPaneContainer parent, List projections) {
        this(parent, projections, true);
    }

    /**
     * Create a new ProjectionManager.
     * @param parent      JFrame (application) or JApplet (applet)
     * @param projections  list of initial projections
     * @param makeDialog   true to make this a dialog
     */
    public ProjectionManager(RootPaneContainer parent, List projections,
                             boolean makeDialog) {
        this(parent, projections, makeDialog, DEFAULT_NAME);
    }

    /**
     * Create a new ProjectionManager.
     * @param parent      JFrame (application) or JApplet (applet)
     * @param projections  list of initial projections
     * @param makeDialog   true to make this a dialog
     * @param helpId       help id if dialog
     */
    public ProjectionManager(RootPaneContainer parent, List projections,
                             boolean makeDialog, String helpId) {

        this(parent, projections, makeDialog, helpId, null);
    }

    /**
     * Create a new ProjectionManager.
     * @param parent      JFrame (application) or JApplet (applet)
     * @param projections  list of initial projections
     * @param makeDialog   true to make this a dialog
     * @param helpId       help id if dialog
     * @param maps         List of MapData
     */
    public ProjectionManager(RootPaneContainer parent, List projections,
                             boolean makeDialog, String helpId, List maps) {

        this.helpId = helpId;
        this.maps   = maps;
        this.parent = parent;

        // manage NewProjectionListeners
        lm = new ListenerManager("java.beans.PropertyChangeListener",
                                 "java.beans.PropertyChangeEvent",
                                 "propertyChange");

        // here's where the map will be drawn: but cant be changed/edited
        npViewControl = new NPController();
        if (maps == null) {
            maps = getDefaultMaps();  // we use the system default
        }
        for (int mapIdx = 0; mapIdx < maps.size(); mapIdx++) {
            MapData mapData = (MapData) maps.get(mapIdx);
            if (mapData.getVisible()) {
                npViewControl.addMap(mapData.getSource(), mapData.getColor());
            }
        }
        mapViewPanel = npViewControl.getNavigatedPanel();
        mapViewPanel.setPreferredSize(new Dimension(250, 250));
        mapViewPanel.setToolTipText(
            "Shows the default zoom of the current projection");
        mapViewPanel.setChangeable(false);


        if ((projections == null) || (projections.size() == 0)) {
            projections = makeDefaultProjections();
        }


        // the actual list is a JTable subclass
        projTable = new JTableProjection(this, projections);


        JComponent listContents = new JScrollPane(projTable);



        JComponent buttons =
            GuiUtils.hbox(GuiUtils.makeButton("Edit", this, "doEdit"),
                          GuiUtils.makeButton("New", this, "doNew"),
                          GuiUtils.makeButton("Export", this, "doExport"),
                          GuiUtils.makeButton("Delete", this, "doDelete"));


        mapLabel = new JLabel(" ");
        JComponent leftPanel = GuiUtils.inset(GuiUtils.topCenter(mapLabel,
                                   mapViewPanel), 5);


        JComponent rightPanel = GuiUtils.topCenter(buttons, listContents);
        rightPanel = GuiUtils.inset(rightPanel, 5);
        contents   = GuiUtils.inset(GuiUtils.hbox(leftPanel, rightPanel), 5);



        // default current and working projection
        if (null != (current = projTable.getSelected())) {
            setWorkingProjection(current);
            projTable.setCurrentProjection(current);
            mapLabel.setText(current.toString());
        }

        /* listen for new working Projections from projTable */
        projTable.addNewProjectionListener(new NewProjectionListener() {
            public void actionPerformed(NewProjectionEvent e) {
                if (e.getProjection() != null) {
                    setWorkingProjection(e.getProjection());
                }
            }
        });

        eventsOK = true;

        // put it together in the viewDialog
        if (makeDialog) {
            Container buttPanel = GuiUtils.makeApplyOkHelpCancelButtons(this);
            contents   = GuiUtils.centerBottom(contents, buttPanel);
            viewDialog = GuiUtils.createDialog(GuiUtils.getApplicationTitle() +"Projection Manager", false);
            viewDialog.getContentPane().add(contents);
            viewDialog.pack();
            ucar.unidata.util.Msg.translateTree(viewDialog);
            viewDialog.setLocation(100, 100);
        }
    }

    /**
     * Handle action events
     *
     * @param event  event to handle
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            accept();
        }
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_CANCEL)) {
            close();
        }
        if (cmd.equals(GuiUtils.CMD_HELP)) {
            ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(helpId);
        }
    }

    /**
     * Close this widget
     */
    public void close() {
        if (viewDialog != null) {
            viewDialog.setVisible(false);
        }
    }



    /**
     * Export a list of user selected projections
     */
    public void doExport() {
        List   projections = getProjections();
        Vector v           = new Vector();
        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl projection = (ProjectionImpl) projections.get(i);
            v.add(projection);
            //            v.add(tfo);
        }

        JList jlist = new JList(v);
        JPanel contents =
            GuiUtils
                .topCenter(GuiUtils
                    .cLabel("Please select the projections you want to export"), GuiUtils
                    .makeScrollPane(jlist, 200, 400));

        if ( !GuiUtils.showOkCancelDialog(null, "Export Projections",
                                          contents, null)) {
            return;
        }

        Object[] items = jlist.getSelectedValues();
        if ((items == null) || (items.length == 0)) {
            return;
        }
        List selected = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            selected.add(items[i]);
        }

        String xml = (new XmlEncoder()).toXml(selected);
        String filename = FileManager.getWriteFile(FileManager.FILTER_XML,
                              FileManager.SUFFIX_XML);
        if (filename == null) {
            return;
        }
        try {
            IOUtil.writeFile(filename, xml);
        } catch (Exception exc) {
            LogUtil.logException("Writing projection file: " + filename, exc);
        }




    }





    /**
     * Make the default projections from the internal list of classes.
     * @return list of default projections
     */
    public static List makeDefaultProjections() {
        List defaults   = new ArrayList();
        List classNames = getDefaultProjections();
        for (int i = 0; i < classNames.size(); i++) {
            String className = (String) classNames.get(i);
            try {
                Class          projClass = Misc.findClass(className);
                ProjectionImpl pi = (ProjectionImpl) projClass.newInstance();
                pi.setName("Default " + pi.getProjectionTypeLabel());
                defaults.add(pi);
            } catch (Exception ee) {
                System.err.println("Error creating default projection: "
                                   + className);
                ee.printStackTrace();
            }
        }
        return defaults;
    }

    /**
     * Set the list of maps to use in this ProjectionManager
     *
     * @param maps list of MapData's (may be null)
     */
    public void setMaps(List maps) {
        this.maps = maps;
    }

    /**
     * Set the list of projections for this manager
     *
     * @param list   list of projections
     */
    public void setProjections(List list) {
        projTable.setProjections(list);
    }

    /**
     * Get the list of projections that this manager manages
     * @return list of projections
     */
    public List getProjections() {
        return projTable.getProjections();
    }



    //// ManagerBean methods

    /**
     * Get the name of this manager to put in a menu
     * @return manager name
     */
    public String getManagerName() {
        return GuiUtils.getApplicationTitle() +"Projection Manager";
    }

    /**
     * Get the Class type of the objects managed; must implement
     * Serializable, Cloneable
     * @return the type of class that this manager manages
     */
    public Class getPersistentObjectClass() {
        return ProjectionImpl.class;
    }

    /**
     * Get the contents as a component.
     * @return the contents
     */
    public Component getContents() {
        return contents;
    }

    /**
     * Add a property change listener.
     *
     * @param l  listener to add
     */
    public void addPropertyChangeListener(
            java.beans.PropertyChangeListener l) {
        lm.addListener(l);
    }

    /**
     * Remove a property change listener.
     *
     * @param l  listener to remove
     */
    public void removePropertyChangeListener(
            java.beans.PropertyChangeListener l) {
        lm.removeListener(l);
    }

    /**
     * Get this as a dialog.
     * @return the dialog or null if not made as a dialog
     */
    public JDialog getDialog() {
        return viewDialog;
    }

    /** Popup the Manager Dialog */
    public void show() {
        if (viewDialog != null) {
            viewDialog.setVisible(true);
        }
    }

    /**
     * Destroy this object
     */
    public void destroy() {
        if (viewDialog != null) {
            viewDialog.dispose();
            viewDialog = null;
        }

    }




    /**
     * Get the currently selected object, of type getPersistentObjectClass()
     * @return currently selected object
     */
    public PersistentObject getSelection() {
        return (PersistentObject) current;
    }

    /**
     * Set the currently selected thing: must be of type
     * getPersistentObjectClass(). This does NOT fire a PropertyChangeEvent
     *
     * @param select  the object to select.
     */
    public void setSelection(PersistentObject select) {
        setWorkingProjection((ProjectionImpl) select);
    }

    ////// PersistentDataManager methods

    /**
     * Accept a change.
     */
    public void accept() {
        ProjectionRect bb = mapViewPanel.getMapArea();
        if ((bb == null) || (current == null)) {
            return;
        }
        current.setDefaultMapArea((ProjectionRect) bb.clone());
        lm.sendEvent(new java.beans.PropertyChangeEvent(this,
                PROPERTY_CHANGE, null, current));
    }

    /**
     * See if the manager contains the projection by name.
     *
     * @param id    name of projection
     * @return true if the projection is managed by this
     */
    public boolean contains(String id) {
        return projTable.contains(id);
    }

    /**
     * Delete the selected projection.
     */
    public void doDelete() {
        projTable.deleteSelected();
    }

    /**
     * _more_
     */
    public void doEdit() {
        edit(false);
    }

    /**
     * _more_
     */
    public void doNew() {
        edit(true);
    }


    /**
     * Edit a projection.  Used to change an existing projection or
     * add a new one.
     *
     * @param isNew  true if this is a new  projection.
     */
    public void edit(boolean isNew) {

        String name = (isNew || (current == null))
                      ? ""
                      : current.getName().trim();
        getEditor().setDoingNewProjection(isNew, name);
        if (isNew) {
            getEditor().setDefaultProjection();
        } else {
            getEditor().setProjection(current);
        }
        getEditor().setVisible(true);
    }

    /**
     * Store this Projection in the data table
     * @param proj the Projection to store
     */
    public void saveProjection(ProjectionImpl proj) {
        //setVisible(true);  how to make this work? seperate Thread ?

        // force new name
        ProjectionImpl newProj = (ProjectionImpl) proj.clone();
        newProj.setName("");
        setWorkingProjection(newProj);




        // start up edit Dialog
        getEditor().setProjection(current);
        getEditor().setVisible(true);
    }

    /**
     * Set the map renderer.
     *
     * @param map  map renderer to use.
     */
    private void setMap(ucar.unidata.view.Renderer map) {
        if (map != null) {
            npViewControl.setRenderer(map);
            getEditor().setMap(map);
        }
    }


    /**
     * Get the editor dialog
     *
     * @return The editor dialog
     */
    private NewProjectionDialog getEditor() {
        if (null == newDialog) {
            if (parent == null) {
                newDialog = new NewProjectionDialog(viewDialog);
            } else {
                newDialog = new NewProjectionDialog(parent);
            }
        }
        return newDialog;
    }

    /**
     * Set the working projection.
     *
     * @param proj   projection to use
     */
    private void setWorkingProjection(ProjectionImpl proj) {
        if (proj == null) {
            return;
        }
        if (debug) {
            System.out.println("ProjManager.setWorkingProjection " + proj);
        }
        current = (ProjectionImpl) proj.clone();
        if (debug) {
            System.out.println("ProjManager.setWorkingProjection map area = "
                               + current.getDefaultMapArea());
        }

        if (current != null) {
            mapLabel.setText(current.toString());
        }
        npViewControl.setProjectionImpl(current);
    }

    //////////////////////////////////////////////////////////////////////////////
    // heres where projections get defined / edited

    /**
     * Class NewProjectionDialog
     *
     * @author Unidata development team
     */
    private class NewProjectionDialog extends ucar.unidata.ui
        .IndependentDialog {

        /** projection being edited */
        private ProjectionImpl editProjection;

        /** listener manager */
        private ListenerManager lm;  // manage NewProjection events

        /** track projection name */
        private String startingName = null;

        /** flag for new or old */
        private boolean doingNew = true;

        /** label for type of projectcion */
        private JLabel typeLabel;

        /** combo box for selecting projection type */
        private JComboBox projClassCB;

        /** panel for projection parameters */
        private JPanel paramPanel;

        /** text field for inputting the name */
        private JTextField nameTF;

        /** navigated panel controller */
        private NPController npEditControl;

        /** map editing panel */
        private NavigatedPanel mapEditPanel;

        /** border for panel */
        private Border standardBorder = new EtchedBorder();

        /**
         * Create a new projection editor
         *
         * @param parent  anchoring parent
         *
         */
        NewProjectionDialog(RootPaneContainer parent) {
            super(parent, true, "Define/Edit Projection");
            makeUI();
            setLocation(100, 100);
        }

        /**
         * Create a new projection editor
         *
         * @param parent  anchoring parent
         *
         */
        NewProjectionDialog(JDialog parent) {
            super(parent, true, "Define/Edit Projection");
            makeUI();
            setLocation(100, 100);
        }

        /**
         * Create a new projection editor
         *
         * @param parent  anchoring parent
         *
         */
        NewProjectionDialog(JFrame parent) {
            super(parent, true, "Define/Edit Projection");
            makeUI();
            setLocation(100, 100);
        }


        /**
         * Toggle on/off the visibility of the map
         *
         * @param mapData Map to toggle
         * @param onOff Visibility
         */
        private void toggleMap(MapData mapData, boolean onOff) {
            if (onOff) {
                npEditControl.addMap(mapData.getSource(), mapData.getColor());
            } else {
                npEditControl.removeMap(mapData.getSource());
            }
        }


        /**
         * Create the UI for this editor
         */
        void makeUI() {

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(new LineBorder(Color.blue));
            getContentPane().add(mainPanel, BorderLayout.CENTER);

            // the map and associated toolbar
            npEditControl = new NPController();
            mapEditPanel = npEditControl.getNavigatedPanel();  // here's where the map will be drawn
            mapEditPanel.setPreferredSize(new Dimension(250, 250));
            mapEditPanel.setSelectRegionMode(true);
            JToolBar navToolbar = mapEditPanel.getNavToolBar();
            navToolbar.setFloatable(false);
            JToolBar moveToolbar = mapEditPanel.getMoveToolBar();
            moveToolbar.setFloatable(false);
            //toolbar.remove("setReference");


            JPanel toolbar   = new JPanel();
            List   localMaps = maps;
            if (localMaps == null) {
                localMaps = getDefaultMaps();
            }
            JMenu    mapMenu    = new JMenu("Maps");
            JMenuBar menuHolder = new JMenuBar();
            menuHolder.setBorder(null);
            menuHolder.add(mapMenu);
            toolbar.add(menuHolder);
            for (int mapIdx = 0; mapIdx < localMaps.size(); mapIdx++) {
                final MapData mapData = (MapData) localMaps.get(mapIdx);
                final JCheckBoxMenuItem cbx =
                    new JCheckBoxMenuItem(mapData.getDescription(),
                                          mapData.getVisible());
                if (mapData.getVisible()) {
                    toggleMap(mapData, true);
                }
                mapMenu.add(cbx);
                cbx.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent event) {
                        toggleMap(mapData, cbx.isSelected());
                    }
                });
            }
	    GuiUtils.limitMenuSize(mapMenu, "Maps ", 20);

            toolbar.add(navToolbar);
            toolbar.add(moveToolbar);

            JPanel mapSide = new JPanel();
            mapSide.setLayout(new BorderLayout());
            TitledBorder mapBorder = new TitledBorder(standardBorder,
                                         "Edit Projection",
                                         TitledBorder.ABOVE_TOP,
                                         TitledBorder.CENTER);
            mapSide.setBorder(mapBorder);
            mapSide.add(toolbar, BorderLayout.NORTH);
            mapSide.add(mapEditPanel, BorderLayout.CENTER);
            mainPanel.add(mapSide, BorderLayout.WEST);

            // the projection parameters

            // the Projection name
            JLabel nameLabel = GuiUtils.rLabel("Name: ");
            nameTF = new JTextField(20);

            // the list of Projection classes is kept in a comboBox
            typeLabel   = GuiUtils.rLabel("Type: ");
            projClassCB = new JComboBox();
            // standard list of projection classes
            List classNames = getDefaultProjections();
            for (int i = 0; i < classNames.size(); i++) {
                String className = (String) classNames.get(i);
                try {
                    projClassCB.addItem(new ProjectionClass(className));
                } catch (ClassNotFoundException ee) {
                    System.err.println("ProjectionManager failed on "
                                       + className + " " + ee);
                } catch (IntrospectionException ee) {
                    System.err.println("ProjectionManager failed on "
                                       + className + " " + ee);
                }
            }
            GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
            JPanel topPanel = GuiUtils.doLayout(new Component[] { nameLabel,
                    nameTF, typeLabel, projClassCB }, 2, GuiUtils.WT_N,
                        GuiUtils.WT_N);

            // the Projection parameter area
            paramPanel = new JPanel();
            paramPanel.setLayout(new BorderLayout());
            paramPanel.setBorder(new TitledBorder(standardBorder,
                    "Projection Parameters", TitledBorder.ABOVE_TOP,
                    TitledBorder.CENTER));

            // the bottom button panel
            JPanel  buttPanel     = new JPanel();
            JButton acceptButton  = new JButton("Save");
            JButton previewButton = new JButton("Preview");
            JButton cancelButton  = new JButton("Cancel");
            buttPanel.add(acceptButton, null);
            buttPanel.add(previewButton, null);
            buttPanel.add(cancelButton, null);

            JPanel mainBox = GuiUtils.topCenterBottom(topPanel, paramPanel,
                                 buttPanel);
            mainPanel.add(mainBox, BorderLayout.CENTER);
            pack();

            //enable event listeners when we're done constructing the UI
            projClassCB.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ProjectionClass selectClass =
                        (ProjectionClass) projClassCB.getSelectedItem();
                    setProjection(selectClass.makeDefaultProjection());
                }
            });

            acceptButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    accept();
                }
            });
            previewButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    ProjectionClass projClass =
                        findProjectionClass(editProjection);
                    if (null != projClass) {
                        setProjFromDialog(projClass, editProjection);
                        setProjection(editProjection);
                    }
                }
            });
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    NewProjectionDialog.this.setVisible(false);
                }
            });

        }

        /**
         * Set the projection for this editor
         *
         * @param proj  projection to use
         */
        public void setProjection(ProjectionImpl proj) {
            ProjectionClass pc = findProjectionClass(proj);
            if (pc == null) {
                System.out.println("Projection Manager: cant find Class for "
                                   + proj);
                return;
            }
            if (debug) {
                System.out.println(" NPDialog set projection " + proj);
            }

            setProjectionClass(pc, proj);
            npEditControl.setProjectionImpl(proj);
            startingName = new String(proj.getName());
        }


        /**
         * Set whether this a new projection
         *
         * @param v    true for new projection
         * @param name  name for new projection
         */
        void setDoingNewProjection(boolean v, String name) {
            doingNew = v;
            //For now don't disable the type box
            nameTF.setText(name);

            if (typeLabel != null) {
                //              typeLabel.setEnabled (doingNew);
                //              projClassCB.setEnabled (doingNew);
            }

        }

        /**
         * Set the default projection to display
         */
        void setDefaultProjection() {
            // let the first projection class be the default
            ProjectionClass selectedClass =
                (ProjectionClass) projClassCB.getItemAt(0);
            if (selectedClass == null) {
                System.out.println(
                    "Projection Manager: no Default Projection available");
                return;
            }
            ProjectionImpl proj = selectedClass.makeDefaultProjection();
            setProjection(proj);
            startingName = "";
        }

        /**
         * Set the map renderer for this editor
         *
         * @param map  map renderer
         */
        public void setMap(ucar.unidata.view.Renderer map) {
            if (map != null) {
                npEditControl.setRenderer(map);
            }
        }


        /**
         * user has hit the "accept/save" button
         */
        private void accept() {
            ProjectionClass projClass = findProjectionClass(editProjection);
            if (null == projClass) {
                System.out.println(
                    "Projection Manager accept: findProjectionClass failed"
                    + editProjection);
                return;
            }
            setProjFromDialog(projClass, editProjection);
            ProjectionRect mapArea = mapEditPanel.getSelectedRegion();
            if (mapArea == null) {
                mapArea = mapEditPanel.getMapArea();
            }
            editProjection.setDefaultMapArea(mapArea);

            if (debug) {
                System.out.println("Projection Manager accept bb ="
                                   + editProjection.getDefaultMapArea());
            }
            ProjectionImpl newProj = (ProjectionImpl) editProjection.clone();  // use a copy

            if (viewDialog != null) {
                //                if ( !viewDialog.checkSaveOK(startingName,
                //                                             newProj.getName())) {
                //                    return;
                //                }
            }

            if (ProjectionManager.this.contains(newProj.getName())) {
                projTable.replaceProjection(newProj);
            } else {
                projTable.addProjection(newProj);
            }

            // set new projection to working projection and exit this Dialog
            setWorkingProjection(newProj);
            NewProjectionDialog.this.setVisible(false);
        }

        /**
         * Find the class for the projection
         *
         * @param proj   projection
         * @return corresponding ProjectionClass (or null if not found)
         */
        private ProjectionClass findProjectionClass(Projection proj) {
            Class         want          = proj.getClass();
            ComboBoxModel projClassList = projClassCB.getModel();
            for (int i = 0; i < projClassList.getSize(); i++) {
                ProjectionClass pc =
                    (ProjectionClass) projClassList.getElementAt(i);
                if (want.equals(pc.projClass)) {
                    return pc;
                }
            }
            return null;
        }

        /**
         * Set the projection class for a projection
         *
         * @param pc    ProjectionClass
         * @param proj  projection to set
         */
        private void setProjectionClass(ProjectionClass pc,
                                        ProjectionImpl proj) {
            if ((null == proj) || (null == pc)) {
                return;
            }

            if (debug) {
                System.out.println("Projection setProjectionClass= " + proj);
            }

            setFieldsWithClassParams(pc);
            editProjection = (ProjectionImpl) proj.clone();
            putProjInDialog(pc, editProjection);

            if (debug) {
                System.out.println("Projection setProjectionClass ok ");
            }

            invalidate();  // force new layout
            validate();
        }

        // construct input fields based on Projection Class

        /**
         * Set the fields from the ProjectionClass
         *
         * @param projClass  projection class to use
         */
        private void setFieldsWithClassParams(ProjectionClass projClass) {

            // set the projection in the JComboBox
            String want = projClass.toString();
            for (int i = 0; i < projClassCB.getItemCount(); i++) {
                ProjectionClass pc =
                    (ProjectionClass) projClassCB.getItemAt(i);
                if (pc.toString().equals(want)) {
                    projClassCB.setSelectedItem((Object) pc);
                    break;
                }
            }

            // set the parameter fields
            paramPanel.removeAll();
            paramPanel.setVisible(0 < projClass.paramList.size());

            List widgets = new ArrayList();
            for (int i = 0; i < projClass.paramList.size(); i++) {
                ProjectionParam pp =
                    (ProjectionParam) projClass.paramList.get(i);
                // construct the label
                String name = pp.name;
                String text = "";
                //Create a decent looking label
                for (int cIdx = 0; cIdx < name.length(); cIdx++) {
                    char c = name.charAt(cIdx);
                    if (cIdx == 0) {
                        c = Character.toUpperCase(c);
                    } else {
                        if (Character.isUpperCase(c)) {
                            text += " ";
                            c    = Character.toLowerCase(c);
                        }
                    }
                    text += c;
                }
                widgets.add(GuiUtils.rLabel(text + ": "));
                // text input field
                JTextField tf = new JTextField();
                pp.setTextField(tf);
                tf.setColumns(12);
                widgets.add(tf);
            }
            GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
            JPanel widgetPanel = GuiUtils.doLayout(widgets, 2, GuiUtils.WT_N,
                                     GuiUtils.WT_N);

            paramPanel.add("North", widgetPanel);
            paramPanel.add("Center", GuiUtils.filler());
        }

        // get values from this projection and put into Dialog fields

        /**
         * Put a projection in the dialog for editing
         *
         * @param projClass   projection class
         * @param proj        projection
         */
        private void putProjInDialog(ProjectionClass projClass,
                                     Projection proj) {
            //      nameTF.setText (proj.getName().trim());

            for (int i = 0; i < projClass.paramList.size(); i++) {
                ProjectionParam pp =
                    (ProjectionParam) projClass.paramList.get(i);
                // fetch the value from the projection object
                Double value;
                try {
                    if (debugBeans) {
                        System.out.println(
                            "Projection putProjInDialog invoke reader on "
                            + pp);
                    }
                    value = (Double) pp.reader.invoke(proj, VOIDOBJECTARG);
                    if (debugBeans) {
                        System.out.println(
                            "Projection putProjInDialog value " + value);
                    }
                } catch (Exception ee) {
                    System.err.println(
                        "ProjectionManager: putProjInDialog failed "
                        + " invoking read " + pp.name + " class "
                        + projClass);
                    continue;
                }
                String valstr = Format.d(value.doubleValue(), 5);
                pp.getTextField().setText(valstr);
            }
        }

        // set values from the Dialog fields into the projection

        /**
         * Set the projection from the dialog properties
         *
         * @param projClass  projection class
         * @param proj       projection
         */
        private void setProjFromDialog(ProjectionClass projClass,
                                       ProjectionImpl proj) {
            proj.setName(nameTF.getText().trim());

            for (int i = 0; i < projClass.paramList.size(); i++) {
                ProjectionParam pp =
                    (ProjectionParam) projClass.paramList.get(i);
                // fetch the value from the projection object
                try {
                    String   valstr = pp.getTextField().getText();
                    Double   valdub = new Double(valstr);
                    Object[] args   = { valdub };
                    if (debugBeans) {
                        System.out.println(
                            "Projection setProjFromDialog invoke writer on "
                            + pp);
                    }
                    pp.writer.invoke(proj, args);
                } catch (Exception ee) {
                    System.err.println(
                        "ProjectionManager: setProjParams failed "
                        + " invoking write " + pp.name + " class "
                        + projClass);
                    continue;
                }
            }
        }

    }  // end NewProjectionDialog

    // inner class ProjectionClass: parsed projection classes

    /**
     * Class ProjectionClass
     *
     * @author Unidata development team
     */
    private class ProjectionClass {

        /** class */
        Class projClass;

        /** name for class */
        String name;

        /** list of parameters */
        List paramList = new ArrayList();


        /**
         * Create a new ProjectionClass from the class name
         *
         * @param className   full package name of class
         *
         * @throws ClassNotFoundException  couldn't find the class
         * @throws IntrospectionException  problem with introspection
         */
        ProjectionClass(String className)
                throws ClassNotFoundException, IntrospectionException {
            this(Misc.findClass(className));
        }

        /**
         * Create a new ProjectionClass from the class
         *
         * @param pc   projection class
         *
         * @throws ClassNotFoundException  couldn't find the class
         * @throws IntrospectionException  problem with introspection
         */
        ProjectionClass(Class pc)
                throws ClassNotFoundException, IntrospectionException {
            projClass = pc;

            // eliminate common properties with "stop class" for getBeanInfo()
            Class stopClass;
            try {
                stopClass =
                    Misc.findClass("ucar.unidata.geoloc.ProjectionImpl");
            } catch (Exception ee) {
                System.err.println("constructParamInput failed ");
                stopClass = null;
            }

            // analyze current projection class as a bean; may throw IntrospectionException
            BeanInfo info = java.beans.Introspector.getBeanInfo(projClass,
                                stopClass);

            // find read/write methods
            PropertyDescriptor[] props = info.getPropertyDescriptors();
            if (debugBeans) {
                System.out.print("Bean Properties for class " + projClass);
                if ((props == null) || (props.length == 0)) {
                    System.out.println("none");
                    return;
                }
                System.out.println("");
            }
            for (int i = 0; i < props.length; i++) {
                PropertyDescriptor pd     = props[i];
                Method             reader = pd.getReadMethod();
                Method             writer = pd.getWriteMethod();
                // only interesetd in read/write properties
                if ((reader == null) || (writer == null)) {
                    continue;
                }
                //A hack to exclude some attributes
                if (pd.getName().equals("name")
                        || pd.getName().equals("defaultMapArea")) {
                    continue;
                }
                ProjectionParam p = new ProjectionParam(pd.getName(), reader,
                                        writer, pd.getPropertyType());
                paramList.add(p);

                if (debugBeans) {
                    System.out.println("  -->" + p);
                }
            }

            // get an instance of this class so we can call toClassName()
            Projection project;
            if (null == (project = makeDefaultProjection())) {
                name = "none";
                return;
            }

            // invoke the toClassName method
            try {
                Method m = projClass.getMethod("getProjectionTypeLabel",
                               VOIDCLASSARG);
                name = (String) m.invoke(project, VOIDOBJECTARG);
            } catch (NoSuchMethodException ee) {
                System.err.println(
                    "ProjectionManager: class " + projClass
                    + " does not have method getProjectionTypeLabel()");
                throw new ClassNotFoundException();
            } catch (SecurityException ee) {
                System.err.println(
                    "ProjectionManager: class " + projClass
                    + " got SecurityException on getProjectionTypeLabel()"
                    + ee);
                throw new ClassNotFoundException();
            } catch (Exception ee) {
                System.err.println(
                    "ProjectionManager: class " + projClass
                    + " Exception when invoking getProjectionTypeLabel()"
                    + ee);
                throw new ClassNotFoundException();
            }
        }

        /**
         * Create the default projection for the default class
         * @return a default projection
         */
        private ProjectionImpl makeDefaultProjection() {
            // the default constructor
            try {
                Constructor c = projClass.getConstructor(VOIDCLASSARG);
                return (ProjectionImpl) c.newInstance(VOIDOBJECTARG);
            } catch (Exception ee) {
                System.err.println(
                    "ProjectionManager makeDefaultProjection failed to construct class "
                    + projClass);
                System.err.println("   " + ee);
                return null;
            }
        }


        /**
         * Get a string representation of this
         * @return  string representation of this
         */
        public String toString() {
            return name;
        }




    }  // end ProjectionClass inner class

    // inner class ProjectionParam: parameters for Projection Class

    /**
     * Class ProjectionParam
     *
     * @author Unidata development team
     */
    private class ProjectionParam {

        /** method for a reader */
        Method reader;

        /** method for a writer */
        Method writer;

        /** Name of param */
        String name;

        /** class of param */
        Class paramType;

        /** edit field component */
        JTextField tf;

        /**
         * Create a new ProjectionParam
         *
         * @param name     name of parameter
         * @param reader   reader method (getter)
         * @param writer   writer method (setter)
         * @param paramType  class of parameter
         *
         */
        ProjectionParam(String name, Method reader, Method writer,
                        Class paramType) {
            this.name      = name;
            this.reader    = reader;
            this.writer    = writer;
            this.paramType = paramType;
        }

        /**
         * Return a String representation of the object
         * @return a String representation of the object
         */
        public String toString() {
            return paramType.getName() + " " + name + " " + ((reader == null)
                    ? "-"
                    : "R") + ((writer == null)
                              ? "-"
                              : "W");
        }

        /**
         * Set the editing text field
         *
         * @param tf  text field for editing
         */
        void setTextField(JTextField tf) {
            this.tf = tf;
        }

        /**
         * Get the editing text field
         * @return the text field for editing
         */
        JTextField getTextField() {
            return tf;
        }

    }  // end inner class ProjectionParam



    // testing 1-2-3

    /**
     * Test the class
     *
     * @param args  not used
     */
    public static void main(String[] args) {
        ProjectionManager pm = new ProjectionManager();
        pm.show();
    }

    /**
     * Get the default maps
     *
     * @return  a list of default maps
     */
    private List getDefaultMaps() {
        if (defaultMaps == null) {
            defaultMaps = new ArrayList();
            defaultMaps.add(new MapData("/auxdata/maps/OUTLSUPU",
                                        "U.S. State Outlines", Color.blue,
                                        1.0f, 0));
            defaultMaps.add(new MapData("/auxdata/maps/Countries.zip",
                                        "World Country Outlines", Color.blue,
                                        1.0f, 0));
        }
        return defaultMaps;
    }

}


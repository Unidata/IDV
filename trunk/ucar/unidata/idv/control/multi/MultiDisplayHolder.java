/*
 * $Id: MultiDisplayHolder.java,v 1.19 2007/04/16 21:32:37 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.chart.*;

import ucar.unidata.idv.ui.DataTreeDialog;
import ucar.unidata.idv.ui.IdvWindow;


import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.ui.TableSorter;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.visad.Util;


import ucar.visad.display.*;


import visad.*;


import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;




/**
 * A DisplayControl for station models
 *
 * @author MetApps Development Team
 * @version $Revision: 1.19 $
 */
public class MultiDisplayHolder extends DisplayControlImpl {

    /** Used for something?? */
    private static final String CONTROL_PREFIX = "control:";

    /** used to create new components */
    private static final String ID_VIEW = "id.mapview";

    /** used to create new components */
    private static final String ID_GLOBE = "id.globe";

    /** used to create new components */
    private static final String ID_TRANSECT = "id.transect";

    /** used to create new components */
    private static final String ID_FIELDSELECTOR = "id.fieldselector";

    /** used to create new components */
    private static final String ID_GROUP = "id.group";

    /** The types we can add */
    private static final String[] TYPES = {
        "TimeSeriesChartWrapper", "ScatterPlotChartWrapper",
        "HistogramWrapper", "StatisticsChartWrapper", "TableChartWrapper",
        "ReadoutTable"
    };


    /**
     * List of chart prototypes. We use this in the New menus to
     * get the label and the class.
     */
    private List prototypes;


    /** The initial type to add */
    private String initialComponentClass = null;

    /** Holds all data */
    private List dataList;


    /** gui */
    JTabbedPane tabbedPane = new JTabbedPane();

    /** gui */
    JDesktopPane desktopPane = new JDesktopPane();

    /** The outermost container */
    JPanel container;

    /** List of groups */
    List displayGroups = new ArrayList();

    /** Use jdesktop view */
    private boolean useDesktop = false;

    /** Used to offset new frames */
    private int frameLocation = 0;

    /** The tree in the Manage dialog */
    private DisplayTree displayTree;

    /** List of control descriptors to use for adding new display control components */
    private List controlDescriptors = new ArrayList();

    /** Data categories */
    private List dataCategories = new ArrayList();

    /**
     * The last data sources we used when we collected the possible control descriptors
     *   that can be added
     */
    private List lastDataSources;



    /**
     * Default ctor
     */
    public MultiDisplayHolder() {}


    /**
     * Gets called when all initialization is complete
     */
    public void initDone() {
        super.initDone();
        for (int groupIdx = 0; groupIdx < displayGroups.size(); groupIdx++) {
            DisplayGroup displayGroup =
                (DisplayGroup) displayGroups.get(groupIdx);
            displayGroup.initDone();
        }
        loadData();
        for (int groupIdx = 0; groupIdx < displayGroups.size(); groupIdx++) {
            DisplayGroup displayGroup =
                (DisplayGroup) displayGroups.get(groupIdx);
            insertGroup(displayGroup);
            displayGroup.setLabelShown(false);
        }
    }






    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param choices the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     */
    public boolean init(List choices) throws VisADException, RemoteException {

        for (int groupIdx = 0; groupIdx < displayGroups.size(); groupIdx++) {
            DisplayGroup displayGroup =
                (DisplayGroup) displayGroups.get(groupIdx);
            displayGroup.setDisplayControl(this);
        }

        if (displayGroups.size() == 0) {
            DisplayGroup displayGroup = createDisplayGroup("Default Group");
            if (initialComponentClass != null) {
                DisplayComponent displayComponent =
                    createDisplayComponent(initialComponentClass, choices,
                                           displayGroup);
                if (displayComponent == null) {
                    return false;
                }
            }
        }


        //Add in some point so we are part of a display
        /*
          IndicatorPoint pt =
          new IndicatorPoint("Selected Point",
          RealTupleType.LatitudeLongitudeTuple);
          pt.setVisible(false);
          addDisplayable(pt, FLAG_NONE);
        */
        setDataChoices(new ArrayList());
        return true;
    }



    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties) {
        controlContext = vc;
        for (int i = 0; i < displayGroups.size(); i++) {
            DisplayGroup displayGroup = (DisplayGroup) displayGroups.get(i);
            displayGroup.initAfterUnPersistence(this, vc, properties);
        }
        super.initAfterUnPersistence(vc, properties);
    }



    /**
     * Some component changed. Update the manage tree.
     */
    public void componentChanged() {
        if (displayTree != null) {
            displayTree.loadComponents();
        }
    }


    /**
     * Show the manage dialog
     */
    public void showDisplayTree() {
        if (displayTree == null) {
            displayTree = new DisplayTree(this);
        }
        displayTree.show(getContents());
    }


    /**
     * Return the display tree
     *
     * @return the display tree
     */
    protected DisplayTree getDisplayTree() {
        return displayTree;
    }

    /**
     * Get the extra label used for the legend.
     *
     * @param labels   labels to append to
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < displayGroups.size(); i++) {
            DisplayGroup displayGroup = (DisplayGroup) displayGroups.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(displayGroup.getName());
        }
        labels.add(sb.toString());
    }




    /**
     * Find the internal frame for the given group
     *
     * @param displayGroup The  group
     *
     * @return The frame or null if none found
     */
    protected JInternalFrame findFrameForGroup(DisplayGroup displayGroup) {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            MyInternalFrame myFrame = (MyInternalFrame) frames[i];
            if (myFrame.displayGroup == displayGroup) {
                return myFrame;
            }
        }
        return null;
    }

    /**
     * Find the tab index for the group
     *
     * @param displayGroup The  group
     *
     * @return The tab index or -1
     */
    protected int findIdxForGroup(DisplayGroup displayGroup) {
        if (tabbedPane.getTabCount() > 0) {
            return tabbedPane.indexOfComponent(displayGroup.getContents());
        }
        return -1;
    }


    /**
     * A utility to format a value using the display conventions
     *
     * @param v The value
     *
     * @return The formatted value
     */
    public String formatValue(double v) {
        DisplayConventions dc = getDisplayConventions();
        return dc.format(v);
    }


    /**
     * The  group has a new name
     *
     * @param displayGroup  group
     * @param oldName the old name
     */
    public void newName(DisplayGroup displayGroup, String oldName) {
        if (useDesktop) {
            JInternalFrame frame = findFrameForGroup(displayGroup);
            if (frame != null) {
                frame.setTitle(displayGroup.getName());
            }
        } else {
            int idx = findIdxForGroup(displayGroup);
            if (idx >= 0) {
                tabbedPane.setTitleAt(idx, displayGroup.getName());
            }
        }
    }

    /**
     * Switch between tabbed pane and internal frame
     */
    public void toggleUI() {
        if (useDesktop) {
            saveGroupLocations();
        }
        frameLocation = 0;
        useDesktop    = !useDesktop;
        container.removeAll();
        tabbedPane.removeAll();
        desktopPane.removeAll();
        for (int i = 0; i < displayGroups.size(); i++) {
            DisplayGroup displayGroup = (DisplayGroup) displayGroups.get(i);
            insertGroup(displayGroup);
        }
        if (useDesktop) {
            container.add(BorderLayout.CENTER, desktopPane);
        } else {
            container.add(BorderLayout.CENTER, tabbedPane);
        }
        container.validate();
        container.repaint();

    }

    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem((useDesktop
                                         ? "Show tabs"
                                         : "Show desktop"), this,
                                         "toggleUI"));


        super.getViewMenuItems(items, forMenuBar);
    }



    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem("Manage Displays...", this,
                                        "showDisplayTree"));
        super.getEditMenuItems(items, forMenuBar);
    }



    /**
     * If the given list just holds data choices then just return the list.
     * Else if it holds lists that hold data choices then expand the lists
     * out.
     *
     * @param theChoices Either a list of data choices or a list of lists of data choices.
     *
     * @return The flattened list.
     */
    private List flattenChoices(List theChoices) {
        if (theChoices == null) {
            return null;
        }
        if (theChoices.size() == 0) {
            return theChoices;
        }
        if (theChoices.get(0) instanceof List) {
            List flatChoices = new ArrayList();
            for (int i = 0; i < theChoices.size(); i++) {
                flatChoices.addAll((List) theChoices.get(i));
            }
            theChoices = flatChoices;
        }
        return theChoices;
    }



    /**
     * Have the user select 1 or more data choices
     *
     * @param titles One or more field selector titles. There will be
     * one data tree in the dialog for each entry in the titles list.
     * @param multiples Can we select more than one data choice in each data tree
     * @param selectedDataChoices List of data choices already selected
     * @param categories The data categories to choose with
     *
     * @return List of choices or null if none selected
     */
    private List selectDataChoices(List titles, boolean multiples,
                                   List selectedDataChoices,
                                   List categories) {

        /*        List dataSourcesForTree = getControlContext().getAllDataSources();
        DataTreeDialog dataDialog = new DataTreeDialog(getIdv(), null, null,
                                        categories, titles, multiples,
                                        dataSourcesForTree,
                                        selectedDataChoices);


        List selected = flattenChoices(dataDialog.getSelected());
        dataDialog.dispose();
        return selected;*/
        //TODO:
        return null;
    }



    /**
     * Utility to create a component. The args holds the component type
     * and the display group
     *
     * @param args args
     */
    public void createComponentIn(Object[] args) {
        createComponent((String) args[0], (DisplayGroup) args[1]);
    }



    /**
     * Get the component name.
     *
     * @return tab or frame
     */
    protected String getLowerComponentName() {
        return (useDesktop
                ? "frame"
                : "tab");
    }

    /**
     * Get the component name.
     *
     * @return Tab or Frame
     */
    protected String getUpperComponentName() {
        return (useDesktop
                ? "Frame"
                : "Tab");
    }


    /**
     * Create the component.
     *
     * @param type type of component
     */
    public void createComponentInNewTab(String type) {
        String groupName =
            GuiUtils.getInput("Please enter " + getLowerComponentName()
                              + " name", getUpperComponentName() + " name: ",
                                         "");
        if (groupName == null) {
            return;
        }
        createComponent(type, createDisplayGroup(groupName));
    }


    /**
     * Create the display group.
     *
     */
    public void createDisplayGroup() {
        String groupName =
            GuiUtils.getInput("Please enter " + getLowerComponentName()
                              + " name", getUpperComponentName() + " name: ",
                                         "");
        if (groupName == null) {
            return;
        }
        showDisplayComponent(createDisplayGroup(groupName));
        componentChanged();
    }




    /**
     * Create the component.
     *
     * @param type type of component
     */
    public void createComponentInCurrentTab(String type) {
        createComponent(type, getCurrentDisplayGroup());
    }



    /**
     * Create the component
     *
     * @param type type of component
     * @param displayGroup The display group
     */
    protected void createComponent(String type, DisplayGroup displayGroup) {
        if (type.equals(ID_GROUP)) {
            addGroup(displayGroup);
            return;
        }

        DisplayComponent displayComponent = createDisplayComponent(type,
                                                null, displayGroup);
        if (displayComponent == null) {
            return;
        }
        try {
            displayComponent.loadData();
        } catch (Exception excp) {
            logException("loading data ", excp);
        }
        showDisplayComponent(displayComponent);
        componentChanged();
    }

    /**
     * Get the data cagtegories for the given chart
     *
     * @param chartWrapper The chart  to get the categories for
     *
     * @return The data categories
     */
    private List getCategories(ChartWrapper chartWrapper) {
        List categories = chartWrapper.getCategories();
        if (categories == null) {
            categories = Misc.newList(getCategories());
        }
        return categories;
    }


    /**
     * Add field
     *
     * @param chartWrapper chart
     */
    public void addFieldToChartWrapper(ChartWrapper chartWrapper) {
        showDisplayComponent(chartWrapper);

        List choices =
            selectDataChoices(chartWrapper.getFieldSelectionLabels(),
                              chartWrapper.doMultipleAddFields(), null,
                              getCategories(chartWrapper));
        if (choices == null) {
            return;
        }
        try {
            chartWrapper.addDataChoices(choices);
        } catch (Exception excp) {
            logException("loading data ", excp);
        }
    }


    /**
     * Instantiate the list of prototype charts
     *
     * @return List of ChartWrapper objects that serves as prototypes.
     */
    public List getPrototypes() {
        if (prototypes == null) {
            prototypes = new ArrayList();
            for (int i = 0; i < TYPES.length; i++) {
                try {
                    Class c = findChartClass(TYPES[i]);
                    prototypes.add(c.newInstance());
                } catch (Throwable thr) {
                    LogUtil.userErrorMessage("Unable to find chart:"
                                             + TYPES[i]);
                }
            }
        }
        return prototypes;
    }



    /**
     * Add the display control entries to the menu
     *
     * @param newMenu Menu to add to
     * @param displayGroup  The group  to add the display control to
     */
    protected void addControlsToMenu(JMenu newMenu,
                                     final DisplayGroup displayGroup) {

        //Collect all of the data categories
        List dataSources = getIdv().getDataSources();
        if ( !Misc.equals(dataSources, lastDataSources)) {
            Hashtable seen = new Hashtable();
            lastDataSources = new ArrayList(dataSources);
            dataCategories  = new ArrayList();
            long t1 = System.currentTimeMillis();
            for (int dataSourceIdx = 0; dataSourceIdx < dataSources.size();
                    dataSourceIdx++) {
                DataSource dataSource =
                    (DataSource) dataSources.get(dataSourceIdx);
                List dataChoices = dataSource.getDataChoices();
                for (int dataChoiceIdx = 0;
                        dataChoiceIdx < dataChoices.size(); dataChoiceIdx++) {
                    DataChoice dataChoice =
                        (DataChoice) dataChoices.get(dataChoiceIdx);
                    List categories = dataChoice.getCategories();
                    for (int catIdx = 0; catIdx < categories.size();
                            catIdx++) {
                        DataCategory dataCategory =
                            (DataCategory) categories.get(catIdx);
                        String string = dataCategory.toString();
                        if (seen.get(string) == null) {
                            seen.put(string, string);
                            dataCategories.add(dataCategory);
                        }
                    }
                }
            }

            long t2 = System.currentTimeMillis();
            //      System.err.println ("time:" +(t2-t1));

            //Only find control descriptors that are applicable to the data categories
            //or can stand alone
            controlDescriptors =
                ControlDescriptor.getApplicableControlDescriptors(
                    dataCategories, getIdv().getControlDescriptors(), true,
                    false);
            long t3 = System.currentTimeMillis();
            //      System.err.println ("time:" +(t3-t2));
        }


        final JMenu controlsMenu = new JMenu("Controls");
        newMenu.add(controlsMenu);
        controlsMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeControlsMenu(controlsMenu, displayGroup);
            }
        });



        final JMenu locationsMenu = new JMenu("Locations");
        newMenu.add(locationsMenu);
        locationsMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeLocationsMenu(locationsMenu, displayGroup);
            }
        });
    }


    /**
     * Populate the menu
     *
     * @param controlsMenu menu to populate
     * @param displayGroup Group to add the new display to
     */
    public void makeControlsMenu(JMenu controlsMenu,
                                 final DisplayGroup displayGroup) {
        if (controlsMenu.getItemCount() > 0) {
            return;
        }

        Hashtable map = new Hashtable();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd =
                (ControlDescriptor) controlDescriptors.get(i);
            String displayCategory = cd.getDisplayCategory();
            JMenu  catMenu         = (JMenu) map.get(displayCategory);
            if (catMenu == null) {
                catMenu = new JMenu(displayCategory);
                map.put(displayCategory, catMenu);
                controlsMenu.add(catMenu);
            }

            if (displayGroup != null) {
                catMenu.add(GuiUtils.makeMenuItem(cd.getLabel(), this,
                        "createComponentIn",
                        new Object[] { CONTROL_PREFIX + cd.getControlId(),
                                       displayGroup }));


            } else {
                catMenu.add(GuiUtils.makeMenuItem(cd.getLabel(), this,
                        "createComponentInCurrentTab",
                        CONTROL_PREFIX + cd.getControlId()));
            }
        }
    }

    /**
     * Make the menu for adding new location displays
     *
     * @param locationsMenu menu to add to
     * @param displayGroup group to add new display to
     */
    public void makeLocationsMenu(JMenu locationsMenu,
                                  final DisplayGroup displayGroup) {
        if (locationsMenu.getItemCount() > 0) {
            return;
        }
        List stations = getIdv().getLocationList();
        ObjectListener listener = new ObjectListener(
                                      getIdv().getControlDescriptor(
                                          "locationcontrol")) {
            public void actionPerformed(ActionEvent ae, Object obj) {
                addStationDisplay((NamedStationTable) obj,
                                  (ControlDescriptor) theObject,
                                  displayGroup);
            }
        };
        List menuItems = NamedStationTable.makeMenuItems(stations, listener);
        GuiUtils.makeMenu(locationsMenu, menuItems);
    }



    /**
     * Override base class method to create a new window. We make the new IdvWindow as
     * a main window.
     *
     * @return The new window
     */
    protected IdvWindow createIdvWindow() {
        return new IdvWindow(getTitle(), getIdv(), true);
    }


    /**
     * Initialize the window. We add the status bar to it.
     *
     * @param window The window to init
     */
    public void initWindow(IdvWindow window) {
        JPanel status =
            getControlContext().getIdv().getIdvUIManager().doMakeStatusBar(
                window);
        JComponent contents = GuiUtils.centerBottom(window.getContents(),
                                  status);
        window.setContents(contents);
        super.initWindow(window);
    }


    /**
     * Don't close the window
     */
    protected void handleWindowClosing() {
        //Noop
    }


    /**
     * Add the new station location display control
     *
     * @param stationTable The locations
     * @param cd The control descriptor
     * @param displayGroup The group to add the new display to
     */
    public void addStationDisplay(NamedStationTable stationTable,
                                  ControlDescriptor cd,
                                  DisplayGroup displayGroup) {
        if (displayGroup == null) {
            displayGroup = getCurrentDisplayGroup();
        }
        if (displayGroup == null) {
            return;
        }



        String properties = "stationTableName=" + stationTable.getFullName()
                            + ";makeWindow=false;showInLegend=true;";


        DisplayControlImpl displayControl =
            (DisplayControlImpl) getIdv().doMakeControl(new ArrayList(), cd,
                properties, null, false);

        if (displayControl == null) {
            return;
        }
        DisplayControlWrapper displayWrapper =
            new DisplayControlWrapper(displayControl);
        displayWrapper.setDisplayControl(this);
        displayGroup.addDisplayComponent(displayWrapper);
    }



    /**
     *
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {
        JMenu newMenu = new JMenu("New");
        items.add(newMenu);
        newMenu.add(GuiUtils.makeMenuItem(getUpperComponentName(), this,
                                          "createDisplayGroup"));

        newMenu.add(GuiUtils.makeMenuItem("Group", this,
                                          "createComponentInCurrentTab",
                                          ID_GROUP));


        newMenu.addSeparator();
        newMenu.add(GuiUtils.makeMenuItem("Map Display", this,
                                          "createComponentInCurrentTab",
                                          ID_VIEW));

        newMenu.add(GuiUtils.makeMenuItem("Transect Display", this,
                                          "createComponentInCurrentTab",
                                          ID_TRANSECT));


        newMenu.add(GuiUtils.makeMenuItem("Globe Display", this,
                                          "createComponentInCurrentTab",
                                          ID_GLOBE));
        newMenu.addSeparator();

        newMenu.add(GuiUtils.makeMenuItem("Field Selector", this,
                                          "createComponentInCurrentTab",
                                          ID_FIELDSELECTOR));
        newMenu.addSeparator();


        addControlsToMenu(newMenu, null);
        newMenu.addSeparator();


        JMenu chartMenu = new JMenu("Charts");
        newMenu.add(chartMenu);

        List prototypes = getPrototypes();
        for (int i = 0; i < prototypes.size(); i++) {
            ChartWrapper prototype = (ChartWrapper) prototypes.get(i);
            chartMenu.add(GuiUtils.makeMenuItem(prototype.getTypeName(),
                    this, "createComponentInCurrentTab",
                    prototype.getClass().getName()));
        }


        List  displayControls = getIdv().getDisplayControls();
        JMenu importMenu      = null;
        for (int i = 0; i < displayControls.size(); i++) {
            DisplayControlImpl displayControl =
                (DisplayControlImpl) displayControls.get(i);
            if ( !displayControl.getMakeWindow()) {
                continue;
            }
            if (displayControl == this) {
                continue;
            }
            if (importMenu == null) {
                importMenu = new JMenu("Import");
                items.add(importMenu);
            }
            importMenu.add(GuiUtils.makeMenuItem(displayControl.toString(),
                    this, "importDisplayControl", displayControl));
        }




        super.getFileMenuItems(items, forMenuBar);
    }


    /**
     * Import the given display control into me
     *
     * @param displayControl The control to import
     */
    public void importDisplayControl(DisplayControlImpl displayControl) {
        displayControl.guiImported();
        DisplayGroup displayGroup = getCurrentDisplayGroup();
        DisplayControlWrapper displayWrapper =
            new DisplayControlWrapper(displayControl);
        displayWrapper.setDisplayControl(this);
        if (displayGroup != null) {
            displayGroup.addDisplayComponent(displayWrapper);
        }
    }


    /**
     * Get te display group in the current tab
     *
     * @return chaart group in the current tab
     */
    public DisplayGroup getCurrentDisplayGroup() {
        if (useDesktop) {
            JInternalFrame[] frames = desktopPane.getAllFrames();
            if (frames.length > 0) {
                MyInternalFrame myFrame =
                    (MyInternalFrame) desktopPane.getSelectedFrame();
                if (myFrame != null) {
                    return myFrame.displayGroup;
                }
            }
        } else {
            if (tabbedPane.getTabCount() > 0) {
                JComponent panel =
                    (JComponent) tabbedPane.getSelectedComponent();
                for (int i = 0; i < displayGroups.size(); i++) {
                    DisplayGroup displayGroup =
                        (DisplayGroup) displayGroups.get(i);
                    if (panel == displayGroup.getContents()) {
                        return displayGroup;
                    }
                }
            }
        }
        return createDisplayGroup("Default Group");
    }


    /**
     * Show the tab the group is in
     *
     * @param displayComponent The component to show
     */
    public void showDisplayComponent(DisplayComponent displayComponent) {
        if (displayComponent.getDisplayGroup() != null) {
            GuiUtils.showComponentInTabs(displayComponent.getContents(),
                                         false);
            showDisplayComponent(displayComponent.getDisplayGroup());
            return;
        }
        if ( !(displayComponent instanceof DisplayGroup)) {
            return;
        }


        if (useDesktop) {
            JInternalFrame frame =
                findFrameForGroup((DisplayGroup) displayComponent);
            if (frame != null) {
                desktopPane.setSelectedFrame(frame);
            }
        } else {
            if (tabbedPane.getTabCount() > 0) {
                int idx = findIdxForGroup((DisplayGroup) displayComponent);
                if (idx >= 0) {
                    tabbedPane.setSelectedIndex(idx);
                }
            }
        }
    }




    /**
     * Try to find the chart wrapper class with the given name.
     * This will prepend ucar.unidata.idv.control.chart to the name
     * if it first fails
     *
     * @param name Class name
     *
     * @return The Class
     *
     * @throws ClassNotFoundException When we cannot find the class
     */
    private Class findChartClass(String name) throws ClassNotFoundException {
        Class c = null;
        try {
            c = Misc.findClass(name);
        } catch (Throwable cnfe) {
            c = Misc.findClass("ucar.unidata.idv.control.chart." + name);
        }
        return c;
    }

    /**
     * Add the new  menu items for the given group.
     *
     * @param displayGroup The group to add the display to
     * @param newMenu The menu
     */
    public void addGroupNewMenu(DisplayGroup displayGroup, JMenu newMenu) {
        newMenu.add(GuiUtils.makeMenuItem("Group", this, "addGroup",
                                          displayGroup));

        newMenu.addSeparator();
        newMenu.add(GuiUtils.makeMenuItem("Map Display", this,
                                          "createComponentIn",
                                          new Object[] { ID_VIEW,
                displayGroup }));


        newMenu.add(GuiUtils.makeMenuItem("Transect Display", this,
                                          "createComponentIn",
                                          new Object[] { ID_TRANSECT,
                displayGroup }));



        newMenu.add(GuiUtils.makeMenuItem("Globe Display", this,
                                          "createComponentIn",
                                          new Object[] { ID_GLOBE,
                displayGroup }));


        newMenu.addSeparator();
        newMenu.add(GuiUtils.makeMenuItem("Field Selector", this,
                                          "createComponentIn",
                                          new Object[] { ID_FIELDSELECTOR,
                displayGroup }));

        newMenu.addSeparator();



        addControlsToMenu(newMenu, displayGroup);
        newMenu.addSeparator();
        JMenu chartMenu = new JMenu("Charts");
        newMenu.add(chartMenu);

        List prototypes = getPrototypes();
        for (int i = 0; i < prototypes.size(); i++) {
            ChartWrapper prototype = (ChartWrapper) prototypes.get(i);
            chartMenu.add(GuiUtils.makeMenuItem(prototype.getTypeName(),
                    this, "createComponentIn",
                    new Object[] { prototype.getClass().getName(),
                                   displayGroup }));
        }
    }



    /**
     * Make the menu bar. Add in the system menu bar and make the DisplayControl menus
     * as a sub-menu.
     *
     *
     * @return menu bar
     */
    protected JMenuBar doMakeMenuBar() {
        List     menus       = doMakeMenuBarMenus(new ArrayList());

        JMenuBar menuBar     = getIdv().getIdvUIManager().doMakeMenuBar();
        JMenu    displayMenu = GuiUtils.makeMenu("Multi Display", menus);
        menuBar.add(displayMenu);
        return menuBar;
    }



    /**
     * Create the display component
     *
     * @param addType The thing to add
     * @param dataChoices data
     * @param displayGroup group for display component
     *
     * @return new display component
     */
    public DisplayComponent createDisplayComponent(String addType,
            List dataChoices, DisplayGroup displayGroup) {

        if (addType == null) {
            return null;
        }
        try {
            if (addType.equals(ID_VIEW) || addType.equals(ID_TRANSECT)
                    || addType.equals(ID_GLOBE)) {
                String         properties = "";
                ViewDescriptor vd         = new ViewDescriptor();
                if (addType.equals(ID_GLOBE) || addType.equals(ID_VIEW)) {
                    vd.setClassNames(
                        Misc.newList("ucar.unidata.idv.MapViewManager"));
                } else if (addType.equals(ID_TRANSECT)) {
                    vd.setClassNames(
                        Misc.newList("ucar.unidata.idv.TransectViewManager"));
                }
                if (addType.equals(ID_GLOBE)) {
                    properties =
                        "useGlobeDisplay=true;initialMapResources=/auxdata/maps/globemaps.xml;";
                }

                ViewManager vm =
                    (ViewManager) getIdv().getVMManager().createViewManager(
                        vd, properties);
                vm.setInitialSplitPaneLocation(1.0);
                getIdv().getVMManager().setLastActiveViewManager(vm);
                ViewWrapper viewWrapper = new ViewWrapper(vm);
                viewWrapper.setDisplayControl(this);
                if (displayGroup != null) {
                    displayGroup.addDisplayComponent(viewWrapper);
                }

                return viewWrapper;
            }



            if (addType.equals(ID_FIELDSELECTOR)) {
                FieldSelectorWrapper fieldSelectorWrapper =
                    new FieldSelectorWrapper();
                fieldSelectorWrapper.setDisplayControl(this);
                if (displayGroup != null) {
                    displayGroup.addDisplayComponent(fieldSelectorWrapper);
                }

                return fieldSelectorWrapper;

            }


            if (addType.startsWith(CONTROL_PREFIX)) {
                addType = addType.substring(8);
                ControlDescriptor controlDescriptor =
                    getControlContext().getIdv().getControlDescriptor(
                        addType);
                if (controlDescriptor == null) {
                    return null;
                }
                List fieldNames = Misc.newList("Please select a field");
                if (controlDescriptor.canStandAlone()) {
                    dataChoices = new ArrayList();
                } else {
                    dataChoices = selectDataChoices(fieldNames, false,
                            dataChoices,
                            Misc.newList(controlDescriptor.getCategories()));
                    if (dataChoices == null) {
                        return null;
                    }
                }

                DisplayControlImpl displayControl =
                    (DisplayControlImpl) getControlContext().getIdv()
                        .doMakeControl(dataChoices, controlDescriptor,
                                       "makeWindow=false;showInLegend=true;",
                                       null, false);
                if (displayControl == null) {
                    return null;
                }
                DisplayControlWrapper displayWrapper =
                    new DisplayControlWrapper(displayControl);
                displayWrapper.setDisplayControl(this);
                if (displayGroup != null) {
                    displayGroup.addDisplayComponent(displayWrapper);
                }
                return displayWrapper;
            }


            Class        c            = findChartClass(addType);
            ChartWrapper chartWrapper = (ChartWrapper) c.newInstance();


            if (dataChoices == null) {
                dataChoices = new ArrayList();
            }
            List fieldNames = chartWrapper.getFieldSelectionLabels();
            if (dataChoices.size() < fieldNames.size()) {
                dataChoices = selectDataChoices(fieldNames,
                        chartWrapper.doMultipleAddFields(), dataChoices,
                        getCategories(chartWrapper));
                if (dataChoices == null) {
                    return null;
                }
            }
            chartWrapper.init(this, dataChoices);
            if (displayGroup != null) {
                displayGroup.addDisplayComponent(chartWrapper);
            }
            return chartWrapper;
        } catch (Exception exc) {
            LogUtil.logException("Error: Unable to load class: " + addType,
                                 exc);
        }
        return null;

    }


    /**
     * Add the group
     *
     * @param displayGroup The display group
     *
     */
    public void addGroup(DisplayGroup displayGroup) {
        String name = GuiUtils.getInput("Please enter group name",
                                        "Group name: ", "");
        if (name != null) {
            displayGroup.addDisplayComponent(new DisplayGroup(name));
            componentChanged();
        }
    }



    /**
     * Find or creaate the group
     *
     * @param groupName group name
     *
     * @return group
     */
    public DisplayGroup createDisplayGroup(String groupName) {
        DisplayGroup displayGroup = new DisplayGroup(groupName);
        displayGroup.setDisplayControl(this);
        displayGroups.add(displayGroup);
        insertGroup(displayGroup);
        displayGroup.setLabelShown(false);
        return displayGroup;
    }





    /**
     * Find the display group by name
     *
     * @param groupName name
     *
     * @return display group or null
     */
    public DisplayGroup getDisplayGroup(String groupName) {
        for (int groupIdx = 0; groupIdx < displayGroups.size(); groupIdx++) {
            DisplayGroup displayGroup =
                (DisplayGroup) displayGroups.get(groupIdx);
            if (displayGroup.getName().equals(groupName)) {
                return displayGroup;
            }
        }
        return null;
    }




    /**
     * Class MyInternalFrame holds a display group
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.19 $
     */
    private static class MyInternalFrame extends JInternalFrame {

        /** The display group */
        DisplayGroup displayGroup;

        /**
         * ctor
         *
         * @param displayGroup The display group
         */
        public MyInternalFrame(DisplayGroup displayGroup) {
            super(displayGroup.getName(), true, false, true, true);
            this.displayGroup = displayGroup;
        }
    }

    ;




    /**
     * Insert the group into the main ui. Either into the desktop or
     * into the tabbed pane
     *
     * @param displayGroup The display group
     */
    private void insertGroup(DisplayGroup displayGroup) {
        if (useDesktop) {
            JInternalFrame internalFrame = new MyInternalFrame(displayGroup);
            internalFrame.getContentPane().add(displayGroup.getContents());
            desktopPane.add(internalFrame);
            internalFrame.pack();
            Rectangle lastPosition = displayGroup.getLastPosition();
            if (lastPosition != null) {
                internalFrame.setBounds(lastPosition);
            } else {
                internalFrame.setSize(new Dimension(500, 400));
                internalFrame.setLocation(frameLocation, frameLocation);
                frameLocation += 10;
            }
            internalFrame.show();
            try {
                if (displayGroup.getIconified()) {
                    internalFrame.setIcon(true);
                } else {
                    internalFrame.setSelected(true);
                }
            } catch (java.beans.PropertyVetoException e) {}
        } else {
            tabbedPane.add(displayGroup.getName(),
                           displayGroup.getContents());
        }
        updateLegendLabel();
    }




    /**
     * A hook to allow derived classes to tell us to add this
     * as an animation listener
     *
     * @return Add as animation listener
     */
    protected boolean shouldAddAnimationListener() {
        return true;
    }

    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        super.timeChanged(time);
        if (time.isMissing()) {
            return;
        }
        for (int i = 0; i < displayGroups.size(); i++) {
            ((DisplayGroup) displayGroups.get(i)).animationTimeChanged(time);
        }
    }


    /**
     * User clicked on a chart
     *
     * @param timeValue The time
     */
    public void setTimeInMainDisplay(double timeValue) {
        try {
            Animation anime = getSomeAnimation();
            if (anime.getNumSteps() <= 1) {
                return;
            }
            //Turn it to seconds
            timeValue = timeValue / 1000.0;
            DateTime dttm = new DateTime(timeValue);

            anime.setAniValue(dttm);
        } catch (Exception excp) {
            logException("setting time", excp);
        }

    }



    /**
     * Make the UI contents for this control.
     *
     * @return  UI container
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        if (useDesktop) {
            desktopPane.validate();
            container = GuiUtils.center(desktopPane);
        } else {
            tabbedPane.validate();
            container = GuiUtils.center(tabbedPane);
        }
        JComponent toolbar = getIdv().getIdvUIManager().getToolbarUI();
        JPanel     filler  = new JPanel();
        filler.setMinimumSize(new Dimension(1, 200));
        filler.setPreferredSize(new Dimension(1, 200));
        return GuiUtils.topCenter(toolbar,
                                  GuiUtils.leftCenter(filler, container));
    }



    /**
     * Called when a ViewManager which holds the display is destoryed
     * We don't remove ourselves here because this DisplayControl can stand alone.
     *
     * @param viewManager The view manager that was destroyed
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void viewManagerDestroyed(ViewManager viewManager)
            throws VisADException, RemoteException {
        defaultViewManager = null;
        defaultView        = null;
    }


    /**
     * Remove the display control
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doRemove() throws RemoteException, VisADException {
        if ( !getActive()) {
            return;
        }
        if (displayTree != null) {
            displayTree.close();
            displayTree = null;
        }
        for (int i = 0; i < displayGroups.size(); i++) {
            DisplayGroup displayGroup = (DisplayGroup) displayGroups.get(i);
            displayGroup.doRemove();
        }
        super.doRemove();
    }



    /**
     * Remove the component.
     *
     * @param displayComponent The component
     */
    public void removeDisplayComponent(DisplayComponent displayComponent) {
        try {
            if (displayComponent instanceof DisplayGroup) {
                removeDisplayGroup((DisplayGroup) displayComponent);
            }
            displayComponent.doRemove();
            updateLegendLabel();
            componentChanged();
        } catch (Exception excp) {
            logException("removing component ", excp);
        }
    }


    /**
     * Remove the list of displayables
     *
     * @param displayables List of displayables to remove from the main display
     */
    public void removeDisplayables(List displayables) {
        try {
            for (int i = 0; i < displayables.size(); i++) {
                Displayable d = (Displayable) displayables.get(i);
                removeDisplayable(d);
            }
        } catch (Exception excp) {
            logException("removing displayables ", excp);
        }

    }

    /**
     * Remove the display group
     *
     * @param displayGroup The display group
     */
    public void removeDisplayGroup(DisplayGroup displayGroup) {
        displayGroups.remove(displayGroup);
        if (useDesktop) {
            JInternalFrame frame = findFrameForGroup(displayGroup);
            if (frame != null) {
                desktopPane.remove(frame);
                desktopPane.repaint();
            }
        } else {
            int idx = findIdxForGroup(displayGroup);
            if (idx >= 0) {
                tabbedPane.remove(idx);
                tabbedPane.repaint();
            }
        }
    }





    /**
     * Load data into the <code>Displayable</code>.  This is called from
     * {@link #setData(DataChoice)} and whenever the projection changes.
     * Subclasses should override this to do whatever they need to.
     * This implementation uses a
     * {@link ucar.unidata.data.point.PointDataInstance PointDataInstance}
     * to manager the data.
     * @see #doMakeDataInstance(DataChoice)
     */
    protected void loadData() {
        try {
            for (int i = 0; i < displayGroups.size(); i++) {
                ((DisplayGroup) displayGroups.get(i)).loadData();
            }

        } catch (Exception excp) {
            logException("loading data ", excp);
        }
    }


    /**
     * Set the DisplayGroups property.
     *
     * @param value The new value for DisplayGroups
     */
    public void setDisplayGroups(List value) {
        displayGroups = value;
    }

    /**
     * Get the DisplayGroups property.
     *
     * @return The DisplayGroups
     */
    public List getDisplayGroups() {
        saveGroupLocations();
        return displayGroups;
    }


    /**
     * Copy the locations of the internal frames to the group objects
     * so we can recreate the layout later.
     */
    private void saveGroupLocations() {
        if (useDesktop) {
            for (int groupIdx = 0; groupIdx < displayGroups.size();
                    groupIdx++) {
                DisplayGroup displayGroup =
                    (DisplayGroup) displayGroups.get(groupIdx);
                JInternalFrame frame = findFrameForGroup(displayGroup);
                if (frame != null) {
                    displayGroup.setLastPosition(frame.getBounds());
                    displayGroup.setIconified(frame.isIcon());
                }

            }
        }

    }


    /**
     *  Set the title of the window if it has been created.
     *
     *  @param title The title
     */
    public void setTitle(String title) {
        super.setTitle(title);
    }


    /**
     *  Set the AddType property.
     *
     *  @param value The new value for AddType
     */
    public void setInitialComponentClass(String value) {
        initialComponentClass = value;
    }

    /**
     *  Get the AddType property.
     *
     *  @return The AddType
     */
    public String getInitialComponentClass() {
        return initialComponentClass;
    }


    /**
     * Set the UseDesktop property.
     *
     * @param value The new value for UseDesktop
     */
    public void setUseDesktop(boolean value) {
        useDesktop = value;
    }

    /**
     * Get the UseDesktop property.
     *
     * @return The UseDesktop
     */
    public boolean getUseDesktop() {
        return useDesktop;
    }


    /**
     * Should this show up in the list of displays
     *
     * @return Should this be added to the menu list
     */
    protected boolean shouldShowInDisplayList() {
        return false;
    }


}


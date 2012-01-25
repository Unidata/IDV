/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv;


import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataUtil;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.ui.HelpTipDialog;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.EventMap;

import visad.DateTime;
import visad.Unit;
import visad.VisADException;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;


/**
 * This class is responsible for the preference dialog and
 * managing general preference state.
 * A  set of {@link ucar.unidata.xml.PreferenceManager}-s are added
 * into the dialog. This class then constructs a tabbed pane
 * window, one pane for each PreferenceManager.
 * On  the user's Ok or Apply the dialog will
 * have each PreferenceManager apply its preferences.
 *
 * @author IDV development team
 */
public class IdvPreferenceManager extends IdvManager implements ActionListener {

    /** test value for formatting */
    private static double latlonValue = -104.56284;

    /** Decimal format */
    private static DecimalFormat latlonFormat = new DecimalFormat();

    /** Date formats */
    public static final List<String> DATE_FORMATS =
        Arrays.asList(DEFAULT_DATE_FORMAT, "MM/dd/yy HH:mm z",
                      "dd.MM.yy HH:mm z", "yyyy-MM-dd",
                      "EEE, MMM dd yyyy HH:mm z", "HH:mm:ss", "HH:mm",
                      "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssZ");

    /** The space to use */
    private static Insets prefPanelSpacer = new Insets(0, 10, 0, 0);

    /** A mapping that holds the choosers that should be shown */
    protected Hashtable choosersToShow = null;

    /** A mapping that holds the control descriptors that should be shown */
    protected Hashtable controlDescriptorsToShow = null;

    /** Have we initialized the what control descriptors should be shown facility */
    protected boolean haveInitedControlDescriptorsToShow = false;

    /** Should we show all of the display control descriptors */
    protected boolean showAllControls = true;

    /** Should all choosers be shown */
    protected boolean showAllChoosers = true;

    /** Each PreferenceManager has a JComponent which is its gui */
    private List panels = new ArrayList();

    /** The list of {@link ucar.unidata.xml.PreferenceManager}-s */
    private List managers = new ArrayList();

    /** Have we initialized the what choosers should be shown facility */
    protected boolean haveInitedChoosersToShow = false;

    /**
     * Each PreferenceManager has an associated data object
     *   that this list contains
     */
    private List dataList = new ArrayList();

    /** mapping between checkbox and control descriptor */
    protected Hashtable cbxToCdMap;

    /** holds components for event maps */
    private Hashtable eventPanelMap;

    /** holds components for key maps */
    private List keyInfos;

    /** Holds key event components */
    private JComponent keyPanel;

    /** Holds mouse event components */
    private JComponent mousePanel;

    /** Manager */
    private PreferenceManager navManager;

    /** The tabbed pane to show each PreferenceManager in */
    private JTabbedPane pane;

    /** Holds the pane */
    private JPanel paneHolder;

    /**
     * Create the dialog with the given idv
     *
     * @param idv The IDV
     *
     */
    public IdvPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
        init();
    }

    /**
     * Add in the given PreferenceManager with its associated
     * GUI Container and data Object.
     *
     * @param tabLabel The tabbed pane label
     * @param description Text to make a JLabel from
     * @param listener The handler for this set of preferences
     * @param panel The gui for the preferences
     * @param data Associated data that is passed to the PreferenceManager when done
     */
    public void add(String tabLabel, String description,
                    PreferenceManager listener, Container panel,
                    Object data) {
        managers.add(listener);
        dataList.add(data);

        JLabel    label = new JLabel(description, JLabel.CENTER);
        Container full  = GuiUtils.topCenter(GuiUtils.inset(label, 6), panel);

        if (pane == null) {
            pane = new JTabbedPane();
            paneHolder.add(pane, BorderLayout.CENTER);
        }

        pane.add(tabLabel, GuiUtils.top(full));
    }

    /**
     * Show the tab whose title matches the given tabNameToShow. This can
     * be a regular expression.
     *
     * @param tabNameToShow Tab name to show.
     */
    public void showTab(String tabNameToShow) {

        // Make sure we are showing
        show();
        toFront();

        if (pane == null) {
            return;
        }

        for (int tabIdx = 0; tabIdx < pane.getTabCount(); tabIdx++) {
            String tabName = pane.getTitleAt(tabIdx);

            if (StringUtil.stringMatch(tabName, tabNameToShow)) {
                pane.setSelectedIndex(tabIdx);

                return;
            }
        }

        System.err.println("Could not find preference tab:" + tabNameToShow);
    }

    /**
     * Init the dialog
     */
    private void init() {
        paneHolder = new JPanel(new BorderLayout());

        Component buttons = GuiUtils.makeApplyOkHelpCancelButtons(this);

        contents = GuiUtils.centerBottom(paneHolder, buttons);
        GuiUtils.setTimeZone(getDefaultTimeZone());
        GuiUtils.setDefaultDateFormat(getDefaultDateFormat());
    }

    /**
     * Handle the CANCEL, HELP, OK and APPLY events
     *
     * @param event The event
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();

        if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            close();
        } else if (cmd.equals(GuiUtils.CMD_HELP)) {
            getIdvUIManager().showHelp("idv.tools.preferences");
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            if ( !apply()) {
                return;
            }

            getIdv().applyPreferences();
            close();
        } else if (cmd.equals(GuiUtils.CMD_APPLY)) {
            if ( !apply()) {
                return;
            }

            getIdv().applyPreferences();
        }
    }

    /**
     * Close the dialog
     */
    protected void windowIsClosing() {
        if (paneHolder != null) {
            paneHolder.removeAll();
            pane = null;
        }
    }

    /**
     * Get the window title
     *
     * @return window title
     */
    public String getWindowTitle() {
        return "User Preferences";
    }

    /**
     * Show the dialog
     */
    public void show() {
        if (pane == null) {
            getIdv().initPreferences(this);
        }

        super.show();
    }

    /**
     * Init the preference gui
     */
    protected void initPreferences() {
        navManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {}
        };
        addBasicPreferences();
        (new MapViewManager(getIdv())).initPreferences(this);
        this.add("Navigation", "", navManager, makeEventPanel(),
                 new Hashtable());
        getIdv().getIdvUIManager().addToolbarPreferences(this);
        addChooserPreferences();
        addDisplayPreferences();
        addSystemPreferences();
    }

    /**
     * Apply the preferences
     *
     * @return ok
     */
    public boolean apply() {
        try {
            for (int i = 0; i < managers.size(); i++) {
                PreferenceManager manager =
                    (PreferenceManager) managers.get(i);

                manager.applyPreference(getStore(), dataList.get(i));
            }

            getStore().save();
            GuiUtils.setTimeZone(getDefaultTimeZone());
            GuiUtils.setDefaultDateFormat(getDefaultDateFormat());

            return true;
        } catch (Exception exc) {
            LogUtil.logException("Error applying preferences", exc);

            return false;
        }
    }

    /**
     * A utility that goes through the set of widgets (String preference name to
     * UI widget), finds their value and adds it into the given store.
     *
     * @param widgets The preference name to GUI widget map
     * @param store The store to put preferences in.
     */
    protected static void applyWidgets(Hashtable widgets,
                                       XmlObjectStore store) {

        for (Enumeration keys = widgets.keys(); keys.hasMoreElements(); ) {
            String key    = (String) keys.nextElement();
            Object widget = widgets.get(key);

            if (key.equals(PREF_CACHESIZE)) {
                double value = Misc.parseNumber(
                                   (((JTextField) widget).getText().trim()));

                store.put(key, value);

                continue;
            }

            if (key.equals(PREF_THREADS_RENDER)) {
                int value =
                    ((Integer) ((JComboBox) widget).getSelectedItem())
                        .intValue();

                store.put(key, new Integer(value));
                visad.util.ThreadManager.setGlobalMaxThreads(value);

                continue;
            }

            if (key.equals(PREF_THREADS_DATA)) {
                int value =
                    ((Integer) ((JComboBox) widget).getSelectedItem())
                        .intValue();

                store.put(key, new Integer(value));

                continue;
            }

            if (key.equals(PREF_MAXIMAGESIZE)) {
                int value = (int) Misc.parseNumber(
                                (((JTextField) widget).getText().trim()));

                store.put(key, value);

                continue;
            }

            if (key.equals(DataManager.PROP_CACHE_PERCENT)) {
                double value =
                    (double) Misc.parseNumber(
                        (((JTextField) widget).getText().trim())) / 100.0;

                store.put(key, value);
                visad.data.DataCacheManager.getCacheManager()
                    .setMemoryPercent(value);

                continue;
            }

            if (key.equals(PREF_SITEPATH)) {
                String text = ((JTextField) widget).getText();

                if (text.trim().equals("")) {
                    store.remove(key);
                } else {
                    store.put(key, text);
                }

                continue;
            }

            if (widget instanceof JCheckBox) {
                store.put(key, ((JCheckBox) widget).isSelected());
            } else if (widget instanceof JTextField) {
                store.put(key, ((JTextField) widget).getText());
            } else if (widget instanceof JRadioButton) {
                if (key.equals("WEIGHTED_AVERAGE")
                        || key.equals("NEAREST_NEIGHBOR")) {
                    if (((JRadioButton) widget).isSelected()) {
                        store.put(PREF_SAMPLINGMODE,
                                  ((JRadioButton) widget).getText());
                    }
                } else if (key.equals("SYSTEM_LOCALE")
                           || key.equals("US_LOCALE")) {
                    if (key.equals("SYSTEM_LOCALE")
                            && ((JRadioButton) widget).isSelected()) {
                        store.remove(PREF_LOCALE);
                    } else if (((JRadioButton) widget).isSelected()) {
                        store.put(PREF_LOCALE, key);
                    }
                } else {
                    if (((JRadioButton) widget).isSelected()) {
                        store.put(PREF_VERTICALCS, key);
                    }
                }
            } else if (widget instanceof List) {
                List data   = (List) widget;
                List result = new ArrayList();

                for (int i = 0; i < data.size(); i++) {
                    widget = data.get(i);

                    if (widget instanceof JTextField) {
                        result.add(((JTextField) widget).getText());
                    }
                }

                store.put(key, result);
            } else if (widget instanceof JComboBox) {
                Object selected = ((JComboBox) widget).getSelectedItem();

                if (selected instanceof TwoFacedObject) {
                    store.put(key, TwoFacedObject.getIdString(selected));
                } else {
                    store.put(key, selected.toString());
                }
            } else {
                continue;
            }
        }

    }

    /**
     * Add in the user preference tab for the controls to show
     */
    protected void addDisplayPreferences() {

        cbxToCdMap = new Hashtable();

        List       compList           = new ArrayList();
        List       controlDescriptors = getIdv().getAllControlDescriptors();
        final List catPanels          = new ArrayList();
        Hashtable  catMap             = new Hashtable();

        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd =
                (ControlDescriptor) controlDescriptors.get(i);
            String displayCategory = cd.getDisplayCategory();
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(displayCategory);

            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(displayCategory, false);
                catPanels.add(catPanel);
                catMap.put(displayCategory, catPanel);
                compList.add(catPanel.getTopPanel());
                compList.add(catPanel);
            }

            JCheckBox cbx = new JCheckBox(cd.getLabel(),
                                          shouldShowControl(cd, true));

            cbx.setToolTipText(cd.getDescription());
            cbxToCdMap.put(cbx, cd);
            catPanel.addItem(cbx);
            catPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));

            // compList.add(cb);
        }

        final JButton allOn = new JButton("All on");

        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < catPanels.size(); i++) {
                    ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(
                        true);
                }
            }
        });

        final JButton allOff = new JButton("All off");

        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < catPanels.size(); i++) {
                    ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(
                        false);
                }
            }
        });

        Boolean controlsAll =
            (Boolean) getIdv().getPreference(PROP_CONTROLDESCRIPTORS_ALL,
                                             Boolean.TRUE);
        final JRadioButton useAllBtn = new JRadioButton("Use all displays",
                                           controlsAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected displays:",
                             !controlsAll.booleanValue());

        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        final JPanel cbPanel    = GuiUtils.vbox(compList);
        JScrollPane  cbScroller = new JScrollPane(cbPanel);

        cbScroller.getVerticalScrollBar().setUnitIncrement(10);
        cbScroller.setPreferredSize(new Dimension(300, 300));

        JComponent exportComp =
            GuiUtils.right(GuiUtils.makeButton("Export to Plugin", this,
                "exportControlsToPlugin"));
        JComponent cbComp = GuiUtils.centerBottom(cbScroller, exportComp);
        JPanel bottomPanel =
            GuiUtils.leftCenter(
                GuiUtils.inset(
                    GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                    4), new Msg.SkipPanel(
                        GuiUtils.hgrid(
                            Misc.newList(cbComp, GuiUtils.filler()), 0)));
        JPanel controlsPanel =
            GuiUtils.inset(GuiUtils.topCenter(GuiUtils.hbox(useAllBtn,
                useTheseBtn), bottomPanel), 6);

        GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                allOn.setEnabled( !useAllBtn.isSelected());
                allOff.setEnabled( !useAllBtn.isSelected());
            }
        });
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                allOn.setEnabled( !useAllBtn.isSelected());
                allOff.setEnabled( !useAllBtn.isSelected());
            }
        });
        GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
        allOn.setEnabled( !useAllBtn.isSelected());
        allOff.setEnabled( !useAllBtn.isSelected());

        PreferenceManager controlsManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                controlDescriptorsToShow = new Hashtable();

                Hashtable table         = (Hashtable) data;
                List controlDescriptors = getIdv().getAllControlDescriptors();

                for (Enumeration keys =
                        table.keys(); keys.hasMoreElements(); ) {
                    JCheckBox         cbx = (JCheckBox) keys.nextElement();
                    ControlDescriptor cd  =
                        (ControlDescriptor) table.get(cbx);

                    controlDescriptorsToShow.put(cd.getControlId(),
                            new Boolean(cbx.isSelected()));
                }

                showAllControls = useAllBtn.isSelected();
                theStore.put(PROP_CONTROLDESCRIPTORS,
                             controlDescriptorsToShow);
                theStore.put(PROP_CONTROLDESCRIPTORS_ALL,
                             new Boolean(showAllControls));
            }
        };

        this.add("Available Displays",
                 "What displays should be available in the user interface?",
                 controlsManager, controlsPanel, cbxToCdMap);

    }

    /**
     * Add in the user preference tab for the controls to show
     */
    protected void addSystemPreferences() {

        Hashtable systemWidgets = new Hashtable();
        List      systemComps   = new ArrayList();

        final AtomicLong memVal =
            new AtomicLong(
                getStore().get(
                    IdvConstants.PREF_MEMORY,
                    SystemMemoryManager.getDefaultMemory()));
        final SystemPreference systemPref    = new SystemPreference(memVal);
        PreferenceManager      systemManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                systemPref.getSystemManager().applyPreference(theStore,
                        memVal);
                applyWidgets((Hashtable) data, theStore);
            }
        };
        systemComps.add(GuiUtils.topCenter(GuiUtils.rLabel("Memory:"),
                                           GuiUtils.filler()));
        systemComps.add(systemPref.getComponent(false));

        Vector permGenSize = new Vector();

        int[]  pgSizes     = { 64, 128, 256, 512 };
        for (int i = 0; i < pgSizes.length; i++) {
            permGenSize.add(new Integer(pgSizes[i]));
        }

        JComboBox maxPermGenCbx = new JComboBox(permGenSize);

        maxPermGenCbx.setSelectedItem(
            new Integer(getIdv().getMaxPermGenSize()));
        systemWidgets.put(PREF_MAX_PERMGENSIZE, maxPermGenCbx);

        /*  Uncomment this to add the widgets to the System tab
        systemComps.add(GuiUtils.rLabel("PermGen Size:"));

        systemComps.add(GuiUtils.leftRight(GuiUtils.hbox(maxPermGenCbx,
                new JLabel("megabytes")), GuiUtils.filler()));
        */

        systemComps.add(GuiUtils.rLabel("Caching:"));

        JCheckBox cacheCbx = new JCheckBox("Cache Data in Memory",
                                           getStore().get(PREF_DOCACHE,
                                               true));

        systemWidgets.put(PREF_DOCACHE, cacheCbx);

        JTextField diskCacheSizeFld =
            new JTextField(Misc.format(getStore().get(PREF_CACHESIZE, 20.0)),
                           5);
        List cacheComps =
            Misc.newList(new JLabel("   Disk Cache Size: "),
                         diskCacheSizeFld,
                         new JLabel(" (MB)  (for temporary files)"));

        systemWidgets.put(PREF_CACHESIZE, diskCacheSizeFld);

        JCheckBox gribIdxCacheCbx =
            new JCheckBox("Write Grib Index in Disk Cache",
                          getStore().get(DataManager.PREF_GRIBINDEXINCACHE,
                                         true));

        systemWidgets.put(DataManager.PREF_GRIBINDEXINCACHE, gribIdxCacheCbx);

        Vector threadCnt = new Vector();

        for (int i = 1; i <= Runtime.getRuntime().availableProcessors();
                i++) {
            threadCnt.add(new Integer(i));
        }

        JComboBox maxRenderThreadsFld = new JComboBox(threadCnt);

        maxRenderThreadsFld.setSelectedItem(
            new Integer(getIdv().getMaxRenderThreadCount()));
        systemWidgets.put(PREF_THREADS_RENDER, maxRenderThreadsFld);

        Vector threadCnt2 = new Vector();

        for (int i = 1; i <= 12; i++) {
            threadCnt2.add(new Integer(i));
        }

        JComboBox maxDataThreadsFld = new JComboBox(threadCnt2);

        maxDataThreadsFld.setSelectedItem(
            new Integer(getIdv().getMaxDataThreadCount()));
        systemWidgets.put(PREF_THREADS_DATA, maxDataThreadsFld);
        systemComps.add(GuiUtils.left(cacheCbx));
        systemComps.add(GuiUtils.filler());
        systemComps.add(GuiUtils.left(GuiUtils.hbox(cacheComps)));
        systemComps.add(GuiUtils.filler());
        systemComps.add(GuiUtils.left(gribIdxCacheCbx));
        systemComps.add(GuiUtils.rLabel("Thread Count:"));
        systemComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    new JLabel("Rendering: "), maxRenderThreadsFld,
                    new JLabel("   Data Reading: "), maxDataThreadsFld)));
        systemComps.add(GuiUtils.rLabel("Data Cache Memory Percent:"));

        JTextField cacheSizeFld = new JTextField(
                                      "" + (int) (100
                                          * getStore().get(
                                              DataManager.PROP_CACHE_PERCENT,
                                              0.25)), 7);

        systemWidgets.put(DataManager.PROP_CACHE_PERCENT, cacheSizeFld);
        systemComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    cacheSizeFld,
                    new JLabel(
                        " (Percent of available memory to be used in the data cache)"))));
        systemComps.add(GuiUtils.rLabel("Max Image Size:"));

        JTextField imageSizeFld =
            new JTextField(Misc.format(getStore().get(PREF_MAXIMAGESIZE,
                -1)), 7);

        systemWidgets.put(PREF_MAXIMAGESIZE, imageSizeFld);
        systemComps.add(GuiUtils.left(GuiUtils.hbox(imageSizeFld,
                new JLabel(" (Pixels, -1=no limit)"))));

        systemComps.add(GuiUtils.rLabel("Java 3D:"));

        JCheckBox geomByRefCbx =
            new JCheckBox(
                "Enable geometry by reference",
                getStateManager().getPreferenceOrProperty(
                    PREF_GEOMETRY_BY_REF, true));

        systemWidgets.put(PREF_GEOMETRY_BY_REF, geomByRefCbx);

        JCheckBox imageByRefCbx =
            new JCheckBox(
                "Enable access to image data by reference",
                getStateManager().getPreferenceOrProperty(
                    PREF_IMAGE_BY_REF, true));

        systemWidgets.put(PREF_IMAGE_BY_REF, imageByRefCbx);

        JCheckBox nPowerOf2Cbx =
            new JCheckBox(
                "Enable Non-Power of Two (NPOT) textures",
                getStateManager().getPreferenceOrProperty(
                    PREF_NPOT_IMAGE, false));

        systemWidgets.put(PREF_NPOT_IMAGE, nPowerOf2Cbx);
        systemComps.add(GuiUtils.left(geomByRefCbx));
        systemComps.add(GuiUtils.filler());
        systemComps.add(GuiUtils.left(imageByRefCbx));
        systemComps.add(GuiUtils.filler());
        systemComps.add(GuiUtils.left(nPowerOf2Cbx));

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

        JPanel systemPrefs =
            GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(systemComps, 2,
                GuiUtils.WT_N, GuiUtils.WT_N)), 5);

        /*
        JPanel systemPanel =
            GuiUtils.topCenter(GuiUtils.top(systemPref.getComponent()),
                               systemPrefs);
                               */

        this.add("System",
                 "System Preferences (requires a restart to take effect)",
                 systemManager,
                 GuiUtils.topCenter(systemPrefs, new JPanel()),
                 systemWidgets);

    }

    /**
     * Export the selected control descriptors to the plugin manager
     */
    public void exportControlsToPlugin() {
        Hashtable    selected           = new Hashtable();
        Hashtable    table              = cbxToCdMap;
        List         controlDescriptors = getIdv().getAllControlDescriptors();
        StringBuffer sb                 =
            new StringBuffer(XmlUtil.XML_HEADER);

        sb.append("<" + ControlDescriptor.TAG_CONTROLS + ">\n");

        for (Enumeration keys = table.keys(); keys.hasMoreElements(); ) {
            JCheckBox cbx = (JCheckBox) keys.nextElement();

            if ( !cbx.isSelected()) {
                continue;
            }

            ControlDescriptor cd = (ControlDescriptor) table.get(cbx);

            cd.getDescriptorXml(sb);
        }

        sb.append("</" + ControlDescriptor.TAG_CONTROLS + ">\n");
        getIdv().getPluginManager().addText(sb.toString(), "controls.xml");
    }

    /**
     * Add in the user preference tab for the choosers to show.
     */
    protected void addChooserPreferences() {

        Hashtable choosersData = new Hashtable();
        Boolean choosersAll =
            (Boolean) getIdv().getPreference(PROP_CHOOSERS_ALL, Boolean.TRUE);
        List chooserIdList = getIdv().getIdvChooserManager().getChooserIds();
        final List choosersList = new ArrayList();
        final JRadioButton useAllBtn = new JRadioButton("Use all choosers",
                                           choosersAll.booleanValue());
        final JRadioButton useTheseBtn =
            new JRadioButton("Use selected choosers:",
                             !choosersAll.booleanValue());

        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        final JButton allOn = new JButton("All on");

        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < choosersList.size(); i++) {
                    ((JCheckBox) choosersList.get(i)).setSelected(true);
                }
            }
        });

        final JButton allOff = new JButton("All off");

        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < choosersList.size(); i++) {
                    ((JCheckBox) choosersList.get(i)).setSelected(false);
                }
            }
        });

        for (int i = 0; i < chooserIdList.size(); i++) {
            String chooserId = (String) chooserIdList.get(i);
            String name =
                getIdv().getIdvChooserManager().getChooserName(chooserId);
            JCheckBox cb = new JCheckBox(name,
                                         shouldShowChooser(chooserId, true));

            choosersData.put(chooserId, cb);
            choosersList.add(cb);
        }

        final JPanel chooserPanel = GuiUtils.top(GuiUtils.vbox(choosersList));

        GuiUtils.enableTree(chooserPanel, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOff, !useAllBtn.isSelected());

        JScrollPane chooserScroller = new JScrollPane(chooserPanel);

        chooserScroller.getVerticalScrollBar().setUnitIncrement(10);
        chooserScroller.setPreferredSize(new Dimension(300, 300));

        JPanel widgetPanel =
            GuiUtils.topCenter(
                GuiUtils.hbox(useAllBtn, useTheseBtn),
                GuiUtils.leftCenter(
                    GuiUtils.inset(
                        GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                        4), chooserScroller));
        JPanel choosersPanel =
            GuiUtils.topCenter(
                GuiUtils.inset(
                    new JLabel("Note: This will take effect the next run"),
                    4), widgetPanel);

        choosersPanel = GuiUtils.inset(GuiUtils.left(choosersPanel), 6);
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(chooserPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
            }
        });
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(chooserPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
            }
        });

        PreferenceManager choosersManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                Hashtable newToShow = new Hashtable();
                Hashtable table     = (Hashtable) data;

                for (Enumeration keys =
                        table.keys(); keys.hasMoreElements(); ) {
                    String    chooserId = (String) keys.nextElement();
                    JCheckBox chooserCB = (JCheckBox) table.get(chooserId);

                    newToShow.put(chooserId,
                                  new Boolean(chooserCB.isSelected()));
                }

                choosersToShow = newToShow;
                theStore.put(PROP_CHOOSERS_ALL,
                             new Boolean(useAllBtn.isSelected()));
                theStore.put(PROP_CHOOSERS, choosersToShow);
            }
        };

        this.add("Available Choosers",
                 "What data choosers should be shown in the user interface?",
                 choosersManager, choosersPanel, choosersData);

    }

    /**
     * Get the default time zone
     *
     * @return  the default time zone
     */
    public TimeZone getDefaultTimeZone() {
        String timezoneString = getStore().get(PREF_TIMEZONE, "GMT");

        return TimeZone.getTimeZone(timezoneString);
    }

    /**
     * Get the default date format
     *
     * @return  the default time zone
     */
    public String getDefaultDateFormat() {
        return getStore().get(PREF_DATE_FORMAT, DEFAULT_DATE_FORMAT);
    }

    /**
     * Adds the basic preference tab
     */
    protected void addBasicPreferences() {

        PreferenceManager basicManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                boolean oldIconsValue =
                    getStateManager().getPreferenceOrProperty(
                        PREF_LEGEND_SHOWICONS, false);

                getIdv().getArgsManager().sitePathFromArgs = null;
                applyWidgets((Hashtable) data, theStore);
                getIdv().getIdvUIManager().setDateFormat();
                getIdv().initCacheManager();
                applyEventPreferences(theStore);

                boolean newIconsValue =
                    getStateManager().getPreferenceOrProperty(
                        PREF_LEGEND_SHOWICONS, false);

                if (oldIconsValue != newIconsValue) {
                    getVMManager().updateAllLegends();
                }
            }
        };
        Hashtable widgets  = new Hashtable();
        List      miscList = new ArrayList();
        JTextField sitePathField =
            new JTextField(getStore().get(PREF_SITEPATH, ""), 40);

        widgets.put(PREF_SITEPATH, sitePathField);

        JTextField jythonEditorField =
            new JTextField(
                getStateManager().getPreferenceOrProperty(
                    JythonManager.PROP_JYTHON_EDITOR, ""), 40);

        widgets.put(JythonManager.PROP_JYTHON_EDITOR, jythonEditorField);

        JComboBox lookAndFeelBox = null;

        if ( !GuiUtils.isMac()) {
            UIManager.LookAndFeelInfo[] landfs =
                UIManager.getInstalledLookAndFeels();
            Vector         lookAndFeelItems    = new Vector();
            TwoFacedObject selectedLookAndFeel = null;
            LookAndFeel    lookAndFeel         = UIManager.getLookAndFeel();

            for (int i = 0; i < landfs.length; i++) {
                TwoFacedObject tfo;

                lookAndFeelItems.add(tfo =
                    new TwoFacedObject(landfs[i].getName(),
                                       landfs[i].getClassName()));

                if (lookAndFeel.getClass().getName().equals(
                        landfs[i].getClassName())) {
                    selectedLookAndFeel = tfo;
                }
            }

            lookAndFeelBox = new JComboBox(lookAndFeelItems);

            if (selectedLookAndFeel != null) {
                lookAndFeelBox.setSelectedItem(selectedLookAndFeel);
            }

            widgets.put(PREF_LOOKANDFEEL, lookAndFeelBox);
        }

        GuiUtils.setHFill();

        JComponent editorComp = GuiUtils.doLayout(new Component[] {
                                    jythonEditorField,
                                    GuiUtils.makeFileBrowseButton(
                                        jythonEditorField) }, 2,
                                            GuiUtils.WT_YN, GuiUtils.WT_N);
        JComponent topPanel;

        if (lookAndFeelBox != null) {
            topPanel = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Resource Sitepath:"),
                GuiUtils.left(sitePathField),
                GuiUtils.rLabel("External Editor:"),
                GuiUtils.left(editorComp), GuiUtils.rLabel("Look & Feel:"),
                GuiUtils.left(lookAndFeelBox)
            });
        } else {
            topPanel = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Resource Sitepath:"),
                GuiUtils.left(sitePathField),
                GuiUtils.rLabel("External Editor:"),
                GuiUtils.left(editorComp) });
        }

        Object[][] prefs1 = {
            { "General:", null },
            { "Show Help Tip Dialog On Start",
              HelpTipDialog.PREF_HELPTIPSHOW },
            { "Confirm Before Exiting", PREF_SHOWQUITCONFIRM },
            { "Show Dashboard On Start", PREF_SHOWDASHBOARD, Boolean.TRUE },
            { "Show Hidden Files in File Chooser", PREF_SHOWHIDDENFILES,
              Boolean.FALSE },
            { "Show Toolbar in Windows", PREF_WINDOW_SHOWTOOLBAR,
              Boolean.TRUE },
            /*
             * { "Check for Updates", InstallManager.PREF_CHECKFORNEWRELEASE,
             * Boolean.TRUE },
             * { "Dock in Dashboard:", null },
             * { "Quick Links", PREF_EMBEDQUICKLINKSINDASHBOARD, Boolean.TRUE },
             * { "Data Chooser", PREF_EMBEDDATACHOOSERINDASHBOARD,
             * Boolean.TRUE },
             * { "Field Selector", PREF_EMBEDFIELDSELECTORINDASHBOARD,
             * Boolean.TRUE },
             * { "Display Control Windows", PREF_CONTROLSINTABS, Boolean.TRUE },
             * { "Legends", PREF_EMBEDLEGENDINDASHBOARD, Boolean.FALSE }
             */
        };
        JPanel     panel1 = makePrefPanel(prefs1, widgets, getStore());
        Object[][] prefs2 = {
            { "When Opening a Bundle:", null },
            { "Prompt user to remove displays and data", PREF_OPEN_ASK },
            { "Remove all displays and data sources", PREF_OPEN_REMOVE },
            { "Ask where to put zipped data files", PREF_ZIDV_ASK }
        };
        JPanel     panel2 = makePrefPanel(prefs2, widgets, getStore());
        Object[][] prefs3 = {
            { "Display Controls:", null },
            { "Show windows when they are created", PREF_SHOWCONTROLWINDOW },
            { "Show icons in legend", PREF_LEGEND_SHOWICONS, Boolean.FALSE,
              "<html>Show the toggle and delete icons in the legend" },
            { "Use Fast Rendering", PREF_FAST_RENDER, Boolean.TRUE,
              "<html>Turn this on for better performance at<br> the risk of having funky displays</html>" },
            { "Auto-select data when loading a template",
              IdvConstants.PREF_AUTOSELECTDATA, Boolean.FALSE,
              "<html>When loading a display template should the data be automatically selected</html>" },
            { "When Display Control Window is Closed:", null },
            { "Remove the display", DisplayControl.PREF_REMOVEONWINDOWCLOSE,
              Boolean.FALSE },
            { "Remove standalone displays",
              DisplayControl.PREF_STANDALONE_REMOVEONCLOSE, Boolean.FALSE },
            /*
             * { "Ask to remove standalone displays",
             * DisplayControl.PREF_STANDALONE_REMOVEONCLOSE_ASK,
             * Boolean.TRUE },
             * { "Enable automatic creation of displays",
             * PREF_AUTODISPLAYS_ENABLE, Boolean.TRUE },
             * { "Show the dialog when automatically creating a display",
             * PREF_AUTODISPLAYS_SHOWGUI, Boolean.TRUE }
             */
        };
        JPanel panel3    = makePrefPanel(prefs3, widgets, getStore());
        JLabel timeLabel = GuiUtils.rLabel("");

        try {
            timeLabel.setText("ex:  " + new DateTime().toString());
        } catch (Exception ve) {
            timeLabel.setText("Can't format date: " + ve);
        }

        final JComboBox dateFormatBox =
            GuiUtils.getEditableBox(new LinkedList<String>(DATE_FORMATS),
                                    getDefaultDateFormat());

        widgets.put(PREF_DATE_FORMAT, dateFormatBox);

        final JComboBox timeZoneBox = new JComboBox();
        String timezoneString = getStore().get(PREF_TIMEZONE,
                                    DEFAULT_TIMEZONE);
        String[] zones = TimeZone.getAvailableIDs();

        Arrays.sort(zones);
        GuiUtils.setListData(timeZoneBox, zones);
        timeZoneBox.setSelectedItem(timezoneString);

        Dimension d = timeZoneBox.getPreferredSize();

        GuiUtils.setPreferredWidth(timeZoneBox, (int) (d.width * .6));
        widgets.put(PREF_TIMEZONE, timeZoneBox);

        ObjectListener timeLabelListener = new ObjectListener(timeLabel) {
            public void actionPerformed(ActionEvent ae) {
                JLabel label  = (JLabel) theObject;
                String format = dateFormatBox.getSelectedItem().toString();
                String zone   = timeZoneBox.getSelectedItem().toString();

                try {
                    TimeZone tz = TimeZone.getTimeZone(zone);

                    // hack to make it the DateTime default
                    if (format.equals(DEFAULT_DATE_FORMAT)) {
                        if (zone.equals(DEFAULT_TIMEZONE)) {
                            format = DateTime.DEFAULT_TIME_FORMAT + "'Z'";
                        }
                    }

                    label.setText("ex:  "
                                  + new DateTime().formattedString(format,
                                      tz));
                } catch (Exception ve) {
                    label.setText("Invalid format or time zone");
                    LogUtil.userMessage("Invalid format or time zone");
                }
            }
        };

        dateFormatBox.addActionListener(timeLabelListener);
        timeZoneBox.addActionListener(timeLabelListener);

        String defaultLocale = getStore().get(PREF_LOCALE, "SYSTEM_LOCALE");
        JRadioButton sysLocale = new JRadioButton("System Default",
                                     defaultLocale.equals("SYSTEM_LOCALE"));

        sysLocale.setToolTipText(
            "Use the system default locale for number formatting");

        JRadioButton usLocale = new JRadioButton("English/US",
                                    !defaultLocale.equals("SYSTEM_LOCALE"));

        usLocale.setToolTipText("Use the US number formatting");
        GuiUtils.buttonGroup(sysLocale, usLocale);
        widgets.put("SYSTEM_LOCALE", sysLocale);
        widgets.put("US_LOCALE", usLocale);

        String probeFormat =
            getStore().get(DisplayControl.PREF_PROBEFORMAT,
                           DisplayControl.DEFAULT_PROBEFORMAT);
        JComboBox probeFormatFld =
            GuiUtils.getEditableBox(
                Misc.newList(
                    DisplayControl.DEFAULT_PROBEFORMAT,
                    "%rawvalue% [%rawunit%]", "%value%", "%rawvalue%",
                    "%value% <i>%unit%</i>"), probeFormat);

        widgets.put(DisplayControl.PREF_PROBEFORMAT, probeFormatFld);

        String defaultMode =
            getStore().get(PREF_SAMPLINGMODE,
                           DisplayControlImpl.WEIGHTED_AVERAGE);
        JRadioButton wa = new JRadioButton(
                              DisplayControlImpl.WEIGHTED_AVERAGE,
                              defaultMode.equals(
                                  DisplayControlImpl.WEIGHTED_AVERAGE));

        wa.setToolTipText("Use a weighted average sampling");

        JRadioButton nn = new JRadioButton(
                              DisplayControlImpl.NEAREST_NEIGHBOR,
                              defaultMode.equals(
                                  DisplayControlImpl.NEAREST_NEIGHBOR));

        nn.setToolTipText("Use a nearest neighbor sampling");
        GuiUtils.buttonGroup(wa, nn);
        widgets.put("WEIGHTED_AVERAGE", wa);
        widgets.put("NEAREST_NEIGHBOR", nn);

        String defaultVertCS = getStore().get(PREF_VERTICALCS,
                                   DataUtil.STD_ATMOSPHERE);

        // System.out.println("def vertCS = " + defaultVertCS);
        JRadioButton sa =
            new JRadioButton("Standard Atmosphere",
                             defaultVertCS.equals(DataUtil.STD_ATMOSPHERE));

        sa.setToolTipText("Use a standard atmosphere height approximation");

        JRadioButton v5d =
            new JRadioButton("Logarithmic",
                             defaultVertCS.equals(DataUtil.VIS5D_VERTICALCS));

        v5d.setToolTipText(
            "Use a logarithmic pressure to height vertical transformation");
        widgets.put(DataUtil.STD_ATMOSPHERE, sa);
        widgets.put(DataUtil.VIS5D_VERTICALCS, v5d);
        GuiUtils.buttonGroup(sa, v5d);

        String formatString = getStore().get(PREF_LATLON_FORMAT, "##0.0");
        JComboBox formatBox = GuiUtils.getEditableBox(getDefaultFormatList(),
                                  formatString);
        JLabel formatLabel = new JLabel("");

        try {
            latlonFormat.applyPattern(formatString);
            formatLabel.setText("ex: " + latlonFormat.format(latlonValue));
        } catch (IllegalArgumentException iae) {
            formatLabel.setText("Bad format: " + formatString);
        }

        formatBox.addActionListener(new ObjectListener(formatLabel) {
            public void actionPerformed(ActionEvent ae) {
                JLabel    label   = (JLabel) theObject;
                JComboBox box     = (JComboBox) ae.getSource();
                String    pattern = box.getSelectedItem().toString();

                try {
                    latlonFormat.applyPattern(pattern);
                    label.setText("ex: " + latlonFormat.format(latlonValue));
                } catch (IllegalArgumentException iae) {
                    label.setText("bad pattern: " + pattern);
                    LogUtil.userMessage("Bad format:" + pattern);
                }
            }
        });
        widgets.put(PREF_LATLON_FORMAT, formatBox);

        List formatComps = new ArrayList();

        GuiUtils.tmpInsets = new Insets(0, 5, 0, 5);

        JPanel datePanel = GuiUtils.doLayout(new Component[] {
                               new JLabel("Pattern:"),
                               new JLabel("Time Zone:"), dateFormatBox,
                               GuiUtils.hbox(
                                   timeZoneBox,
                                   getIdv().makeHelpButton(
                                       "idv.tools.preferences.dateformat")) }, 2,
                                           GuiUtils.WT_N, GuiUtils.WT_N);

        /*
         *         formatComps.add(GuiUtils.rLabel("Date Format:"));
         * formatComps.add(GuiUtils.left(timeLabel));
         * formatComps.add(GuiUtils.filler());
         * formatComps.add(GuiUtils.left(datePanel));
         */
        formatComps.add(GuiUtils.rLabel("Date Format:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(dateFormatBox,
                getIdv().makeHelpButton("idv.tools.preferences.dateformat"),
                timeLabel, 5)));
        formatComps.add(GuiUtils.rLabel("Time Zone:"));
        formatComps.add(GuiUtils.left(timeZoneBox));
        formatComps.add(GuiUtils.rLabel("Lat/Lon Format:"));

        // formatComps.add(GuiUtils.left(formatLabel));
        // formatComps.add(GuiUtils.filler());
        formatComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    formatBox,
                    getIdv().makeHelpButton(
                        "idv.tools.preferences.latlonformat"), formatLabel,
                            5)));
        formatComps.add(GuiUtils.rLabel("Number Style:"));
        formatComps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    GuiUtils.hbox(sysLocale, usLocale),
                    getIdv().makeHelpButton(
                        "idv.tools.preferences.numberstyle"), new JLabel(""),
                            5)));
        formatComps.add(GuiUtils.rLabel("Probe Format:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(probeFormatFld,
                getIdv().makeHelpButton("idv.tools.preferences.probeformat"),
                5)));

        Unit distanceUnit = null;

        try {
            distanceUnit =
                ucar.visad.Util.parseUnit(getStore().get(PREF_DISTANCEUNIT,
                    "km"));
        } catch (Exception exc) {}

        JComboBox unitBox =
            getIdv().getDisplayConventions().makeUnitBox(distanceUnit, null);

        widgets.put(PREF_DISTANCEUNIT, unitBox);
        formatComps.add(GuiUtils.rLabel("Distance Unit:"));
        formatComps.add(GuiUtils.left(unitBox));
        formatComps.add(GuiUtils.rLabel("Sampling Mode:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(wa, nn)));
        formatComps.add(GuiUtils.rLabel("Pressure to Height:"));
        formatComps.add(GuiUtils.left(GuiUtils.hbox(sa, v5d)));

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

        JPanel formatPrefs =
            GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(formatComps, 2,
                GuiUtils.WT_N, GuiUtils.WT_N)), 5);

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

        JPanel rightPanel = panel3;
        JPanel leftPanel = GuiUtils.inset(GuiUtils.vbox(panel1, panel2),
                                          new Insets(0, 40, 0, 0));

        List panelComps = Misc.newList(GuiUtils.top(leftPanel),
                                       GuiUtils.top(rightPanel));
        JPanel panels = GuiUtils.doLayout(panelComps, 2, GuiUtils.WT_N,
                                          GuiUtils.WT_N);

        panels = GuiUtils.inset(panels, new Insets(15, 0, 0, 0));

        JPanel miscContents =
            GuiUtils.inset(GuiUtils.centerBottom(GuiUtils.left(panels),
                topPanel), 10);

        this.add("General", "General Preferences", basicManager,
                 GuiUtils.topCenter(miscContents, new JPanel()), widgets);
        this.add("Formats & Data",
                 "Formatting and Data Handling Preferences", navManager,
                 GuiUtils.topCenter(GuiUtils.top(formatPrefs), new JPanel()),
                 new Hashtable());
    }

    /**
     * Make a checkbox preference panel
     *
     * @param objects Holds (Label, preference id, Boolean default value).
     * If preference id is null then just show the label. If the entry is only length
     * 2 (i.e., no value) then default to true.
     * @param widgets The map to store the id to widget
     * @param store  Where toi look up the preference value
     *
     * @return The created panel
     */
    public static JPanel makePrefPanel(Object[][] objects, Hashtable widgets,
                                       XmlObjectStore store) {
        List comps = new ArrayList();

        for (int i = 0; i < objects.length; i++) {
            String name = (String) objects[i][0];
            String id   = (String) objects[i][1];

            if (id == null) {

                // Spacer
                if (i > 0) {
                    comps.add(new JLabel(" "));
                }

                comps.add(new JLabel(name));

                continue;
            }

            boolean   value = ((objects[i].length > 2)
                               ? ((Boolean) objects[i][2]).booleanValue()
                               : true);
            JCheckBox cb    = new JCheckBox(name, store.get(id, value));

            if (objects[i].length > 3) {
                cb.setToolTipText(objects[i][3].toString());
            }

            widgets.put(id, cb);
            comps.add(GuiUtils.inset(cb, prefPanelSpacer));
        }

        return GuiUtils.vbox(comps);
    }

    /**
     * This determines whether the IDV should do a remove display and data before
     * a bundle is loaded. It returns a 2 element boolean array. The first element
     * is whether the open should take place at all. The second element determines
     * whether displays and data should be removed before the load.
     *
     *
     * @param name The bundle name - may be null.
     * @return Element 0- did user hit cancel. Element 1 - Should remove data and displays.
     * Element 2 Should merge
     * element 3 did we ask the user
     */
    public boolean[] getDoRemoveBeforeOpening(String name) {
        boolean shouldAsk    = getStore().get(PREF_OPEN_ASK, true);
        boolean shouldRemove = getStore().get(PREF_OPEN_REMOVE, true);
        boolean shouldMerge  = getStore().get(PREF_OPEN_MERGE, true);

        if (shouldAsk) {
            JCheckBox makeAsPreferenceCbx =
                new JCheckBox("Make this my preference", true);
            JCheckBox askCbx = new JCheckBox("Don't show this window again",
                                             false);
            JCheckBox removeCbx = new JCheckBox("Remove all displays & data",
                                      shouldRemove);
            JCheckBox changeDataCbx = getIdv().getChangeDataPathCbx();
            JPanel    btnPanel      = GuiUtils.left(removeCbx);
            JCheckBox mergeCbx =
                new JCheckBox("Try to add displays to current windows",
                              shouldMerge);
            JPanel inner = GuiUtils.vbox(new Component[] {
                btnPanel, mergeCbx,
                // GuiUtils.filler(10,10),
                changeDataCbx, GuiUtils.filler(10, 10), askCbx
                // new JLabel(
                // "Note: This can be reset in the preferences window "),
            });

            inner = GuiUtils.leftCenter(new JLabel("     "), inner);

            String label;

            if (name != null) {
                label = "  Before opening the bundle, " + name
                        + ", do you want to:  ";
            } else {
                label = "  Before opening this bundle do you want to:  ";
            }

            // For now just have the nameless label
            label = "  Before opening this bundle do you want to:  ";

            JPanel panel =
                GuiUtils.topCenter(GuiUtils.inset(GuiUtils.cLabel(label), 5),
                                   inner);

            panel = GuiUtils.inset(panel, 5);

            if ( !GuiUtils.showOkCancelDialog(null, "Open bundle", panel,
                    null)) {
                return new boolean[] { false, false, false, shouldAsk };
            }

            shouldRemove = removeCbx.isSelected();
            shouldMerge  = mergeCbx.isSelected();

            if (makeAsPreferenceCbx.isSelected()) {
                getStore().put(PREF_OPEN_REMOVE, shouldRemove);
            }

            getStore().put(PREF_OPEN_MERGE, shouldMerge);
            getStore().put(PREF_OPEN_ASK, !askCbx.isSelected());
            getStore().save();
        }

        return new boolean[] { true, shouldRemove, shouldMerge, shouldAsk };
    }

    /**
     * Should the given control descriptor be shown
     *
     * @param cd The control descriptor
     * @return Should the cd be shown
     */
    public boolean shouldShowControl(ControlDescriptor cd) {
        return shouldShowControl(cd, false);
    }

    /**
     * Should the given control descriptor be shown.
     *
     * @param cd The control descriptor
     * @param ignoreAllFlag If true then don't pay attention to the show all flag. We have this
     * here so this can return the actual show value for the control descriptor when we are constructing
     * the preference gui buttons.
     * @return Should the cd be shown
     */
    public boolean shouldShowControl(ControlDescriptor cd,
                                     boolean ignoreAllFlag) {
        if ( !haveInitedControlDescriptorsToShow) {
            haveInitedControlDescriptorsToShow = true;
            showAllControls = ((Boolean) getIdv().getPreference(
                PROP_CONTROLDESCRIPTORS_ALL, Boolean.TRUE)).booleanValue();
            controlDescriptorsToShow =
                (Hashtable) getIdv().getPreference(PROP_CONTROLDESCRIPTORS);

            if (controlDescriptorsToShow == null) {
                String prop = getIdv().getProperty(PROP_CONTROLDESCRIPTORS,
                                  (String) null);

                if (prop != null) {
                    List controlIds = StringUtil.split(prop, ",");

                    for (int i = 0; i < controlIds.size(); i++) {
                        String name = (String) controlIds.get(i);

                        if (controlDescriptorsToShow == null) {
                            controlDescriptorsToShow = new Hashtable();
                        }

                        controlDescriptorsToShow.put(name, new Boolean(true));
                    }
                }
            }
        }

        if ( !ignoreAllFlag && showAllControls) {
            return true;
        }

        if (controlDescriptorsToShow != null) {
            Boolean b =
                (Boolean) controlDescriptorsToShow.get(cd.getControlId());

            if (b == null) {
                return true;
            } else {
                return b.booleanValue();
            }
        }

        return true;
    }

    /**
     *  Check if the given chooser should be shown in the chooser gui. This will first try to read
     *  the user preference PROP_CHOOSERS (A hashtable). If null then it looks for the comma separated
     *  list of choosers from the property PROP_CHOOSERS.
     *
     *  @param chooserName The name of the chooser.
     *  @return Should the chooser be shown.
     */
    public boolean shouldShowChooser(String chooserName) {
        return shouldShowChooser(chooserName, false);
    }

    /**
     * Should the named chooser be shown
     *
     * @param chooserName The chooser name
     * @param ignoreAllFlag If true then don't pay attention to the show all flag. We have this
     * here so this can return the actual show value for the chooser when we are constructing
     * the preference gui buttons.
     * @return Should the chooser be shown
     */
    protected boolean shouldShowChooser(String chooserName,
                                        boolean ignoreAllFlag) {
        if (chooserName == null) {
            return false;
        }

        if ( !haveInitedChoosersToShow) {
            haveInitedChoosersToShow = true;
            showAllChoosers =
                ((Boolean) getIdv().getPreference(PROP_CHOOSERS_ALL,
                    Boolean.TRUE)).booleanValue();
            choosersToShow =
                (Hashtable) getIdv().getPreference(PROP_CHOOSERS);

            if (choosersToShow == null) {
                String prop = getIdv().getProperty(PROP_CHOOSERS,
                                  (String) null);

                if (prop != null) {
                    List chooserIds = StringUtil.split(prop, ",");

                    for (int i = 0; i < chooserIds.size(); i++) {
                        String name = (String) chooserIds.get(i);

                        if (choosersToShow == null) {
                            choosersToShow = new Hashtable();
                        }

                        choosersToShow.put(name, Boolean.TRUE);
                    }
                }
            }
        }

        if ( !ignoreAllFlag && showAllChoosers) {
            return true;
        }

        if (choosersToShow != null) {
            Object o = choosersToShow.get(chooserName);

            if (o == null) {
                return true;
            }

            if (o instanceof Boolean) {
                return ((Boolean) o).booleanValue();
            }

            return true;
        }

        return true;
    }

    /**
     * Get list of format strings
     *
     * @return The format strings
     */
    private static List getDefaultFormatList() {
        List     formatList = new ArrayList();
        String[] formats    = {
            "##0", "##0.0", "##0.0#", "##0.0##", "0.0", "0.00", "0.000"
        };

        for (int i = 0; i < formats.length; i++) {
            formatList.add(formats[i]);
        }

        return formatList;
    }

    /**
     * Get the unit to be used to show distance
     *
     * @return default distance unit
     */
    public Unit getDefaultDistanceUnit() {
        try {
            return ucar.visad.Util.parseUnit(
                getIdv().getObjectStore().get(
                    IdvConstants.PREF_DISTANCEUNIT, "km"));
        } catch (Exception exc) {
            return null;
        }
    }

    /**
     * Get the mouse mapping preference. Always will return something
     *
     * @return mouse mappings
     */
    public int[][][] getMouseMap() {
        int[][][] map =
            (int[][][]) getStore().get(IdvConstants.PREF_EVENT_MOUSEMAP);

        if (map == null) {
            map = EventMap.IDV_MOUSE_FUNCTIONS;
        }

        return map;
    }

    /**
     * Get teh scroll wheel mappings
     *
     * @return scroll wheel mappings
     */
    public int[][] getWheelMap() {
        int[][] map =
            (int[][]) getStore().get(IdvConstants.PREF_EVENT_WHEELMAP);

        if (map == null) {
            map = EventMap.IDV_WHEEL_FUNCTIONS;
        }

        return map;
    }

    /**
     * Keyboard mappings
     *
     * @return keyboard mappings
     */
    public int[][] getKeyboardMap() {
        int[][] map =
            (int[][]) getStore().get(IdvConstants.PREF_EVENT_KEYBOARDMAP);

        if (map == null) {
            map = EventMap.IDV_KEYBOARD_FUNCTIONS;
        }

        return map;
    }

    /**
     * Apply the mouse/scroll/keyboard preferences
     *
     * @param theStore The store
     */
    protected void applyEventPreferences(XmlObjectStore theStore) {
        try {
            int[][][] map = new int[3][2][2];

            for (int mouse = 0; mouse < 3; mouse++) {
                for (int control = 0; control < 2; control++) {
                    for (int shift = 0; shift < 2; shift++) {
                        JComboBox box =
                            (JComboBox) eventPanelMap.get("mouse_" + mouse
                                + "_" + control + "_" + shift);

                        map[mouse][control][shift] =
                            GuiUtils.getValueFromBox(box);
                    }
                }
            }

            theStore.put(IdvConstants.PREF_EVENT_MOUSEMAP, map);

            int[][] wheel = new int[2][2];

            for (int control = 0; control < 2; control++) {
                for (int shift = 0; shift < 2; shift++) {
                    JComboBox box = (JComboBox) eventPanelMap.get("wheel_"
                                        + control + "_" + shift);

                    wheel[control][shift] = GuiUtils.getValueFromBox(box);
                }
            }

            theStore.put(IdvConstants.PREF_EVENT_WHEELMAP, wheel);

            int[][] keyboardMap = new int[keyInfos.size()][3];

            for (int i = 0; i < keyInfos.size(); i++) {
                KeyboardInfo ki = (KeyboardInfo) keyInfos.get(i);

                ki.set(keyboardMap[i]);
            }

            theStore.put(IdvConstants.PREF_EVENT_KEYBOARDMAP, keyboardMap);

            List vms = getIdv().getVMManager().getViewManagers();

            for (int i = 0; i < vms.size(); i++) {
                ViewManager vm = (ViewManager) vms.get(i);

                vm.getMaster().setMouseFunctions(map);
                vm.getMaster().setKeyboardEventMap(keyboardMap);

                if (vm.getMaster() instanceof NavigatedDisplay) {
                    ((NavigatedDisplay) vm.getMaster()).setWheelEventMap(
                        wheel);
                }
            }
        } catch (VisADException vae) {
            LogUtil.logException("Setting mouse event maps", vae);
        }
    }

    /**
     * Make a gui component for the mouse events
     *
     * @param map maping
     * @param mouse mouse button
     * @param control control key
     * @param shift shift key
     *
     * @return mouse component
     */
    private JComboBox makeMouseEventBox(int[][][] map, int mouse,
                                        int control, int shift) {
        int function = map[mouse][control][shift];
        JComboBox box = GuiUtils.makeComboBox(EventMap.MOUSE_FUNCTION_VALUES,
                            EventMap.MOUSE_FUNCTION_NAMES, function);

        eventPanelMap.put("mouse_" + mouse + "_" + control + "_" + shift,
                          box);

        return box;
    }

    /**
     * Make gui component for scroll wheel
     *
     * @param map map
     * @param control control key
     * @param shift shift key
     *
     * @return gui comp for scroll wheel
     */
    private JComboBox makeWheelEventBox(int[][] map, int control, int shift) {
        int function = map[control][shift];
        JComboBox box = GuiUtils.makeComboBox(EventMap.WHEEL_FUNCTION_VALUES,
                            EventMap.WHEEL_FUNCTION_NAMES, function);

        eventPanelMap.put("wheel_" + control + "_" + shift, box);

        return box;
    }

    /**
     * Apply event mappings
     *
     * @param functions functions
     */
    public void applyEventsToGui(List functions) {
        if (functions.size() == 0) {
            GuiUtils.enableTree(mousePanel, true);
            GuiUtils.enableTree(keyPanel, true);

            return;
        } else {
            GuiUtils.enableTree(mousePanel, false);
            GuiUtils.enableTree(keyPanel, false);
        }

        int[][][] mouseFunctions = (int[][][]) functions.get(0);
        int[][]   wheelFunctions = (int[][]) functions.get(1);
        int[][]   keyFunctions   = (int[][]) functions.get(2);

        for (int mouse = 0; mouse < 3; mouse++) {
            for (int control = 0; control < 2; control++) {
                for (int shift = 0; shift < 2; shift++) {
                    JComboBox box = (JComboBox) eventPanelMap.get("mouse_"
                                        + mouse + "_" + control + "_"
                                        + shift);

                    GuiUtils.setValueOfBox(
                        box, mouseFunctions[mouse][control][shift],
                        EventMap.MOUSE_FUNCTION_VALUES,
                        EventMap.MOUSE_FUNCTION_NAMES);
                }
            }
        }

        for (int control = 0; control < 2; control++) {
            for (int shift = 0; shift < 2; shift++) {
                JComboBox box = (JComboBox) eventPanelMap.get("wheel_"
                                    + control + "_" + shift);

                GuiUtils.setValueOfBox(box, wheelFunctions[control][shift],
                                       EventMap.WHEEL_FUNCTION_VALUES,
                                       EventMap.WHEEL_FUNCTION_NAMES);
            }
        }

        for (int i = 0; i < keyInfos.size(); i++) {
            KeyboardInfo ki = (KeyboardInfo) keyInfos.get(i);

            ki.applyToGui(keyFunctions);
        }
    }

    /**
     * Are the two lists, which contain mouse/scroll mappings, equal
     *
     * @param l1 list 1
     * @param l2 list 2
     *
     * @return equals
     */
    private boolean navEquals(List l1, List l2) {
        if ((l1.size() != 3) || (l2.size() != 3)) {
            return false;
        }

        if ( !Misc.arraysEquals((int[][][]) l1.get(0), (int[][][]) l2.get(0))
                && Misc.arraysEquals((int[][]) l1.get(1),
                                     (int[][]) l2.get(1))) {
            return false;
        }

        int[][] a1 = (int[][]) l1.get(2);
        int[][] a2 = (int[][]) l2.get(2);

        for (int i = 0; i < a1.length; i++) {
            int[]   tmp   = a1[i];
            boolean found = false;

            for (int j = 0; (j < a2.length) && !found; j++) {
                found = Arrays.equals(tmp, a2[j]);
            }

            if ( !found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Make the mouse/scroll/key mapping panel
     *
     *
     * @return mouse/scroll/key mapping panel
     */
    protected JComponent makeEventPanel() {

        eventPanelMap = new Hashtable();
        keyInfos      = new ArrayList();

        int[][][] mouse    = getMouseMap();
        int[][]   wheel    = getWheelMap();
        int[][]   keyboard = getKeyboardMap();
        List      current  = Misc.newList(mouse, wheel, keyboard);
        String[] predefinedNames = { "IDV", "VisAD", "Google Earth",
                                     "Custom:" };
        List[] predefinedData = { Misc.newList(EventMap.IDV_MOUSE_FUNCTIONS,
                                    EventMap.IDV_WHEEL_FUNCTIONS,
                                    EventMap.IDV_KEYBOARD_FUNCTIONS),
                                  Misc.newList(
                                      EventMap.VISAD_MOUSE_FUNCTIONS,
                                      EventMap.IDV_WHEEL_FUNCTIONS,
                                      EventMap.IDV_KEYBOARD_FUNCTIONS),
                                  Misc.newList(
                                      EventMap.GEARTH_MOUSE_FUNCTIONS,
                                      EventMap.GEARTH_WHEEL_FUNCTIONS,
                                      EventMap.GEARTH_KEYBOARD_FUNCTIONS),
                                  new ArrayList() };
        ButtonGroup  bg        = new ButtonGroup();
        List         radioBtns = new ArrayList();
        JRadioButton rb        = null;
        boolean      anyOn     = false;

        for (int i = 0; i < predefinedNames.length; i++) {
            final List list = predefinedData[i];

            rb = new JRadioButton(predefinedNames[i]);
            radioBtns.add(rb);

            if (navEquals(current, list)) {
                rb.setSelected(true);
                anyOn = true;
            }

            bg.add(rb);
            rb.addActionListener(
                GuiUtils.makeActionListener(
                    IdvPreferenceManager.this, "applyEventsToGui", list));
        }

        if ( !anyOn) {
            rb.setSelected(true);
        }

        JPanel buttons = GuiUtils.hbox(radioBtns);

        buttons.setBorder(new TitledBorder("Navigation Mode"));

        List   comps    = new ArrayList();
        JLabel keyLabel = GuiUtils.makeVerticalLabel("Key Combination");

        comps.add(GuiUtils.filler());

        String[] labels = { "Left", "Middle", "Right" };

        for (int mouseBtn = 0; mouseBtn < 3; mouseBtn++) {
            comps.add(GuiUtils.bottom(GuiUtils.cLabel(labels[mouseBtn]
                    + " (MB" + (mouseBtn + 1) + ")")));
        }

        comps.add(GuiUtils.cLabel("Scroll Wheel"));

        int CONTROL_OFF = 0;
        int CONTROL_ON  = 1;
        int SHIFT_OFF   = 0;
        int SHIFT_ON    = 1;

        comps.add(GuiUtils.rLabel("None:"));
        comps.add(makeMouseEventBox(mouse, 0, CONTROL_OFF, SHIFT_OFF));
        comps.add(makeMouseEventBox(mouse, 1, CONTROL_OFF, SHIFT_OFF));
        comps.add(makeMouseEventBox(mouse, 2, CONTROL_OFF, SHIFT_OFF));
        comps.add(makeWheelEventBox(wheel, CONTROL_OFF, SHIFT_OFF));
        comps.add(GuiUtils.rLabel("Control:"));
        comps.add(makeMouseEventBox(mouse, 0, CONTROL_ON, SHIFT_OFF));
        comps.add(makeMouseEventBox(mouse, 1, CONTROL_ON, SHIFT_OFF));
        comps.add(makeMouseEventBox(mouse, 2, CONTROL_ON, SHIFT_OFF));
        comps.add(makeWheelEventBox(wheel, CONTROL_ON, SHIFT_OFF));
        comps.add(GuiUtils.rLabel("Shift:"));
        comps.add(makeMouseEventBox(mouse, 0, CONTROL_OFF, SHIFT_ON));
        comps.add(makeMouseEventBox(mouse, 1, CONTROL_OFF, SHIFT_ON));
        comps.add(makeMouseEventBox(mouse, 2, CONTROL_OFF, SHIFT_ON));
        comps.add(makeWheelEventBox(wheel, CONTROL_OFF, SHIFT_ON));
        comps.add(GuiUtils.rLabel("Control+Shift:"));
        comps.add(makeMouseEventBox(mouse, 0, CONTROL_ON, SHIFT_ON));
        comps.add(makeMouseEventBox(mouse, 1, CONTROL_ON, SHIFT_ON));
        comps.add(makeMouseEventBox(mouse, 2, CONTROL_ON, SHIFT_ON));
        comps.add(makeWheelEventBox(wheel, CONTROL_ON, SHIFT_ON));
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        mousePanel = GuiUtils.doLayout(comps, 5, GuiUtils.WT_N,
                                       GuiUtils.WT_N);
        mousePanel = GuiUtils.leftCenter(keyLabel, mousePanel);

        List keyComps = new ArrayList();

        keyComps.add(GuiUtils.cLabel(" Function "));
        keyComps.add(GuiUtils.cLabel(" Key "));
        keyComps.add(GuiUtils.cLabel(" Control "));
        keyComps.add(GuiUtils.cLabel(" Shift "));

        int[] defaultFunc = { 0, -1, EventMap.NO_MASK };

        for (int i = 0; i < EventMap.KEY_FUNCTION_VALUES.length; i++) {
            int[] func = findKeyFunction(EventMap.KEY_FUNCTION_VALUES[i],
                                         keyboard);

            if (func == null) {
                func = defaultFunc;
            }

            keyComps.add(new JLabel(EventMap.KEY_FUNCTION_NAMES[i] + "   "));
            keyInfos.add(new KeyboardInfo(func, keyComps));
        }

        GuiUtils.tmpInsets = new Insets(3, 0, 0, 0);
        keyPanel = GuiUtils.doLayout(keyComps, 4, GuiUtils.WT_N,
                                     GuiUtils.WT_N);

        JTabbedPane tab = new JTabbedPane();

        tab.add("Mouse", GuiUtils.top(GuiUtils.inset(mousePanel, 10)));
        tab.add("Keyboard", GuiUtils.top(GuiUtils.inset(keyPanel, 10)));

        if (anyOn) {
            GuiUtils.enableTree(mousePanel, false);
            GuiUtils.enableTree(keyPanel, false);
        }

        return GuiUtils.topCenter(buttons, tab);

    }

    /**
     * Find the key function
     *
     * @param func func
     * @param keyMap map
     *
     * @return func
     */
    private int[] findKeyFunction(int func, int[][] keyMap) {
        for (int i = 0; i < keyMap.length; i++) {
            if (keyMap[i][0] == func) {
                return keyMap[i];
            }
        }

        return null;
    }

    /**
     * Class KeyboardInfo holds key event gui components
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.117 $
     */
    private static class KeyboardInfo {

        /** Holds all of the keys that have been entered */
        static Vector items = new Vector();

        /** is control key */
        JCheckBox controlCbx;

        /** The function that is mapped to */
        int function;

        /** key value */
        JComboBox keyBox;

        /** is shift key */
        JCheckBox shiftCbx;

        /**
         * ctor
         *
         * @param func func
         * @param keyComps comps
         */
        public KeyboardInfo(int[] func, List keyComps) {
            function = func[0];

            if (items.size() == 0) {
                items.add("none");
                items.add(getKeyText(KeyEvent.VK_UP, false));
                items.add(getKeyText(KeyEvent.VK_DOWN, false));
                items.add(getKeyText(KeyEvent.VK_LEFT, false));
                items.add(getKeyText(KeyEvent.VK_RIGHT, false));
                items.add("R");
                items.add("U");
            }

            keyBox = new JComboBox(items);
            keyBox.setEditable(true);
            keyComps.add(keyBox);
            controlCbx = new JCheckBox("");
            shiftCbx   = new JCheckBox("");
            keyComps.add(GuiUtils.inset(controlCbx, new Insets(0, 10, 0, 0)));
            keyComps.add(GuiUtils.inset(shiftCbx, new Insets(0, 10, 0, 0)));
            applyToGui(func);
        }

        /**
         * set the value
         *
         * @param funcs key functions
         */
        public void set(int[] funcs) {
            funcs[0] = function;
            funcs[2] = EventMap.NO_MASK;

            if (shiftCbx.isSelected() && controlCbx.isSelected()) {
                funcs[2] = KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK;
            } else if (shiftCbx.isSelected()) {
                funcs[2] = KeyEvent.SHIFT_MASK;
            } else if (controlCbx.isSelected()) {
                funcs[2] = KeyEvent.CTRL_MASK;
            }

            String key =
                keyBox.getSelectedItem().toString().trim().toUpperCase();

            if (key.equals("NONE")) {
                funcs[1] = 0;
            } else if (key.equals(getKeyText(KeyEvent.VK_UP, true))) {
                funcs[1] = KeyEvent.VK_UP;
            } else if (key.equals(getKeyText(KeyEvent.VK_DOWN, true))) {
                funcs[1] = KeyEvent.VK_DOWN;
            } else if (key.equals(getKeyText(KeyEvent.VK_RIGHT, true))) {
                funcs[1] = KeyEvent.VK_RIGHT;
            } else if (key.equals(getKeyText(KeyEvent.VK_LEFT, true))) {
                funcs[1] = KeyEvent.VK_LEFT;
            } else {
                if (key.length() > 1) {
                    throw new IllegalArgumentException("Unknown key:" + key);
                }

                funcs[1] = GuiUtils.charToKeyCode(key);
            }
        }

        /**
         * Get the text for the key.
         * @param  key  the key identifier (e.g. VK_UP)
         * @param  upCase  true to return string as upcase
         *
         * @return the corresponding String
         */
        private String getKeyText(int key, boolean upCase) {
            String s = KeyEvent.getKeyText(key);

            return (upCase)
                   ? s.toUpperCase()
                   : s;
        }

        /**
         * apply
         *
         * @param funcs functions
         */
        public void applyToGui(int[][] funcs) {
            for (int i = 0; i < funcs.length; i++) {
                if (funcs[i][0] == function) {
                    applyToGui(funcs[i]);

                    break;
                }
            }
        }

        /**
         * apply
         *
         * @param func func
         */
        public void applyToGui(int[] func) {
            String keyText = ((func[1] <= 0)
                              ? "none"
                              : getKeyText(func[1], false));

            if ( !items.contains(keyText)) {
                items.add(keyText);
            }

            keyBox.setSelectedItem(keyText);
            controlCbx.setSelected((func[2] != EventMap.NO_MASK)
                                   && ((func[2] & KeyEvent.CTRL_MASK) != 0));
            shiftCbx.setSelected((func[2] != EventMap.NO_MASK)
                                 && ((func[2] & KeyEvent.SHIFT_MASK) != 0));
        }
    }
}

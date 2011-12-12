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


import ucar.unidata.idv.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.MapDisplayControl;
import ucar.unidata.ui.DndImageButton;

import ucar.unidata.ui.DropPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.beans.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * Manages the user interface for the IDV
 *
 *
 * @author IDV development team
 */
public class ViewPanelImpl extends IdvManager implements ViewPanel {

    /** gui state */
    static Image BUTTON_ICON =
        GuiUtils.getImage("/auxdata/ui/icons/Selected.gif");

    /** icon for toggle button */
    static ImageIcon CATEGORY_OPEN_ICON;

    /** icon for toggle button */
    static ImageIcon CATEGORY_CLOSED_ICON;

    /** gui state */
    static Color BUTTON_FG_COLOR = null;

    /** gui state */
    static Color BUTTON_ON_COLOR = null;

    /** gui state */
    static Border BUTTON_BORDER;

    /** gui state */
    static boolean BUTTON_SHOWPOPUP = true;

    /** gui state */
    static boolean BUTTON_SHOWCATEGORIES = false;

    /** gui state */
    static Font BUTTON_FONT;

    /** gui state */
    static Color BUTTON_LINE_COLOR;

    /** gui state */
    static Font CATEGORY_FONT;

    /** icon for map views */
    public static ImageIcon ICON_MAP =
        GuiUtils.getImageIcon("/auxdata/ui/icons/MapIcon.png",
                              ViewPanelImpl.class);

    /** icon for transect views */
    public static ImageIcon ICON_TRANSECT =
        GuiUtils.getImageIcon("/auxdata/ui/icons/TransectIcon.png",
                              ViewPanelImpl.class);

    /** icon for globe views */
    public static ImageIcon ICON_GLOBE =
        GuiUtils.getImageIcon("/auxdata/ui/icons/GlobeIcon.png",
                              ViewPanelImpl.class);

    /** default icon */
    public static ImageIcon ICON_DEFAULT =
        GuiUtils.getImageIcon("/auxdata/ui/icons/Host24.gif",
                              ViewPanelImpl.class);




    /** The border for the header panel */
    public static Border headerPanelBorder =
        BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,
            0, 0, 0), BorderFactory.createMatteBorder(0, 0, 2, 0,
                Color.black));

    /** highlight border for view infos */
    public static Border headerPanelHighlightBorder =
        BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,
            0, 0, 0), BorderFactory.createMatteBorder(0, 0, 2, 0,
                ViewManager.borderHighlightColor));




    /** The contents */
    private JComponent contents;

    /** gui component */
    private JPanel leftPanel;

    /** gui component */
    private JPanel viewContainer;

    /** Holds the toggle buttons */
    ButtonGroup buttonGroup = new ButtonGroup();

    /** right panel */
    private GuiUtils.CardLayoutPanel rightPanel;


    /** Maps viewManager to the tab in the displays tab */
    private List vmInfos = new ArrayList();

    /** _more_ */
    private static final String PROP_CONTROLINFO = "prop.controlinfo";





    /**
     * Create me with the IDV
     *
     * @param idv The IDV
     */
    public ViewPanelImpl(IntegratedDataViewer idv) {
        super(idv);
    }

    /**
     * initialize
     */
    private void init() {
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        leftPanel.add(BorderLayout.NORTH, GuiUtils.filler(150, 1));
        viewContainer = new JPanel();
        viewContainer.setLayout(new BoxLayout(viewContainer,
                BoxLayout.Y_AXIS));
        JScrollPane viewScroll = new JScrollPane(GuiUtils.top(viewContainer));
        viewScroll.setBorder(null);
        leftPanel.add(BorderLayout.CENTER, viewScroll);

        rightPanel = new GuiUtils.CardLayoutPanel() {
            public void show(Component comp) {
                super.show(comp);
                List localVMInfos = new ArrayList(vmInfos);
                for (int vmIdx = 0; vmIdx < localVMInfos.size(); vmIdx++) {
                    VMInfo vmInfo = (VMInfo) localVMInfos.get(vmIdx);
                    for (int i = 0; i < vmInfo.controlInfos.size(); i++) {
                        ControlInfo controlInfo =
                            (ControlInfo) vmInfo.controlInfos.get(i);
                        if (controlInfo.outer == comp) {
                            controlInfo.button.setSelected(true);
                            break;
                        }
                    }
                }
            }
        };
        contents = GuiUtils.leftCenter(leftPanel, rightPanel);
        ucar.unidata.util.Msg.translateTree(contents);
        //        contents.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
        //                Color.lightGray.darker()));
        //                Color.gray));
    }


    /**
     * Find the currently selected button and select the next/previous button
     *
     * @param up If true select previous, else select next
     */
    private void selectNext(boolean up) {
        boolean gotit        = false;
        VMInfo  select       = null;
        int     index        = 0;
        List    localVMInfos = new ArrayList(vmInfos);
        for (int vmIdx = 0; !gotit && (vmIdx < localVMInfos.size());
                vmIdx++) {
            VMInfo vmInfo = (VMInfo) localVMInfos.get(vmIdx);
            List   cis    = vmInfo.controlInfos;
            for (int i = 0; i < cis.size(); i++) {
                ControlInfo ci = (ControlInfo) cis.get(i);
                if ( !ci.button.isSelected()) {
                    continue;
                }
                if (up) {
                    if (vmInfo.getCatOpen() && (i > 0)) {
                        select = vmInfo;
                        index  = i - 1;
                    } else {
                        vmIdx--;
                        while (vmIdx >= 0) {
                            VMInfo prev = (VMInfo) localVMInfos.get(vmIdx--);
                            if (prev.getCatOpen()
                                    && (prev.controlInfos.size() > 0)) {
                                select = prev;
                                index  = select.controlInfos.size() - 1;
                                break;
                            }
                        }
                    }
                } else {
                    if (vmInfo.getCatOpen() && (i < cis.size() - 1)) {
                        select = vmInfo;
                        index  = i + 1;
                    } else {
                        vmIdx++;
                        while (vmIdx < localVMInfos.size()) {
                            VMInfo next = (VMInfo) localVMInfos.get(vmIdx++);
                            if (next.getCatOpen()
                                    && (next.controlInfos.size() > 0)) {
                                select = next;
                                index  = 0;
                                break;
                            }
                        }
                    }
                }
                gotit = true;
                break;
            }
        }
        if ((select != null) && (index >= 0)
                && (index < select.controlInfos.size())) {
            ((ControlInfo) select.controlInfos.get(index)).button.doClick();
        }
    }


    /**
     * Make, if needed, and return the contents
     *
     * @return the gui contents
     */
    public JComponent getContents() {
        if (contents == null) {
            init();
        }
        return contents;
    }


    /**
     * Add the given display control
     *
     * @param control display control
     */
    public void addDisplayControl(DisplayControl control) {
        addControlTab(control, false);
    }


    /**
     * Be notified of the addition of a VM
     *
     * @param viewManager The VM
     */
    public void viewManagerAdded(ViewManager viewManager) {
        //Force the addition
        VMInfo vmInfo = getVMInfo(viewManager);
    }

    /**
     * Called when the ViewManager is removed. If we are showing legends in
     * a separate window then we remove the tab
     *
     * @param viewManager The ViewManager that was destroyed
     */
    public void viewManagerDestroyed(ViewManager viewManager) {
        synchronized (VM_MUTEX) {
            VMInfo vmInfo = findVMInfo(viewManager);
            if (vmInfo != null) {
                vmInfos.remove(vmInfo);
                vmInfo.viewManagerDestroyed();
            }
        }
    }


    /**
     * Called when the ViewManager is changed. If we are showing legends in
     * a separate window then we update the tab label
     *
     * @param viewManager The ViewManager that was changed
     */
    public void viewManagerChanged(ViewManager viewManager) {
        VMInfo vmInfo = findVMInfo(viewManager);
        if (vmInfo != null) {
            vmInfo.viewManagerChanged();
        }
    }



    /**
     * Add the control tothe control tab if we are doing control tabs
     *
     * @param control The control
     * @param forceShow If true then show the component in the window no matter what
     */
    private void addControlTab(final DisplayControl control,
                               final boolean forceShow) {
        if ( !control.getActive() || !control.canBeDocked()
                || !control.shouldBeDocked()) {
            return;
        }
        //For now cheat a little with the cast
        ((DisplayControlImpl) control).setMakeWindow(false);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                addControlTabInThread(control, forceShow);
            }
        });
    }


    /**
     * _more_
     *
     * @param control _more_
     * @param forceShow _more_
     */
    private void addControlTabInThread(final DisplayControl control,
                                       final boolean forceShow) {

        if ( !control.getActive()) {
            return;
        }

        if ( !control.canBeDocked() || !control.shouldBeDocked()) {
            return;
        }

        //Check if there are any groups that have autoimport set
        ViewManager viewManager = control.getViewManager();
        if (viewManager != null) {
            IdvWindow window = viewManager.getDisplayWindow();
            if (window != null) {
                List groups = window.getComponentGroups();
                for (int i = 0; i < groups.size(); i++) {
                    Object obj = groups.get(i);
                    if (obj instanceof IdvComponentGroup) {
                        if (((IdvComponentGroup) obj)
                                .tryToImportDisplayControl(
                                    (DisplayControlImpl) control)) {
                            return;
                        }
                    }
                }
            }
        }

        ControlInfo controlInfo =
            (ControlInfo) control.getTmpProperty(PROP_CONTROLINFO);
        if (controlInfo != null) {
            return;
        }
        //For now cheat a little with the cast
        ((DisplayControlImpl) control).setMakeWindow(false);

        JButton removeBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/delete.png", control,
                                     "doRemove");
        removeBtn.setToolTipText("Remove Display Control");

        GuiUtils.makeMouseOverBorder(removeBtn);
        JButton expandBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif", this,
                                     "expandControl", control);

        expandBtn.setToolTipText("Expand in the tabs");
        GuiUtils.makeMouseOverBorder(expandBtn);
        JButton exportBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/application_get.png",
                                     this, "undockControl", control);
        exportBtn.setToolTipText("Undock control window");
        GuiUtils.makeMouseOverBorder(exportBtn);

        JButton propBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/information.png",
                                     control, "showProperties");
        propBtn.setToolTipText("Show Display Control Properties");
        GuiUtils.makeMouseOverBorder(propBtn);

        DndImageButton dnd = new DndImageButton(control, "idv/display");
        GuiUtils.makeMouseOverBorder(dnd);
        dnd.setToolTipText("Drag and drop to a window component");
        JPanel buttonPanel =
            GuiUtils.left(GuiUtils.hbox(Misc.newList(expandBtn, exportBtn,
                dnd, propBtn, removeBtn), 4));


        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                Color.lightGray.darker()));


        JComponent inner =
            (JComponent) ((DisplayControlImpl) control).getOuterContents();
        inner = GuiUtils.centerBottom(inner, buttonPanel);
        final JComponent outer = GuiUtils.top(inner);
        outer.setBorder(BorderFactory.createEmptyBorder(2, 1, 0, 0));

        if ( !control.getActive()) {
            return;
        }
        Object dfltViewManager = control.getDefaultViewManager();
        controlInfo = new ControlInfo(control, expandBtn, outer, inner,
                                      getVMInfo(dfltViewManager));
        control.putTmpProperty(PROP_CONTROLINFO, controlInfo);
        boolean didToggle = false;
        if ( !getStateManager().getProperty(IdvConstants.PROP_LOADINGXML,
                                            false)) {
            if (forceShow || true
                    || ((DisplayControlImpl) control)
                        .shouldWindowBeVisible()) {
                //A hack for now
                //                if ( !(control instanceof MapDisplayControl)) {
                didToggle = true;
                GuiUtils.toggleHeavyWeightComponents(outer, true);
                GuiUtils.showComponentInTabs(outer);
                //                }
            }
        }
        if ( !didToggle) {
            GuiUtils.toggleHeavyWeightComponents(outer, false);
        }


    }


    /**
     * Called by the IDV when there has been a change to the display controls.
     *
     * @param control The control that changed
     */
    public void displayControlChanged(DisplayControl control) {
        ControlInfo controlInfo =
            (ControlInfo) control.getTmpProperty(PROP_CONTROLINFO);
        if (controlInfo != null) {
            controlInfo.displayControlChanged();
        }
    }



    /** _more_ */
    private Object VM_MUTEX = new Object();

    /**
     * Create, if needed, a new tab for the given VM. Note this can also be null.
     *
     * @param vm The vm
     *
     * @return Its tab
     */
    private VMInfo getVMInfo(Object vm) {
        String vmName;
        if (vm == null) {
            vm = "No View";
        }
        synchronized (VM_MUTEX) {
            VMInfo vmInfo = findVMInfo(vm);
            if (vmInfo == null) {
                vmInfo = new VMInfo(vm);
                vmInfos.add(vmInfo);
            }
            return vmInfo;
        }
    }


    /**
     * Find the VMInfo object for the given vm.
     *
     * @param vm The vm or a string for the non-viewmanager tab
     *
     * @return The VMInfo or null
     */
    private VMInfo findVMInfo(Object vm) {
        List localVMInfos = new ArrayList(vmInfos);
        for (int i = 0; i < localVMInfos.size(); i++) {
            VMInfo vmInfo = (VMInfo) localVMInfos.get(i);
            if (vmInfo.holds(vm)) {
                return vmInfo;
            }
        }
        return null;
    }




    /**
     *
     * @param control The removed control
     */
    public void removeDisplayControl(DisplayControl control) {
        removeControlTab(control);
    }


    /**
     * Remove the control from the control tab if we are doing control tabs
     * This calls removeControlTabInThread in the Swing thread
     * @param control The control
     */
    public void removeControlTab(final DisplayControl control) {
        removeControlTab(control, true);
    }

    /**
     * _more_
     *
     * @param control _more_
     * @param inSwingThread _more_
     */
    public void removeControlTab(final DisplayControl control,
                                 boolean inSwingThread) {
        if (inSwingThread) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeControlTabInThread(control);
                }
            });
        } else {
            removeControlTabInThread(control);
        }
    }

    /**
     * Remove the control from the control tab if we are doing control tabs
     *
     * @param control The control
     */
    private void removeControlTabInThread(DisplayControl control) {
        ControlInfo tabInfo =
            (ControlInfo) control.removeTmpProperty(PROP_CONTROLINFO);
        if (tabInfo != null) {
            tabInfo.removeDisplayControl();
        }
    }

    /**
     * Add view menu items for the display control
     *
     * @param control the display control
     * @param items List of menu items
     */
    public void addViewMenuItems(DisplayControl control, List items) {
        if (control.canBeDocked()) {
            items.add(GuiUtils.MENU_SEPARATOR);
            if ( !control.shouldBeDocked()) {
                items.add(
                    GuiUtils.setIcon(
                        GuiUtils.makeMenuItem(
                            "Dock in Dashboard", this, "dockControl",
                            control), "/auxdata/ui/icons/application_put.png"));
            } else {
                items.add(
                    GuiUtils.setIcon(
                        GuiUtils.makeMenuItem(
                            "Undock from Dashboard", this, "undockControl",
                            control), "/auxdata/ui/icons/application_get.png"));
            }
            List groups   = getIdv().getIdvUIManager().getComponentGroups();
            List subItems = new ArrayList();
            for (int i = 0; i < groups.size(); i++) {
                IdvComponentGroup group = (IdvComponentGroup) groups.get(i);
                subItems.add(
                    GuiUtils.makeMenuItem(
                        group.getHierachicalName(), group,
                        "importDisplayControl", control));

            }
            if (subItems.size() > 0) {
                items.add(GuiUtils.makeMenu("Export to component", subItems));
            }
        }
    }



    /**
     * Expand the control's gui in the tabs
     *
     * @param control The control
     */
    public void expandControl(DisplayControl control) {
        ControlInfo tabInfo =
            (ControlInfo) control.getTmpProperty(PROP_CONTROLINFO);
        if (tabInfo == null) {
            return;
        }
        tabInfo.expand();
    }





    /**
     * Reinserts the control into the control tabs
     *
     * @param control the control
     */
    public void dockControl(DisplayControl control) {
        control.setShowInTabs(true);
        ((DisplayControlImpl) control).guiImported();
        addControlTab(control, true);
    }

    /**
     * Remove the control from the tabs
     *
     * @param control The control
     */
    public void undockControl(final DisplayControl control) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                removeControlTab(control, false);
                control.setShowInTabs(false);
                ((DisplayControlImpl) control).setMakeWindow(true);
                ((DisplayControlImpl) control).popup(null);
            }
        });
    }

    /**
     * Handle a control moved
     *
     * @param control  the moved control
     */
    public void controlMoved(DisplayControl control) {
        removeControlTab(control);
        addControlTab(control, true);
    }

    /** _more_ */
    private static final Object BUTTONSTATE_MUTEX = new Object();

    /**
     * Initialize the button state
     *
     * @param idv the idv
     */
    protected static void initButtonState(IntegratedDataViewer idv) {
        synchronized (BUTTONSTATE_MUTEX) {
            if (BUTTON_FG_COLOR == null) {
                BUTTON_SHOWPOPUP =
                    idv.getProperty("idv.ui.viewpanel.showpopup", false);
                BUTTON_SHOWCATEGORIES =
                    idv.getProperty("idv.ui.viewpanel.showcategories", false);

                BUTTON_BORDER = BorderFactory.createEmptyBorder(2, 6, 2, 0);
                BUTTON_FONT       = new Font("Dialog", Font.PLAIN, 11);
                CATEGORY_FONT     = new Font("Dialog", Font.BOLD, 11);
                BUTTON_LINE_COLOR = Color.gray;
                CATEGORY_OPEN_ICON = GuiUtils.getImageIcon(
                    "/auxdata/ui/icons/CategoryOpen.gif");
                CATEGORY_CLOSED_ICON = GuiUtils.getImageIcon(
                    "/auxdata/ui/icons/CategoryClosed.gif");
                BUTTON_FG_COLOR = Color.black;
            }
        }
    }


    /**
     * Class VMInfo Holds gui stuff for a viewmanager
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.33 $
     */
    public class VMInfo implements ImageObserver {


        /** The identifying object. might be a string or a vm */
        Object obj;

        /** The view manager */
        ViewManager viewManager;

        /** popup the vm menu */
        JButton popupButton;


        /** Whats in the tab */
        JComponent tabContents;

        /** The header */
        JPanel headerPanel;


        /** My control infos */
        List controlInfos = new ArrayList();

        /** Should we ignore button changes */
        boolean ignore = false;

        /** The toggle buttons */
        List buttons = new ArrayList();

        /** Holds the buttons */
        JComponent buttonPanel;

        /** The contents */
        JComponent contents;

        /** The label */
        JLabel viewLabel;


        /** toggle open.close the sub buttons */
        JButton categoryToggleBtn;

        /** are buttons shown */
        boolean catOpen = true;

        /** for up/down arrow listening */
        private KeyListener keyListener;

        /**
         * ctor
         *
         * @param obj Either a ViewManager or a string label
         */
        public VMInfo(Object obj) {

            keyListener = new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        selectNext(true);
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        selectNext(false);
                    }
                }
            };

            //Initialize stuff
            initButtonState(getIdv());
            BUTTON_ICON.getWidth(this);


            this.obj         = obj;
            this.tabContents = new JPanel(new BorderLayout());
            if (obj instanceof ViewManager) {
                this.viewManager = (ViewManager) obj;
            }
            ImageIcon icon = ICON_DEFAULT;
            if (obj instanceof MapViewManager) {
                if (((MapViewManager) obj).getUseGlobeDisplay()) {
                    icon = ICON_GLOBE;
                } else {
                    icon = ICON_MAP;
                }
            } else if (obj instanceof TransectViewManager) {
                icon = ICON_TRANSECT;
            }
            viewLabel = new JLabel(" " + getLabel());
            viewLabel.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent evt) {
                    if (viewManager != null) {
                        getVMManager().setLastActiveViewManager(viewManager);
                        /*
                        final JColorChooser cc= new JColorChooser(headerPanel.getBackground());
                        GuiUtils.showOkCancelDialog(null,"",cc,null);
                        headerPanel.setBackground(cc.getColor());
                        ViewManager.highlightColor = cc.getColor();

                        ViewManager.highlightOuterBorder = new MatteBorder(new Insets(bw,
                                                bw, bw,
                                                bw), cc.getColor());
                        ViewManager.highlightBorder =
                            BorderFactory.createCompoundBorder(ViewManager.highlightOuterBorder, ViewManager.lineBorder);
                        */
                        if (evt.getClickCount() == 2) {
                            viewManager.toFront();
                        }
                    }
                }
            });
            categoryToggleBtn = GuiUtils.getImageButton(getCatOpen()
                    ? CATEGORY_OPEN_ICON
                    : CATEGORY_CLOSED_ICON);
            categoryToggleBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setCatOpen( !getCatOpen());
                }
            });

            categoryToggleBtn.addKeyListener(keyListener);
            popupButton = GuiUtils.makeImageButton(
                "/auxdata/ui/icons/Information16.gif", VMInfo.this,
                "showPopupMenu");


            popupButton = new JButton(icon);
            popupButton.addKeyListener(keyListener);
            popupButton.setContentAreaFilled(false);
            popupButton.addActionListener(
                GuiUtils.makeActionListener(
                    VMInfo.this, "showPopupMenu", null));
            popupButton.setToolTipText("Show View Menu");
            popupButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
                    0));

            headerPanel = GuiUtils.leftCenter(
                GuiUtils.hbox(
                    GuiUtils.inset(categoryToggleBtn, 1),
                    popupButton), viewLabel);
            if (viewManager != null) {
                headerPanel = viewManager.makeDropPanel(headerPanel, true);
            }
            JComponent headerWrapper = GuiUtils.center(headerPanel);
            headerPanel.setBorder(headerPanelBorder);
            contents = GuiUtils.topCenter(headerWrapper, tabContents);
            viewContainer.add(contents);
            popupButton.setHorizontalAlignment(SwingConstants.LEFT);
            buttonsChanged();
            setCatOpen(getCatOpen());
            if (viewManager != null) {
                viewManagerChanged();
            }

        }

        /**
         * are buttons shown.
         *
         * @return buttons shown
         */
        private boolean getCatOpen() {
            if (viewManager != null) {
                Boolean b = (Boolean) viewManager.getProperty(
                                "viewpanel.catgegory.open");
                if (b != null) {
                    return b.booleanValue();
                }
            }
            return catOpen;
        }


        /**
         * set buttons shown
         *
         * @param v buttons shown
         */
        private void setCatOpen(boolean v) {
            if (viewManager != null) {
                viewManager.putProperty("viewpanel.catgegory.open",
                                        new Boolean(v));
            }
            catOpen = v;
            categoryToggleBtn.setIcon(v
                                      ? CATEGORY_OPEN_ICON
                                      : CATEGORY_CLOSED_ICON);
            tabContents.setVisible(v);
        }



        /**
         * SHow the ViewManager popup menu
         */
        public void showPopupMenu() {
            List menuItems = new ArrayList();
            if (viewManager == null) {
                return;
            }
            viewManager.addContextMenuItems(menuItems);
            JPopupMenu popup = GuiUtils.makePopupMenu(menuItems);
            popup.show(popupButton, 0,
                       (int) popupButton.getSize().getHeight());
        }

        /**
         * Handle update
         *
         * @param img param
         * @param flags param
         * @param x param
         * @param y param
         * @param width param
         * @param height param
         *
         * @return continue
         */
        public boolean imageUpdate(Image img, int flags, int x, int y,
                                   int width, int height) {
            if ((flags & ImageObserver.ALLBITS) != 0) {
                leftPanel.repaint();
                return false;
            }
            return true;
        }



        /**
         * Do we hold the given object
         *
         * @param obj The object
         *
         * @return Is it my object
         */
        public boolean holds(Object obj) {
            return this.obj == obj;
        }

        /**
         * Remove the control
         *
         * @param controlInfo the contorl info to remove
         */
        public void removeControlInfo(ControlInfo controlInfo) {
            int index = controlInfos.indexOf(controlInfo);
            if (index >= 0) {
                int btnIndex = buttons.indexOf(controlInfo.button);
                buttons.remove(controlInfo.button);
                controlInfos.remove(controlInfo);
                rightPanel.remove(controlInfo.outer);
                if (controlInfo.button.isSelected() && (buttons.size() > 0)) {
                    while ((btnIndex >= buttons.size()) && (btnIndex >= 0)) {
                        btnIndex--;
                    }
                    if (btnIndex >= 0) {
                        ((JToggleButton) buttons.get(btnIndex)).doClick();
                    }
                }
                GuiUtils.toggleHeavyWeightComponents(controlInfo.outer, true);
                buttonsChanged();
                ignore = true;
                buttonGroup.remove(controlInfo.button);
                ignore = false;
                if (controlInfos.size() == 0) {
                    List localVMInfos = new ArrayList(vmInfos);
                    for (int vmIdx = 0; vmIdx < localVMInfos.size();
                            vmIdx++) {
                        VMInfo vmInfo = (VMInfo) localVMInfos.get(vmIdx);
                        if (vmInfo.controlInfos.size() > 0) {
                            ControlInfo ci =
                                (ControlInfo) vmInfo.controlInfos.get(0);
                            ci.button.doClick();
                        }
                    }
                }
            }
        }


        /**
         * The control info changed
         *
         * @param controlInfo the control info_
         */
        public void changeControlInfo(ControlInfo controlInfo) {
            if ( !Misc.equals(controlInfo.lastCategory,
                              controlInfo.control.getDisplayCategory())) {
                buttonsChanged();
            } else {}
        }

        /**
         * Paint the button
         *
         * @param g graphics
         * @param controlInfo the control info_
         */
        protected void paintButton(Graphics g, ControlInfo controlInfo) {
            int     idx    = controlInfos.indexOf(controlInfo);
            boolean isLast = idx == controlInfos.size() - 1;

            g.setFont(BUTTON_FONT);
            FontMetrics   fm          = g.getFontMetrics(g.getFont());

            JToggleButton btn         = controlInfo.button;
            Rectangle     b           = btn.getBounds();
            String        text        = controlInfo.getLabel();
            int           y = (btn.getHeight() + fm.getHeight()) / 2 - 2;
            int           buttonWidth = BUTTON_ICON.getWidth(null);
            int           offset      = 2 + buttonWidth + 4;
            g.setColor(btn.getBackground());
            g.fillRect(0, 0, b.width, b.height);
            if (btn.isSelected()) {
                if (BUTTON_ON_COLOR == null) {
                    Color c = btn.getBackground();
                    //Just go a little bit darker than the normal background
                    BUTTON_ON_COLOR = new Color((int) Math.max(0,
                            c.getRed() - 20), (int) Math.max(0,
                                c.getGreen() - 20), (int) Math.max(0,
                                    c.getBlue() - 20));
                }

                g.setColor(BUTTON_ON_COLOR);
                g.fillRect(offset - 1, 0, b.width, b.height);
            }
            g.setColor(BUTTON_LINE_COLOR);


            if ( !isLast || true) {
                g.drawLine(offset - 1, b.height - 1, b.width, b.height - 1);
            }

            g.setColor(BUTTON_FG_COLOR);
            int rightSide = b.width;
            if (btn.isSelected()) {
                rightSide = b.width - buttonWidth - 2;
            }

            int textPos   = offset;
            int textRight = textPos + fm.stringWidth(text);
            if (textRight >= rightSide) {
                while ((text.length() > 5) && (textRight >= rightSide)) {
                    text      = text.substring(0, text.length() - 2);
                    textRight = textPos + fm.stringWidth(text + ".");
                }
                text = text + ".";
            }
            g.drawString(text, offset, y);
            //      g.drawLine(offset, y, offset+width, y);
            if (btn.isSelected()) {
                int imageHeight = BUTTON_ICON.getHeight(null);
                //                g.drawImage(BUTTON_ICON, 2, b.height / 2 - imageHeight / 2,
                //                            null);
                g.drawImage(BUTTON_ICON, b.width - 2 - buttonWidth,
                            b.height / 2 - imageHeight / 2, null);
            }
        }



        /**
         * new control
         *
         * @param controlInfo the control info
         */
        public void addControlInfo(final ControlInfo controlInfo) {
            JToggleButton btn = controlInfo.button =
                                    new JToggleButton(StringUtil.padRight("",
                                        20), true) {
                public void paint(Graphics g) {
                    paintButton(g, controlInfo);
                }
            };
            btn.addKeyListener(keyListener);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(controlInfo.getLabel());
            btn.setFont(BUTTON_FONT);
            btn.setForeground(BUTTON_FG_COLOR);
            btn.setBorder(BUTTON_BORDER);
            controlInfo.button.setHorizontalAlignment(SwingConstants.LEFT);
            controlInfo.button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ce) {
                    if ( !ignore) {
                        GuiUtils.toggleHeavyWeightComponents(
                            controlInfo.outer, true);
                        rightPanel.show(controlInfo.outer);
                    }
                }
            });
            buttons.add(controlInfo.button);
            controlInfos.add(controlInfo);
            rightPanel.addCard(controlInfo.outer);
            GuiUtils.toggleHeavyWeightComponents(controlInfo.outer, false);
            controlInfo.displayControlChanged();
            if (controlInfo.control.getExpandedInTabs()) {
                controlInfo.expand();
            }
            buttonGroup.add(controlInfo.button);
            buttonsChanged();
            setCatOpen(getCatOpen());
        }

        /**
         * Holds the list of all of the String categories  that we have shown. We keep
         *   this around so the order the categories are shown over time stays the same
         */
        private List categories = new ArrayList();

        /**
         * Redo the buttons
         */
        private void buttonsChanged() {
            List      comps  = new ArrayList();
            Hashtable catMap = new Hashtable();
            int       catCnt = 0;
            for (int i = 0; i < controlInfos.size(); i++) {
                ControlInfo controlInfo = (ControlInfo) controlInfos.get(i);
                String      cat = controlInfo.control.getDisplayCategory();
                if (cat == null) {
                    cat = "Displays";
                }
                controlInfo.lastCategory = cat;
                if ( !BUTTON_SHOWCATEGORIES) {
                    comps.add(controlInfo.button);
                    continue;
                }


                List catList = (List) catMap.get(cat);
                if (catList == null) {
                    if ( !categories.contains(cat)) {
                        categories.add(cat);
                    }
                    catList = new ArrayList();
                    catMap.put(cat, catList);
                    JLabel catLabel = new JLabel(" " + cat);
                    catLabel.setFont(CATEGORY_FONT);
                    if (++catCnt > 1) {
                        //                        catLabel.setBorder(BorderFactory.createMatteBorder(1,
                        //                                0, 0, 0, Color.gray));
                    }
                    catList.add(catLabel);
                }
                catList.add(controlInfo.button);
            }
            if (BUTTON_SHOWCATEGORIES) {
                for (int i = 0; i < categories.size(); i++) {
                    List catList = (List) catMap.get(categories.get(i));
                    if (catList == null) {
                        continue;
                    }
                    comps.addAll(catList);
                }
            }
            if (comps.size() == 0) {
                JLabel noLbl = new JLabel("No Displays");
                noLbl.setFont(BUTTON_FONT);
                JPanel inset = GuiUtils.inset(noLbl, new Insets(0, 10, 0, 0));
                inset.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                        Color.gray));
                comps.add(inset);
            }
            comps.add(GuiUtils.filler(10, 2));
            JComponent buttonPanel = GuiUtils.vbox(comps);
            tabContents.removeAll();
            tabContents.add(BorderLayout.NORTH, buttonPanel);
            tabContents.repaint();
        }


        /**
         * my viewmanager had been removed
         */
        public void viewManagerDestroyed() {
            viewContainer.remove(contents);
        }


        /**
         * my viewmanager has changed. Update the gui.
         */
        public void viewManagerChanged() {
            viewLabel.setText(getLabel());
            if (viewManager.showHighlight()) {

                headerPanelHighlightBorder =
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(3, 0, 0, 0),
                        BorderFactory.createMatteBorder(
                            0, 0, 2, 0,
                            getStore().get(
                                ViewManager.PREF_BORDERCOLOR, Color.blue)));

                headerPanel.setBorder(headerPanelHighlightBorder);
            } else {
                headerPanel.setBorder(headerPanelBorder);
            }
        }

        /**
         * Get the viewmanager label
         *
         * @return label
         */
        private String getLabel() {
            if (viewManager != null) {
                String name = viewManager.getName();
                if ((name == null) || (name.trim().length() == 0)) {
                    List localVMInfos = new ArrayList(vmInfos);
                    int  idx          = localVMInfos.indexOf(this);
                    if (idx == -1) {
                        idx = localVMInfos.size();
                    }
                    name = "View " + (idx + 1);
                }
                return name;
            }
            return obj.toString();
        }


    }


    /**
     * Class ControlInfo holds info about the components in the control tabs
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.33 $
     */
    private static class ControlInfo {

        /** The control */
        DisplayControl control;

        /** expand/contract button */
        JButton expandButton;

        /** Outermost gui. This is what is added to the tabs */
        JComponent outer;

        /** The component from the display control */
        JComponent inner;

        /** Are we expanded */
        boolean expanded = false;

        /** Keep track of past non-expanded sizes */
        Dimension innerSize;

        /** my vminfo */
        VMInfo vmInfo;

        /** button */
        JToggleButton button;

        /** The last category shown */
        String lastCategory = "";

        /** my label */
        String label = null;


        /**
         * ctor
         *
         * @param control control
         * @param expandButton expand button
         * @param outer outer comp
         * @param inner inner comp
         * @param vmInfo my vminfo
         */
        public ControlInfo(DisplayControl control, JButton expandButton,
                           JComponent outer, JComponent inner,
                           VMInfo vmInfo) {
            this.vmInfo  = vmInfo;
            this.control = control;
            if (control.getExpandedInTabs()) {
                expanded = false;
            }
            this.expandButton = expandButton;
            this.outer        = outer;
            this.inner        = inner;
            innerSize         = inner.getSize();
            vmInfo.addControlInfo(this);
        }

        /**
         * get the label for the display control
         *
         * @return display control label
         */
        public String getLabel() {
            if (label == null) {
                label = control.getMenuLabel();
            }
            return label;
        }

        /**
         * display control changed
         */
        public void displayControlChanged() {
            String tmp = label;
            label = null;
            getLabel();
            if ( !Misc.equals(tmp, label) && (button != null)) {
                button.setToolTipText(label);
                button.repaint();
            }
            vmInfo.changeControlInfo(this);
        }

        /**
         * display control is removed
         */
        public void removeDisplayControl() {
            vmInfo.removeControlInfo(this);
        }


        /**
         * Expand the contents
         */
        public void expand() {
            outer.removeAll();
            outer.setLayout(new BorderLayout());
            if ( !expanded) {
                outer.add(BorderLayout.CENTER, inner);
                expandButton.setIcon(
                    GuiUtils.getImageIcon("/auxdata/ui/icons/UpUp.gif"));
                innerSize = inner.getSize();
            } else {
                outer.add(BorderLayout.NORTH, inner);
                expandButton.setIcon(
                    GuiUtils.getImageIcon("/auxdata/ui/icons/DownDown.gif"));
                inner.setSize(innerSize);
            }
            expanded = !expanded;
            control.setExpandedInTabs(expanded);
            //        outer.setSize(outer.getSize());
            final Container parent = outer.getParent();
            final Component comp   = outer;
            outer.invalidate();
            parent.validate();
            parent.doLayout();
        }




    }


}

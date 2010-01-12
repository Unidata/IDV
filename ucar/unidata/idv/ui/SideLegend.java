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


import ucar.unidata.idv.ui.*;

import ucar.unidata.ui.DragPanel;
import ucar.unidata.ui.DropPanel;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;




/**
 * This class provides the side legend for display controls in view managers.
 * It organizes the set of display controls hierarhcically, according to
 * their display category. It provides facilities to show/hide, in the legend,
 * both categories  of display  controls as well as the GUI details  of individual
 * display controls.
 *
 * @author IDV development team
 */

public class SideLegend extends IdvLegend {



    /** This holds the display control legends */
    private JPanel legendsPanel;

    /**
     * Mapping of display category to the CategoryPanel display controls
     * of that category are shown in.
     */
    private Hashtable categoryToPanel = new Hashtable();

    /** Maps category name to whether it is open or not */
    private Hashtable categoryToPanelOpen = new Hashtable();

    /** List of all of the CategoryPanels that show the catergories */
    private List categoryPanels = new ArrayList();



    /**
     * Parameterless constructor for xml encoding.
     */
    public SideLegend() {}


    /**
     * Create me with the given ViewManager
     *
     * @param viewManager The view manager I am part of
     *
     */
    public SideLegend(ViewManager viewManager) {
        super(viewManager);
    }




    /**
     * Override the base class method to create the GUI contents.
     *
     * @return The GUI contents
     */
    protected JComponent doMakeContents() {
        legendsPanel = new JPanel();
        legendsPanel.setLayout(new BorderLayout());
        JLabel displaysLbl = GuiUtils.cLabel("Legend");
        Font   font        = displaysLbl.getFont();
        displaysLbl.setFont(font.deriveFont(font.getSize()
                                            + 3.0f).deriveFont(Font.BOLD));

        JComponent floatComp = getFloatButton();
        JComponent outerPanel =
            GuiUtils.topCenter(GuiUtils.centerRight(displaysLbl, floatComp),
                               legendsPanel);
        JScrollPane scroller =
            new JScrollPane(
                outerPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(250, 500));
        return scroller;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getDisplayCategories() {
        List<String> cats = new ArrayList();
        for (int i = 0; i < categoryPanels.size(); i++) {
            CategoryPanel categoryPanel =
                (CategoryPanel) categoryPanels.get(i);
            cats.add(categoryPanel.category);
        }
        return cats;
    }


    /**
     * Apply the category visibility state from the given that legend
     * to this legend
     *
     * @param that The that legend
     */
    public void initWith(SideLegend that) {
        categoryToPanelOpen = new Hashtable();
        Hashtable thatMap = that.categoryToPanelOpen;
        if (thatMap != null) {
            for (int i = 0; i < categoryPanels.size(); i++) {
                CategoryPanel categoryPanel =
                    (CategoryPanel) categoryPanels.get(i);
                Boolean b = (Boolean) thatMap.get(categoryPanel.category);
                if (b != null) {
                    categoryPanel.setInnerVisible(b.booleanValue());
                }
            }
            categoryToPanelOpen.putAll(thatMap);
        }
    }


    /**
     * Override the case class method to refill the legend. This gets called
     * from within a synchronized block when the display controls we are showing
     * have changed in some way.
     */
    protected void fillLegendSafely() {
        ViewManager theViewManager = viewManager;
        if ((legendsPanel == null) || (theViewManager == null)) {
            return;
        }
        List controls = theViewManager.getControlsForLegend();
        for (int i = 0; i < categoryPanels.size(); i++) {
            CategoryPanel categoryPanel =
                (CategoryPanel) categoryPanels.get(i);
            //            categoryPanel.clear();
            categoryPanel.reInitialize();
            categoryPanel.getContents().setVisible(true);
        }


        boolean showIcons = true;
        if (theViewManager != null) {
            IntegratedDataViewer idv = theViewManager.getIdv();
            if (idv != null) {
                showIcons = idv.getStateManager().getPreferenceOrProperty(
                    IdvConstants.PREF_LEGEND_SHOWICONS, false);
            }
        }
        Hashtable seen = new Hashtable();
        for (int i = controls.size() - 1; i >= 0; i--) {
            final DisplayControl control  = (DisplayControl) controls.get(i);
            String               category = control.getDisplayCategory();
            if ((category == null) || (category.length() == 0)) {
                category = "Displays";
            }
            CategoryPanel categoryPanel =
                (CategoryPanel) categoryToPanel.get(category);
            if (categoryPanel == null) {
                categoryPanel = new CategoryPanel(this, category);
                categoryPanels.add(categoryPanel);
                categoryToPanel.put(category, categoryPanel);
                Boolean b = (Boolean) categoryToPanelOpen.get(category);
                if (b != null) {
                    categoryPanel.setInnerVisible(b.booleanValue());
                }
            }
            seen.put(categoryPanel, categoryPanel);
            DisplayControlLegendPanel legendPanel =
                (DisplayControlLegendPanel) control.getTransientProperty(
                    "SIDELEGEND");
            if (legendPanel == null) {
                JCheckBox    visCbx = control.doMakeVisibilityControl("");
                ItemListener itemListener = new ItemListener() {
                    public void itemStateChanged(ItemEvent event) {
                        displayControlVisibilityChanged(control,
                                event.getStateChange() == ItemEvent.SELECTED);
                    }
                };
                visCbx.addItemListener(itemListener);
                visCbx.setBorder(BorderFactory.createEmptyBorder());
                JComponent sideLegendLabel =
                    control.getLegendLabel(control.SIDE_LEGEND);
                sideLegendLabel.setBorder(BorderFactory.createEmptyBorder(0,
                        5, 0, 3));
                JComponent buttons =
                    control.getLegendButtons(control.SIDE_LEGEND);
                legendPanel = new DisplayControlLegendPanel(control,
                        sideLegendLabel, false,
                        GuiUtils.inset(visCbx, new Insets(0, 0, 0, 2)),
                        buttons);

                JComponent controlLegend =
                    control.getLegendComponent(control.SIDE_LEGEND);
                legendPanel.add(controlLegend, false);
                control.putTransientProperty("SIDELEGEND", legendPanel);
            }
            legendPanel.getExtraRight().setVisible(showIcons);
            categoryPanel.add(control, legendPanel.getContents());
        }

        List orderedCategoryPanels = new ArrayList();
        for (int i = 0; i < categoryPanels.size(); i++) {
            CategoryPanel categoryPanel =
                (CategoryPanel) categoryPanels.get(i);
            if (seen.get(categoryPanel) != null) {
                orderedCategoryPanels.add(categoryPanel.getContents());
            } else {
                categoryPanel.clear();
            }
        }


        JPanel panels = GuiUtils.vbox(orderedCategoryPanels);
        //        synchronized (legendsPanel.getTreeLock()) {
        legendsPanel.removeAll();
        legendsPanel.add(panels, BorderLayout.NORTH);
        legendsPanel.invalidate();
        //        }
    }



    /**
     * Look for the display category that contains the given control
     * and tell it its control visibility has changed.
     *
     * @param control The control
     * @param selected Is visibility on or off
     */
    private void displayControlVisibilityChanged(
            final DisplayControl control, final boolean selected) {
        for (CategoryPanel panel : (List<CategoryPanel>) categoryPanels) {
            if (panel.containsDisplayControl(control)) {
                control.setDisplayVisibility(selected);
                panel.controlVisibilityChanged(selected);
                break;
            }
        }
    }





    /**
     * Set the CategoryToPanel property.
     *  @param value The new value for CategoryToPanel
     */
    public void setCategoryToPanelOpen(Hashtable value) {
        categoryToPanelOpen = value;
    }

    /**
     *  Get the CategoryToPanel property.
     *  @return The CategoryToPanel
     */
    public Hashtable getCategoryToPanelOpen() {
        categoryToPanelOpen = new Hashtable();
        for (int i = 0; i < categoryPanels.size(); i++) {
            CategoryPanel categoryPanel =
                (CategoryPanel) categoryPanels.get(i);
            categoryToPanelOpen.put(
                categoryPanel.category,
                new Boolean(categoryPanel.getInnerVisible()));
        }

        return categoryToPanelOpen;
    }



    /**
     * Class DisplayControlLegendPanel is used to hold a display control.
     *
     * @author IDV development team
     */
    private static class DisplayControlLegendPanel extends LegendPanel {

        /** control associated with this legend */
        DisplayControl control;

        /**
         * Create a new DisplayControlLegendPanel
         *
         * @param control     control
         * @param topLabel    top label
         * @param makeBorder  true to make a border
         * @param extraLeft   extra left component
         * @param extraRight  extra right component
         */
        public DisplayControlLegendPanel(DisplayControl control,
                                         JComponent topLabel,
                                         boolean makeBorder,
                                         JComponent extraLeft,
                                         JComponent extraRight) {

            super(topLabel, makeBorder, extraLeft, extraRight);
            this.control = control;
            setInnerVisible( !control.getCollapseLegend());
        }


        /**
         * old code
         */
        protected void doMakeContents() {
            super.doMakeContents();
            contents = new DragPanel(control, contents);
            //            contents  = GuiUtils.inset(contents, new Insets(0, 6, 0, 0));
        }


        /**
         * Show or hide the inner panel. Change the icon in the toggle button
         * accordingly.
         *
         * @param b Is the inner component visible or not
         */
        protected void setInnerVisible(boolean b) {
            super.setInnerVisible(b);
            if (control != null) {
                control.setCollapseLegend( !b);
            }
        }

    }


    /**
     * Class CategoryPanel is used to hold  the set of display control
     * legends for a particular display category.
     *
     *
     * @author IDV development team
     */
    public static class CategoryPanel extends LegendPanel {

        /** The legend we are part of */
        private SideLegend legend;

        /** The display category we represent */
        String category;

        /**
         * The list of DisplayControls currently displayed by this panel.
         * i.e., These are the ones that have the display category.
         */
        private List displayControls = new ArrayList();


        /** The visibility checkbox */
        private JCheckBox visCbx;

        /** Keep from looping */
        private boolean ignoreVisChanges = false;


        /**
         *  Maps the control's uniqueid to what their visibility was
         *  before it had been set by this panels group visiblity
         *  mechanism.
         */
        Hashtable previousVisibilities = new Hashtable();

        /**
         * Create me with the given legend and category
         *
         * @param legend The legend we are part of
         * @param theCategory Our display category
         */
        public CategoryPanel(SideLegend legend, String theCategory) {
            this.legend   = legend;
            this.category = theCategory;
            visCbx        = new JCheckBox("", true);
            visCbx.setBorder(BorderFactory.createEmptyBorder());
            visCbx.addActionListener(new ObjectListener(null) {
                public void actionPerformed(ActionEvent event) {
                    if (ignoreVisChanges) {
                        return;
                    }
                    ignoreVisChanges = true;
                    setDisplayVisiblity(visCbx.isSelected());
                    ignoreVisChanges = false;
                }
            });
            setComponents(new JLabel("  " + category), visCbx, null);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean makeDropPanel() {
            return true;
        }

        /**
         * _more_
         *
         * @param object _more_
         *
         * @return _more_
         */
        public boolean dropOk(Object object) {
            return object instanceof DisplayControl;
        }

        /**
         * _more_
         *
         * @param object _more_
         */
        public void doDrop(Object object) {
            ((DisplayControl) object).setDisplayCategory(category);
            legend.viewManager.displayControlChanged((DisplayControl) object);
        }


        /**
         * Handle when the visibility of a display control has been changed
         *
         * @param toWhat What the visibility was changed to
         */
        //        public void controlVisibilityChanged(boolean toWhat) {
        //            if (ignoreVisChanges) {
        //              System.err.println("debug: ignoring for some reason");
        //                return;
        //            }
        //            ignoreVisChanges = toWhat;
        //            boolean anyOn = false;
        //            for (int i = displayControls.size() - 1; i >= 0; i--) {
        //                DisplayControl control =
        //                    (DisplayControl) displayControls.get(i);
        //                anyOn = control.getDisplayVisibility();
        //                
        //                System.err.println("control=" + control.getMenuLabel() + " vis=" + control.getDisplayVisibility() + " anyOn=" + anyOn + " toWhat=" + toWhat);
        //            }
        //            //            System.err.println ("anyOn:" + anyOn + " toWhat:" + toWhat);
        //            //Don't do this now.
        //                        //visCbx.setSelected(anyOn);
        //            ignoreVisChanges = false;
        //        }
        public void controlVisibilityChanged(final boolean toWhat) {
            boolean anyOn = false;
            for (DisplayControl dc : (List<DisplayControl>) displayControls) {
                if (dc.getDisplayVisibility()) {
                    anyOn = true;
                    break;
                }
            }
            visCbx.setSelected(anyOn);
        }



        /**
         * Add the given display control and its GUI component to this panel
         *
         * @param control The display control to add
         * @param comp Its GUI component
         */
        public void add(DisplayControl control, JComponent comp) {
            displayControls.add(control);
            super.add(comp);
        }

        /**
         * Does this category panel contain the given control
         *
         * @param control The control
         *
         * @return Contains the control
         */
        public boolean containsDisplayControl(DisplayControl control) {
            return displayControls.contains(control);
        }


        /**
         * Clear out the current state
         */
        public void reInitialize() {
            super.reInitialize();
            displayControls = new ArrayList();
        }

        /**
         * clear out contents
         */
        public void clear() {
            //            super.clear();
            displayControls = new ArrayList();
        }


        /**
         * This turns on/off the visibility of all of the display controls show
         * in this panel.
         *
         * @param on Visibility state
         */
        private void setDisplayVisiblity(boolean on) {
            Boolean dflt = (on
                            ? Boolean.TRUE
                            : Boolean.FALSE);
            for (int i = displayControls.size() - 1; i >= 0; i--) {
                DisplayControl control =
                    (DisplayControl) displayControls.get(i);
                //Preserve the prior visibility
                Boolean lastVisibility =
                    (Boolean) previousVisibilities.get(control.getUniqueId());
                if (lastVisibility == null) {
                    lastVisibility = dflt;
                }
                control.setDisplayVisibility(on);
                //Don't be too tricky here for now
                if (true) {
                    continue;
                }

                if (on) {
                    control.setDisplayVisibility(
                        lastVisibility.booleanValue());
                } else {
                    previousVisibilities.put(
                        control.getUniqueId(),
                        new Boolean(control.getDisplayVisibility()));
                    control.setDisplayVisibility(false);
                }
            }
        }
    }




}

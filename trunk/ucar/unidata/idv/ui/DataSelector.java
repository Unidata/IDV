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


import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceFactory;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DescriptorDataSource;

import ucar.unidata.idv.*;

import ucar.unidata.ui.ButtonTabbedPane;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;




/**
 *
 * This class provides  facilities for managing
 * a collection of {@link ucar.unidata.data.DataSource},
 * {@link ucar.unidata.data.DataCategory} and
 * {@link ucar.unidata.data.DataChoice}
 * in a selection GUI.
 * <p>
 * It holds a list of DataSources. For each DataSource
 * there is a {@link DataTree} that shows the
 * DataChoices of the DataSource. There is a list of
 * {@link ucar.unidata.idv.ControlDescriptor}-s
 * that are shown in the {@link DataControlDialog}
 *
 * @author IDV development team
 * @version $Revision: 1.87 $Date: 2007/08/10 13:38:11 $
 */


public class DataSelector extends DataSourceHolder {

    /** Icon for text search field */
    private static ImageIcon cancelIcon;

    /** Icon for text search field */
    private static ImageIcon searchIcon;


    /** Color for failed search */
    public static final Color COLOR_BADSEARCH = new Color(255, 102, 102);

    /** Width dimension to use for hor list sizes */
    private static final int DIM_H_X = 200;

    /** Height dimension to use for hor list sizes */
    private static final int DIM_H_Y = 100;

    /** Layout display/times list horizontally */
    private boolean horizontalOrientation = false;


    /** The size of this DataSelector (defaults to 200x300) */
    Dimension defaultDimension;

    /** The main GUI contents */
    JComponent contents;


    /** Does this dialog just show one data source */
    private boolean singletonDataSource = false;



    /** The main contents */
    private ButtonTabbedPane tabbedPane;


    /** The list of data source wrappers */
    private List wrappers = new ArrayList();





    /**
     * Create a DataSelector with the given idv and lsit of formulas
     *
     * @param idv The IDV
     * @param formulaDataSource Contains the end-user formulas
     *
     */
    public DataSelector(IntegratedDataViewer idv,
                        DataSource formulaDataSource) {
        this(idv, true, formulaDataSource);
    }



    /**
     * Create a DataSelector with the given idv and lsit of formulas
     *
     * @param idv The IDV
     * @param horizontalOrientation Should this be configured with normal dimensions
     * @param formulaDataSource Contains the end-user formulas
     *
     */

    public DataSelector(IntegratedDataViewer idv,
                        boolean horizontalOrientation,
                        DataSource formulaDataSource) {
        super(idv, formulaDataSource, (Dimension) null);
        this.horizontalOrientation = horizontalOrientation;
        init(null);
        if (formulaDataSource != null) {
            addDataSource(formulaDataSource);
        }
    }


    /**
     * Create a DataSelector with the given idv and list of formulas
     *
     * @param idv The IDV
     * @param defaultSize Size of the window
     * @param singletonDataSource  Does this dialog just show one data source
     */
    public DataSelector(IntegratedDataViewer idv, Dimension defaultSize,
                        boolean singletonDataSource) {
        super(idv, null, defaultSize);
        this.singletonDataSource = singletonDataSource;
        init(null);
    }



    /**
     * Initialize the GUI with the given list of
     * {@link ucar.unidata.data.DataSource}-s
     *
     * @param sources List of data sources
     */
    private void init(List sources) {
        tabbedPane =
            new ButtonTabbedPane(GuiUtils.inset(new JLabel("Data Sources:"),
                new Insets(5, 5, 0, 0)), 175);
        //        tabbedPane.setDeleteEnabled(true);
        contents = tabbedPane;

        if (sources != null) {
            for (int i = 0; i < sources.size(); i++) {
                addDataSource((DataSource) sources.get(i));
            }
        }

    }


    /**
     * Get the currently displayed component
     *
     * @return currently displayed component
     */
    private Component getCurrentComponent() {
        List wrappers = getWrappers();
        for (int i = 0; i < wrappers.size(); i++) {
            DataSourceWrapper wrapper = (DataSourceWrapper) wrappers.get(i);
            if (wrapper.button.isSelected()) {
                return wrapper.contents;
            }
        }
        return null;
    }


    /**
     * Find the currently displayed data source
     *
     * @return currently displayed data source
     */
    private DataSource findCurrentDataSource() {
        Component comp = getCurrentComponent();
        if (comp == null) {
            return null;
        }
        List wrappers = getWrappers();
        for (int i = 0; i < wrappers.size(); i++) {
            DataSourceWrapper wrapper = (DataSourceWrapper) wrappers.get(i);
            if (wrapper.contents == comp) {
                return wrapper.dataSource;
            }
        }
        return null;
    }




    /**
     * Set the data source shown by this selector
     *
     * @param dataSource The data source
     */
    public void setDataSource(DataSource dataSource) {
        if (singletonDataSource) {
            removeAllDataSources();
            addDataSource(dataSource);
        } else {
            addDataSource(dataSource);
        }
    }


    /**
     * Helper method to make a JLabel inset in a panel 4 pixels
     *
     * @param s The label string
     * @return Panel containing the label
     */
    private static JComponent makeLabel(String s) {
        return GuiUtils.inset(new JLabel(s), 4);
    }



    /**
     * Show the menu for the data source
     *
     * @param event Where the mouse was clicked
     * @param where On what was  the mouse clicked
     * @param dataSource The data source
     */
    private void showDataSourceMenu(MouseEvent event, JComponent where,
                                    DataSource dataSource) {
        if ( !SwingUtilities.isRightMouseButton(event)) {
            return;
        }
        List items =
            getIdv().getIdvUIManager().doMakeDataSourceMenuItems(dataSource,
                where);
        if ((items != null) && (items.size() > 0)) {
            JPopupMenu menu = GuiUtils.makePopupMenu(items);
            menu.show(where, event.getX(), event.getY());
        }

    }



    /**
     * Find and return the {@link ucar.unidata.data.DataSource}
     * in the List of DataSource-s at the given
     * index
     *
     * @param listIndex The index of the data source
     * @return The {@link ucar.unidata.data.DataSource}
     */
    private DataSource getDataSourceAt(int listIndex) {
        return (DataSource) getDataSources().get(listIndex);

    }



    /**
     * Return the GUI contents
     *
     * @return The GUI contents
     */
    public JComponent getContents() {
        return contents;
    }



    /**
     * Return the JComponent that holds the Create and Close buttons.
     *
     * @return Button panel
     */
    public JComponent getButtons() {
        return new JPanel();
    }




    /**
     *  Remove all references to anything we may have. We do this because (stupid) Swing
     *  seems to keep around lots of different references to thei component and/or it's
     *  frame. So when we do a window.dispose () this DataSelector  does not get gc'ed.
     */
    public void dispose() {
        for (int i = 0; i < getDataSources().size(); i++) {
            DataSource dataSource = (DataSource) getDataSources().get(i);
            if (dataSource == null) {
                continue;  //??just in case
            }
        }
        super.dispose();
    }


    /**
     * Find the wrapper for the given data source
     *
     * @param dataSource the data source
     *
     * @return The wrapper or null if none found
     */
    private DataSourceWrapper findWrapper(DataSource dataSource) {
        List wrappers = getWrappers();
        for (int i = 0; i < wrappers.size(); i++) {
            DataSourceWrapper wrapper = (DataSourceWrapper) wrappers.get(i);
            if (Misc.equals(dataSource, wrapper.dataSource)) {
                return wrapper;
            }
        }
        return null;
    }

    /**
     *  Remove the specified data source only if it is
     * <em>not</em> the formulaDataSource.
     *
     * @param dataSource The data source to remove
     * @return Did we remove the data source
     */
    protected boolean removeDataSourceInner(DataSource dataSource) {
        if ( !super.removeDataSourceInner(dataSource)) {
            return false;
        }
        DataSourceWrapper wrapper = findWrapper(dataSource);
        if (wrapper != null) {
            wrappers.remove(wrapper);
            wrapper.remove();
        }
        return true;
    }


    /**
     * Be notified of a change to the display templates
     */
    public void displayTemplatesChanged() {
        List wrappers = getWrappers();
        for (int i = 0; i < wrappers.size(); i++) {
            DataSourceWrapper wrapper = (DataSourceWrapper) wrappers.get(i);
            wrapper.dcd.displayTemplatesChanged();
        }
    }

    /**
     * Some data source was added or removed. This method
     * updates the entire GUI if needed
     *
     * @param forceUpdate Force the GUI update
     */
    private void dataSourcesChanged(boolean forceUpdate) {
        List wrappers = getWrappers();
        for (int i = 0; i < wrappers.size(); i++) {
            DataSourceWrapper wrapper = (DataSourceWrapper) wrappers.get(i);
            wrapper.dataSourceChanged();
        }
    }


    /**
     * Get the list of wrappers
     *
     * @return list of wrappers
     */
    private List getWrappers() {
        return new ArrayList(wrappers);
    }


    /**
     * Change the gui when the given data source has changed.
     * This gets called by the {@link ucar.unidata.idv.IntegratedDataViewer}
     * When one of the {@link DataControlDialog}-s changes the
     * times on the data source.
     *
     * @param dataSource The data source that changed
     */
    public void dataSourceTimeChanged(DataSource dataSource) {
        dataSourceChanged(dataSource);
    }


    /**
     * The given data source has changed. This method just re-adds
     * the datasource to force an update of the GUI
     *
     * @param dataSource The data source that changed
     */
    public void dataSourceChanged(DataSource dataSource) {
        List wrappers = getWrappers();
        for (int i = 0; i < wrappers.size(); i++) {
            DataSourceWrapper wrapper = (DataSourceWrapper) wrappers.get(i);
            if (Misc.equals(wrapper.dataSource, dataSource)) {

                if (Misc.equals(wrapper.dataSource, dataSource)) {
                    wrapper.dataSourceChanged();
                }
            }
        }
    }


    /**
     *  Add the {@link ucar.unidata.data.DataSource} and its
     * {@link ucar.unidata.data.DataChoice}-s into the gui
     *
     * @param dataSource The data source to add
     */
    public void addDataSource(DataSource dataSource) {
        addDataSource(dataSource,
        // always force update for formula tree
        ((formulaDataSource != null)
         && dataSource.equals(formulaDataSource)));
    }




    /**
     *  Add the {@link ucar.unidata.data.DataSource} and its
     * {@link ucar.unidata.data.DataChoice}-s into the gui
     *
     * @param dataSource The data source to add
     * @param forceUpdate  force the update of the UI
     */
    private void addDataSource(DataSource dataSource, boolean forceUpdate) {
        super.addDataSource(dataSource);
        DataSourceWrapper wrapper = findWrapper(dataSource);
        if (wrapper == null) {
            wrapper = new DataSourceWrapper(this, dataSource);
            wrappers.add(wrapper);
        }
        dataSourcesChanged(forceUpdate);
    }


    /**
     * A helper method to find the label to use for the given
     * {@link ucar.unidata.data.DataSource}. If the length of
     * the toString of the data source is less than 30 just use that.
     * Else be a bit smart about truncating it.
     *
     * @param ds The data soruce to get a label for
     * @return The label
     */
    public static String getNameForDataSource(DataSource ds) {
        return getNameForDataSource(ds, 30, true);
    }


    /**
     * A helper method to find the label to use for the given
     * {@link ucar.unidata.data.DataSource}. If the length of
     * the toString of the data source is less than 30 just use that.
     * Else be a bit smart about truncating it.
     *
     * @param ds The data soruce to get a label for
     * @param length String length to clip to
     * @param alwaysDoIt If false then we only lip if this is a file or url
     * @return The label
     */
    public static String getNameForDataSource(DataSource ds, int length,
            boolean alwaysDoIt) {
        return ucar.unidata.data.DataSourceImpl.getNameForDataSource(ds,
                length, alwaysDoIt);
    }



    /**
     * Class DataSourceWrapper Holds the selector gui  for a data source
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.87 $
     */
    public static class DataSourceWrapper extends JPanel {

        /** mutex for searching the data tree */
        private final Object SEARCH_MUTEX = new Object();

        /** my parent */
        DataSelector dataSelector;


        /** The JPanel the JTree is put in */
        private JPanel treePanel;

        /** Search field button */
        private JButton searchBtn;

        /** holds search comps */
        private JPanel searchPanel;

        /** search field */
        private JTextField searchFld;

        /** sort button */
        private JButton sortBtn;

        /** holds search field */
        private JPanel searchFldPanel;

        /** Is search field shown */
        private boolean showingSearchFld = false;


        /** The data source */
        DataSource dataSource;

        /** the dcd */
        DataControlDialog dcd;

        /** my contents */
        JComponent contents;

        /** my tree */
        DataTree dataTree;

        /** For showing this selector */
        JToggleButton button;


        /**
         * ctor
         *
         * @param theDataSelector parent
         * @param dataSource data source
         */
        public DataSourceWrapper(DataSelector theDataSelector,
                                 DataSource dataSource) {
            this.dataSelector = theDataSelector;
            this.dataSource   = dataSource;
            doMakeContents();
            button = dataSelector.tabbedPane.addTab(getLabel(), contents);
            initButton();
            dataSourceChanged();
        }


        /**
         * Initialize the button
         */
        private void initButton() {
            button.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    if ( !SwingUtilities.isRightMouseButton(event)
                            && (event.getClickCount() > 1)) {
                        if (DataManager.isFormulaDataSource(dataSource)) {
                            dataSelector.getIdv().getJythonManager()
                                .showFormulaDialog();
                        } else {
                            dataSource.showPropertiesDialog();
                        }
                    }
                }

                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isRightMouseButton(event)) {
                        dataSelector.showDataSourceMenu(event,
                                (JComponent) event.getSource(), dataSource);
                    }
                }
            });

        }


        /**
         * make the gui
         */
        private void doMakeContents() {

            dcd = new DataControlDialog(dataSelector.getIdv(), false,
                                        dataSelector.horizontalOrientation);
            dataTree  = createDataTree(dataSource);




            treePanel = new JPanel();
            treePanel.setLayout(new BorderLayout());
            if (dataSelector.horizontalOrientation) {
                treePanel.setPreferredSize(new Dimension(DIM_H_X, DIM_H_Y));
            } else {
                treePanel.setPreferredSize(new Dimension(200, 200));
            }
            JComponent treeContents = dataTree.getContents();
            if (treeContents != null) {
                treePanel.add(BorderLayout.CENTER, treeContents);
                DataChoice dataChoice = dataTree.getSelectedDataChoice();
                if (dataChoice != null) {
                    dcd.setDataChoice(dataChoice);
                }
            }

            searchFldPanel = new JPanel(new CardLayout());
            searchFld      = new JTextField("", 7);
            searchFld.setToolTipText(
                "<html>" + Msg.msg("Enter a search term") + "<br>"
                + Msg.msg("Press return to repeat search") + "<br>"
                + Msg.msg("Escape to close") + "</html>");
            searchFld.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        searchBtnPressed();
                    } else {
                        doSearch(e.getKeyCode() != KeyEvent.VK_ENTER);
                    }
                }
            });
            sortBtn =
                GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif",
                                         this, "sort");
            sortBtn.setToolTipText("Sort the entries");

            searchFldPanel.add("empty", new JLabel(" "));
            //            searchFldPanel.add("field", GuiUtils.leftCenter(sortBtn,searchFld));
            searchFldPanel.add("field", searchFld);
            searchBtn =
                GuiUtils.makeImageButton("/auxdata/ui/icons/Search16.gif",
                                         this, "searchBtnPressed");
            if (cancelIcon == null) {
                cancelIcon =
                    GuiUtils.getImageIcon("/auxdata/ui/icons/cancel.gif",
                                          true);
                searchIcon =
                    GuiUtils.getImageIcon("/auxdata/ui/icons/Search16.gif",
                                          true);
            }

            searchPanel = GuiUtils.centerRight(searchFldPanel, searchBtn);

            JComponent left =
                GuiUtils.topCenter(GuiUtils.leftRight(makeLabel("Fields"),
                    searchPanel), treePanel);
            left.setBorder(null);
            GuiUtils.makeMouseOverBorder(searchBtn);
            searchBtn.setToolTipText("Search for a field by name");
            searchBtn.setFocusPainted(false);

            JComponent right;
            if (dataSelector.horizontalOrientation) {
                dcd.displayScroller.setPreferredSize(new Dimension(DIM_H_X,
                        DIM_H_Y));
                JButton createBtn = GuiUtils.makeButton("Create Display",
                                        dcd, "doOk");
                createBtn.setHorizontalAlignment(SwingConstants.LEFT);
                dcd.addCreateButton(createBtn);
                dcd.getDataSelectionWidget().getTimesList().setPreferredSize(
                    new Dimension(DIM_H_X, DIM_H_Y));
                right = GuiUtils.hsplit(
                    dcd.getDataSelectionWidget().getTimesList(),
                    GuiUtils.topCenter(
                        GuiUtils.wrap(GuiUtils.inset(createBtn, 1)),
                        dcd.displayScroller), 0.5);
                right.setBorder(null);
                contents = GuiUtils.hsplit(left, right, 0.5);
                //            contents = GuiUtils.hsplit(GuiUtils.inset(left,5), GuiUtils.inset(right,5), 0.5);
            } else {
                right = GuiUtils.topCenter(makeLabel("Displays    "),
                                           GuiUtils.inset(dcd.getContents(),
                                               4));
                contents = GuiUtils.hsplit(left, right, 0.5);
            }
            if ( !dataSelector.horizontalOrientation) {
                JButton createBtn = new JButton("Create Display");
                createBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        dcd.doOk();
                    }
                });
                dcd.addCreateButton(createBtn);
                contents = GuiUtils.centerBottom(contents,
                        GuiUtils.inset(GuiUtils.wrap(createBtn), 5));
                contents = GuiUtils.inset(contents, new Insets(0, 0, 0, 0));
            }


        }


        /**
         * Sort the tree
         */
        public void sort() {
            dataTree.sort();
        }

        /**
         * remove me
         */
        private void remove() {
            dataSelector.tabbedPane.remove(button, contents);
        }


        /**
         * update gui
         */
        public void dataSourceChanged() {
            String toolTip =
                "<html>&nbsp;" + dataSource.toString()
                + "&nbsp;<br>&nbsp;Right mouse to show context menu</html>";
            button.setText(getLabel());
            button.setToolTipText(toolTip);
            dcd.dataSourceChanged(dataSource);
            dataTree.dataSourceChanged(dataSource);
        }


        /**
         * Search the tree
         *
         * @param andClear Clear the search
         */
        public void doSearch(boolean andClear) {
            synchronized (SEARCH_MUTEX) {
                String s = searchFld.getText().trim();
                if (s.length() == 0) {
                    dataTree.clearSearchState();
                    searchFld.setBackground(Color.white);
                    return;
                }
                if (andClear) {
                    dataTree.clearSearchState();
                }

                if (dataTree.doSearch(s, searchBtn)) {
                    searchFld.setBackground(Color.white);
                } else {
                    dataTree.clearSearchState();
                    if (dataTree.doSearch(s, searchBtn)) {
                        searchFld.setBackground(Color.white);
                    } else {
                        searchFld.setBackground(COLOR_BADSEARCH);
                    }
                }
            }
        }


        /**
         * Create the data tree
         *
         * @param dataSource the data source
         *
         * @return the data tree
         */
        private DataTree createDataTree(final DataSource dataSource) {
            final DataTree dataTree = new DataTree(dataSelector.getIdv(),
                                          false, true);
            dataTree.getTree().addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ( !showingSearchFld) {
                        if (((e.getKeyCode() == KeyEvent.VK_F)
                                && e.isControlDown()) || (e.getKeyCode()
                                   == KeyEvent.VK_SLASH)) {
                            searchBtnPressed();
                        }
                    } else {
                        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            searchBtnPressed();
                        }
                    }
                }
            });

            dataTree.getTree().addTreeSelectionListener(
                new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    DataChoice selectedChoice =
                        dataTree.getSelectedDataChoice();
                    if (selectedChoice != null) {
                        dcd.setDataChoice(selectedChoice);
                    } else {
                        String choiceName =
                            (String) dataSource.getProperty(
                                DataSource.PROP_DATACHOICENAME);
                        if (choiceName != null) {
                            selectedChoice = dataSource.findDataChoice(
                                (Object) choiceName);
                            if (selectedChoice != null) {
                                List selectedChoices = new ArrayList();
                                selectedChoices.add(selectedChoice);
                                dataTree.selectChoices(selectedChoices, true);
                            } else {
                                dcd.setDataChoice(null);
                            }
                        } else {
                            dcd.setDataChoice(null);
                        }
                    }
                }
            });

            dataTree.getTree().addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    Object object = dataTree.getObjectAt(event.getX(),
                                        event.getY());
                    if ((object == null) || !(object instanceof DataChoice)) {
                        return;
                    }
                    DataChoice dataChoice = (DataChoice) object;
                    if (SwingUtilities.isRightMouseButton(event)
                            && ((DataChoice) object).isEndUserFormula()) {
                        dataSelector.getIdv().getIdvUIManager()
                            .showDataTreeMenu(dataTree, event, false);
                    } else if (SwingUtilities.isLeftMouseButton(event)
                               && (event.getClickCount() > 1)) {
                        dcd.setDataChoice(dataChoice);
                        //                    dcd.setDataChoice((DataChoice) object);
                        dataSelector.getIdv().getIdvUIManager().processDialog(
                            dcd);
                    }
                }
            });
            dataTree.getTree().addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent event) {
                    if (GuiUtils.isDeleteEvent(event)) {
                        dataSelector.getIdv().getIdvUIManager()
                            .deleteKeyPressed(dataTree);
                    }
                }
            });


            dataTree.setDataSource(dataSource);
            dataTree.openUp();
            DataChoice selectedChoice = dataTree.getSelectedDataChoice();
            if (selectedChoice != null) {
                dcd.setDataChoice(selectedChoice);
            }
            return dataTree;
        }



        /**
         * gert the data source label
         *
         * @return label
         */
        public String getLabel() {
            return getNameForDataSource(dataSource, 30, true);
        }


        /**
         * Handle when search button is pressed. Show or hide the field.
         */
        public void searchBtnPressed() {
            CardLayout cardLayout = (CardLayout) searchFldPanel.getLayout();
            if ( !showingSearchFld) {
                cardLayout.show(searchFldPanel, "field");
                searchFld.requestFocus();
                searchBtn.setIcon(cancelIcon);
            } else {
                searchBtn.setIcon(searchIcon);
                cardLayout.show(searchFldPanel, "empty");
            }
            showingSearchFld = !showingSearchFld;
        }



    }



}

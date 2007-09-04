/*
 * $Id: DataTreeDialog.java,v 1.43 2007/06/08 21:25:20 jeffmc Exp $
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

package ucar.unidata.idv.ui;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceResults;
import ucar.unidata.data.DerivedDataChoice;



import ucar.unidata.idv.*;





import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.VisADException;




import java.awt.*;
import java.awt.event.*;



import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;



/**
 * This handles popping up a dialog full of {@link DataTree}s
 * for when the user is choosing operands for a formula or when
 * a display is changing parameters.
 * <p>
 * This has a list of String labels and param names
 * and constructs a gui consisting of one DataTree
 * for each label/param name
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.43 $
 */

public class DataTreeDialog extends JDialog implements ActionListener {


    /** Window title  to use */
    private static String TITLE = "Field Selector";

    /** Used for the button title/command */
    private static final String CMD_POPUPDATACHOOSER = "Add New Data Source";


    /** Reference to the IDV */
    IntegratedDataViewer idv;

    /** The param labels */
    private List paramNames;


    /** The param names */
    private List fieldNames;


    /** Shold we add the italics and the prefix to the label */
    private boolean addLabelDecoration = true;

    /** The labels to show in the GUI */
    private List labels = new ArrayList();


    /** All of the data trees, one per label/param name */
    List dataTrees = new ArrayList();

    List dataSelectionWidgets = new ArrayList();

    /** Listener to fire select events off to */
    ActionListener listener;

    /** List of lists of selected data choices, one per data tree */
    List selected = null;

    /** The data sources to show in the data trees */
    List dataSources;

    /** Any DataCatgeory-s to use to filter out the DataChoice-s with */
    List categories;

    List patterns;

    /** Support multiple selection? */
    boolean multiples = true;



    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param frame Parent Frame
     * @param src  Component to place ourselves near
     * @param categories List of list of {@link ucar.unidata.data.DataCategory}-s
     * @param paramNames List of param names
     * @param multiples Support multiples
     * @param dataSources List of data sources
     */
    public DataTreeDialog(IntegratedDataViewer idv, JFrame frame,
                          Component src, List categories, List paramNames,
                          boolean multiples, List dataSources) {

        this(idv, frame, src, categories, paramNames, multiples, dataSources,
             (List) null);
    }


    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param frame Parent Frame
     * @param src  Component to place ourselves near
     * @param categories List of list of {@link ucar.unidata.data.DataCategory}-s
     * @param paramNames List of param names
     * @param patterns List of patterns to match the data choices with
     * @param multiples Support multiples
     * @param dataSources List of data sources
     */
    public DataTreeDialog(IntegratedDataViewer idv, JFrame frame,
                          Component src, List categories, List paramNames, 
                          List patterns,
                          boolean multiples, List dataSources) {

        this(idv, frame, src, categories, paramNames, patterns, multiples, dataSources,
             (List) null);
    }

    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param frame Parent Frame
     * @param src  Component to place ourselves near
     * @param categories List of list of {@link ucar.unidata.data.DataCategory}-s
     * @param paramNames List of param names
     * @param multiples Support multiples
     * @param dataSources List of data sources
     * @param selectedDataChoices list of already selected data choices
     */
    public DataTreeDialog(IntegratedDataViewer idv, JFrame frame,
                          Component src, List categories, List paramNames,
                          boolean multiples, List dataSources,
                          List selectedDataChoices) {
        this(idv, frame, src, categories, paramNames, null, multiples,
             dataSources, selectedDataChoices);
    }



    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param frame Parent Frame
     * @param src  Component to place ourselves near
     * @param categories List of list of {@link ucar.unidata.data.DataCategory}-s
     * @param paramNames List of param names
     * @param multiples Support multiples
     * @param dataSources List of data sources
     * @param selectedDataChoices list of already selected data choices
     */
    public DataTreeDialog(IntegratedDataViewer idv, JFrame frame,
                          Component src, List categories, List paramNames, List patterns,
                          boolean multiples, List dataSources,
                          List selectedDataChoices) {


        super(frame, TITLE, true);
        this.idv         = idv;
        this.paramNames  = paramNames;
        this.dataSources = dataSources;
        this.categories  = categories;
        this.patterns    = patterns;
        init(multiples, src, selectedDataChoices);
    }



    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param listener Route select events to
     * @param label The label to use in the GUI
     * @param src  Component to place ourselves near
     * @param categories List of {@link ucar.unidata.data.DataCategory}-s
     * @param multiples Support multiples
     * @param dataSources List of data sources
     * @param selectedDataChoices Pre-selected data choices
     */
    public DataTreeDialog(IntegratedDataViewer idv, ActionListener listener,
                          List categories, String label, boolean multiples,
                          Component src, List dataSources,
                          List selectedDataChoices) {
        this(idv, listener, categories, null, multiples, src, dataSources,
             selectedDataChoices, false);
    }

    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param listener Route select events to
     * @param label The label to use in the GUI
     * @param src  Component to place ourselves near
     * @param categories List of {@link ucar.unidata.data.DataCategory}-s
     * @param multiples Support multiples
     * @param dataSources List of data sources
     * @param selectedDataChoices Pre-selected data choices
     * @param modal Should this be a modal dialog
     */
    public DataTreeDialog(IntegratedDataViewer idv, ActionListener listener,
                          List categories, String label, boolean multiples,
                          Component src, List dataSources,
                          List selectedDataChoices, boolean modal) {

        this(idv, listener, categories, label, null, multiples, src,
             dataSources, selectedDataChoices, modal);
    }



    /**
     * Create the dialog
     *
     * @param idv Reference to the IDV
     * @param listener Route select events to
     * @param label The label to use in the GUI
     * @param fieldName The name of the field to intially have selected. May be ull.
     * @param src  Component to place ourselves near
     * @param categories List of {@link ucar.unidata.data.DataCategory}-s
     * @param multiples Support multiples
     * @param dataSources List of data sources
     * @param selectedDataChoices Pre-selected data choices
     * @param modal Should this be a modal dialog
     */
    public DataTreeDialog(IntegratedDataViewer idv, ActionListener listener,
                          List categories, String label, String fieldName,
                          boolean multiples, Component src, List dataSources,
                          List selectedDataChoices, boolean modal) {

        super(GuiUtils.getFrame(src), TITLE, modal);
        this.idv = idv;
        if (categories == null) {
            this.categories = null;
        } else {
            this.categories = Misc.newList(categories);
        }
        this.listener      = listener;
        addLabelDecoration = false;
        this.paramNames    = Misc.newList(label);
        this.fieldNames    = Misc.newList(fieldName);
        this.dataSources   = dataSources;
        init(multiples, src, selectedDataChoices);
    }


    /**
     * Initalize the dialog
     *
     * @param multiples Supports multiple selection
     * @param src Component to popup near
     * @param selectedDataChoices Pre-selected data choices
     */
    private void init(boolean multiples, Component src,
                      List selectedDataChoices) {
        this.multiples = multiples;
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i).toString();
            String label     = paramName;
            labels.add(label);
            List   categoryList     = ((categories != null)
                                       ? (List) categories.get(i)
                                       : null);
            String initialFieldName = null;
            if (fieldNames != null) {
                initialFieldName = (String) fieldNames.get(i);
            }
            final DataTree dataTree = new DataTree(idv, dataSources,
                                          categoryList, initialFieldName,
                                          null);
            final int theIndex = i;
            dataTree.getTree().addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    if (me.getClickCount() > 1) {
                        doOk();
                        return;
                    }
                    treeClick(theIndex);
                }
            });
            idv.getIdvUIManager().addDataSourceHolder(dataTree);
            dataTree.setMultipleSelect(multiples);
            if (selectedDataChoices != null) {
                dataTree.selectChoices(selectedDataChoices);
            } else if(patterns!=null && i<patterns.size()) {
                String pattern = (String) "pattern:" + patterns.get(i);
                 for(int dataSourceIdx=0;dataSourceIdx<dataSources.size();dataSourceIdx++) {
                    DataSource dataSource = (DataSource) dataSources.get(dataSourceIdx);
                    DataChoice dataChoice = dataSource.findDataChoice(pattern);
                    if(dataChoice!=null) {
                        dataTree.selectChoices(Misc.newList(dataChoice));
                        break;
                    }
                }

            }
            dataTrees.add(dataTree);
        }
        doMakeWindow(src);
    }



    /**
     * The user has clicked on the index'th data tree. This routine
     * sets state in the DataSelectionWidget
     *
     * @param index Which data tree was clicked
     */
    private void treeClick(int index) {
        DataTree   tree       = (DataTree) dataTrees.get(index);
        DataChoice dataChoice = tree.getSelectedDataChoice();

        DataSelectionWidget dsw  = (DataSelectionWidget) dataSelectionWidgets.get(index);
        dsw.updateSelectionTab(dataChoice);
    }

    /**
     * Get the list of selected {@link ucar.unidata.data.DataChoice}-s
     *
     * @return list of selected data choices
     */
    public List getSelected() {
        return selected;
    }

    /**
     * Create and popup the window
     *
     * @param src Where should the window go
     */
    private void doMakeWindow(Component src) {
        Component contents = doMakeContents();
        Container cpane    = getContentPane();
        cpane.setLayout(new BoxLayout(cpane, BoxLayout.Y_AXIS));
        cpane.add(contents);
        //src may be null
        try {
            Point     loc  = src.getLocationOnScreen();
            Dimension size = src.getSize();
            setLocation(loc.x + size.width, loc.y - 30);
        } catch (Exception exc) {
            setLocation(50, 50);
        }
        pack();
        show();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    private Component doMakeContents() {
        List topComponents  = new ArrayList();
        List timeComponents = new ArrayList();
        for (int i = 0; i < labels.size(); i++) {
            JScrollPane scroller =
                ((DataTree) dataTrees.get(i)).getScroller();

            DataSelectionWidget dsw = new DataSelectionWidget(idv,false);
            dsw.updateSelectionTab(null);
            dataSelectionWidgets.add(dsw);

            String labelString = StringUtil.replace(labels.get(i).toString(),
                                     "_", " ");
            if (addLabelDecoration) {
                labelString = "<html>Field: <i><b>" + labelString
                              + "</b></i></html>";
            }
            JLabel label = new JLabel(labelString);
            scroller.setPreferredSize(new Dimension(250, 300));
            topComponents.add(GuiUtils.topCenter(GuiUtils.inset(label,
                    new Insets(10, 5, 0, 10)), scroller));
            timeComponents.add(dsw.getContents());
        }
        topComponents.addAll(timeComponents);
        GuiUtils.tmpInsets = new Insets(4, 6, 4, 6);
        Component trees = GuiUtils.doLayout(topComponents,
                                            topComponents.size() / 2,
                                            GuiUtils.WT_Y, GuiUtils.WT_Y);
        JPanel buttons = GuiUtils.makeButtons(this, new String[] {  /* CMD_POPUPDATACHOOSER,*/
            GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL
        });
        return GuiUtils.centerBottom(trees, buttons);
    }

    /**
     * Remove the data trees and close the window.
     */
    private void doClose() {
        for (int i = 0; i < dataTrees.size(); i++) {
            idv.getIdvUIManager().removeDataSourceHolder(
                (DataTree) dataTrees.get(i));
        }
        hide();
    }


    /**
     * Remove the data trees and close the window.
     */
    public void dispose() {
        dataTrees   = null;
        listener    = null;
        selected    = null;
        dataSources = null;
        super.dispose();
    }


    /**
     * User pressed ok. Get the select data choices from each DataTree
     * and send them off to the listener
     */
    public void doOk() {
        selected = new ArrayList();
        for (int i = 0; i < dataTrees.size(); i++) {
            DataSelection dataSelection = null;
            DataSelectionWidget dsw = (DataSelectionWidget) dataSelectionWidgets.get(i);
            dataSelection = dsw.createDataSelection(true);
            List selectedFromTree =
                ((DataTree) dataTrees.get(i)).getSelectedDataChoices();
            selectedFromTree = DataChoice.cloneDataChoices(selectedFromTree);
            for (int dataChoiceIdx = 0;
                    dataChoiceIdx < selectedFromTree.size();
                    dataChoiceIdx++) {
                ((DataChoice) selectedFromTree.get(
                    dataChoiceIdx)).setDataSelection(dataSelection);
            }
            if (multiples) {
                //If we allow multiple selections then we add the list
                selected.add(selectedFromTree);
            } else {
                //else each list should contain at most one DataChoice
                if (selectedFromTree.size() == 0) {
                    //For now if the user did not choose one then we set selected = null
                    //and break out of the loop
                    selected = null;
                    break;
                }
                selected.add(selectedFromTree.get(0));
            }
        }

        if (listener != null) {
            listener.actionPerformed(new ActionEvent(selected, 0,
                    "selected"));
        }
        doClose();
    }

    /**
     * Handle UI actions
     *
     * @param event The event
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(CMD_POPUPDATACHOOSER)) {
            if (idv != null) {
                idv.showChooserModal();
            }
            return;
        }

        if (cmd.equals(GuiUtils.CMD_OK)) {
            doOk();
            return;
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            selected = null;
            doClose();
            return;
        }
    }

}


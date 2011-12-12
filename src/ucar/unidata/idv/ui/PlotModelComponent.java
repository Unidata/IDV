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


import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.Method;

import java.util.List;

import javax.swing.*;


/**
 *
 *
 * @author MetApps Development Team
 * @version $Revision: 1.228 $
 */

public class PlotModelComponent extends JPanel {

    /** calling object */
    IntegratedDataViewer idv;

    /** the listener */
    Object plotModelListener;

    /** the method to call on the listener */
    Method method;

    /** widget */
    private JButton changeButton;

    /** gui comp */
    private JLabel label;

    /** station model to use */
    StationModel plotModel;

    /** ??? */
    private boolean addNone = false;


    /**
     * Create a new PlotModelComponent
     *
     * @param idv   the associated IDV
     * @param plotModelListener the listener
     * @param methodName the method to call on listener
     * @param plotModel the plot model
     */
    public PlotModelComponent(IntegratedDataViewer idv,
                              Object plotModelListener, String methodName,
                              StationModel plotModel) {
        this(idv, plotModelListener, methodName, plotModel, false);
    }

    /**
     * Create a new PlotModelComponent
     *
     * @param idv   the associated IDV
     * @param plotModelListener the listener
     * @param methodName method on listener to call
     * @param plotModel the plot model
     * @param addNone should we add the 'none' entry to the widget
     */
    public PlotModelComponent(IntegratedDataViewer idv,
                              Object plotModelListener, String methodName,
                              StationModel plotModel, boolean addNone) {
        this.idv     = idv;
        this.addNone = addNone;
        setLayout(new BorderLayout());
        this.add(makeStationModelWidget());
        this.plotModelListener = plotModelListener;
        method = Misc.findMethod(plotModelListener.getClass(), methodName,
                                 new Class[] { StationModel.class });
        setPlotModel(plotModel);
    }


    /**
     * Make the gui widget for setting the station model
     *
     * @return the widget
     */
    private JComponent makeStationModelWidget() {
        JButton editButton =
            GuiUtils.getImageButton("/ucar/unidata/idv/images/edit.gif",
                                    getClass());
        editButton.setToolTipText("Show the plot model editor");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {}
        });

        label = new JLabel(" ");
        changeButton =
            GuiUtils.getImageButton("/auxdata/ui/icons/DownDown.gif",
                                    getClass());
        changeButton.setToolTipText("Click to change plot model");
        changeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StationModelManager smm      = idv.getStationModelManager();
                ObjectListener      listener = new ObjectListener(null) {
                    public void actionPerformed(ActionEvent ae) {
                        Misc.run(new Runnable() {
                            public void run() {
                                idv.showWaitCursor();
                                try {
                                    plotModel = (StationModel) theObject;
                                    if (plotModel != null) {
                                        label.setText(
                                            plotModel.getDisplayName());
                                    }
                                    method.invoke(plotModelListener,
                                            new Object[] { plotModel });
                                } catch (Exception exc) {
                                    idv.logException("Changing plot model",
                                            exc);
                                }
                                idv.showNormalCursor();
                            }
                        });
                    }
                };

                List items = StationModelCanvas.makeStationModelMenuItems(
                                 smm.getStationModels(), listener, smm);
                items.add(0, GuiUtils.MENU_SEPARATOR);
                if (addNone) {
                    items.add(0, GuiUtils.makeMenuItem("None",
                            PlotModelComponent.this, "setNone"));
                }
                items.add(0, GuiUtils.makeMenuItem("Edit",
                        PlotModelComponent.this, "editPlotModel"));
                JPopupMenu popup = GuiUtils.makePopupMenu(items);
                popup.show(changeButton, changeButton.getSize().width / 2,
                           changeButton.getSize().height);
            }
        });

        return GuiUtils.centerRight(label,
                                    GuiUtils.inset(changeButton,
                                        new Insets(0, 4, 0, 0)));

    }


    /**
     * user selected 'none'
     */
    public void setNone() {
        plotModel = null;
        label.setText("None");
        try {
            method.invoke(plotModelListener, new Object[] { plotModel });
        } catch (Exception exc) {
            idv.logException("Clearing plot model", exc);
        }
    }

    /**
     * edit the plot model
     */
    public void editPlotModel() {
        if (plotModel != null) {
            idv.getStationModelManager().show(plotModel);
        }
    }

    /**
     * get the plot model
     *
     * @return the plot model
     */
    public StationModel getPlotModel() {
        return plotModel;
    }

    /**
     * Utility method to set the plot model by name
     *
     * @param name the name of the plot model
     */
    public void setPlotModelByName(String name) {
        setPlotModel(idv.getStationModelManager().getStationModel(name));
    }

    /**
     * set the plot model
     *
     * @param sm the plot model
     */
    public void setPlotModel(StationModel sm) {
        this.plotModel = sm;
        if (sm != null) {
            label.setText(sm.getDisplayName());
        } else {
            label.setText("None");
        }
        label.repaint();
    }

}

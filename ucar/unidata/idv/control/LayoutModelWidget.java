/*
 * $Id: StationModelControl.java,v 1.228 2007/08/08 18:27:47 jeffmc Exp $
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


package ucar.unidata.idv.control;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.MenuUtil;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import java.lang.reflect.*;


/**

 *
 * @author MetApps Development Team
 * @version $Revision: 1.228 $
 */

public class LayoutModelWidget extends JPanel {
    DisplayControlImpl control;

    Object layoutModelListener;
    Method method;

    /** widget */
    private JButton changeButton;

    /** station model to use */
    StationModel layoutModel;

    private boolean addNone = false;


    /**
     * Default constructor.
     */
    public LayoutModelWidget(DisplayControlImpl control, Object layoutModelListener,  String methodName, StationModel layoutModel) {
        this(control,layoutModelListener, methodName, layoutModel, false);
    }

    public LayoutModelWidget(DisplayControlImpl control, Object layoutModelListener,  String methodName, StationModel layoutModel, boolean addNone) {
        this.control = control;
        this.addNone = addNone;
        setLayout(new BorderLayout());
        this.add(makeStationModelWidget());
        this.layoutModelListener = layoutModelListener;
        method =  Misc.findMethod(layoutModelListener.getClass(), methodName,
                         new Class[]{StationModel.class});
        setLayoutModel(layoutModel);
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
        editButton.setToolTipText("Show the layout model editor");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            }
        });

        changeButton = new JButton("");
        changeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StationModelManager smm =
                    control.getControlContext().getStationModelManager();
                ObjectListener listener = new ObjectListener(null) {
                    public void actionPerformed(ActionEvent ae) {
                        Misc.run(new Runnable() {
                            public void run() {
                                control.showWaitCursor();
                                try {
                                    layoutModel =  (StationModel) theObject;
                                    changeButton.setText(layoutModel.getDisplayName());
                                    method.invoke(layoutModelListener, new Object[]{layoutModel});
                                } catch (Exception exc) {
                                    control.logException("Changing layout model",
                                                         exc);
                                }
                                control.showNormalCursor();
                            }
                        });
                    }
                };

                List items = StationModelCanvas.makeStationModelMenuItems(
                                                                          smm.getStationModels(), listener, smm);
                items.add(0,GuiUtils.MENU_SEPARATOR);
                if(addNone) {
                    items.add(0,GuiUtils.makeMenuItem("None", LayoutModelWidget.this, "setNone"));
                }
                items.add(0,
                          GuiUtils.makeMenuItem("Edit",LayoutModelWidget.this, "editLayoutModel"));
                JPopupMenu popup =GuiUtils.makePopupMenu(items);
                popup.show(changeButton, changeButton.getSize().width / 2,
                           changeButton.getSize().height);
            }
        });

        return changeButton;
        //        return GuiUtils.hflow(Misc.newList(editButton, changeButton), 4, 0);

    }


    public void setNone() {
        layoutModel =  null;
        changeButton.setText("None");
        try {
        method.invoke(layoutModelListener, new Object[]{layoutModel});
        } catch (Exception exc) {
            control.logException("Clearing layout model",
                                 exc);
        }
    }

    public void editLayoutModel() {
        if(layoutModel!=null) {
            control.getControlContext().getStationModelManager().show(layoutModel);
        }
    }

    public StationModel getLayoutModel() {
        return layoutModel;
    }

    public void setLayoutModel(StationModel sm) {
        this.layoutModel = sm;
        if (sm != null) {
            changeButton.setText(sm.getDisplayName());
        } else {
            changeButton.setText("None");
        }

    } 

}


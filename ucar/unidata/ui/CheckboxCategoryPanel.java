/*
 * $Id: CheckboxCategoryPanel.java,v 1.3 2007/07/06 20:45:29 jeffmc Exp $
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

package ucar.unidata.ui;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;

import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class CheckboxCategoryPanel  holds the checkboxes under a category
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CheckboxCategoryPanel extends JPanel {

    /** Toggle icon used to show open categories and legend details */
    public static ImageIcon categoryOpenIcon;

    /** Toggle icon used to show closed categories and legend details */
    public static ImageIcon categoryClosedIcon;


    static {
        categoryOpenIcon = new ImageIcon(
            Resource.getImage("/auxdata/ui/icons/CategoryOpen.gif"));
        categoryClosedIcon = new ImageIcon(
            Resource.getImage("/auxdata/ui/icons/CategoryClosed.gif"));
    }



    /** The list of checkboxes */
    private List items = new ArrayList();

    /** The visibility checkbox */
    private JCheckBox visCbx;

    /** The toggle button */
    private JButton toggleBtn;

    /**
     * Create me
     *
     * @param catName The name of the category
     * @param visible Is it initially visible
     */
    public CheckboxCategoryPanel(String catName, boolean visible) {
        setLayout(new GridLayout(0, 1, 0, 0));
        setVisible(visible);
        final CheckboxCategoryPanel theCatPanel = this;
        toggleBtn = GuiUtils.getImageButton(categoryClosedIcon);
        toggleBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (theCatPanel.isVisible()) {
                    theCatPanel.setVisible(false);
                    toggleBtn.setIcon(categoryClosedIcon);
                } else {
                    theCatPanel.setVisible(true);
                    toggleBtn.setIcon(categoryOpenIcon);
                }
            }
        });

        visCbx = new JCheckBox(catName);
        visCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                toggleAll(visCbx.isSelected());
            }
        });
    }

    /**
     * Add the given item into the list of children
     *
     * @param box The item
     */
    public void addItem(JCheckBox box) {
        items.add(box);
    }

    /**
     * Create and return the top panel. That is, the one that holds
     * the toggle button, vis checkbox and the label.
     *
     * @return The top panel
     */
    public JPanel getTopPanel() {
        return GuiUtils.hbox(Misc.newList(toggleBtn, visCbx));
    }


    /**
     * Turn on/off all of the checkboxes held under this category
     *
     * @param toWhat What do we turn the checkboxes to
     */
    public void toggleAll(boolean toWhat) {
        visCbx.setSelected(toWhat);
        for (int i = 0; i < items.size(); i++) {
            ((JCheckBox) items.get(i)).setSelected(toWhat);
        }
    }

    /**
     * Turn on the vis checkbox if all sub elements are on
     */
    public void checkVisCbx() {
        boolean allOn  = true;
        boolean allOff = true;
        for (int i = 0; i < items.size(); i++) {
            if (((JCheckBox) items.get(i)).isSelected()) {
                allOff = false;
            } else {
                allOn = false;
            }
        }
        if (allOn) {
            visCbx.setSelected(true);
        }
        if (allOff) {
            visCbx.setSelected(false);
        }
    }

}


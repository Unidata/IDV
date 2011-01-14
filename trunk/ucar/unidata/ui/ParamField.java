/*
 * $Id: ParamField.java,v 1.4 2007/07/06 20:45:32 jeffmc Exp $
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
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A wrapper around a text field to show parameter names.
 * Has a  popup button to list param names and aliases.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public class ParamField extends JPanel {

    /** The text field */
    private JTextField field;

    /** The button */
    private JButton popupButton;

    /** Text delimiter */
    private String delimiter;

    /** also show aliases */
    private boolean includeAliases;

    /**
     * ctor
     *
     */
    public ParamField() {
        this(null, true);
    }


    /**
     * ctor
     *
     * @param delimiter delimiter, may be null
     */
    public ParamField(String delimiter) {
        this(delimiter, true);
    }


    /**
     * ctor
     *
     * @param delimiter delimiter, may be null
     * @param includeAliases alos show data alias names
     */
    public ParamField(String delimiter, boolean includeAliases) {
        this.includeAliases = includeAliases;
        this.delimiter      = delimiter;
        field = ucar.unidata.idv.ui.IdvUIManager.doMakeParamField(delimiter,
                includeAliases);
        this.setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER, field);
        popupButton =
            GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif", this,
                                     "showPopup");
        if (includeAliases) {
            popupButton.setToolTipText(
                "Click to seelct current parameters and aliases");
        } else {
            popupButton.setToolTipText("Click to seelct current parameters");
        }
        this.add(BorderLayout.EAST, popupButton);

    }

    /**
     * popup menu
     */
    public void showPopup() {
        List items =
            ucar.unidata.idv.ui.IdvUIManager.getParamsMenuItems(field,
                delimiter, includeAliases);
        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        popup.show(popupButton, 0, popupButton.getBounds().height);
    }

    /**
     * wrapper around textfield.addActionListener
     *
     * @param listener listener
     */
    public void addActionListener(ActionListener listener) {
        field.addActionListener(listener);
    }

    /**
     * Wrapper around textfield.setToolTipText
     *
     * @param tooltip tooltip
     */
    public void setToolTipText(String tooltip) {
        field.setToolTipText(tooltip);
    }


    /**
     *  Set the Text property.
     *
     *  @param value The new value for Text
     */
    public void setText(String value) {
        field.setText(value);
    }


    /**
     *  Get the Text property.
     *
     *  @return The Text
     */
    public String getText() {
        return field.getText();
    }



}


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

package ucar.unidata.idv.control;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Removable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



import java.util.ArrayList;
import java.util.List;

import javax.swing.*;



/**
 * A widget for a control.
 *
 * @author  Jeff McWhirter
 * @version $Revision: 1.15 $
 */
public class ControlWidget implements ActionListener, Removable {


    /** the associated control */
    DisplayControlImpl displayControl;

    /**
     * Construct a new ControlWidget
     *
     * @param displayControl    associated DisplayContol
     */
    public ControlWidget(DisplayControlImpl displayControl) {
        this.displayControl = displayControl;
    }

    /**
     *  Remove the reference to the displayControl
     */
    public void doRemove() {
        displayControl = null;
    }

    /**
     * Public as a result of implementing ActionListener
     *
     * @param ae   ActionEvent to act on
     */
    public void actionPerformed(ActionEvent ae) {}

    /**
     * Fill a list of widgets.
     *
     * @param l          list to fill
     * @param columns    number of columns for layout
     */
    public void fillList(List l, int columns) {}

    /**
     * Fill a list of widgets.
     *
     * @param components     components
     * @param widgets        widgets for components
     * @return  filled list
     */
    public static List fillList(List components, List widgets) {
        for (int i = 0; i < widgets.size(); i++) {
            ((ControlWidget) widgets.get(i)).fillList(components, 2);
        }
        return components;
    }

    /**
     * Fill a list of widgets.
     *
     * @param widgets  widgets to add to
     * @return  filled list
     */
    public static List fillList(List widgets) {
        return fillList(new ArrayList(), widgets);
    }


    /**
     * Get the display conventions from the contol
     * @return The {@link ucar.unidata.idv.DisplayConventions} to use.
     *
     */

    public DisplayConventions getDisplayConventions() {
        if (displayControl == null) {
            return null;
        }
        return displayControl.getDisplayConventions();
    }

    /**
     * Utility method to log an exception.
     *
     * @param message The message
     * @param exc The exception
     */
    public void logException(String message, Exception exc) {
        if (displayControl == null) {
            return;
        }
        displayControl.logException(message, exc);
    }

    /**
     * Utility method to notify the user with a message
     *
     * @param message The message
     */
    public void userMessage(String message) {
        if (displayControl == null) {
            return;
        }
        displayControl.userMessage(message);
    }


    /**
     * Get the DisplayControl that this widget is associated with.
     * @return the DisplayControl
     */
    public DisplayControlImpl getDisplayControl() {
        return displayControl;
    }
}

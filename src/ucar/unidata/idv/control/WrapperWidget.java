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


import ucar.unidata.util.GuiUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;




/**
 * Class for wrapping a set of widgets.
 *
 * @author  Jeff McWhirter
 * @version $Revision: 1.12 $
 */
public class WrapperWidget extends ControlWidget {

    /** first component */
    Component c1;

    /** second component */
    Component c2;

    /** third component */
    Component c3;


    /**
     * Wrap a component with a couple of labels
     *
     * @param control   control for widget
     * @param c1        component to wrap
     *
     */
    public WrapperWidget(DisplayControlImpl control, Component c1) {
        this(control, c1, new JLabel(" "), new JLabel(" "));
    }

    /**
     * Wrap two components with a label
     *
     * @param control   control for widget
     * @param c1        first component
     * @param c2        second component
     *
     */
    public WrapperWidget(DisplayControlImpl control, Component c1,
                         Component c2) {
        this(control, c1, c2, new JLabel(" "));
    }

    /**
     * Wrap three components.
     *
     * @param control   control for widget
     * @param c1        first component
     * @param c2        second component
     * @param c3        third component
     *
     */
    public WrapperWidget(DisplayControlImpl control, Component c1,
                         Component c2, Component c3) {
        super(control);
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
    }

    /**
     * Fill the list of components with this widgets components.
     *
     * @param l            list to fill
     * @param columns      number of columns
     */
    public void fillList(List l, int columns) {
        l.add(c1);
        if (c3 != null) {
            l.add(GuiUtils.doLayout(new Component[] { c2, new Label(" "),
                    c3 }, 3, GuiUtils.WT_YNY, GuiUtils.WT_N));
        } else {
            l.add(c2);
        }
    }

}

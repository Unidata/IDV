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
public interface ViewPanel {


    /** icon for map views */
    public static ImageIcon ICON_MAP =
        GuiUtils.getImageIcon("/auxdata/ui/icons/MapIcon.png",
                              ViewPanel.class);

    /** icon for transect views */
    public static ImageIcon ICON_TRANSECT =
        GuiUtils.getImageIcon("/auxdata/ui/icons/TransectIcon.png",
                              ViewPanel.class);

    /** icon for globe views */
    public static ImageIcon ICON_GLOBE =
        GuiUtils.getImageIcon("/auxdata/ui/icons/GlobeIcon.png",
                              ViewPanel.class);

    /** default icon */
    public static ImageIcon ICON_DEFAULT =
        GuiUtils.getImageIcon("/auxdata/ui/icons/Host24.gif",
                              ViewPanel.class);



    /**
     * Make, if needed, and return the contents
     *
     * @return the gui contents
     */
    public JComponent getContents();


    /**
     * Add the given display control
     *
     * @param control display control
     */
    public void addDisplayControl(DisplayControl control);


    /**
     * Be notified of the addition of a VM
     *
     * @param viewManager The VM
     */
    public void viewManagerAdded(ViewManager viewManager);


    /**
     * Called when the ViewManager is removed. If we are showing legends in
     * a separate window then we remove the tab
     *
     * @param viewManager The ViewManager that was destroyed
     */
    public void viewManagerDestroyed(ViewManager viewManager);


    /**
     * Called when the ViewManager is changed. If we are showing legends in
     * a separate window then we update the tab label
     *
     * @param viewManager The ViewManager that was changed
     */
    public void viewManagerChanged(ViewManager viewManager);





    /**
     * Called by the IDV when there has been a change to the display controls.
     *
     * @param displayControl The control that changed
     */
    public void displayControlChanged(DisplayControl displayControl);

    /**
     *
     * @param control The removed control
     */
    public void removeDisplayControl(DisplayControl control);



    /**
     * Add view menu items for the display control
     *
     * @param control the display control
     * @param items List of menu items
     */
    public void addViewMenuItems(DisplayControl control, List items);


    /**
     * _more_
     *
     * @param control _more_
     */
    public void controlMoved(DisplayControl control);

}

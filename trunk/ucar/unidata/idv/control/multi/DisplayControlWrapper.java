/*
 * $Id: DisplayControlWrapper.java,v 1.6 2007/04/16 21:32:37 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control.multi;


import ucar.unidata.data.DataChoice;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayControl;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.TableSorter;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;

import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * Provides a table view
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.6 $
 */
public class DisplayControlWrapper extends DisplayComponent {

    /** type of decoration */
    public static final int DECORATION_NONE = 0;

    /** type of decoration */
    public static final int DECORATION_BORDER = 1;

    /** type of decoration */
    public static final int DECORATION_TITLEDBORDER = 2;

    /** type of decoration */
    public static final int[] DECORATIONS = { DECORATION_NONE,
            DECORATION_BORDER, DECORATION_TITLEDBORDER };

    /** type of decoration */
    public static final String[] DECORATION_NAMES = { "None", "Border",
            "Titled Border" };

    /** type of decoration */
    private int decoration = DECORATION_TITLEDBORDER;


    /** My display control */
    DisplayControlImpl myDisplayControl;


    /** Component to put things in */
    JComponent myWrapper;

    /** Widget for properties */
    private JComboBox decorationBox;


    /**
     * Default ctor
     */
    public DisplayControlWrapper() {}

    /**
     * Default ctor
     *
     * @param myDisplayControl My control
     */
    public DisplayControlWrapper(DisplayControlImpl myDisplayControl) {
        this.myDisplayControl = myDisplayControl;
        this.myDisplayControl.addPropertyChangeListener(this);
        setName(myDisplayControl.getLabel());
    }



    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     *
     * @param displayControl The display  control I am part of
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     */
    public void initAfterUnPersistence(MultiDisplayHolder displayControl,
                                       ControlContext vc,
                                       Hashtable properties) {
        super.initAfterUnPersistence(displayControl, vc, properties);
        if (myDisplayControl != null) {
            myDisplayControl.initAfterUnPersistence(vc, properties);
            this.myDisplayControl.addPropertyChangeListener(this);
        }
    }


    /**
     * Handle the property change event
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(DisplayControlImpl.PROP_REMOVED)) {
            if (myDisplayControl != null) {
                myDisplayControl = null;
                DisplayGroup displayGroup = getDisplayGroup();
                if (displayGroup != null) {
                    displayGroup.removeDisplayComponent(this);
                }
                getDisplayControl().removeDisplayComponent(this);
            }
        } else {
            super.propertyChange(event);
        }
    }


    /**
     * Make the properties gui
     *
     * @param comps  List of components
     * @param tabIdx Which tab in the gui
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx == 0) {
            decorationBox = GuiUtils.makeComboBox(DECORATIONS,
                    DECORATION_NAMES, decoration);
            comps.add(GuiUtils.rLabel("Decoration: "));
            comps.add(decorationBox);
        }
    }

    /**
     * Apply properties
     *
     *
     * @return Successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        decoration = GuiUtils.getValueFromBox(decorationBox);
        applyDecoration();
        return true;
    }


    /**
     * Apply the border decoration
     */
    private void applyDecoration() {
        if (decoration == DECORATION_TITLEDBORDER) {
            myWrapper.setBorder(BorderFactory.createTitledBorder(getName()));
        } else if (decoration == DECORATION_BORDER) {
            myWrapper.setBorder(BorderFactory.createEtchedBorder());
        } else {
            myWrapper.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    /**
     * Add to the popup menu
     *
     * @param items List of menu items
     *
     * @return The list
     */
    protected List getPopupMenuItems(List items) {
        JMenu menu;
        List  displayItems;

        items.add(menu = new JMenu("File"));
        GuiUtils.makeMenu(
            menu,
            ((DisplayControlImpl) myDisplayControl).getFileMenuItems(
                new ArrayList()));

        items.add(menu = new JMenu("Edit"));
        GuiUtils.makeMenu(
            menu,
            ((DisplayControlImpl) myDisplayControl).getEditMenuItems(
                new ArrayList()));

        items.add(menu = new JMenu("View"));
        GuiUtils.makeMenu(
            menu,
            ((DisplayControlImpl) myDisplayControl).getViewMenuItems(
                new ArrayList()));

        items.add(GuiUtils.MENU_SEPARATOR);
        super.getPopupMenuItems(items);
        return items;
    }



    /**
     * Return the human readable name of this component
     *
     * @return component type name
     */
    public String getTypeName() {
        return "Display";
    }






    /**
     * Cleanup the component
     */
    public void doRemove() {
        if (myDisplayControl != null) {
            try {
                myDisplayControl.removePropertyChangeListener(this);
                DisplayControlImpl tmp = myDisplayControl;
                myDisplayControl = null;
                tmp.doRemove();
            } catch (Exception exc) {
                LogUtil.logException("Removing display", exc);
            }
        }
        super.doRemove();
    }


    /**
     * make the gui
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        if (myWrapper == null) {
            JComponent innerComp =
                (JComponent) ((DisplayControlImpl) myDisplayControl)
                    .getMainPanel();
            myWrapper = GuiUtils.center(innerComp);
            applyDecoration();
        }
        return myWrapper;
    }




    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return myDisplayControl.toString();
    }


    /**
     * Set the DisplayControl property.
     *
     * @param value The new value for DisplayControl
     */
    public void setMyDisplayControl(DisplayControlImpl value) {
        myDisplayControl = value;
    }

    /**
     * Get the DisplayControl property.
     *
     * @return The DisplayControl
     */
    public DisplayControlImpl getMyDisplayControl() {
        return myDisplayControl;
    }

    /**
     * Set the Decoration property.
     *
     * @param value The new value for Decoration
     */
    public void setDecoration(int value) {
        decoration = value;
    }

    /**
     * Get the Decoration property.
     *
     * @return The Decoration
     */
    public int getDecoration() {
        return decoration;
    }



}


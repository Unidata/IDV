/*
 * $Id: DisplayComponent.java,v 1.11 2007/04/16 21:32:37 jeffmc Exp $
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


import ucar.unidata.collab.PropertiedThing;

import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.ui.ImageSequenceGrabber;

import ucar.unidata.ui.ImageUtils;


import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.*;
import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * Base class of things that are shown in the MultiDisplayHolder
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.11 $
 */
public abstract class DisplayComponent extends PropertiedThing implements PropertyChangeListener {

    /** Property change id */
    public static final String PROP_REMOVED = "prop.removed";


    /** Has this component been removed */
    public boolean isRemoved = false;

    /** Has this component been initialized */
    protected boolean hasBeenInitialized = false;

    /** Action command to save an image_ */
    public static final String CMD_SAVEIMAGE = "Save As...";

    /** Action command to save an moveie */
    public static final String CMD_SAVEMOVIE = "Save As Movie...";

    /** The group I'm in */
    private DisplayGroup displayGroup;


    /** Used in properties dialog for name */
    private JTextField propertiesNameFld;

    /** properties widget */
    private JCheckBox labelShownCbx;



    /** The label */
    protected JLabel displayLabel;

    /** Are we showing the label. DisplayGroups can turn this off. */
    private boolean labelShown = true;

    /** name */
    private String name = null;

    /** Outer contents */
    private JComponent contents;

    /** color */
    private Color backgroundColor = Color.white;

    /** The displaycontrol I'm in */
    protected MultiDisplayHolder displayControl;


    /**
     * default ctor
     */
    public DisplayComponent() {}


    /**
     * ctor
     *
     * @param name name
     */
    public DisplayComponent(String name) {
        this.name = name;
    }


    /**
     * Finish with initialization
     */
    public void initDone() {
        hasBeenInitialized = true;
    }


    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     *
     * @param displayControl The display control I am part of
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     */
    public void initAfterUnPersistence(MultiDisplayHolder displayControl,
                                       ControlContext vc,
                                       Hashtable properties) {
        this.displayControl = displayControl;
        hasBeenInitialized  = true;
    }


    /**
     * Make the gui
     *
     * @return The gui
     */
    protected abstract JComponent doMakeContents();


    /**
     * Create, if needed, and return the gui contents
     *
     * @return gui contents
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    /**
     * Set the display control I'm in
     *
     * @param displayControl The display control
     */
    public void setDisplayControl(MultiDisplayHolder displayControl) {
        this.displayControl = displayControl;
    }


    /**
     * Get the display control I'm in
     *
     * @return  The display control
     */
    protected MultiDisplayHolder getDisplayControl() {
        return displayControl;
    }



    /**
     * Set animation time on components
     *
     * @param time time
     */
    public void animationTimeChanged(visad.Real time) {}



    /**
     * Tell components to load
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws RemoteException, VisADException {}


    /**
     * Create a movie
     */
    public void doSaveMovie() {
        ImageSequenceGrabber isg =
            new ImageSequenceGrabber(getDisplayControl().getViewManager(),
                                     getContents());
    }


    /**
     * Write the image
     */
    public void doSaveImage() {
        String filename =
            FileManager.getWriteFile(FileManager.FILTER_IMAGEWRITE,
                                     FileManager.SUFFIX_JPG);
        if (filename != null) {
            try {
                ImageUtils.writeImageToFile(getContents(), filename);
            } catch (Exception exc) {
                LogUtil.logException("Error writing image", exc);
            }
        }
    }


    /**
     * Add the parent group's menu items
     *
     *
     * @param items menu items
     *
     * @return the items
     */
    protected List addGroupMenuItems(List items) {
        if (displayGroup != null) {
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(
                GuiUtils.makeMenu(
                    "Parent " + displayGroup.toString(),
                    displayGroup.getPopupMenuItems(new ArrayList())));
        }
        return items;
    }


    /**
     * Get the list of displayables
     *
     * @return empty list
     */
    public List getDisplayables() {
        return new ArrayList();
    }


    /**
     * Remove me
     *
     * @return was removed
     */
    public boolean removeDisplayComponent() {
        if (GuiUtils.askYesNo("Remove Display",
                              "Are you sure you want to remove: "
                              + toString())) {
            DisplayGroup displayGroup = getDisplayGroup();
            if (displayGroup != null) {
                displayGroup.removeDisplayComponent(this);
            }
            getDisplayControl().removeDisplayComponent(this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Apply the properties
     *
     * @return Success
     */
    protected boolean doApplyProperties() {
        if ( !super.doApplyProperties()) {
            return false;
        }
        getDisplayControl().componentChanged();
        return true;
    }



    /**
     * Been removed, do any cleanup
     */
    public void doRemove() {
        isRemoved = true;
        List displayables = getDisplayables();
        if (displayables.size() > 0) {
            getDisplayControl().removeDisplayables(displayables);
        }
        firePropertyChange(PROP_REMOVED, null, this);
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
        if (displayLabel != null) {
            displayLabel.setText(value);
        }

    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }



    /**
     *  Set the DisplayGroup property.
     *
     *  @param value The new value for DisplayGroup
     */
    public void setDisplayGroup(DisplayGroup value) {
        displayGroup = value;
    }

    /**
     *  Get the DisplayGroup property.
     *
     *  @return The DisplayGroup
     */
    public DisplayGroup getDisplayGroup() {
        return displayGroup;
    }




    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return name;
    }

    /**
     * Set the BackgroundColor property.
     *
     * @param value The new value for BackgroundColor
     */
    public void setBackgroundColor(Color value) {
        backgroundColor = value;
        if (contents != null) {
            //      contents.setBackground(backgroundColor);
        }
    }

    /**
     * Get the BackgroundColor property.
     *
     * @return The BackgroundColor
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }


    /**
     * Return the human readable name of this component
     *
     * @return component type name
     */
    public String getTypeName() {
        return "";
    }



    /**
     * Create the properties contents
     *
     * @param comps  List of components
     * @param tabIdx Which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 0) {
            return;
        }
        propertiesNameFld = new JTextField(getName());
        labelShownCbx     = new JCheckBox("Label Shown", getLabelShown());
        comps.add(GuiUtils.rLabel("Name: "));
        comps.add(GuiUtils.centerRight(propertiesNameFld, labelShownCbx));
    }


    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        setLabelShown(labelShownCbx.isSelected());
        setName(propertiesNameFld.getText().trim());
        return true;
    }




    /**
     * Get the menu items for the popup menu
     *
     * @param items List of items to add to
     *
     * @return The items list
     */
    protected List getPopupMenuItems(List items) {
        items.add(GuiUtils.makeMenuItem("Remove " + getTypeName(), this,
                                        "removeDisplayComponent"));
        return items;
    }




    /**
     * Show the properties dialog
     *
     * @return Was it ok
     */
    public boolean showProperties() {
        return showProperties(displayControl.getDisplayTree(), 0, 0);
    }


    /**
     * SHow the popup menu
     *
     * @param where component to show near to
     * @param x x
     * @param y y
     */
    public void showPopup(JComponent where, int x, int y) {
        List items = new ArrayList();
        getPopupMenuItems(items);
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Properties...", this,
                                        "showProperties"));
        //        addGroupMenuItems(items);
        if (items.size() == 0) {
            return;
        }
        GuiUtils.makePopupMenu(items).show(where, x, y);
    }


    /**
     * Create, if needed, and return the component label
     *
     * @return component label
     */
    protected JLabel getDisplayLabel() {
        return doMakeDisplayLabel();
    }


    /**
     * Create, if needed, and return the component label
     *
     * @return component label
     */
    protected JLabel doMakeDisplayLabel() {
        if (displayLabel == null) {
            displayLabel = GuiUtils.cLabel(getName());

            Font f = displayLabel.getFont();
            f = f.deriveFont(18.0f);
            displayLabel.setFont(f);
            if ( !labelShown) {
                displayLabel.setVisible(false);
            }
        }
        return displayLabel;
    }



    /**
     * Set the LabelShown property.
     *
     * @param value The new value for LabelShown
     */
    public void setLabelShown(boolean value) {
        labelShown = value;
        if (displayLabel != null) {
            displayLabel.setVisible(value);
        }
    }

    /**
     * Get the LabelShown property.
     *
     * @return The LabelShown
     */
    public boolean getLabelShown() {
        return labelShown;
    }



    /**
     *  Set the IsRemoved property.
     *
     *  @param value The new value for IsRemoved
     */
    public void setIsRemoved(boolean value) {
        isRemoved = value;
    }

    /**
     *  Get the IsRemoved property.
     *
     *  @return The IsRemoved
     */
    public boolean getIsRemoved() {
        return isRemoved;
    }


}


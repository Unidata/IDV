/*
 * $Id: PropertiedThing.java,v 1.1 2005/10/20 20:47:33 jeffmc Exp $
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


package ucar.unidata.collab;


import ucar.unidata.collab.SharableImpl;



import ucar.unidata.data.DataChoice;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionRect;




import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import visad.util.BaseRGBMap;

import visad.util.ColorPreview;



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.beans.*;


import java.rmi.RemoteException;







import java.text.SimpleDateFormat;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * This abstract class handles showing a properties dialog
 * and manages a set of property change listeners.
 *
 * @author IDV Development Team
 * @version $Revision: 1.1 $
 */
public abstract class PropertiedThing extends SharableImpl implements PropertyChangeListener {


    /**
     * The PropertyChangeListener-s.
     */
    private volatile PropertyChangeSupport propertyListeners;


    /** properties */
    protected JDialog propertiesDialog;


    /**
     * Default ctro
     */
    public PropertiedThing() {}



    /**
     * Handle the property change event
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {}


    /**
     * Fires a PropertyChangeEvent.
     * @param event             The PropertyChangeEvent.
     */
    public void firePropertyChange(PropertyChangeEvent event) {
        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(event);
        }
    }

    /**
     * Fires a PropertyChangeEvent.
     * @param propertyName      The name of the property.
     * @param oldValue          The old value of the property.
     * @param newValue          The new value of the property.
     */
    public void firePropertyChange(String propertyName, Object oldValue,
                                   Object newValue) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(propertyName, oldValue,
                    newValue);
        }
    }



    /**
     * Removes a PropertyChangeListener from this instance.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(listener);
        }
    }

    /**
     * Returns the PropertyChangeListener-s of this instance.
     * @return                  The PropertyChangeListener-s.
     */
    protected PropertyChangeSupport getPropertyListeners() {

        if (propertyListeners == null) {
            synchronized (this) {
                if (propertyListeners == null) {
                    propertyListeners = new PropertyChangeSupport(this);
                }
            }
        }

        return propertyListeners;
    }


    /**
     * Adds a PropertyChangeListener to this instance.
     *
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyListeners().addPropertyChangeListener(listener);
    }





    /**
     * Make the properties gui
     *
     * @param comps  List of components
     * @param tabIdx Which tab in the gui
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {}


    /**
     * return the array of tab names for the proeprties dialog
     *
     * @return array of tab names
     */
    public String[] getPropertyTabs() {
        return new String[] { "" };
    }

    /**
     * Show the properties dialog
     *
     * @return Success
     */
    public boolean showProperties() {
        return showProperties(null, 0, 0);
    }


    /**
     * _more_
     *
     * @param where _more_
     * @param x _more_
     * @param y _more_
     *
     * @return _more_
     */
    public boolean showProperties(JComponent where, int x, int y) {

        if (isShowing()) {
            return false;
        }
        try {
            String[]   tabs = getPropertyTabs();
            JComponent contents;
            if (tabs.length <= 1) {
                List comps = new ArrayList();
                getPropertiesComponents(comps, 0);
                GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
                contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                             GuiUtils.WT_N);
            } else {
                JTabbedPane tabbedPane = new JTabbedPane();
                for (int tabIdx = 0; tabIdx < tabs.length; tabIdx++) {
                    List comps = new ArrayList();
                    getPropertiesComponents(comps, tabIdx);
                    GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
                    JComponent comp = GuiUtils.top(GuiUtils.doLayout(comps,
                                          2, GuiUtils.WT_NY, GuiUtils.WT_N));
                    tabbedPane.add(tabs[tabIdx], GuiUtils.inset(comp, 5));

                }
                contents = tabbedPane;
            }



            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String cmd = ae.getActionCommand();
                    if (cmd.equals(GuiUtils.CMD_OK)
                            || cmd.equals(GuiUtils.CMD_APPLY)) {
                        if ( !doApplyProperties()) {
                            return;
                        }
                    }
                    if (cmd.equals(GuiUtils.CMD_OK)
                            || cmd.equals(GuiUtils.CMD_CANCEL)) {
                        propertiesDialog.dispose();
                        propertiesDialog = null;
                    }
                }
            };
            JPanel buttons = GuiUtils.makeButtons(listener,
                                 new String[] { GuiUtils.CMD_APPLY,
                    GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });

            contents = GuiUtils.centerBottom(contents, buttons);
            contents = GuiUtils.inset(contents, 5);
            propertiesDialog = GuiUtils.createDialog("Properties: "
                    + toString(), true);
            propertiesDialog.getContentPane().add(GuiUtils.top(contents));
            propertiesDialog.pack();

            if (where != null) {
                Point loc = where.getLocationOnScreen();
                loc.x += x;
                loc.y += y;
                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                //Offset a bit for the icon bar
                screenSize.height -= 50;
                Dimension windowSize = propertiesDialog.getSize();

                if (loc.y + windowSize.height > screenSize.height) {
                    loc.y = screenSize.height - windowSize.height;
                }
                if (loc.x + windowSize.width > screenSize.width) {
                    loc.x = screenSize.width - windowSize.width;
                }
                propertiesDialog.setLocation(loc);
            }
            propertiesDialog.show();
            propertiesDialog = null;
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Creating properties dialog", exc);
            return false;
        }


    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isShowing() {
        return propertiesDialog != null;
    }

    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        return true;
    }


    /**
     * Apply the properties
     *
     *
     * @return Was ok
     */
    protected boolean doApplyProperties() {
        if ( !applyProperties()) {
            return false;
        }
        return true;
    }



}


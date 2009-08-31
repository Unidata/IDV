/*
 * $Id: IdvLegend.java,v 1.30 2006/12/01 19:54:03 jeffmc Exp $
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

package ucar.unidata.idv.ui;


import ucar.unidata.idv.*;


import ucar.unidata.idv.ui.*;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;




/**
 * An abstract  base class used to represents display control legends
 * for view managers.
 *
 * @author IDV development team
 */

public abstract class IdvLegend {

    public static final String STATE_HIDDEN = "hidden";
    public static final String STATE_DOCKED = "docked";
    public static final String STATE_FLOAT  = "float";


    public static final ImageIcon  ICON_FLOAT = GuiUtils.getImageIcon("/auxdata/ui/icons/application_get.png");
    public static final ImageIcon  ICON_DOCKED = GuiUtils.getImageIcon("/auxdata/ui/icons/application_put.png");

    /** The icon used to bring up the display properties window */
    protected static ImageIcon ICON_PROPERTIES;

    /** Icon used to remove displays */
    public static ImageIcon ICON_REMOVE;


    static {
        ICON_PROPERTIES = new ImageIcon(
            Resource.getImage("/auxdata/ui/icons/information.png"));
        ICON_REMOVE = new ImageIcon(
            Resource.getImage("/auxdata/ui/icons/delete.png"));
    }


    /** The {@link ucar.unidata.idv.ViewManager} this legend is part of */
    protected ViewManager viewManager;

    /** This is the gui component in which this legend is placed in its parent window */
    private JComponent container;

    /** This is the main GUI contents */
    private JComponent contents;

    /** The button used to float/embed this legend */
    private JButton floatBtn;

    /** If floating, this is the window we are floating in */
    private JFrame floatFrame;

    /** Are we active or has the ViewManager we are part of  been closed */
    private boolean closed = false;


    private Point lastLocation = new Point(20,20);


    /**
     * Parameterless constructor for xml persistence
     */
    public IdvLegend() {}


    /**
     * Create the legend with the given {@link ucar.unidata.idv.ViewManager}
     *
     * @param viewManager The view manager this legend is a part of
     *
     */
    public IdvLegend(ViewManager viewManager) {
        this.viewManager = viewManager;
    }


    /**
     * Create, if needed, and return the GUI contents.
     *
     * @return The GUI contents
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }


    /**
     * Set the view manager that this legend is part of
     *
     * @param viewManager The view manager
     */
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }


    /**
     * Set the Container property. This is the container in the
     * parent ViewManager  that this legend is shown in.
     * This method is differently named from the getContainer method
     * so that the value is not persisted when doing xml encoding.
     *
     * @param value The new value for Container
     */
    public void setTheContainer(JComponent value) {
        container = value;
    }

    /**
     * Get the Container property.
     *
     * @return The Container
     */
    public JComponent getContainer() {
        return container;
    }


    /**
     * No-op. To be overrode by any subclasses that might use the
     * colors of the ViewManager
     *
     * @param foreground The foreground color of the ViewManager
     * @param background The background color of the ViewManager
     */
    public void setColors(Color foreground, Color background) {}


    /**
     * Create, if needed, and return the floatButton.
     *
     * @return The float button
     */
    protected JButton getFloatButton() {
        if (floatBtn == null) {
            floatBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/Export16.gif",
                                        getClass());
            setFloatToolTip();
            floatBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if(floatFrame!=null) {
                        viewManager.setLegendState(IdvLegend.this,STATE_DOCKED);
                    } else {
                        viewManager.setLegendState(IdvLegend.this,STATE_FLOAT);
                    }
                }
            });
        }
        return floatBtn;
    }


    /**
     * This will float the legend.
     */
    public void showLegend() {
        if (closed) {
            return;
        }
        if (floatFrame == null) {
            floatLegend();
        } else if (floatFrame != null) {
            floatFrame.setState(Frame.NORMAL);
            floatFrame.show();
        }
        setFloatToolTip();
    }

    /**
     *  Set the tool tip text on the float button
     * to show the current float state.
     */
    private void setFloatToolTip() {
        if (floatBtn != null) {
            if (floatFrame!=null) {
                floatBtn.setIcon(ICON_DOCKED);
                floatBtn.setToolTipText("Embed the legend");
            } else {
                floatBtn.setIcon(ICON_FLOAT);
                floatBtn.setToolTipText("Float the legend");
            }
        }
    }

    /**
     * To be overrode by derived classes to return the actual GUI contents.
     *
     * @return The GUI contents
     */
    protected abstract JComponent doMakeContents();


    /** For synchronizing when filling the legend */
    private Object MUTEX = new Object();


    /**
     *  This is called when there is a change to the list of DisplayControls shown in this legend.
     *  It simply is a wrapper around fillLegendSafely, synchronizing on a MUTEX lock.
     */
    public final void fillLegend() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fillLegendInner();
                }
            });

    }


    private final void fillLegendInner() {
        synchronized (MUTEX) {
            fillLegendSafely();
            if (contents != null) {
                contents.validate();
                contents.repaint();
            }
        }
    }

    /**
     * To be overrode by derived classes to fill the legend. This is called
     * from {@link #fillLegend()} within a synchronized block.
     */
    protected abstract void fillLegendSafely();




    /**
     * Create an icon  button for showing the window for the given display control.
     *
     * @param control The display control
     * @return The button used to show the window
     */
    protected JButton makePropertiesButton(final DisplayControl control) {
        JButton propertiesBtn = GuiUtils.getImageButton(ICON_PROPERTIES);
        propertiesBtn.setToolTipText("Show or hide the control window");
        propertiesBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    control.toggleWindow();
                } catch (Exception exc) {
                    viewManager.logException("Showing display", exc);
                }
            }
        });
        //Set the bg to null so we pick up the container's bgcolor
        propertiesBtn.setBackground(null);
        return propertiesBtn;
    }

    /**
     *  The ViewManager in which thie legend is a part of has been closed.
     *  This method disposes of the floatFrame if it is non-null.
     */
    public void doClose() {
        closed = true;
        if (floatFrame != null) {
            floatFrame.dispose();
            floatFrame = null;
        }
    }


    /**
     * Get the window title to use.
     *
     * @return The window title
     */
    protected String getTitle() {
        return "Display Legend";
    }


    public void unFloatLegend() {
        if(floatFrame!=null) {
            lastLocation = floatFrame.getLocation();
            floatFrame.dispose();
            floatFrame = null;
            setFloatToolTip();
        }
    }


    /**
     * Float the legend in its own window.
     *
     */
    public void floatLegend() {
        floatFrame = GuiUtils.createFrame(getTitle());
        floatFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                lastLocation = floatFrame.getLocation();
                viewManager.setLegendState(IdvLegend.this,STATE_HIDDEN);
                floatFrame = null;
            }
        });
        floatFrame.getContentPane().add(getContents());
        floatFrame.pack();
        floatFrame.setLocation(lastLocation);
        floatFrame.show();
        setFloatToolTip();
    }


}


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

package ucar.unidata.idv;


import ucar.unidata.data.DataCategory;



import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceFactory;
import ucar.unidata.data.DerivedDataChoice;



import ucar.unidata.idv.ui.*;


import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;
import ucar.unidata.util.SerializedObjectStore;
import ucar.unidata.util.SuffixFileFilter;


import ucar.visad.display.DisplayMaster;

import visad.*;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.lang.reflect.Constructor;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;





/**
 * An example IDV application that supports muliple view panes
 * in the same window, ala AWIPS
 *
 * @author IDV development team
 */


public class MultiPaneIdv extends IntegratedDataViewer {


    /** Holds the main view window */
    JPanel mainPanel;

    /** Where we put the non main view windows */
    JPanel leftPanel;

    /** The {@link ViewManager} in the main panel */
    ViewManager currentViewManager;

    /** The other {@link ViewManager}s */
    ArrayList otherViewManagers = new ArrayList();

    /** The main window contents */
    JPanel contents;

    /** Enables clicking on the non-main views */
    JCheckBox doClickCbx;

    /** A count of the view managers that have been created */
    int viewManagerCnt = 0;



    /**
     * Create this object with the given command line arguments
     *
     * @param args command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     *
     */
    public MultiPaneIdv(String[] args)
            throws VisADException, RemoteException {
        super(args);
        init();
    }


    /**
     * Override the base class method to always return
     * current {@link ViewManager}
     *
     * @param viewDescriptor Ignore this
     * @return The current view manager
     */
    public ViewManager getViewManager(ViewDescriptor viewDescriptor) {
        return currentViewManager;
    }

    /**
     * Class ViewManagerListener allows us to route mouseclicks
     * from the display  to the MultiPaneIdv class
     *
     *
     * @author IDV development team
     */
    class ViewManagerListener implements DisplayListener {

        /** The ViewManager we listen to */
        ViewManager viewManager;

        /** The IDV */
        MultiPaneIdv idv;

        /**
         * Create me.
         *
         * @param idv The IDV
         * @param viewManager The view manager we listen to
         *
         */
        public ViewManagerListener(MultiPaneIdv idv,
                                   ViewManager viewManager) {
            this.viewManager = viewManager;
            this.idv         = idv;
        }

        /**
         * process the event from the display
         *
         * @param e The <code>DisplayEvent</code>
         */
        public void displayChanged(DisplayEvent e) {
            if (e.isRemote()) {
                return;
            }
            if (e.getId() == DisplayEvent.MOUSE_PRESSED_RIGHT) {
                idv.viewClicked(viewManager, e);
            }
        }
    }

    /**
     * The user has clicked on the given viewManager. If it is
     * not the current view manager than swap it in.
     *
     * @param viewManager The view manager that was clicked on
     * @param e The <code>DisplayEvent</code>
     */
    public void viewClicked(ViewManager viewManager, DisplayEvent e) {
        if (viewManager == currentViewManager) {
            return;
        }
        int idx = otherViewManagers.indexOf(viewManager);
        otherViewManagers.remove(viewManager);
        otherViewManagers.add(idx, currentViewManager);
        currentViewManager = viewManager;
        updateViewManagers();
    }



    /**
     * Make the gui contents
     *
     * @param frame The main window
     * @return The  main gui contents
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public Component doMakeContents(IdvWindow frame)
            throws VisADException, RemoteException {
        doClickCbx = new JCheckBox("Enable click", true);
        JPanel topLeft = new JPanel();
        topLeft.add(doClickCbx);

        currentViewManager = getViewManager(new ViewDescriptor("awips"
                + (++viewManagerCnt)), false, getViewManagerProperties());
        currentViewManager.addDisplayListener(new ViewManagerListener(this,
                currentViewManager));
        for (int i = 0; i < 3; i++) {
            ViewManager other = getViewManager(new ViewDescriptor("awips"
                                    + (++viewManagerCnt)), false,
                                        getViewManagerProperties());
            other.addDisplayListener(new ViewManagerListener(this, other));
            otherViewManagers.add(other);
        }

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        //      treeScroller.setPreferredSize (new Dimension (200,400));
        //      JFrame treeWindow = GuiUtils.makeWindow  ("Data sources", treeScroller);
        //        treeWindow.setDefaultCloseOperation (WindowConstants.DO_NOTHING_ON_CLOSE);

        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));


        contents = new JPanel(new BorderLayout());
        JPanel leftWrapper = new JPanel();
        GuiUtils.doLayout(leftWrapper, new Component[] { topLeft,
                GuiUtils.inset(leftPanel, 2) }, 1, GuiUtils.WT_Y,
                GuiUtils.WT_NY);
        contents.add("West", leftWrapper);
        contents.add("Center", mainPanel);
        updateViewManagers();
        return contents;
    }

    /**
     * Override the base class method to create a new view manager and
     * put it into the main window.
     */
    public void createNewWindow() {
        try {
            ViewManager other = getViewManager(new ViewDescriptor("awips"
                                    + (++viewManagerCnt)), false,
                                        getViewManagerProperties());
            other.addDisplayListener(new ViewManagerListener(this, other));
            otherViewManagers.add(other);
            updateViewManagers();
        } catch (Exception excp) {
            logException("createNewWindow", excp);
        }
    }


    /**
     * Layout the view managers in the window. We actually
     * call {@link #updateViewManagersInner()} in a thread.
     */
    public void updateViewManagers() {
        //      updateViewManagersInner ();
        Misc.runInABit(1, new Runnable() {
            public void run() {
                updateViewManagersInner();
            }
        });
    }

    /** An icon to use */
    ImageIcon downArrowIcon;

    /**
     * Layout the view managers in the window.
     */
    public synchronized void updateViewManagersInner() {
        if (downArrowIcon == null) {
            downArrowIcon = new ImageIcon(
                Resource.getImage("/auxdata/ui/icons/SortDown.gif"));
        }
        mainPanel.removeAll();
        leftPanel.removeAll();
        mainPanel.add(currentViewManager.getContents());
        for (int i = 0; i < otherViewManagers.size(); i++) {
            ViewManager other   = (ViewManager) otherViewManagers.get(i);
            JLabel      menuBtn = new JLabel(downArrowIcon);
            menuBtn.addMouseListener(
                new ObjectListener(new ObjectArray(other, menuBtn)) {
                public void mouseClicked(MouseEvent e) {
                    ViewManager vm =
                        (ViewManager) ((ObjectArray) theObject).getObject1();
                    Component src =
                        (Component) ((ObjectArray) theObject).getObject2();
                    JPopupMenu menu =
                        GuiUtils.makePopupMenu(vm.doMakeMenuList());
                    menu.show(src, e.getX(), e.getY());
                }
            });

            JComponent otherContents = (JComponent) other.getInnerContents();
            otherContents.setPreferredSize(new Dimension(100, 100));
            JPanel p = GuiUtils.topCenter(menuBtn, otherContents);
            p.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,
                    Color.gray, Color.gray));
            leftPanel.add(p);
        }
        contents.revalidate();
    }




    /**
     * The main
     *
     * @param args Command line arguments
     *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        LogUtil.configure();
        //Create the default idv app, passing the args array
        MultiPaneIdv idv = new MultiPaneIdv(args);
    }


}

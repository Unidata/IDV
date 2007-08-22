/*
 * $Id: ExampleUIManager.java,v 1.8 2007/06/19 22:19:01 jeffmc Exp $
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

package ucar.unidata.apps.example;


import ucar.unidata.idv.*;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.ui.XmlUi;

import ucar.unidata.idv.ui.*;
import ucar.unidata.data.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import ucar.unidata.idv.ui.*;


import visad.VisADException;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import org.w3c.dom.Element;


/**
 * Derive our own ui manager to do some  example specific things.
 */

public class ExampleUIManager extends IdvUIManager {

    /** Do we change how we create windows */
    boolean testNewWindow = false;

    /** Do we change the order of the quicklinks */
    boolean testQuicklinkChange = true;



    /** The tag in the xml ui for creating the special example chooser */
    public static final String TAG_EXAMPLECHOOSER = "examplechooser";


    /**
     * The ctor. Just pass along the reference to the idv.
     *
     * @param idv The idv
     */
    public ExampleUIManager(IntegratedDataViewer idv) {
        super(idv);
    }



    /**
     * Create a new window
     *
     * @param viewManagers The view managers to put in the window.
     * @param notifyCollab Should we tell the collab facility
     * @param title        The title
     * @param skinPath The skin. May be null.
     * @param skinRoot Root of the skin xml. May be null.
     *
     * @return The window.
     */
    public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
                                     String title, String skinPath,
                                     Element skinRoot) {
        if(!testNewWindow)  {
            return super.createNewWindow(viewManagers, notifyCollab,title, skinPath, skinRoot);
        }
        try {
            if (viewManagers == null) {
                viewManagers = new ArrayList();
            }
            //Create the view manager if needed
            MapViewManager mapViewManager;
            if (viewManagers.size() == 0) {
                mapViewManager = new MapViewManager(getIdv(),
                                                    new ViewDescriptor(),
                                                    null);
                viewManagers.add(mapViewManager);
                getVMManager().addViewManager(mapViewManager);
            } else {
                mapViewManager = (MapViewManager) viewManagers.get(0);
            }


            IdvWindow window = new IdvWindow(getStateManager().getTitle(),
                                             getIdv(), true);

            //Create the menu bar. This comes from the defaultmenubar.xml resource
            JMenuBar menuBar = doMakeMenuBar();
            if (menuBar != null) {
                window.setJMenuBar(menuBar);
            }
            JComponent toolbar = getToolbarUI();

            //Throw some stuff in a tabbed pane
            //Note: heavyweight components like the 3D display does not
            //behave well in tabbed panes.
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.add("Page 1", mapViewManager.getContents());
            tabbedPane.add("Page 2", new JLabel("page2 stuff"));

            JComponent contents = GuiUtils.topCenter(toolbar, tabbedPane);
            window.setContents(contents);

            //Tell the window what view managers it has.
            window.setTheViewManagers(viewManagers);

            //Show the window if needed
            if (getIdv().okToShowWindows()) {
                window.show();
            }

            return window;
        } catch (Exception exc) {
            logException("Creating ui", exc);
            return null;

        }
    }



    /**
     * If we are using skins this method allos us to create special skin components
     * Factory method to create an xmlui.
     * We override the base class method (in IdvUIManager)
     * to create our own uixml object that knows how to handle
     * our special ui tags.
     *
     * @param window The window we will be putting the ui in
     * @param viewManagers The list of view managers. This may already
     * have view managers in them (if we are recreating a window from
     * bundle). Else it will be empty if we are just  creating a new window.
     * @param skinRoot The skin xml
     *
     * @return The xmlui
     */
    protected IdvXmlUi doMakeIdvXmlUi(IdvWindow window, List viewManagers,
                                      Element skinRoot) {
        return new IdvXmlUi(window, viewManagers, getIdv(), skinRoot) {
            public Component createComponent(Element node, String id) {
                return super.createComponent(node, id);
            }
        };
    }



}








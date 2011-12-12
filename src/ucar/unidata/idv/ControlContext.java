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






import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataSource;

import ucar.unidata.idv.ui.IdvWindow;

import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.xml.XmlObjectStore;

import ucar.visad.display.DisplayMaster;

import visad.VisADException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;


/**
 * This interface is how {@link DisplayControl}s view the
 * {@link IntegratedDataViewer}. We use an interface so that,
 * instead of just passing the IDV as itself to the display controls,
 * we can keep tabs on what IDV methods are used by the display controls.
 * It is a way of keeping us somewhat honest.
 *
 *
 * @author IDV development team
 */


public interface ControlContext extends DataContext {

    /**
     * a
     * Add the given {@link DisplayControl}
     *
     * @param control The new display control
     */
    public void addDisplayControl(DisplayControl control);

    /**
     * Remove the given {@link DisplayControl}
     *
     * @param control The removed display control
     */
    public void removeDisplayControl(DisplayControl control);



    /**
     * Create, if needed, and return the {@link ViewManager}
     * identified by the given {@link ViewDescriptor}
     *
     * @param viewDescriptor The view descriptor that defines the view manager
     * being looked for
     * @return The view manager
     */
    public ViewManager getViewManager(ViewDescriptor viewDescriptor);



    /**
     * Create, if needed, and return the {@link ViewManager}
     * identified by the given {@link ViewDescriptor}
     *
     * @param viewDescriptor The view descriptor that defines the view manager
     * being looked for
     *
     * @param newWindow If true it will create a new window and place the ViewManager in it
     * if the ViewManager was newly created (as opposed to one that already exists).
     *
     * @param properties Semicolon separated list of properties to configure the ViewManager
     * @return The view manager
     */

    public ViewManager getViewManager(ViewDescriptor viewDescriptor,
                                      boolean newWindow, String properties);

    /**
     * Handle the given action. This may be a url, snippet of jython, etc.
     *
     * @param action The action
     * @param properties Any extra properties (e.g., properties to pass to
     * the DataManager when creating a new data source)
     * @return Was this action handled successfully
     */
    public boolean handleAction(String action, Hashtable properties);

    /**
     * Return the {@link ucar.unidata.xml.XmlObjectStore} that is  used
     * to get and store  persistent user state.
     *
     * @return The object store
     */
    public XmlObjectStore getObjectStore();

    /**
     *  Create a menu of commands for the given DataChoice
     *
     * @param dataChoice The data choice to create a menu for
     * @return The menu
     */
    public JMenu doMakeDataChoiceMenu(DataChoice dataChoice);


    /**
     * Return the list of {@link ucar.unidata.metdata.NamedStationTable}s
     *
     * @return The station tables
     */
    public List getLocationList();


    /**
     * Popup the given window. We have this so the IDV can control when
     * windows are shown.
     *
     * @param control The control whose window is to be popped up
     * @param window The window to be popped up
     */
    public void showWindow(DisplayControl control, IdvWindow window);


    /**
     * Show the  wait cursor.
     */
    public void showWaitCursor();

    /**
     * Show the  normal cursor.
     */
    public void showNormalCursor();


    /**
     * Get the IDV
     *
     * @return The reference to the IDV
     */
    public IntegratedDataViewer getIdv();


    /**
     * Get the persistence manager
     *
     * @return Reference to the persistence manager
     */
    public IdvPersistenceManager getPersistenceManager();

    /**
     * Get the {@link ucar.unidata.ui.colortable.ColorTableManager}
     *
     * @return The color table manager
     */

    public ColorTableManager getColorTableManager();


    /**
     * Get the {@link ucar.unidata.idv.IdvPreferenceManager}
     *
     * @return The preference manager
     */

    public IdvPreferenceManager getPreferenceManager();

    /**
     * Get the {@link ucar.unidata.ui.symbol.StationModelManager}
     *
     * @return Get the station model manager
     */

    public StationModelManager getStationModelManager();


    /**
     * Get the {@link DisplayConventions}
     *
     * @return The display conventions
     */

    public DisplayConventions getDisplayConventions();


    /**
     * Get the {@link IdvResourceManager}
     *
     * @return The resource manager
     */

    public IdvResourceManager getResourceManager();


    /**
     * Create a new DisplayControl
     *
     * @param dataSourceName The identifying object for the data source (e.g., test.nc)
     * @param paramName The name of the parameter
     * @param displayName The display type (from controls.xml)
     * @param properties A set of semi-colon delimited name=value properties
     * @param initDisplayInThread If true then intialize the new display control in its own thread
     * @return The new  DisplayControl
     */
    public DisplayControl createDisplay(String dataSourceName,
                                        String paramName, String displayName,
                                        String properties,
                                        boolean initDisplayInThread);



}

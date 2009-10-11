/*
 * $Id: MapViewManager.java,v 1.382 2007/08/16 14:05:04 jeffmc Exp $
 *
 * Copyright  1997-2008 Unidata Program Center/University Corporation for
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


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.GeoLocationInfo;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.gis.maps.MapData;
import ucar.unidata.gis.maps.MapInfo;

import ucar.unidata.idv.control.MapDisplayControl;
import ucar.unidata.idv.control.ZSlider;

import ucar.unidata.idv.ui.*;

import ucar.unidata.ui.Command;
import ucar.unidata.ui.CommandManager;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.XmlUi;

import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.*;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.GeoUtils;
import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.*;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

import java.awt.*;
import java.awt.event.*;

import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;





/**
 * A wrapper around a MapProjectDisplay display master.
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class MapViewManager extends NavigatedViewManager {

    /** preference id for the list of addresses in the geocode dialog */
    public static final String PREF_ADDRESS_LIST = "view.address.list";

    /** do we reproject when we go to an address */
    public static final String PREF_ADDRESS_REPROJECT =
        "view.address.reproject";

    /** Preference for autorotate in globe mode */
    public static final String PREF_AUTOROTATE = "View.AutoRotate";

    /** Preference for  showing display in perspective view_ */
    public static final String PREF_PERSPECTIVEVIEW = "View.PerspectiveView";

    /** Preference for  default projection */
    public static final String PREF_PROJ_DFLT = "View.ProjectionDflt";

    /** Preference for  setting projection automatically from data_ */
    public static final String PREF_PROJ_USEFROMDATA = "View.UseFromData";

    /** Preference for  showing the pip */
    public static final String PREF_SHOWPIP = "View.ShowPip";

    /** _more_          */
    public static final String PREF_SHOWGLOBEBACKGROUND =
        "View.ShowGlobeBackground";

    /** Preference for  showing the earth nav panel */
    public static final String PREF_SHOWEARTHNAVPANEL =
        "View.ShowEarthNavPanel";


    /** Defines the projection when sharing state */
    public static final String SHARE_PROJECTION =
        "MapViewManager.SHARE_PROJECTION";

    /**
     * This got set from the ViewManager properties. It is a comma
     * delimited list of map resources
     */
    private String initialMapResources;


    /** The display projection we are currently using */
    private MapProjection mainProjection;

    /** The name of the display projection we are currently using */
    private String mainProjectionName = null;


    /** Keep track of the projections we have used */
    private ArrayList projectionHistory = new ArrayList();

    /** Main projections menu */
    private JMenu projectionsMenu;






    /** Big blob of xml map state from the map widget */
    private String mapState;

    /** Are we using the globe display */
    private boolean useGlobeDisplay = false;

    /** Are we 2d or 3d */
    private boolean use3D = true;

    /** The earth nav panel */
    EarthNavPanel earthNavPanel;

    /** Where the earth nav panel goes */
    JPanel earthNavPanelWrapper;

    /** The map panel in the GUI */
    private PipPanel pipPanel;
    private Object  PIP_MUTEX = new Object();

    /** _more_          */
    private JComponent pipPanelWrapper;


    /** Do we reproject when we goto address */
    private static JCheckBox addressReprojectCbx;

    /** For checking if kmz capture is ok */
    private JCheckBox fixViewpointCbx;

    /** For checking if kmz capture is ok */
    private JCheckBox fixProjectionCbx;

    /** rotate button */
    JToggleButton rotateBtn;


    /** _more_ */
    private Color globeBackgroundColor = Color.white;


    /** _more_ */
    private double globeBackgroundLevel = -0.001;

    /** _more_ */
    private LineDrawing globeBackgroundDisplayable;

    /** _more_ */
    private JComponent globeBackgroundColorComp;

    /** _more_ */
    private ZSlider globeBackgroundLevelSlider;


    /** _more_          */
    private Flythrough flythrough;


    /**
     *  Default constructor
     */
    public MapViewManager() {}


    /**
     * Construct a <code>MapViewManager</code> from an IDV
     *
     * @param viewContext Really the IDV
     */
    public MapViewManager(ViewContext viewContext) {
        super(viewContext);
    }

    /**
     * Construct a <code>MapViewManager</code> with the specified params
     * @param viewContext   context in which this MVM exists
     * @param desc   <code>ViewDescriptor</code>
     * @param properties   semicolon separated list of properties (can be null)
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create the VisAD object
     */
    public MapViewManager(ViewContext viewContext, ViewDescriptor desc,
                          String properties)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties);
    }


    /**
     * Get the default projection to use
     *
     * @return The default projection
     */
    public ProjectionImpl getDefaultProjection() {
        return getIdv().getIdvProjectionManager().getDefaultProjection();
    }




    /**
     * Make the DisplayMaster for this ViewManager
     *
     * @return the DisplayMaster
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create the VisAD object
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {

        boolean mode3d =
            getStateManager().getProperty(IdvConstants.PROP_3DMODE, use3D);
        mode3d = getStore().get(PREF_DIMENSION, mode3d);
        // let property override the preference
        use3D = mode3d && use3D;
        int mode = (use3D
                    ? NavigatedDisplay.MODE_3D
                    : NavigatedDisplay.MODE_2Din3D);

        if ( !visad.util.Util.canDoJava3D()) {
            mode = NavigatedDisplay.MODE_2D;
        }

        boolean useGlobe = getUseGlobeDisplay()
                           && (mode != NavigatedDisplay.MODE_2D);

        Dimension dimension = getIdv().getStateManager().getViewSize();
        if (dimension == null) {
            if ((getFullScreenWidth() > 0) && (getFullScreenHeight() > 0)) {
                dimension = new Dimension(getFullScreenWidth(),
                                          getFullScreenHeight());
            } else if (displayBounds != null) {
                dimension = new Dimension(displayBounds.width,
                                          displayBounds.height);
            }
        }

        if ((dimension == null) || (dimension.width == 0)
                || (dimension.height == 0)) {
            dimension = null;
        }

        NavigatedDisplay navDisplay = null;


        if (useGlobe) {
            //TODO: Set the dimension
            GlobeDisplay globeDisplay =
                new GlobeDisplay(getIdv().getArgsManager().getIsOffScreen(),
                                 dimension, null);
            navDisplay = globeDisplay;
            setGlobeBackground(globeDisplay);
        } else {
            Trace.call1("MapViewManager.doMakeDisplayMaster projection");
            if (mainProjection == null) {
                ProjectionImpl dfltProjection = getDefaultProjection();
                mainProjection =
                    new ProjectionCoordinateSystem(dfltProjection);
            }
            if (isInteractive()) {
                addProjectionToHistory(mainProjection, "Default");
            }
            Trace.call1("MapViewManager.new MPD");
            MapProjectionDisplay mapDisplay =
                MapProjectionDisplay.getInstance(mainProjection, mode,
                    getIdv().getArgsManager().getIsOffScreen(), dimension);
            Trace.call2("MapViewManager.new MPD");


            double[] aspect = getAspectRatio();
            if (aspect == null) {
                aspect = new double[] { 1.0, 1.0, 0.4 };
            }
            mapDisplay.setDisplayAspect((mode == NavigatedDisplay.MODE_2D)
                                        ? new double[] { aspect[0],
                    aspect[1] }
                                        : aspect);

            navDisplay = mapDisplay;
            navDisplay.setPerspectiveView(getPerspectiveView());
            Trace.call2("MapViewManager.doMakeDisplayMaster projection");
        }
        return navDisplay;
    }


    /**
     * Get the earth location of the screen center
     *
     * @return screen center
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public EarthLocation getScreenCenter()
            throws VisADException, RemoteException {
        return getNavigatedDisplay().getEarthLocation(
            getNavigatedDisplay().getScreenCenter());
    }





    /**
     * Get a list of named locations of the different points of the view rectangle. e.g., center, upper left, etc.
     *
     * @return list of locations
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public List<TwoFacedObject> getScreenCoordinates()
            throws VisADException, RemoteException {
        List<TwoFacedObject> l = getNavigatedDisplay().getScreenCoordinates();
        List<TwoFacedObject> result = new ArrayList<TwoFacedObject>();
        for (TwoFacedObject tfo : l) {
            result.add(
                new TwoFacedObject(
                    tfo.toString(),
                    getNavigatedDisplay().getEarthLocation(
                        (double[]) tfo.getId())));
        }
        return result;
    }


    /**
     * Initialize this object.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void init() throws VisADException, RemoteException {
        if (getHaveInitialized()) {
            return;
        }
        super.init();
        Trace.call1("MapViewManager.init checkDefaultMap");
        checkDefaultMap();
        Trace.call2("MapViewManager.init checkDefaultMap");

        if (useGlobeDisplay) {
            //            if(!hasBooleanProperty(PREF_SHOWGLOBEBACKGROUND)) {
            initializeBooleanProperty(
                new BooleanProperty(
                    PREF_SHOWGLOBEBACKGROUND, "Show Globe Background",
                    "Show Globe Background", false));
            //            }
        }

    }



    protected void initUI() {
        super.initUI();
        //Initialize the flythrough here
        if (flythrough != null) {
            flythrough.setViewManager(this);
            if (flythrough.getShown()) {
                flythrough.show();
            }
        }
    }


    /**
     * Handle a perspective view change
     *
     * @param v the value
     */
    protected void perspectiveViewChanged(boolean v) {
        setPerspectiveView(v);
        super.perspectiveViewChanged(v);
        notifyDisplayControls(PREF_PERSPECTIVEVIEW);
    }

    /**
     * Handle a vertical scale change
     */
    protected void verticalScaleChanged() {
        super.verticalScaleChanged();
        notifyDisplayControls(SHARE_PROJECTION);
    }

    /**
     * Handle the event
     *
     * @param de The event
     *
     * @param event _more_
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void displayChanged(DisplayEvent event)
            throws VisADException, RemoteException {
        if (getIsDestroyed()) {
            return;
        }

        checkPipPanel();

        if(flythrough!=null) {
            flythrough.displayChanged(event);
        }


        NavigatedDisplay navDisplay = getMapDisplay();
        if ( !navDisplay.getAutoRotate()
                && getViewpointControl().getAutoRotate()) {
            getViewpointControl().setAutoRotate(false);
        }

        int        id         = event.getId();
        InputEvent inputEvent = event.getInputEvent();
        if ((id == DisplayEvent.KEY_PRESSED)
                && (inputEvent instanceof KeyEvent)) {
            KeyEvent keyEvent = (KeyEvent) inputEvent;
            if (keyEvent.isControlDown()
                    && ((keyEvent.getKeyCode() == KeyEvent.VK_H)
                        || (keyEvent.getKeyCode() == KeyEvent.VK_J)
                        || (keyEvent.getKeyCode() == KeyEvent.VK_K)
                        || (keyEvent.getKeyCode() == KeyEvent.VK_L))) {
                double[] matrix = getProjectionControl().getMatrix();
                double[] rot   = new double[3];
                double[] scale = new double[3];
                double[] trans = new double[3];
                MouseBehavior mouseBehavior =
                    getNavigatedDisplay().getMouseBehavior();
                mouseBehavior.instance_unmake_matrix(rot, scale, trans,
                        matrix);

                double[] t = null;
                if (keyEvent.getKeyCode() == KeyEvent.VK_H) {
                    t = mouseBehavior.make_matrix(-5, 0.0, 0, 1.0, 0.0, 0.0,
                            0.0);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_J) {
                    t = mouseBehavior.make_matrix(5, 0.0, 0, 1.0, 0.0, 0.0,
                            0.0);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_K) {
                    t = mouseBehavior.make_matrix(0, -5.0, 0, 1.0, 0.0, 0.0,
                            0.0);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_L) {
                    t = mouseBehavior.make_matrix(0, 5.0, 0, 1.0, 0.0, 0.0,
                            0.0);
                }
                matrix = mouseBehavior.multiply_matrix(t, matrix);
                getMaster().setProjectionMatrix(matrix);

                return;
            }
        }


        super.displayChanged(event);
    }



    /**
     * _more_
     *
     * @param startPoint _more_
     * @param endPoint _more_
     * @param startMatrix _more_
     * @param endMatrix _more_
     * @param speed _more_
     */
    protected void mouseFlicked(Point startPoint, Point endPoint,
                                double[] startMatrix, double[] endMatrix,
                                double speed) {
        if ( !getUseGlobeDisplay()) {
            return;
        }

        double[] trans = { 0.0, 0.0, 0.0 };
        double[] rot1  = { 0.0, 0.0, 0.0 };
        double[] rot2  = { 0.0, 0.0, 0.0 };
        double[] scale = { 0.0, 0.0, 0.0 };
        getNavigatedDisplay().getMouseBehavior().instance_unmake_matrix(rot1,
                scale, trans, startMatrix);
        getNavigatedDisplay().getMouseBehavior().instance_unmake_matrix(rot2,
                scale, trans, endMatrix);

        //If there was no rotation then return
        if ((rot1[0] == rot2[0]) && (rot1[1] == rot2[1])) {
            return;
        }

        double distance = GuiUtils.distance(startPoint.x, startPoint.y,
                                            endPoint.x, endPoint.y);
        if (distance == 0) {
            return;
        }
        double percentX = (endPoint.x - startPoint.x) / distance;
        double percentY = (endPoint.y - startPoint.y) / distance;
        speed *= 2;
        getNavigatedDisplay().setRotationMultiplierMatrix(speed * -percentY,
                speed * -percentX, 0.0);
        getViewpointControl().setAutoRotate(true);
    }


    /**
     * Check if its ok to capture a kmz file
     *
     * @return ok to capture kmz
     */
    protected boolean checkForKmlImageCapture() {

        //Assume when we are running isl everything is ok
        if (getIdv().getArgsManager().getIsOffScreen()) {
            return true;
        }

        NavigatedDisplay navDisplay = getMapDisplay();
        double[]         rotMatrix  = navDisplay.getRotation();
        if ((rotMatrix[0] != 0) || (rotMatrix[1] != 0)
                || (rotMatrix[2] != 0)) {
            if (fixViewpointCbx == null) {
                fixViewpointCbx = new JCheckBox("Fix it", true);
            }

            JComponent question =
                GuiUtils
                    .vbox(new JLabel(
                        "The viewpoint is not overhead. This will result in an incorrect image capture."), GuiUtils
                            .left(fixViewpointCbx));
            if ( !GuiUtils.askOkCancel("KML Capture", question)) {
                return false;
            }
            if (fixViewpointCbx.isSelected()) {
                try {
                    navDisplay.resetProjection();
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            } else {
                return true;
            }
        }


        int cnt = 0;
        while (true) {
            cnt++;
            Rectangle sb = navDisplay.getDisplayComponent().getBounds();
            LatLonPoint ul =
                getMapDisplay().getEarthLocation(
                    getMapDisplay().getSpatialCoordinatesFromScreen(
                        0, 0)).getLatLonPoint();
            LatLonPoint ur =
                getMapDisplay().getEarthLocation(
                    getMapDisplay().getSpatialCoordinatesFromScreen(
                        sb.width, 0)).getLatLonPoint();
            LatLonPoint lr =
                getMapDisplay().getEarthLocation(
                    getMapDisplay().getSpatialCoordinatesFromScreen(
                        sb.width, sb.height)).getLatLonPoint();
            LatLonPoint ll =
                getMapDisplay().getEarthLocation(
                    getMapDisplay().getSpatialCoordinatesFromScreen(
                        0, sb.height)).getLatLonPoint();

            double width = Math.abs(ul.getLongitude().getValue()
                                    - ur.getLongitude().getValue());

            double height = Math.abs(ul.getLatitude().getValue()
                                     - ll.getLatitude().getValue());


            boolean projOk = true;

            if ( !isClose(width, ul.getLongitude().getValue(),
                          ll.getLongitude().getValue())) {
                projOk = false;
            }
            if ( !isClose(width, ur.getLongitude().getValue(),
                          lr.getLongitude().getValue())) {
                projOk = false;
            }
            if ( !isClose(height, ul.getLatitude().getValue(),
                          ur.getLatitude().getValue())) {
                projOk = false;
            }
            if ( !isClose(height, ll.getLatitude().getValue(),
                          lr.getLatitude().getValue())) {
                projOk = false;
            }

            if (projOk) {
                return true;
            }

            if (fixProjectionCbx == null) {
                fixProjectionCbx = new JCheckBox("Fix it", true);
            }
            String msg = ((cnt == 1)
                          ? "The projection is not lat/lon. This will result in an incorrect image capture."
                          : "For some reason the projection is still not lat/lon.");
            JComponent question = GuiUtils.vbox(new JLabel(msg),
                                      GuiUtils.left(fixProjectionCbx));
            if ( !GuiUtils.askOkCancel("KML Capture", question)) {
                return false;
            }

            if ( !fixProjectionCbx.isSelected()) {
                return true;
            }

            if (fixProjectionCbx.isSelected()) {
                try {
                    setCurrentAsProjection();
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
                Misc.sleep(1000);
            }
        }

    }





    /**
     * are the 2 values close
     *
     * @param span the range
     * @param value1 value 1
     * @param value2 value 2
     *
     * @return are the 2 values close
     */
    private boolean isClose(double span, double value1, double value2) {
        //Check that the difference of the two values is < 1% of the given span value
        if (Math.abs((value1 - value2) / span) > 0.01) {
            return false;
        }
        return true;
    }


    /**
     * Check for the default map
     */
    private void checkDefaultMap() {
        MapDisplayControl defaultMap = findDefaultMap();
        if (defaultMap == null) {
            try {
                ControlDescriptor mapCD =
                    getIdv().getControlDescriptor(
                        ControlDescriptor.DISPLAYID_MAP);
                if (mapCD == null) {
                    return;
                }

                MapInfo mapInfo;
                if (mapState != null) {
                    Trace.call1("checkDefaultMap-1");
                    mapInfo = new MapInfo(XmlUtil.getRoot(mapState));
                    mapInfo.setJustLoadedLocalMaps(true);
                    Trace.call2("checkDefaultMap-1");
                    //SKIP the initial map resources for now
                } else if (false && (initialMapResources != null)) {
                    //This got set from the ViewManager properties. It is a comma
                    //delimited list of map resources 
                    Trace.call1("checkDefaultMap-2");
                    List resources = getResourceManager().getResourcePaths(
                                         StringUtil.split(
                                             initialMapResources, ",", true,
                                             true));
                    XmlResourceCollection customMapResources =
                        new XmlResourceCollection("custom maps");
                    customMapResources.addResources(resources);
                    mapInfo = new MapInfo(customMapResources, true, true);
                    Trace.call2("checkDefaultMap-2");
                } else {
                    XmlResourceCollection xrc =
                        getResourceManager().getXmlResources(
                            getResourceManager().RSC_MAPS);
                    mapInfo = new MapInfo(xrc, false, true);
                }

                Trace.call1("checkDefaultMap-making map");
                defaultMap = new MapDisplayControl(this, mapInfo);
                defaultMap.setIsDefaultMap(true);
                Hashtable newProperties =
                    new Hashtable(mapCD.getProperties());
                newProperties.put("displayName", "Default Background Maps");
                mapCD.initControl(defaultMap, new ArrayList(), getIdv(),
                                  newProperties, null);
                Trace.call2("checkDefaultMap-making map");
            } catch (Exception exc) {
                logException("Initializing maps", exc);
            }
        }
    }




    /**
     * Can this view manager be used in exchange for the given view manager
     *
     * @param that The other view manager to check
     * @return Can this be used in place of that
     */
    public boolean canBe(ViewManager that) {
        if ( !super.canBe(that)) {
            return false;
        }
        MapViewManager mvm = (MapViewManager) that;
        if (this.getUseGlobeDisplay() != mvm.getUseGlobeDisplay()) {
            return false;
        }
        if (this.getUse3D() != mvm.getUse3D()) {
            return false;
        }
        return true;
    }




    /**
     * _more_
     *
     * @param viewState _more_
     *
     * @throws Exception _more_
     */
    public void initWith(ViewState viewState) throws Exception {
        MapProjection thatProjection =
            (MapProjection) viewState.get(ViewState.PROP_PROJECTION);
        if (thatProjection != null) {
            setMapProjection(thatProjection, false, "Projection");
        }
        double[] aspect =
            (double[]) viewState.get(ViewState.PROP_ASPECTRATIO);
        if (aspect != null) {
            this.setAspectRatio(aspect);
        }
        super.initWith(viewState);
    }

    protected void animationTimeChanged() {
        super.animationTimeChanged();
        if(flythrough!=null) {
            flythrough.animationTimeChanged();
        }
    }


    /**
     * Initialize this object's state with the state from that.
     *
     * @param that The other obejct to get state from
     * @param ignoreWindow If true then don't set the window size and location
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create the VisAD object
     */
    protected void initWithInner(ViewManager that, boolean ignoreWindow)
            throws VisADException, RemoteException {
        if ( !(that instanceof MapViewManager)) {
            return;
        }
        MapViewManager mvm            = (MapViewManager) that;
        MapProjection  thatProjection = mvm.getMainProjection();
        this.setAspectRatio(that.getAspectRatio());

        if ((mvm.flythrough != null) && (mvm.flythrough != this.flythrough)) {
            System.err.println ("Initing flythrough");
            if (this.flythrough != null) {
                System.err.println ("destroying old one");
                this.flythrough.destroy();
                //                this.flythrough.initWith(mvm.flythrough);
            }
            System.err.println ("setting new one");
            this.flythrough = mvm.flythrough;
            this.flythrough.setViewManager(this);
            if (this.flythrough.getShown()) {
                this.flythrough.show();
            }
        }

        boolean setProjection = false;
        if (thatProjection != null) {
            setProjection = setMapProjection(thatProjection, false,
                                             mvm.mainProjectionName);
        }

        if ( !setProjection) {
            if (getAspectRatio() != null) {
                getMapDisplay().setDisplayAspect(getAspectRatio());
            }
        }


        //Only save the projection if we're not a globe
        //        getMapDisplay().saveProjection();
        if ( !getUseGlobeDisplay()) {
            getMapDisplay().saveProjection();
        }

        this.globeBackgroundColor = mvm.globeBackgroundColor;
        this.globeBackgroundLevel = mvm.globeBackgroundLevel;
        if (globeBackgroundDisplayable != null) {
            setGlobeBackground((GlobeDisplay) getMapDisplay());
        }






        super.initWithInner(that, ignoreWindow);
        try {
            //If we have an old bundle then the other map view has a non-null
            //map state. If so we load it in.
            if (mvm.mapState != null) {
                MapDisplayControl defaultMap = findDefaultMap();
                if (defaultMap != null) {
                    MapInfo mapInfo =
                        new MapInfo(XmlUtil.getRoot(mvm.mapState));
                    MapDisplayControl newMap = new MapDisplayControl(this,
                                                   mapInfo);
                    newMap.init((ucar.unidata.data.DataChoice) null);
                    defaultMap.loadNewMap(newMap);
                }
            }

        } catch (Exception exc) {
            logException("Initializing with MapViewManager", exc);
        }
    }


    /**
     * _more_
     *
     * @param viewState _more_
     */
    public void initViewState(ViewState viewState) {
        super.initViewState(viewState);
        viewState.put(ViewState.PROP_GLOBE,
                      new Boolean(getUseGlobeDisplay()));
        if ( !getUseGlobeDisplay()) {
            viewState.put(ViewState.PROP_PROJECTION, getMainProjection());
        }
    }


    /**
     * Get the <code>JComponent</code> for the VisAD display
     *
     * @return VisAD display's Component
     */
    public JComponent getInnerContents() {
        return (JComponent) getMapDisplay().getComponent();
    }





    /**
     * Leave this here for old bundles
     *
     * @param ms The map specification
     */
    public void setMapState(String ms) {
        mapState = ms;
    }




    /**
     * Add in the different preference panels.
     *
     * @param preferenceManager The preference manager to add things into
     */
    public void initPreferences(
            final IdvPreferenceManager preferenceManager) {

        super.initPreferences(preferenceManager);


        final JComponent[] bgComps =
            GuiUtils.makeColorSwatchWidget(getStore().get(PREF_BGCOLOR,
                getBackground()), "Set Background Color");

        final JComponent[] fgComps =
            GuiUtils.makeColorSwatchWidget(getStore().get(PREF_FGCOLOR,
                getForeground()), "Set Foreground Color");

        final JComponent[] border =
            GuiUtils
                .makeColorSwatchWidget(getStore()
                    .get(PREF_BORDERCOLOR, ViewManager
                        .borderHighlightColor), "Set Selected Panel Border Color");

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel colorPanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("  Background:"), bgComps[0], bgComps[1],
            GuiUtils.rLabel("  Foreground:"), fgComps[0], fgComps[1],
            GuiUtils.rLabel("  Selected Panel:"), border[0], border[1],
        }, 3, GuiUtils.WT_N, GuiUtils.WT_N));
        colorPanel = GuiUtils.vbox(new JLabel("Color Scheme:"), colorPanel);
        final FontSelector fontSelector =
            new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        Font f = getStore().get(PREF_DISPLAYLISTFONT, getDisplayListFont());
        fontSelector.setFont(f);
        final GuiUtils.ColorSwatch dlColorWidget =
            new GuiUtils.ColorSwatch(getStore().get(PREF_DISPLAYLISTCOLOR,
                getDisplayListColor()), "Set Display List Color");
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel fontPanel =
            GuiUtils.vbox(GuiUtils.lLabel("Display List Properties:"),
                          GuiUtils.doLayout(new Component[] {
                              GuiUtils.rLabel("   Font:"),
                              GuiUtils.left(fontSelector.getComponent()),
                              GuiUtils.rLabel("  Color:"),
                              GuiUtils.left(GuiUtils.hbox(dlColorWidget,
                                  dlColorWidget.getSetButton(),
                                  dlColorWidget.getClearButton(), 5)) }, 2,
                                      GuiUtils.WT_N, GuiUtils.WT_N));


        List            projections = getProjectionList();
        final JComboBox projBox     = new JComboBox();
        GuiUtils.setListData(projBox, projections.toArray());
        Object defaultProj = getDefaultProjection();
        if (defaultProj != null) {
            projBox.setSelectedItem(defaultProj);
        }


        PreferenceManager miscManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                IdvPreferenceManager.applyWidgets((Hashtable) data, theStore);
                theStore.put(PREF_PROJ_DFLT, projBox.getSelectedItem());
                theStore.put(PREF_BGCOLOR, bgComps[0].getBackground());
                theStore.put(PREF_FGCOLOR, fgComps[0].getBackground());
                theStore.put(PREF_BORDERCOLOR, border[0].getBackground());
                theStore.put(PREF_DISPLAYLISTFONT, fontSelector.getFont());
                theStore.put(PREF_DISPLAYLISTCOLOR,
                             dlColorWidget.getSwatchColor());
                checkToolBarVisibility();
                ViewManager.setHighlightBorder(border[0].getBackground());

            }
        };


        Hashtable  widgets     = new Hashtable();
        ArrayList  miscList    = new ArrayList();


        Object[][] miscObjects = {
            { "View:", null, null },
            { "Show Wireframe Box", PREF_WIREFRAME,
              new Boolean(getWireframe()) },
            { "Show Cursor Readout", PREF_SHOWCURSOR,
              new Boolean(getShowCursor()) },
            { "Clip View At Box", PREF_3DCLIP, new Boolean(getClipping()) },
            { "Show Display List", PREF_SHOWDISPLAYLIST,
              new Boolean(getShowDisplayList()) },
            { "Show Times In View", PREF_ANIREADOUT,
              new Boolean(getAniReadout()) },
            { "Show Map Display Scales", PREF_SHOWSCALES,
              new Boolean(getLabelsVisible()) },
            { "Show Transect Display Scales", PREF_SHOWTRANSECTSCALES,
              new Boolean(getTransectLabelsVisible()) },
            { "Show \"Please Wait\" Message", PREF_WAITMSG,
              new Boolean(getWaitMessageVisible()) },
            { "Reset Projection With New Data", PREF_PROJ_USEFROMDATA },
            { "Use 3D View", PREF_DIMENSION }
        };


        Object[][] legendObjects = {
            { "Legends:", null, null },
            { "Show Side Legend", PREF_SHOWSIDELEGEND,
              new Boolean(getShowSideLegend()) },
            { "Show Bottom Legend", PREF_SHOWBOTTOMLEGEND,
              new Boolean(getShowBottomLegend()) },
            { "Show Animation Boxes", PREF_SHOWANIMATIONBOXES,
              new Boolean(getShowAnimationBoxes()) },
            { "Show Clock", IdvConstants.PROP_SHOWCLOCK,
              new Boolean(
                  getStateManager().getPreferenceOrProperty(
                      IdvConstants.PROP_SHOWCLOCK, "true")) },
            { "Show Overview Map", PREF_SHOWPIP,
              new Boolean(getStore().get(PREF_SHOWPIP, false)) },
            { "Show Globe Background", PREF_SHOWGLOBEBACKGROUND,
              new Boolean(getStore().get(PREF_SHOWGLOBEBACKGROUND, false)) }
        };

        Object[][] toolbarObjects = {
            { "Toolbars:", null, null },
            { "Show Earth Navigation Panel", PREF_SHOWEARTHNAVPANEL,
              new Boolean(getShowEarthNavPanel()) },
            { "Show Viewpoint Toolbar", PREF_SHOWTOOLBAR + "perspective" },
            { "Show Zoom/Pan Toolbar", PREF_SHOWTOOLBAR + "zoompan" },
            { "Show Undo/Redo Toolbar", PREF_SHOWTOOLBAR + "undoredo" }
        };

        JPanel miscPanel = IdvPreferenceManager.makePrefPanel(miscObjects,
                               widgets, getStore());
        JPanel legendPanel =
            IdvPreferenceManager.makePrefPanel(legendObjects, widgets,
                getStore());
        JPanel toolbarPanel =
            IdvPreferenceManager.makePrefPanel(toolbarObjects, widgets,
                getStore());
        JPanel projPanel =
            GuiUtils.vbox(GuiUtils.lLabel("Default Projection: "),
                          GuiUtils.left(GuiUtils.inset(projBox,
                              new Insets(5, 20, 0, 0))));

        JPanel colorFontPanel = GuiUtils.vbox(GuiUtils.top(colorPanel),
                                    GuiUtils.top(fontPanel),
                                    GuiUtils.top(projPanel));




        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel miscContents =
            GuiUtils.doLayout(Misc.newList(GuiUtils.top(legendPanel),
                                           GuiUtils.top(toolbarPanel),
                                           GuiUtils.top(miscPanel),
                                           GuiUtils.top(colorFontPanel)), 2,
                                               GuiUtils.WT_N, GuiUtils.WT_N);


        miscContents = GuiUtils.inset(GuiUtils.left(miscContents), 5);
        preferenceManager.add("View", "View Preferences", miscManager,
                              miscContents, widgets);


    }


    /**
     * Go the a street address
     */
    public void goToAddress() {
        Misc.run(new Runnable() {
            public void run() {
                goToAddressInner();
            }
        });
    }


    /**
     * Popup the address location dialog and translate to the lat/lon.
     */
    private void goToAddressInner() {
        try {
            if (addressReprojectCbx == null) {

                addressReprojectCbx = new JCheckBox("Reproject",
                        getStore().get(PREF_ADDRESS_REPROJECT, true));
                List savedAddresses =
                    (List) getStore().get(PREF_ADDRESS_LIST);
                if (savedAddresses != null) {
                    GeoUtils.setSavedAddresses(savedAddresses);
                }
            }
            getIdvUIManager().showWaitCursor();
            LatLonPoint llp = GeoUtils.getLocationOfAddress(
                                  GuiUtils.left(getUseGlobeDisplay()
                    ? GuiUtils.filler()
                    : (JComponent) addressReprojectCbx));
            getIdvUIManager().showNormalCursor();
            if (llp == null) {
                return;
            }

            getStore().put(PREF_ADDRESS_LIST, GeoUtils.getSavedAddresses());
            getStore().put(PREF_ADDRESS_REPROJECT,
                           addressReprojectCbx.isSelected());


            float x      = (float) llp.getLongitude().getValue();
            float y      = (float) llp.getLatitude().getValue();
            float offset = (float) (1.0 / 60.0f);
            Rectangle2D.Float rect = new Rectangle2D.Float(x - offset,
                                         y - offset, offset * 2, offset * 2);
            if ( !getUseGlobeDisplay() && addressReprojectCbx.isSelected()) {
                TrivialMapProjection mp =
                    new TrivialMapProjection(
                        RealTupleType.SpatialEarth2DTuple, rect);

                setMapProjection(mp, true);
            } else {
                getMapDisplay().center(GeoUtils.toEarthLocation(llp), true);
                //                getMapDisplay().center(GeoUtils.toEarthLocation(llp), false);
            }
        } catch (Exception e) {
            getIdvUIManager().showNormalCursor();
            logException("Error going to address", e);
        }
    }



    /**
     * Get the map display.
     *
     * @return The map display. This is the main display for thie view manager.
     */
    public NavigatedDisplay getMapDisplay() {
        return (NavigatedDisplay) getMaster();
    }

    /**
     * Are we in 3d mode
     *
     * @return Is display in 3d?
     */
    public boolean isDisplay3D() {
        return (getMapDisplay().getDisplayMode() == NavigatedDisplay.MODE_3D);
    }

    /**
     * Handle the receipt of shared data
     *
     * @param from Who is it from
     * @param dataId What is it
     * @param data Here it is
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if ( !getInitDone()) {
            return;
        }
        if (dataId.equals(SHARE_PROJECTION)) {
            setMapProjection(((MapProjection) data[0]), false);
            return;
        }
        super.receiveShareData(from, dataId, data);
    }




    /**
     * Add the PIP panel if needed
     *
     * @param sideLegend The side legend
     *
     * @return The side legend or the sidelegend coupled with the pip panel
     */
    protected JComponent getSideComponent(JComponent sideLegend) {
        if (false && getUseGlobeDisplay()) {
            return sideLegend;
        }
        pipPanel = new PipPanel(this);
        pipPanel.setPreferredSize(new Dimension(100, 100));
        JButton closeBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Cancel16.gif", this,
                                     "hidePip");
        pipPanelWrapper = GuiUtils.topCenter(GuiUtils.right(closeBtn),
                                             pipPanel);
        if ( !getShowPip()) {
            pipPanelWrapper.setVisible(false);
        }
        return GuiUtils.centerBottom(sideLegend, pipPanelWrapper);
    }


    /**
     * Make the GUI contents.
     *
     * @return The GUI contents
     */
    protected Container doMakeContents() {
        NavigatedDisplay navDisplay   = getMapDisplay();
        JComponent       navComponent = getComponent();
        navComponent.setPreferredSize(getMySize());
        earthNavPanelWrapper = new JPanel(new BorderLayout());
        JPanel contents = GuiUtils.centerBottom(navComponent,
                              earthNavPanelWrapper);

        earthNavPanel = new EarthNavPanel(this);
        if (getShowEarthNavPanel()) {
            earthNavPanelWrapper.add(BorderLayout.CENTER, earthNavPanel);
        }
        return contents;
    }


    /**
     * Initialize the toolbars for the GUI
     */
    protected void initToolBars() {
        if (isDisplay3D()) {
            addToolBar(doMakeViewPointToolBar(JToolBar.VERTICAL),
                       "perspective", "Viewpoint toolbar");
        }
        super.initToolBars();
    }



    /**
     * _more_
     *
     * @param viewpointInfo _more_
     */
    public void setViewpointInfo(ViewpointInfo viewpointInfo) {
        getViewpointControl().setViewpointInfo(viewpointInfo);
    }






    /**
     * Dynamically initialize the view menu
     *
     * @param viewMenu the view menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        super.initializeViewMenu(viewMenu);
        if (isDisplay3D()) {
            viewMenu.add(getViewpointControl().getMenu());
        }
        viewMenu.add(makeColorMenu());
        viewMenu.addSeparator();

        if (isFullScreen()) {
            viewMenu.add(
                GuiUtils.setIcon(
                    GuiUtils.makeMenuItem(
                        "Reset Full Screen", this,
                        "resetFullScreen"), "/auxdata/ui/icons/arrow_in.png"));
        } else {
            viewMenu.add(
                GuiUtils.setIcon(
                    GuiUtils.makeMenuItem(
                        "Full Screen", this,
                        "setFullScreen"), "/auxdata/ui/icons/arrow_out.png"));
        }
        viewMenu.addSeparator();
        viewMenu.add(
            GuiUtils.setIcon(
                GuiUtils.makeMenuItem(
                    "Animation Timeline", this,
                    "showTimeline"), "/auxdata/ui/icons/timeline_marker.png"));

        viewMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Flythrough",
                this, "showFlythrough"), "/auxdata/ui/icons/plane.png"));

        viewMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Properties",
                this,
                "showPropertiesDialog"), "/auxdata/ui/icons/information.png"));
    }

    /**
     * Create and return the list of menus for the menu bar.
     * Just the map and view menu.
     *
     * @return List of menus.
     */
    public ArrayList doMakeMenuList() {
        ArrayList menus = super.doMakeMenuList();
        menus.add(makeViewMenu());
        menus.add(makeProjectionMenu());
        return menus;
    }


    /**
     * Create and return the show menu.
     *
     * @return The Show menu
     */
    protected JMenu makeShowMenu() {
        JMenu showMenu = super.makeShowMenu();
        if (globeBackgroundDisplayable != null) {
            createCBMI(showMenu, PREF_SHOWGLOBEBACKGROUND);
        }

        createCBMI(showMenu, PREF_SHOWSCALES);
        createCBMI(showMenu, PREF_ANIREADOUT);
        createCBMI(showMenu, PREF_SHOWPIP);
        createCBMI(showMenu, PREF_SHOWEARTHNAVPANEL);
        return showMenu;
    }



    /**
     * Set the projection to the first  projection  found in the displays
     */
    public void setProjectionFromFirstDisplay() {
        List controls = getControls();
        for (int i = 0; i < controls.size(); i++) {
            DisplayControl display = (DisplayControl) controls.get(i);
            MapProjection  mp      = display.getDataProjection();
            if (displayProjectionOk(mp)) {
                setMapProjection(
                    mp, true,
                    getDisplayConventions().getMapProjectionLabel(
                        mp, display));
                break;
            }
        }
    }


    /**
     * If we are using a ProjectionImpl then see if it hsa
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void updateProjection() throws RemoteException, VisADException {
        if ((mainProjection == null)
                || !(mainProjection instanceof ProjectionCoordinateSystem)) {
            return;
        }
        ProjectionImpl myProjection =
            (ProjectionImpl) ((ProjectionCoordinateSystem) mainProjection)
                .getProjection();
        ProjectionImpl newProjection =
            getIdv().getIdvProjectionManager().findProjectionByName(
                myProjection.getName());
        if ((newProjection != null) && !myProjection.equals(newProjection)) {
            double[] matrix = getDisplayMatrix();
            setProjection(newProjection);
            setDisplayMatrix(matrix);
        }
    }



    /**
     * Find and set the projection by name
     *
     * @param projName projection name
     */
    public void setProjectionByName(String projName) {
        List projections = getProjectionList();
        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl p = (ProjectionImpl) projections.get(i);
            if (p.getName().equals(projName)) {
                setProjection(p);
                return;
            }
        }

        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl p = (ProjectionImpl) projections.get(i);
            if (StringUtil.stringMatch(p.getName(), projName)) {
                setProjection(p);
                return;
            }
        }
        //        System.err.println("Could not find projection:" + projName);
    }



    /**
     * Set the current projection
     *
     * @param p The new projection.
     */
    public void setProjection(ProjectionImpl p) {
        p = (ProjectionImpl) p.clone();
        try {
            setMapProjection(new ProjectionCoordinateSystem(p), true);
            if (pipPanel != null) {
                pipPanel.setProjectionImpl(p);
            }
        } catch (Exception excp) {
            logException("setMapProjection ()", excp);
        }
    }



    /**
     * Class ProjectionCommand manages changes to the projection
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.382 $
     */
    public static class ProjectionCommand extends Command {

        /** THe view manager I am in */
        MapViewManager viewManager;

        /** old state */
        String oldName;

        /** old state */
        MapProjection oldProjection;

        /** new state */
        String newName;

        /** new state */
        MapProjection newProjection;

        /**
         * ctor
         *
         * @param viewManager The vm
         * @param oldName old state
         * @param oldProjection old state
         * @param newName new state
         * @param newProjection new state
         */
        public ProjectionCommand(MapViewManager viewManager, String oldName,
                                 MapProjection oldProjection, String newName,
                                 MapProjection newProjection) {
            this.viewManager   = viewManager;
            this.oldName       = oldName;
            this.oldProjection = oldProjection;
            this.newName       = newName;
            this.newProjection = newProjection;
        }

        /**
         * Redo
         */
        public void redoCommand() {
            viewManager.setMapProjection(newProjection, true, newName, false,
                                         false);
        }

        /**
         * Undo
         */
        public void undoCommand() {
            viewManager.setMapProjection(oldProjection, true, oldName, false,
                                         false);
        }
    }





    /**
     *  A wrapper aroung the setMapProjection call that takes a projection name.
     *  This passes in null.
     *
     * @param projection The projection
     * @param fromWidget Is it from the projection selection widget
     */
    public void setMapProjection(MapProjection projection,
                                 boolean fromWidget) {
        setMapProjection(projection, fromWidget, null);
    }


    /**
     * Get the current projection.Used for xml encoding/decoding.
     *
     * @return The current projection
     */
    public MapProjection getMainProjection() {
        return mainProjection;
    }

    /**
     * Set the current projection. Used for xml encoding/decoding.
     *
     * @param projection The new projection
     */

    public void setMainProjection(MapProjection projection) {
        mainProjection = projection;
    }


    /**
     * The main projection name.Used for xml encoding/decoding.
     *
     * @return Projection name
     */
    public String getMainProjectionName() {
        return mainProjectionName;
    }

    /**
     * The main projection name.Used for xml encoding/decoding.
     *
     * @param projectionName Projection name
     */
    public void setMainProjectionName(String projectionName) {
        mainProjectionName = projectionName;
    }

    /**
     * Add the projection and the given name into the history list. Add a menu item
     * into the history menu.
     *
     * @param projection The projection
     * @param name Its name
     */
    private void addProjectionToHistory(MapProjection projection,
                                        String name) {
        TwoFacedObject tfo = TwoFacedObject.findId(projection,
                                 projectionHistory);
        if (tfo != null) {
            projectionHistory.remove(tfo);
            projectionHistory.add(0, tfo);
            return;

        }
        tfo = new TwoFacedObject(name, projection);
        projectionHistory.add(0, tfo);


        String label = ((name != null)
                        ? name
                        : projection.toString());
    }


    /**
     * Set map projection in the main display.
     *
     * @param projection a Projection
     * @param fromWidget  true if this was from a widget (ie. widget or
     *                    menu item)
     * @param name        name to put in the history list (may be null)
     *
     * @return _more_
     */
    public boolean setMapProjection(MapProjection projection,
                                    boolean fromWidget, String name) {
        return setMapProjection(projection, fromWidget, name, false);
    }

    /**
     * Set map projection in the main display.
     *
     * @param projection a Projection
     * @param fromWidget  true if this was from a widget (ie. widget or
     *                    menu item)
     * @param name        name to put in the history list (may be null)
     * @param checkDefault  if true, check to see if we
     *                    should call getUseProjectionFromData()
     *
     * @return _more_
     */
    public boolean setMapProjection(MapProjection projection,
                                    boolean fromWidget, String name,
                                    boolean checkDefault) {
        return setMapProjection(projection, fromWidget, name, checkDefault,
                                true);
    }



    /**
     * Set map projection in the main display.
     *
     * @param projection a Projection
     * @param fromWidget  true if this was from a widget (ie. widget or
     *                    menu item)
     * @param name        name to put in the history list (may be null)
     * @param checkDefault  if true, check to see if we
     *                    should call getUseProjectionFromData()
     * @param addToCommandHistory Add this projection to the command history
     *
     * @return _more_
     */
    public boolean setMapProjection(MapProjection projection,
                                    boolean fromWidget, String name,
                                    boolean checkDefault,
                                    boolean addToCommandHistory) {
        return setMapProjection(projection, fromWidget, name, checkDefault,
                                addToCommandHistory, false);
    }


    /**
     * Set map projection in the main display.
     *
     * @param projection a Projection
     * @param fromWidget  true if this was from a widget (ie. widget or
     *                    menu item)
     * @param name        name to put in the history list (may be null)
     * @param checkDefault  if true, check to see if we
     *                    should call getUseProjectionFromData()
     * @param addToCommandHistory Add this projection to the command history
     * @param maintainViewpoint  maintain the viewpoint
     *
     * @return _more_
     */
    public boolean setMapProjection(MapProjection projection,
                                    boolean fromWidget, String name,
                                    boolean checkDefault,
                                    boolean addToCommandHistory,
                                    boolean maintainViewpoint) {


        IdvUIManager.startTime = System.currentTimeMillis();

        if (checkDefault && !getUseProjectionFromData()) {
            return false;
        }

        if (projection == null) {
            return false;
        }

        boolean actuallyChangedProjection = false;
        try {
            setMasterInactive();
            if (addToCommandHistory && (mainProjection != null)) {
                addCommand(new ProjectionCommand(this, mainProjectionName,
                        mainProjection, name, projection));
            }

            if ( !Misc.equals(mainProjection, projection)) {
                if (name == null) {
                    name = getDisplayConventions().getMapProjectionName(
                        projection);
                }

                //If this is the first time we've put one in then save the current (default) proj.
                if ((projectionHistory.size() == 0)
                        && (mainProjection != null)) {
                    addProjectionToHistory(mainProjection, "Default");
                }
                mainProjectionName = name;
                addProjectionToHistory(projection, name);

                mainProjection = projection;
                if (fromWidget) {
                    doShare(SHARE_PROJECTION, projection);
                }
                try {
                    actuallyChangedProjection = true;
                    double[] matrix = getDisplayMatrix();
                    getMapDisplay().setMapProjection(mainProjection);
                    if (getAspectRatio() != null) {
                        getMapDisplay().setDisplayAspect(getAspectRatio());
                    }
                    if (maintainViewpoint) {
                        setDisplayMatrix(matrix);
                    }
                } catch (Exception e) {
                    logException("setProjection", e);
                }
                notifyDisplayControls(SHARE_PROJECTION);
                checkPipPanel();
            }
            // if the projections are the same, reset to main view in case
            // they are zoomed/panned
            try {
                if (projection.equals(mainProjection)) {
                    getMaster().resetProjection();
                }
            } catch (Exception e) {
                logException("setProjection", e);
            }
        } finally {
            updateDisplayList();
            setMasterActive();
        }
        return actuallyChangedProjection;
    }

    /**
     * Check the pip panel. If non-null have it reset its box
     */
    public void checkPipPanel() {
        try {
            synchronized(PIP_MUTEX) {
                if (pipPanel == null) {
                    return;
                }
                pipPanel.resetDrawBounds();
            }
        } catch (Exception exc) {
            pipPanel = null;
            logException("Error setting pip panel", exc);
        }
    }



    /**
     * can this viewmanager import the given display control
     *
     * @param control the control
     *
     * @return ok to import
     */
    public boolean okToImportDisplay(DisplayControl control) {
        //Base class method checks for non-null and class equality
        if ( !super.okToImportDisplay(control)) {
            return false;
        }
        MapViewManager vm = (MapViewManager) control.getViewManager();
        return this.getUseGlobeDisplay() == vm.getUseGlobeDisplay();
    }


    /**
     * Received the first frame done event  from the display
     */
    protected void doneFirstFrame() {
        super.doneFirstFrame();
        checkPipPanel();
    }

    /**
     * Search through the list of display controls
     * looking for a {@link ucar.unidata.idv.control.MapDisplayControl}
     * that has been set to be the &quot;default map&quot;
     *
     * @return The default map display control
     */
    private MapDisplayControl findDefaultMap() {
        List controls = getControls();
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if ( !(control instanceof MapDisplayControl)) {
                continue;
            }
            if (((MapDisplayControl) control).getIsDefaultMap()) {
                return (MapDisplayControl) control;
            }
        }
        return null;
    }







    /**
     * _more_
     *
     * @return _more_
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        if (globeBackgroundDisplayable != null) {
            globeBackgroundColor = globeBackgroundColorComp.getBackground();
            globeBackgroundLevel = globeBackgroundLevelSlider.getValue();
            setGlobeBackground((GlobeDisplay) getMapDisplay());
        }
        return true;
    }



    /**
     * _more_
     *
     * @param tabbedPane _more_
     */
    protected void addPropertiesComponents(JTabbedPane tabbedPane) {
        super.addPropertiesComponents(tabbedPane);
        if (globeBackgroundDisplayable == null) {
            return;
        }





        globeBackgroundLevelSlider = new ZSlider(globeBackgroundLevel);
        JComponent levelComp = globeBackgroundLevelSlider.getContents();
        JComponent[] bgComps =
            GuiUtils.makeColorSwatchWidget(globeBackgroundColor,
                                           "Globe Background Color");


        globeBackgroundColorComp = bgComps[0];
        JComponent comp = GuiUtils.formLayout(new Component[] {
                              GuiUtils.rLabel("Color:"),
                              GuiUtils.left(bgComps[0]),
                              GuiUtils.rLabel("Level:"),
                              levelComp });
        tabbedPane.add("Globe Background", GuiUtils.top(comp));
    }


    /**
     * _more_
     *
     * @param globe _more_
     */
    private void setGlobeBackground(GlobeDisplay globe) {
        try {
            if (globeBackgroundDisplayable == null) {
                FlatField ff = ucar.visad.Util.makeField(-180, 180, 180, 90,
                                   -90, 180, 1, "celsius");
                Data d = ff;
                globeBackgroundDisplayable = new LineDrawing("background");
                globeBackgroundDisplayable.setData(d);
                globe.addDisplayable(globeBackgroundDisplayable);
            }

            //            Color c = new Color(globeBackgroundColor.getRed(),
            //                                globeBackgroundColor.getGreen(),
            //                                globeBackgroundColor.getBlue(),
            //                                (int)(255*0.5));

            globeBackgroundDisplayable.setColor(globeBackgroundColor);
            globeBackgroundDisplayable.setVisible(getGlobeBackgroundShow());

            DisplayRealType drt          = globe.getDisplayAltitudeType();
            double[]        range        = new double[2];
            double          realPosition = 1;
            if (drt.getRange(range)) {
                double pcnt = (globeBackgroundLevel - (-1)) / 2;
                realPosition = Math.min((range[0]
                                         + (range[1] - range[0])
                                           * pcnt), range[1]);
            }

            ConstantMap constantMap = new ConstantMap(realPosition, drt);
            globeBackgroundDisplayable.addConstantMap(constantMap);
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }


    /**
     * _more_
     */
    public void destroy() {
        if (flythrough != null) {
            try {
                flythrough.destroy();
            } catch(Exception ignore) {}
            flythrough = null;
        }
        super.destroy();
    }



    /**
     * _more_
     */
    public void showFlythrough() {
        if (flythrough == null) {
            flythrough = new Flythrough(this);
        }
        flythrough.show();
    }


    /**
     * _more_
     *
     * @param pts _more_
     */
    public void flythrough(final float[][] pts) {
        if (flythrough == null) {
            flythrough = new Flythrough(this);
        }
        flythrough.flythrough(pts);
    }


    /**
     * _more_
     *
     * @param pts _more_
     */
    public void flythrough(List<FlythroughPoint> pts) {
        if (flythrough == null) {
            flythrough = new Flythrough(this);
        }
        flythrough.flythrough(pts);
    }

    /**
     * _more_
     *
     * @param idv _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public final void initAfterUnPersistence(IntegratedDataViewer idv)
            throws VisADException, RemoteException {
        super.initAfterUnPersistence(idv);
    }



    /**
     * Reset projection of display based control's getDataProjection().
     * called by DisplayInfo.addDisplayable (), usually from control's init.
     *
     * @param displayInfo The new display info to add
     * @return True if the addiiton worked. false otherwise
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public boolean addDisplayInfo(DisplayInfo displayInfo)
            throws RemoteException, VisADException {

        if (getIsDestroyed()) {
            return false;
        }

        //Check if we are adding a default map.
        DisplayControl display = displayInfo.getDisplayControl();
        if ((display instanceof MapDisplayControl)
                && ((MapDisplayControl) display).getIsDefaultMap()) {
            MapDisplayControl defaultMap = findDefaultMap();
            if ((defaultMap != null) && (defaultMap != display)) {
                defaultMap.loadNewMap((MapDisplayControl) display);
                //This rebuilds the legends, etc.
                displayControlChanged(defaultMap);
                return false;
            }
        }



        try {
            if ( !getUseGlobeDisplay() && getUseProjectionFromData()
                    && !getStateManager().getProperty(
                        IdvConstants.PROP_LOADINGXML, false)) {
                MapProjection mp = display.getDataProjection();
                if (displayProjectionOk(mp)) {
                    if ((mainProjection == null)
                            || !mp.equals(mainProjection)) {
                        setMapProjection(
                            mp, true,
                            getDisplayConventions().getMapProjectionLabel(
                                mp, display));
                    }
                }
            }
        } catch (Exception exp) {
            // ignore, don't set anything.   Uncomment for debugging
            // LogUtil.logException ( "addDisplayInfo:setMapProjection()", exp);
        }

        if ( !super.addDisplayInfo(displayInfo)) {
            return false;
        }



        return true;
    }

    /**
     * Reset projection of display based on data.
     * @deprecated  no substitute.  Use setMapProjection()
     *
     * @param data The data form the display
     * @param display The display
     */
    public void checkProjection(FieldImpl data, DisplayControl display) {
        try {
            MapProjection mp = GridUtil.getNavigation(data);
            if (mp == null) {
                return;
            }
            setMapProjection(
                mp, false,
                getDisplayConventions().getMapProjectionLabel(mp, display));
        } catch (Exception exp) {}  // do nothing - no projection in data
    }





    /**
     * Required interface for ActionEvents, to implement ActionListener
     * for the UI objects such as JButton-s and MenuItem-s
     *
     * @param event an ActionEvent
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(CMD_NAV_ZOOMIN)) {
            getMapDisplay().zoom(ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_ROTATELEFT)) {
            getMapDisplay().rotateZ(-5.0);
        } else if (cmd.equals(CMD_NAV_ROTATERIGHT)) {
            getMapDisplay().rotateZ(5.0);
        } else if (cmd.equals(CMD_NAV_ZOOMOUT)) {
            getMapDisplay().zoom(1.0 / (double) ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_HOME)) {
            try {
                getMapDisplay().resetProjection();
            } catch (Exception exc) {}
        } else if (cmd.equals(CMD_NAV_RIGHT)) {
            getMapDisplay().translate(-TRANSLATE_FACTOR, 0.0);
        } else if (cmd.equals(CMD_NAV_LEFT)) {
            getMapDisplay().translate(TRANSLATE_FACTOR, 0.0);
        } else if (cmd.equals(CMD_NAV_UP)) {
            getMapDisplay().translate(0.0, -TRANSLATE_FACTOR);
        } else if (cmd.equals(CMD_NAV_DOWN)) {
            getMapDisplay().translate(0.0, TRANSLATE_FACTOR);
        } else if (cmd.equals(CMD_NAV_SMALLZOOMIN)) {
            getMapDisplay().zoom(ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_SMALLZOOMOUT)) {
            getMapDisplay().zoom(1.0 / ZOOM_FACTOR);
        } else if (cmd.equals(CMD_NAV_SMALLROTATELEFT)) {
            getMapDisplay().rotateZ(-2.0);
        } else if (cmd.equals(CMD_NAV_SMALLROTATERIGHT)) {
            getMapDisplay().rotateZ(2.0);
        } else if (cmd.equals(CMD_NAV_SMALLTILTUP)) {
            getMapDisplay().rotateX(-2.0);
        } else if (cmd.equals(CMD_NAV_SMALLTILTDOWN)) {
            getMapDisplay().rotateX(2.0);
        } else if (cmd.equals(CMD_NAV_SMALLRIGHT)) {
            getMapDisplay().translate(-0.02, 0.0);
        } else if (cmd.equals(CMD_NAV_SMALLLEFT)) {
            getMapDisplay().translate(0.02, 0.0);
        } else if (cmd.equals(CMD_NAV_SMALLUP)) {
            getMapDisplay().translate(0.0, -0.02);
        } else if (cmd.equals(CMD_NAV_SMALLDOWN)) {
            getMapDisplay().translate(0.0, 0.02);
        }

    }


    /**
     * Get the bounds that are visible
     *
     * @return bounds
     */
    public GeoLocationInfo getVisibleGeoBounds() {
        NavigatedDisplay navDisplay = getMapDisplay();
        Rectangle        screenBounds;

        screenBounds = navDisplay.getDisplayComponent().getBounds();
        if ( !getIdv().getArgsManager().getIsOffScreen()) {
            //            screenBounds = navDisplay.getComponent().getBounds();
        } else {
            //            Dimension 
        }
        double[] ulXY = getMapDisplay().getSpatialCoordinatesFromScreen(0, 0);
        double[] lrXY = getMapDisplay().getSpatialCoordinatesFromScreen(
                            screenBounds.width, screenBounds.height);

        LatLonPoint ulLLP =
            getMapDisplay().getEarthLocation(ulXY).getLatLonPoint();
        LatLonPoint lrLLP =
            getMapDisplay().getEarthLocation(lrXY).getLatLonPoint();



        double minX = Math.min(ulLLP.getLongitude().getValue(),
                               lrLLP.getLongitude().getValue());
        double maxX = Math.max(ulLLP.getLongitude().getValue(),
                               lrLLP.getLongitude().getValue());
        double minY = Math.min(ulLLP.getLatitude().getValue(),
                               lrLLP.getLatitude().getValue());
        double maxY = Math.max(ulLLP.getLatitude().getValue(),
                               lrLLP.getLatitude().getValue());
        Rectangle2D.Float rect = new Rectangle2D.Float((float) minX,
                                     (float) minY, (float) (maxX - minX),
                                     (float) (maxY - minY));

        return new GeoLocationInfo(maxY, minX, minY, maxX);
    }

    /**
     * Set the current viewpoint as the projection
     */
    public void setCurrentAsProjection() {
        try {
            NavigatedDisplay display      = getMapDisplay();
            Rectangle        screenBounds =
                display.getComponent().getBounds();
            LatLonPoint      ulLLP        = null;
            LatLonPoint      lrLLP        = null;

            int              sw           = screenBounds.width;
            int              sh           = screenBounds.height;
            int              x            = 0;
            int              y            = 0;

            while ((x < sw) && (y < sh)) {
                double[] ulXY = display.getSpatialCoordinatesFromScreen(x, y);
                ulLLP =
                    getMapDisplay().getEarthLocation(ulXY).getLatLonPoint();
                if ( !ulLLP.getLatitude().isMissing()
                        && !ulLLP.getLongitude().isMissing()) {
                    break;
                }
                ulLLP = null;
                x++;
                y++;
            }

            while ((sw > 0) && (sh > 0)) {
                double[] lrXY = display.getSpatialCoordinatesFromScreen(sw,
                                    sh);
                lrLLP =
                    getMapDisplay().getEarthLocation(lrXY).getLatLonPoint();
                if ( !lrLLP.getLatitude().isMissing()
                        && !lrLLP.getLongitude().isMissing()) {
                    break;
                }
                lrLLP = null;
                sw--;
                sh--;
            }

            if ((ulLLP == null) || (lrLLP == null)) {
                LogUtil.userMessage("Could not create a valid projection");
                return;
            }


            double minX = Math.min(ulLLP.getLongitude().getValue(),
                                   lrLLP.getLongitude().getValue());
            double maxX = Math.max(ulLLP.getLongitude().getValue(),
                                   lrLLP.getLongitude().getValue());
            double minY = Math.min(ulLLP.getLatitude().getValue(),
                                   lrLLP.getLatitude().getValue());
            double maxY = Math.max(ulLLP.getLatitude().getValue(),
                                   lrLLP.getLatitude().getValue());
            Rectangle2D.Float rect = new Rectangle2D.Float((float) minX,
                                         (float) minY, (float) (maxX - minX),
                                         (float) (maxY - minY));

            MapProjection mp = ucar.visad.Util.makeMapProjection(minY, minX,
                                   maxY, maxX);
            setMapProjection(mp, true);
            getMapDisplay().zoom(ZOOM_FACTOR);
            getMapDisplay().saveProjection();
        } catch (Exception exp) {
            logException("Setting projection", exp);
        }
    }


    /**
     * Show the projection manager.
     */
    public void showProjectionManager() {
        getIdv().showIdvProjectionManager();
    }




    /**
     * Set or reset map area of view, using NavigatedDisplay method.
     *
     * @param mapArea ProjectionRect the map area of view
     */
    public void setMapArea(ProjectionRect mapArea) {
        try {
            getMapDisplay().setMapArea(mapArea);
        } catch (Exception e) {
            logException("setMapArea", e);
        }
    }

    /**
     * Check if the display projection is okay
     *
     * @param mp  map projection to check
     *
     * @return true if okay
     */
    private boolean displayProjectionOk(MapProjection mp) {
        if (mp == null) {
            return false;
        }
        Rectangle2D rect = mp.getDefaultMapArea();
        if ((rect.getWidth() == 0) || (rect.getHeight() == 0)) {
            return false;
        } else {
            if (rect.getWidth() / rect.getHeight() < 0.1) {
                return false;
            } else if (rect.getHeight() / rect.getWidth() < 0.1) {
                return false;
            }
        }
        return true;
    }



    /**
     * Init menu
     *
     * @param projectionsMenu menu
     */
    public void initializeProjectionMenu(JMenu projectionsMenu) {
        List projections = getProjectionList();
        List controls    = getControls();
        if ( !getUseGlobeDisplay()) {
            //            projectionsMenu.add(GuiUtils.makeMenuItem("Use Displayed Area",
            //                    this, "setCurrentAsProjection"));
        }
        ProjectionImpl currentProjection = null;
        if ((mainProjection != null)
                && (mainProjection instanceof ProjectionCoordinateSystem)) {
            currentProjection =
                ((ProjectionCoordinateSystem) mainProjection).getProjection();
        }


        makeProjectionsMenu(projectionsMenu, projections, this,
                            "setProjection", currentProjection);
    }



    /**
     * Init menu
     *
     * @param displaysMenu menu
     */
    public void initializeDisplaysProjectionMenu(JMenu displaysMenu) {
        List controls = getControls();
        int  cnt      = 0;
        for (int i = 0; i < controls.size(); i++) {
            final DisplayControl control = (DisplayControl) controls.get(i);
            final MapProjection  mp      = control.getDataProjection();
            if ( !displayProjectionOk(mp)) {
                continue;
            }

            final String label =
                getDisplayConventions().getMapProjectionLabel(mp, control);
            JMenuItem mi = new JMenuItem(label);
            displaysMenu.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setMapProjection(mp, false, label);
                }
            });
            cnt++;
        }
        if (cnt == 0) {
            displaysMenu.add(new JMenuItem("None Defined"));
        }
    }




    /**
     * Make the projections menu. Call the method on the given object.
     *
     * @param projectionsMenu Menu to add to
     * @param projections List of projections
     * @param object object to call
     * @param method method to call
     */
    public static void makeProjectionsMenu(JMenu projectionsMenu,
                                           List projections, Object object,
                                           String method) {
        makeProjectionsMenu(projectionsMenu, projections, object, method,
                            null);
    }


    /**
     * Make the projections menu
     *
     * @param projectionsMenu the menu to add to
     * @param projections list of projections
     * @param object object to call
     * @param method  method to call
     * @param currentProjection current projection
     */
    public static void makeProjectionsMenu(JMenu projectionsMenu,
                                           List projections, Object object,
                                           String method,
                                           ProjectionImpl currentProjection) {
        if (currentProjection != null) {
            List names = StringUtil.split(currentProjection.getName(), ">",
                                          true, true);
            if (names.size() > 0) {
                String name = "Current: "
                              + (String) names.get(names.size() - 1);
                JMenuItem mi = GuiUtils.makeMenuItem(name, object, method,
                                   currentProjection);

                projectionsMenu.add(mi);
                projectionsMenu.addSeparator();
            }
        }

        Hashtable catMenus = new Hashtable();
        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl p = (ProjectionImpl) projections.get(i);
            List<String> names = StringUtil.split(p.getName(), ">", true,
                                     true);
            JMenu  theMenu  = projectionsMenu;
            String catSoFar = "";
            int    catIdx   = 0;
            for (catIdx = 0; catIdx < names.size() - 1; catIdx++) {
                String cat = (String) names.get(catIdx);
                catSoFar += "-" + cat;
                JMenu tmpMenu = (JMenu) catMenus.get(catSoFar);
                if (tmpMenu == null) {
                    tmpMenu = new JMenu(cat);
                    catMenus.put(catSoFar, tmpMenu);
                    theMenu.add(tmpMenu);
                }
                theMenu = tmpMenu;
            }
            String  name      = ((catIdx < names.size())
                                 ? names.get(catIdx)
                                 : "");
            boolean isCurrent = Misc.equals(p, currentProjection);
            if (isCurrent) {
                //              name = "> " + name;
            }
            JMenuItem mi = GuiUtils.makeMenuItem(name, object, method, p);
            theMenu.add(mi);
            if (isCurrent) {
                GuiUtils.italicizeFont(mi.getComponent());
            }
        }


    }




    /**
     * Init menu
     *
     * @param menu menu
     */
    public void initializeProjectionHistoryMenu(JMenu menu) {
        menu.removeAll();
        for (int i = 0; i < projectionHistory.size(); i++) {
            final TwoFacedObject tfo =
                (TwoFacedObject) projectionHistory.get(i);
            JMenuItem mi = new JMenuItem(tfo.toString());
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setMapProjection((MapProjection) tfo.getId(), true);
                }
            });
            menu.add(mi);
        }
    }


    /**
     * _more_
     *
     * @param vm _more_
     *
     * @return _more_
     */
    public boolean isCompatibleWith(ViewManager vm) {
        if ( !super.isCompatibleWith(vm)) {
            return false;
        }
        MapViewManager that = (MapViewManager) vm;
        return this.getUseGlobeDisplay() == that.getUseGlobeDisplay();
    }


    /**
     * _more_
     *
     * @param viewState _more_
     *
     * @return _more_
     */
    public boolean isCompatibleWith(ViewState viewState) {
        if ( !super.isCompatibleWith(viewState)) {
            return false;
        }
        Boolean b = (Boolean) viewState.get(ViewState.PROP_GLOBE);
        if (b != null) {
            return getUseGlobeDisplay() == b.booleanValue();
        }
        return true;
    }


    /**
     * Make the "Projections" menu to be added to the menu bar, which provides
     * controls for maps.
     * @return JMenu
     */
    private JMenu makeProjectionMenu() {
        JMenu projMenu = new JMenu("Projections");
        projMenu.setMnemonic(GuiUtils.charToKeyCode("P"));
        projectionsMenu = GuiUtils.makeDynamicMenu("Predefined", this,
                "initializeProjectionMenu");
        JMenu displaysMenu = GuiUtils.makeDynamicMenu("From Displays", this,
                                 "initializeDisplaysProjectionMenu");

        if ( !getUseGlobeDisplay()) {
            projMenu.add(projectionsMenu);
            projMenu.add(displaysMenu);
        }
        projMenu.add(makeSavedViewsMenu());
        JMenu projectionHistoryMenu = GuiUtils.makeDynamicMenu("History",
                                          this,
                                          "initializeProjectionHistoryMenu");

        if ( !getUseGlobeDisplay()) {
            projMenu.add(projectionHistoryMenu);
        }


        if ( !getUseGlobeDisplay()) {
            projMenu.addSeparator();
            projMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("New/Edit...",
                    this,
                    "showProjectionManager"), "/auxdata/ui/icons/world_edit.png"));
            projMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Use Displayed Area",
                    this,
                    "setCurrentAsProjection"), "/auxdata/ui/icons/world_rect.png"));
        }
        projMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Go to Address",
                this, "goToAddress"), "/auxdata/ui/icons/house_go.png"));

        if ( !getUseGlobeDisplay()) {
            projMenu.addSeparator();
            createCBMI(projMenu, PREF_PROJ_USEFROMDATA).setToolTipText(
                "Automatically change the projection to the native data projection of new displays");
        }
        createCBMI(projMenu, PREF_SHAREVIEWS);
        projMenu.add(GuiUtils.makeMenuItem("Set Share Group", this,
                                           "showSharableDialog"));
        return projMenu;
    }


    /**
     * Have this so we don't get warnings on unpersisting old bundles
     *
     * @param location The location
     */
    public void setMapConfigFile(String location) {}



    /**
     * Use globe display. Used for xml encoding/decoding.
     *
     * @param use The globe display value
     */
    public void setUseGlobeDisplay(boolean use) {
        useGlobeDisplay = use;
    }

    /**
     * Get the globe display flag. Used for xml encoding/decoding.
     *
     * @return The globe display value
     */
    public boolean getUseGlobeDisplay() {
        return useGlobeDisplay;
    }


    /**
     * Use a 3D display. Used for xml encoding/decoding.
     *
     * @param use The use 3D display value
     */
    public void setUse3D(boolean use) {
        use3D = use;
    }

    /**
     * Get the use 3D display flag. Used for xml encoding/decoding.
     *
     * @return The use 3D value
     */
    public boolean getUse3D() {
        return use3D;
    }



    /**
     * The BooleanProperty identified byt he given id has changed.
     * Apply the change to the display.
     *
     * @param id Id of the changed BooleanProperty
     * @param value Its new value
     *
     * @throws Exception problem handeling the change
     */
    protected void handleBooleanPropertyChange(String id, boolean value)
            throws Exception {
        super.handleBooleanPropertyChange(id, value);
        if (id.equals(PREF_AUTOROTATE)) {
            if (hasViewpointControl()) {
                getViewpointControl().setAutoRotate(value);
            }
        } else if (id.equals(PREF_SHOWSCALES)) {
            if (hasDisplayMaster()) {
                getNavigatedDisplay().setScalesVisible(value);
            }
        } else if (id.equals(PREF_SHOWEARTHNAVPANEL)) {
            if (earthNavPanelWrapper != null) {
                earthNavPanelWrapper.removeAll();
                if (value) {
                    earthNavPanelWrapper.add(BorderLayout.CENTER,
                                             earthNavPanel);
                }
            }
        } else if (id.equals(PREF_SHOWPIP)) {
            if (pipPanelWrapper != null) {
                pipPanelWrapper.setVisible(value);
            }
        } else if (id.equals(PREF_SHOWGLOBEBACKGROUND)) {
            if (globeBackgroundDisplayable != null) {
                globeBackgroundDisplayable.setVisible(value);
            }
        } else if (id.equals(PREF_PERSPECTIVEVIEW)) {
            if (hasViewpointControl()) {
                getViewpointControl().setPerspectiveView(value);
            }
        }
    }

    /**
     * Create the set of {@link ucar.unidata.util.BooleanProperty}s.
     * These hold all of the different flag based display state.
     *
     * @param props the list of properties
     */
    protected void getInitialBooleanProperties(List props) {
        super.getInitialBooleanProperties(props);
        props.add(new BooleanProperty(PREF_SHOWSCALES, "Show Display Scales",
                                      "Show Display Scales", false));

        props.add(
            new BooleanProperty(
                PREF_PROJ_USEFROMDATA, "Auto-set Projection",
                "Use projection from newly loaded data", true));
        props.add(new BooleanProperty(PREF_PERSPECTIVEVIEW,
                                      "Perspective View",
                                      "Toggle perspective view", false));
        props.add(new BooleanProperty(PREF_AUTOROTATE, "Auto-rotate", "",
                                      false));
        props.add(new BooleanProperty(PREF_SHOWEARTHNAVPANEL,
                                      "Show Earth Navigation Panel",
                                      "Show Earth Navigation Panel", false));

        props.add(new BooleanProperty(PREF_SHOWPIP, "Show Overview Map",
                                      "Show Overview Map", false));

        if (useGlobeDisplay) {
            props.add(new BooleanProperty(PREF_SHOWGLOBEBACKGROUND,
                                          "Show Globe Background",
                                          "Show Globe Background", false));
        }
    }



    /**
     * Set the autorotate property
     *
     * @param value The value
     */
    public void setAutoRotate(boolean value) {
        setBp(PREF_AUTOROTATE, value);
    }

    /**
     * Get  the autorotate flag
     * @return The flag value
     */
    public boolean getAutoRotate() {
        return getBp(PREF_AUTOROTATE);
    }

    /**
     * Set the  perspective view flag
     *
     * @param value The value
     */
    public void setPerspectiveView(boolean value) {
        setBp(PREF_PERSPECTIVEVIEW, value);
    }

    /**
     * Get  the perspective view  flag
     * @return The flag value
     */
    public boolean getPerspectiveView() {
        return getBp(PREF_PERSPECTIVEVIEW);
    }


    /**
     * Dummy for old bundles
     *
     * @param value The value
     */
    public void setShowMap(boolean value) {}


    /**
     * Dummy for old bundles
     *
     * @param value The value
     */
    public void setShowElevation(boolean value) {}


    /**
     * Set the  use projection from data flag
     *
     * @param value The value
     */
    public void setUseProjectionFromData(boolean value) {
        setBp(PREF_PROJ_USEFROMDATA, value);
    }

    /**
     * Get  the use projection from data  flag
     * @return The flag value
     */
    public boolean getUseProjectionFromData() {
        if ( !isInteractive()) {
            return true;
        }
        return getBp(PREF_PROJ_USEFROMDATA);
    }


    /**
     * Set the background color property.
     * @deprecated  Keep this around for old bundles
     * @param bgColor The value
     */
    public void setBgColor(boolean bgColor) {
        if ( !bgColor) {
            setBackground(Color.white);
            setForeground(Color.black);
        } else {
            setBackground(Color.black);
            setForeground(Color.white);
        }
    }


    /**
     * Set the ShowEarthNavPanel property.
     *
     * @param value The new value for ShowEarthNavPanel
     */
    public void setShowEarthNavPanel(boolean value) {
        setBp(PREF_SHOWEARTHNAVPANEL, value);
    }

    /**
     * Get the ShowEarthNavPanel property.
     *
     * @return The ShowEarthNavPanel
     */
    public boolean getShowEarthNavPanel() {
        return getBp(PREF_SHOWEARTHNAVPANEL);
    }


    /**
     * _more_
     */
    public void hidePip() {
        setShowPip(false);
    }

    /**
     * Set the ShowPipPanel property.
     *
     * @param value The new value for ShowPipPanel
     */
    public void setShowPip(boolean value) {
        setBp(PREF_SHOWPIP, value);
    }

    /**
     * Get the ShowPipPanel property.
     *
     * @return The ShowPipPanel
     */
    public boolean getShowPip() {
        return getBp(PREF_SHOWPIP, false);
    }


    /**
     * Set the InitialMapResources property. This gets set
     * by the viewmanager properties and is a comma separated
     * list of map resource paths.
     *
     * @param value The new value for InitialMapResources
     */
    public void setInitialMapResources(String value) {
        initialMapResources = value;
    }



    /**
     * What type of view is this
     *
     * @return The type of view
     */
    public String getTypeName() {
        if (getUseGlobeDisplay()) {
            return "Globe";
        }
        return "Map";
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public float getDefaultMapPosition() {
        if (getUseGlobeDisplay()) {
            return -0.0f;
        } else {
            return -0.99f;
        }
    }

    /**
     *  Set the GlobeBackgroundColor property.
     *
     *  @param value The new value for GlobeBackgroundColor
     */
    public void setGlobeBackgroundColor(Color value) {
        globeBackgroundColor = value;
    }


    /**
     *  Get the GlobeBackgroundColor property.
     *
     *  @return The GlobeBackgroundColor
     */
    public Color getGlobeBackgroundColor() {
        return globeBackgroundColor;
    }

    /**
     *  Set the GlobeBackgroundShow property.
     *
     *  @param value The new value for GlobeBackgroundShow
     */
    public void setGlobeBackgroundShow(boolean value) {
        setBp(PREF_SHOWGLOBEBACKGROUND, value);
    }

    /**
     *  Get the GlobeBackgroundShow property.
     *
     *  @return The GlobeBackgroundShow
     */
    public boolean getGlobeBackgroundShow() {
        if (hasBooleanProperty(PREF_SHOWGLOBEBACKGROUND)) {
            return getBp(PREF_SHOWGLOBEBACKGROUND, false);
        }
        XmlObjectStore store = getStore();
        if (store != null) {
            return store.get(PREF_SHOWGLOBEBACKGROUND, false);
        }
        return false;
    }

    /**
     *  Set the GlobeBackgroundLevel property.
     *
     *  @param value The new value for GlobeBackgroundLevel
     */
    public void setGlobeBackgroundLevel(double value) {
        globeBackgroundLevel = value;
    }

    /**
     *  Get the GlobeBackgroundLevel property.
     *
     *  @return The GlobeBackgroundLevel
     */
    public double getGlobeBackgroundLevel() {
        return globeBackgroundLevel;
    }

    /**
     * Set the Flythrough property.
     *
     * @param value The new value for Flythrough
     */
    public void setFlythrough(Flythrough value) {
        this.flythrough = value;
    }

    /**
     * Get the Flythrough property.
     *
     * @return The Flythrough
     */
    public Flythrough getFlythrough() {
        return this.flythrough;
    }




}


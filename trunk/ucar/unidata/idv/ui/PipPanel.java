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


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;


import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.flythrough.Flythrough;
import ucar.unidata.idv.flythrough.FlythroughPoint;


import ucar.unidata.util.Misc;

import ucar.unidata.view.geoloc.*;
import ucar.unidata.view.geoloc.*;




import ucar.visad.ProjectionCoordinateSystem;


import visad.*;

import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;



import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.List;

import javax.swing.*;
import javax.swing.border.EtchedBorder;


/**
 * A navigated panel that holds a set of maps.
 * @author Jeff McWhirter
 */
public class PipPanel extends NavigatedMapPanel {

    /** How much to zoom in */
    private static final double ZOOM_IN = 1.1;

    /** How much to zoom out */
    private static final double ZOOM_OUT = 0.9;

    /** the mvm */
    private MapViewManager mapViewManager;

    /** Draws the overview box */
    List thePoints = new ArrayList();



    /** Used when dragging rect */
    private ProjectionPointImpl deltaFromOrigin;


    /** Are we about to redraw */
    private boolean pendingRedraw = false;

    /**
     * Create a NMP with a set of maps
     *
     *
     * @param mapViewManager the mvm
     */
    public PipPanel(MapViewManager mapViewManager) {
        this(mapViewManager, null);
    }


    /**
     * Create a NMP with a set of maps
     *
     *
     * @param mapViewManager the mvm
     * @param defaultMaps   default set of maps.
     */
    public PipPanel(MapViewManager mapViewManager, List defaultMaps) {
        super(defaultMaps, false);
        this.mapViewManager = mapViewManager;
        setProjectionImpl(mapViewManager.getDefaultProjection());
        navigatedPanel.setToolTipText(
            "<html>Use arrow keys to scroll.<br>Shift-up: zoom in; Shift-down: zoom out<br>Control-r: reset;<br>Control-p: Use projection from display</html>");

        navigatedPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        navigatedPanel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                keyPressedInMap(e);
            }
        });
        navigatedPanel.zoom(ZOOM_OUT);
        navigatedPanel.zoom(ZOOM_OUT);
    }


    /**
     * Make the map panel
     *
     * @return map panel
     */
    protected NavigatedPanel doMakeMapPanel() {
        return new MyMapPanel();
    }


    /**
     * Handle event
     *
     * @param e The event
     */
    public void keyPressedInMap(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_UP) && e.isShiftDown()) {
            navigatedPanel.zoom(ZOOM_IN);
        } else if ((e.getKeyCode() == KeyEvent.VK_DOWN) && e.isShiftDown()) {
            navigatedPanel.zoom(ZOOM_OUT);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            navigatedPanel.doMoveUp(5);
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            navigatedPanel.doMoveDown(5);
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            navigatedPanel.doMoveRight(5);
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            navigatedPanel.doMoveLeft(5);
        } else if ((e.getKeyCode() == KeyEvent.VK_R) && e.isControlDown()) {
            navigatedPanel.resetZoom();
        } else if ((e.getKeyCode() == KeyEvent.VK_P) && e.isControlDown()) {
            MapProjection proj = mapViewManager.getMainProjection();
            if ((proj instanceof ProjectionCoordinateSystem)) {
                setProjectionImpl(
                    ((ProjectionCoordinateSystem) proj).getProjection());
                navigatedPanel.zoom(ZOOM_OUT);
                navigatedPanel.zoom(ZOOM_OUT);
            }
        }

    }



    /**
     * Utility conversion
     *
     * @param pt From point
     *
     * @return Converted point
     */
    private ucar.unidata.geoloc.LatLonPointImpl getPoint(
            visad.georef.LatLonPoint pt) {
        return new LatLonPointImpl(pt.getLatitude().getValue(),
                                   pt.getLongitude().getValue());

    }

    /**
     * Set the drawing bounds.
     *
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void resetDrawBounds() throws RemoteException, VisADException {
        if (mapViewManager == null) {
            return;
        }
        NavigatedDisplay nav = (NavigatedDisplay) mapViewManager.getMaster();
        if (nav == null) {
            return;
        }
        List points = new ArrayList();
        points.add(
            getPoint(
                nav.getEarthLocation(
                    nav.getScreenUpperLeft()).getLatLonPoint()));
        points.add(
            getPoint(
                nav.getEarthLocation(
                    nav.getScreenUpperRight()).getLatLonPoint()));
        points.add(
            getPoint(
                nav.getEarthLocation(
                    nav.getScreenLowerRight()).getLatLonPoint()));
        points.add(
            getPoint(
                nav.getEarthLocation(
                    nav.getScreenLowerLeft()).getLatLonPoint()));
        this.thePoints = points;
        redrawInABit();
    }


    /**
     * Redraw in a little while
     */
    public void redrawInABit() {
        if (pendingRedraw) {
            return;
        }
        pendingRedraw = true;
        Misc.runInABit(100, this, "doRedraw", null);
    }

    /**
     * Redraw if needed
     */
    public void doRedraw() {
        if ( !pendingRedraw) {
            return;
        }
        pendingRedraw = false;
        redraw();
    }

    /**
     * Draw the overview box in the map
     *
     * @param gNP graphics to draw in
     */
    protected void annotateMap(Graphics2D gNP) {
        List points = thePoints;
        if (points.size() == 0) {
            return;
        }
        ProjectionImpl project = getProjection();
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                           points.size());
        for (int i = 0; i <= points.size(); i++) {
            LatLonPoint llp;
            if (i >= points.size()) {
                llp = (LatLonPoint) points.get(0);
            } else {
                llp = (LatLonPoint) points.get(i);
            }

            ProjectionPoint ppi = project.latLonToProj(llp,
                                      new ProjectionPointImpl());
            if (i == 0) {
                path.moveTo((float) ppi.getX(), (float) ppi.getY());
            } else {
                path.lineTo((float) ppi.getX(), (float) ppi.getY());
            }
        }
        gNP.setColor(Color.red);
        gNP.draw(path);

        Flythrough flythrough = mapViewManager.getFlythrough();
        if (flythrough != null) {
            FlythroughPoint currentPoint = flythrough.getCurrentPoint();
            if (currentPoint != null) {
                try {
                    Real lat =
                        currentPoint.getEarthLocation().getLatLonPoint()
                            .getLatitude();
                    Real lon =
                        currentPoint.getEarthLocation().getLatLonPoint()
                            .getLongitude();

                    ProjectionPoint p =
                        project.latLonToProj(new LatLonPointImpl(lat
                            .getValue(CommonUnit.degree), lon
                            .getValue(CommonUnit
                                .degree)), new ProjectionPointImpl());
                    gNP.setColor(Color.blue);
                    GeneralPath path2 =
                        new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                        points.size());
                    double          dx        = 4;
                    double          dy        = 4;
                    AffineTransform transform = gNP.getTransform();
                    if (transform != null) {
                        double sx = transform.getScaleX();
                        double sy = transform.getScaleX();
                        if (sx != 0) {
                            dx = dx / sx;
                        }
                        if (sy != 0) {
                            dy = dy / sy;
                        }
                    }

                    path2.moveTo((float) (p.getX() - dx),
                                 (float) (p.getY() - dy));
                    path2.lineTo((float) (p.getX() + dx),
                                 (float) (p.getY() - dy));
                    path2.lineTo((float) (p.getX() + dx),
                                 (float) (p.getY() + dy));
                    path2.lineTo((float) (p.getX() - dx),
                                 (float) (p.getY() + dy));
                    path2.lineTo((float) (p.getX() - dx),
                                 (float) (p.getY() - dy));

                    gNP.fill(path2);
                } catch (Exception exc) {}
            }
        }

    }


    /**
     * Class MyMapPanel handles mouse drag events
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.24 $
     */
    private class MyMapPanel extends NavigatedPanel {

        /**
         * Handle the mouse dragged event.
         *
         * @param e  event to handle
         */
        public void mouseDragged(MouseEvent e) {
            if ( !SwingUtilities.isLeftMouseButton(e) || e.isShiftDown()) {
                super.mouseDragged(e);
                return;
            }

            try {
                List points = thePoints;
                if ((points.size() == 0) || (deltaFromOrigin == null)) {
                    return;
                }
                ProjectionPointImpl current =
                    navigatedPanel.getNavigation().screenToWorld(
                        new Point2D.Double(e.getX(), e.getY()),
                        new ProjectionPointImpl());
                ProjectionPointImpl newOrigin =
                    new ProjectionPointImpl(
                        current.getX() - deltaFromOrigin.getX(),
                        current.getY() - deltaFromOrigin.getY());
                LatLonPoint llp = getProjection().projToLatLon(newOrigin);
                EarthLocationTuple el =
                    new EarthLocationTuple(new Real(RealType.Latitude,
                        llp.getLatitude()), new Real(RealType.Longitude,
                            llp.getLongitude()), new Real(RealType.Altitude,
                                0));
                NavigatedDisplay nav =
                    (NavigatedDisplay) mapViewManager.getMaster();
                double[] destXY = nav.getSpatialCoordinates(el,
                                      (double[]) null);
                if ( !mapViewManager.getUseGlobeDisplay()) {
                    nav.moveToScreen(destXY[0], destXY[1], 0, 0);
                }
            } catch (Exception exc) {
                //TODO
            }
        }



        /**
         * Handle event
         *
         * @param e event
         */
        public void mousePressed(MouseEvent e) {
            requestFocus();
            if ( !SwingUtilities.isLeftMouseButton(e) || e.isShiftDown()) {
                super.mousePressed(e);
                return;
            }
            List points = thePoints;
            if (points.size() == 0) {
                return;
            }
            ProjectionPointImpl origin =
                (ProjectionPointImpl) getProjection().latLonToProj(
                    (LatLonPoint) points.get(0), new ProjectionPointImpl());


            ProjectionPointImpl pt =
                navigatedPanel.getNavigation().screenToWorld(
                    new Point2D.Double(e.getX(), e.getY()),
                    new ProjectionPointImpl());

            deltaFromOrigin = new ProjectionPointImpl(pt.getX()
                    - origin.getX(), pt.getY() - origin.getY());
        }
    }


    /**
     * _more_
     *
     * @param x _more_
     * @param y _more_
     *
     * @return _more_
     */
    public LatLonPoint screenToLatLon(int x, int y) {
        ProjectionPointImpl point =
            navigatedPanel.getNavigation().screenToWorld(
                new Point2D.Double(x, y), new ProjectionPointImpl());
        return getProjection().projToLatLon(point);
    }



}

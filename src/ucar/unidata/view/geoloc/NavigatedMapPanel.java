/*
 * $Id: NavigatedMapPanel.java,v 1.13 2007/04/20 14:05:54 dmurray Exp $
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



package ucar.unidata.view.geoloc;


import ucar.unidata.beans.NonVetoableProperty;
import ucar.unidata.beans.Property;
import ucar.unidata.beans.PropertySet;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.view.Renderer;
import ucar.unidata.view.geoloc.*;



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import java.util.ArrayList;

import java.util.List;

import javax.swing.*;
import javax.swing.border.EtchedBorder;


/**
 * A navigated panel that holds a set of maps.
 * @author Jeff McWhirter
 */
public class NavigatedMapPanel extends JPanel {

    /** name of default map */
    public static final String DEFAULT_MAP = "/auxdata/maps/OUTLSUPW";

    /** An application can set the list of maps to use */
    public static List DEFAULT_MAPS;


    /** navigated panel */
    protected NavigatedPanel navigatedPanel;


    /** map renderers */
    private List mapRenderers = new ArrayList();


    /** identity transform */
    private AffineTransform atI = new AffineTransform();  // identity transform


    /** upper left lat/lon point */
    LatLonPoint ul;

    /** lower right lat/lon point */
    LatLonPoint lr;


    /**
     * Default constructor.  Uses the default map
     */
    public NavigatedMapPanel() {
        this((List) null, true);
    }


    /**
     * Default constructor.  Uses the default map
     *
     * @param makeToolBar Make the nav toolbar
     */
    public NavigatedMapPanel(boolean makeToolBar) {
        this(makeToolBar, makeToolBar);
    }


    /**
     * ctor
     *
     * @param makeNavToolBar make nav tool bar
     * @param makeMoveToolBar make move tool bar
     */
    public NavigatedMapPanel(boolean makeNavToolBar,
                             boolean makeMoveToolBar) {
        this((List) null, makeNavToolBar, makeMoveToolBar);
    }


    /**
     * Construct a new <code>NavigatedMapPanel</code> using the specified
     * station selection type, map and template.
     *
     * @param defaultMap      map to use
     */
    public NavigatedMapPanel(String defaultMap) {
        this(makeMapList(defaultMap), true);
    }


    /**
     * Create a NMP with a set of maps
     *
     * @param maps   default set of maps.
     */
    public NavigatedMapPanel(List maps) {
        this(maps, true);
    }


    /**
     * Create a NMP with a set of maps
     *
     * @param defaultMaps   default set of maps.
     * @param makeToolBar Make the nav toolbar
     */
    public NavigatedMapPanel(List defaultMaps, boolean makeToolBar) {
        this(defaultMaps, makeToolBar, makeToolBar);
    }

    /**
     * Create a NMP with a set of maps
     *
     * @param defaultMaps   default set of maps.
     * @param makeNavToolBar Make the nav toolbar
     * @param makeMoveToolBar Make the move toolbar
     */
    public NavigatedMapPanel(List defaultMaps, boolean makeNavToolBar,
                             boolean makeMoveToolBar) {
        init(defaultMaps, makeNavToolBar, makeMoveToolBar);
    }


    /**
     * Factory method to make the map panel. Derived classes can
     * override this to make their own.
     *
     * @return The map panel
     */
    protected NavigatedPanel doMakeMapPanel() {
        return new NavigatedPanel();
    }


    /**
     * Initialize with a list of maps
     *
     * @param maps   list of maps
     * @param makeNavToolBar Make the nav tool bar
     * @param makeMoveToolBar Make the move tool bar
     */
    private void init(List maps, boolean makeNavToolBar,
                      boolean makeMoveToolBar) {
        if ((maps == null) || (maps.size() == 0)) {
            maps = DEFAULT_MAPS;
            if ((maps == null) || (maps.size() == 0)) {
                maps = new ArrayList();
                maps.add(DEFAULT_MAP);
            }
        }
        maps = new ArrayList(maps);

        // set up the properties
        // here's where the map will be drawn:
        navigatedPanel = doMakeMapPanel();
        //mapRender = new ucar.unidata.gis.worldmap.WorldMap(); // map Renderer
        for (int i = 0; i < maps.size(); i++) {
            Renderer mapRender = new ucar.unidata.gis.mcidasmap.McidasMap(
                                     maps.get(i).toString());
            addMapRenderer(mapRender);
        }


        // get NewMapAreaEvents from the navigate object
        navigatedPanel.addNewMapAreaListener(new NewMapAreaListener() {
            public void actionPerformed(NewMapAreaEvent e) {
                ProjectionImpl project = getProjection();
                if (project.isLatLon()) {
                    ProjectionRect boundingBox = navigatedPanel.getMapArea();
                    double wx0 = boundingBox.getX()
                                 + boundingBox.getWidth() / 2;
                    LatLonProjection llproj = (LatLonProjection) project;
                    if (llproj.getCenterLon() != wx0) {
                        llproj.setCenterLon(wx0);  // shift cylinder seam
                        wx0 = llproj.getCenterLon();  // normalize wx0 to  [-180,180]
                        applyProjectionToRenderers(project);
                        //                        setProjectionImpl (project);
                        navigatedPanel.setWorldCenterX(wx0);
                    }
                }
                redraw();
            }
        });

        // get Pick events from the navigated panel
        navigatedPanel.addPickEventListener(new PickEventListener() {
            public void actionPerformed(PickEvent e) {
                if (e.isPointSelect()) {
                    doPickPoint(e);
                } else {
                    //System.err.println("dopickregion");
                    doPickRegion(e);
                }
            }
        });

        // set up the display
        setLayout(new BorderLayout());
        //        navigatedPanel.setPreferredSize(new Dimension(400, 300));
        navigatedPanel.setBorder(BorderFactory.createLoweredBevelBorder());


        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EtchedBorder());
        JLabel positionLabel = new JLabel("position");
        statusPanel.add(positionLabel, BorderLayout.CENTER);
        navigatedPanel.setPositionLabel(positionLabel);
        if (makeNavToolBar || makeMoveToolBar) {
            JComponent toolPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));
            if (makeNavToolBar) {
                toolPanel.add(navigatedPanel.getNavToolBar());
            }
            if (makeMoveToolBar) {
                toolPanel.add(navigatedPanel.getMoveToolBar());
            }
            //            toolPanel =  GuiUtils.hbox(navigatedPanel.getNavToolBar(),
            //                                       navigatedPanel.getMoveToolBar());
            add(toolPanel, BorderLayout.SOUTH);
        }
        add(navigatedPanel, BorderLayout.CENTER);
        //add(statusPanel, BorderLayout.SOUTH);
        redraw();
    }


    /**
     * Make the map list
     *
     * @param map Map. May be null.
     *
     * @return List of maps
     */
    private static List makeMapList(String map) {
        if (map == null) {
            map = DEFAULT_MAP;
        }
        List maps = new ArrayList();
        maps.add(map);
        return maps;
    }




    /**
     * Set the drawing bounds.
     *
     * @param ulx      upper left x
     * @param uly      upper left y
     * @param lrx      lower right x
     * @param lry      lower right y
     */
    public void setDrawBounds(double ulx, double uly, double lrx,
                              double lry) {
        this.ul = new LatLonPointImpl(uly, ulx);
        this.lr = new LatLonPointImpl(lry, lrx);
        redraw();
    }

    /**
     * Set the drawing bounds
     *
     * @param ul    upper left point
     * @param lr    lower right point
     */
    public void setDrawBounds(LatLonPoint ul, LatLonPoint lr) {
        if (ul == null) {
            this.ul = null;
        } else {
            this.ul = new LatLonPointImpl(ul);
        }
        if (lr == null) {
            this.lr = null;
        } else {
            this.lr = new LatLonPointImpl(lr);
        }
        redraw();
    }


    /**
     * A hook so subclasses can override and respond to pick region events.
     *
     *
     * @param pickEvent <code>PickEvent</code> to process
     *
     */

    private void doPickRegion(PickEvent pickEvent) {
        redraw();
    }


    /**
     * A hook so subclasses can override and respond to pick point events.
     *
     * @param e <code>PickEvent</code> to process
     */
    protected void doPickPoint(PickEvent e) {
        redraw();
    }


    /**
     * Access to the navigated panel.
     *
     * @return  navigated panel object
     */
    public NavigatedPanel getNavigatedPanel() {
        return navigatedPanel;
    }


    /**
     * Set the map renderer for this object
     *
     * @param   r   map renderer
     */
    public void setMapRenderer(Renderer r) {
        mapRenderers = new ArrayList();
        addMapRenderer(r);
        redraw();
    }


    /**
     * Add a renderer.
     *
     * @param r  renderer to add.
     */
    public void addMapRenderer(Renderer r) {
        ProjectionImpl project = getProjection();
        if (project != null) {
            r.setProjection(project);
        }
        mapRenderers.add(r);
    }


    /**
     * Apply a projection to the renderers.
     *
     * @param project  new projection
     */
    private void applyProjectionToRenderers(ProjectionImpl project) {
        for (int i = 0; i < mapRenderers.size(); i++) {
            ((Renderer) mapRenderers.get(i)).setProjection(project);
        }
    }



    /**
     * Set the projection to use for this map.
     *
     * @param p  projection to use
     */
    public void setProjectionImpl(ProjectionImpl p) {
        navigatedPanel.setProjectionImpl(p);
        applyProjectionToRenderers(p);
        redraw();
    }

    /**
     * Get the projection used by the nav panel
     *
     * @return projection
     */
    public ProjectionImpl getProjectionImpl() {
        return navigatedPanel.getProjectionImpl();
    }



    /**
     *  Redraw the graphics on the screen.
     */
    public void redraw() {
        java.awt.Graphics2D gNP = navigatedPanel.getBufferedImageGraphics();
        if (gNP == null) {  // panel not drawn on screen yet
            return;
        }

        // clear it
        gNP.setBackground(navigatedPanel.getBackgroundColor());
        java.awt.Rectangle r = gNP.getClipBounds();
        gNP.clearRect(r.x, r.y, r.width, r.height);

        for (int i = 0; i < mapRenderers.size(); i++) {
            ((Renderer) mapRenderers.get(i)).draw(gNP, atI);
        }
        annotateMap(gNP);
        gNP.dispose();

        // copy buffer to the screen
        navigatedPanel.repaint();
    }


    /**
     * Draw any annotations on the map
     *
     * @param gNP The Graphics to draw into
     */
    protected void annotateMap(Graphics2D gNP) {
        if ((ul != null) && (lr != null)) {
            ProjectionImpl project = getProjection();
            ProjectionPointImpl ulpp =
                (ProjectionPointImpl) project.latLonToProj(ul,
                    new ProjectionPointImpl());
            ProjectionPointImpl lrpp =
                (ProjectionPointImpl) project.latLonToProj(lr,
                    new ProjectionPointImpl());
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
            path.moveTo((float) ulpp.getX(), (float) ulpp.getY());
            path.lineTo((float) lrpp.getX(), (float) ulpp.getY());
            path.lineTo((float) lrpp.getX(), (float) lrpp.getY());
            path.lineTo((float) ulpp.getX(), (float) lrpp.getY());
            path.lineTo((float) ulpp.getX(), (float) ulpp.getY());
            gNP.setColor(Color.red);
            gNP.draw(path);
        }
    }

    /**
     * Get the projection
     *
     * @return the projection
     */
    protected ProjectionImpl getProjection() {
        return navigatedPanel.getProjectionImpl();

    }



}


/*
 * $Id: NPController.java,v 1.17 2006/10/30 21:57:54 dmurray Exp $
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


package ucar.unidata.view;



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.mcidasmap.McidasMap;
import ucar.unidata.gis.shapefile.EsriShapefileRenderer;
import ucar.unidata.util.Debug;
import ucar.unidata.util.IOUtil;
import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import java.util.ArrayList;
import java.util.Hashtable;


/**
 * A "default" Navigated Panel controller, with a static Renderer
 * @author John Caron
 * @version $Id: NPController.java,v 1.17 2006/10/30 21:57:54 dmurray Exp $
 */
public class NPController {

    /** The nav panel */
    private NavigatedPanel navigatedPanel;

    /** The renderer */
    private Renderer mainRenderer = null;

    /** The projection */
    private ProjectionImpl project;

    /** The transform */
    private AffineTransform atI = new AffineTransform();  // identity transform

    /** ok to handle event */
    private boolean eventOk = true;


    /** debugging */
    private boolean debug = false;

    /** composite renderer */
    CompositeRenderer compositeRenderer;





    /**
     * ctor
     *
     */
    public NPController() {
        // here's where the map will be drawn:
        navigatedPanel    = new NavigatedPanel();
        compositeRenderer = new CompositeRenderer();
        mainRenderer      = compositeRenderer;
        project           = navigatedPanel.getProjectionImpl();
        mainRenderer.setProjection(project);

        // get NewMapAreaEvents from the navigate object
        navigatedPanel.addNewMapAreaListener(new NewMapAreaListener() {
            public void actionPerformed(NewMapAreaEvent e) {
                if (Debug.isSet("event.NewMapArea")) {
                    System.out.println("Controller got NewMapAreaEvent "
                                       + navigatedPanel.getMapArea());
                }

                if (eventOk && project.isLatLon()) {
                    ProjectionRect   box    = navigatedPanel.getMapArea();
                    LatLonProjection llproj = (LatLonProjection) project;
                    double           center = llproj.getCenterLon();
                    double lonBeg = LatLonPointImpl.lonNormal(box.getMinX(),
                                        center);
                    double  lonEnd    = lonBeg + box.getMaxX()
                                        - box.getMinX();
                    boolean showShift = Debug.isSet("projection.LatLonShift");
                    if (showShift) {
                        System.out.println(
                            "projection.LatLonShift: min,max = "
                            + box.getMinX() + " " + box.getMaxX()
                            + " beg,end= " + lonBeg + " " + lonEnd
                            + " center = " + center);
                    }

                    if ((lonBeg < center - 180) || (lonEnd > center + 180)) {  // got to do it
                        double wx0 = box.getX() + box.getWidth() / 2;
                        llproj.setCenterLon(wx0);  // shift cylinder seam
                        double newWx0 = llproj.getCenterLon();  // normalize wx0 to [-180,180]
                        navigatedPanel.setWorldCenterX(newWx0);  // tell navigation panel to shift
                        if (showShift) {
                            System.out.println(
                                "projection.LatLonShift: shift center to "
                                + wx0 + "->" + newWx0);
                        }

                        mainRenderer.setProjection(project);
                    }
                }

                /* old way
                if (project.isLatLon()) {
                  ProjectionRect boundingBox = navigatedPanel.getMapArea();
                  double wx0 = boundingBox.getX() + boundingBox.getWidth()/2;
                  LatLonProjection llproj = (LatLonProjection) project;
                  if (llproj.getCenterLon() != wx0) {
                    llproj.setCenterLon( wx0);                        // shift cylinder seam
                    wx0 = llproj.getCenterLon();                      // normalize wx0 to [-180,180]
                    navigatedPanel.setWorldCenterX(wx0);                          // tell navigation panel to shift
                      // force recalc of world positions
                    mainRenderer.setProjection(project);
                  }
                } */

                draw(true);
            }
        });
    }


    /** hashtable of renderers */
    Hashtable renderers = new Hashtable();

    /**
     * Remove a map
     *
     * @param mapPath  the path of the map
     */
    public void removeMap(String mapPath) {
        Renderer renderer = (Renderer) renderers.get(mapPath);
        if (renderer != null) {
            compositeRenderer.removeRenderer(renderer);
            draw(true);
        }
    }


    /**
     * Add a map
     *
     * @param mapPath  the path to the map
     * @param c  the map color
     */
    public void addMap(String mapPath, Color c) {
        Renderer mapRenderer = null;
        try {
            mapRenderer = EsriShapefileRenderer.factory(
							IOUtil.getInputStream(mapPath, getClass()));
        } catch (Exception exc) {}

        if (mapRenderer == null) {
            mapRenderer = new McidasMap(mapPath);
        }
        if (mapRenderer != null) {
            mapRenderer.setProjection(navigatedPanel.getProjectionImpl());
            mapRenderer.setColor(c);
            compositeRenderer.addRenderer(mapRenderer);
            renderers.put(mapPath, mapRenderer);
            draw(true);
        }
    }


    /**
     * get nav panel
     * @return nab panel
     */
    public NavigatedPanel getNavigatedPanel() {
        return navigatedPanel;
    }

    /**
     * set renderer
     *
     * @param r renderer
     */
    public void setRenderer(Renderer r) {
        mainRenderer = r;
        mainRenderer.setProjection(project);
        draw(true);
    }

    /**
     * set proj
     *
     * @param p proj
     */
    public void setProjectionImpl(ProjectionImpl p) {
        project = p;
        mainRenderer.setProjection(p);
        eventOk = false;
        navigatedPanel.setProjectionImpl(p);
        navigatedPanel.setSelectedRegion(p.getDefaultMapArea());
        navigatedPanel.zoom(0.8);
        eventOk = true;
        draw(true);
    }

    /**
     * draw
     *
     * @param complete complete
     */
    private void draw(boolean complete) {
        if ((mainRenderer == null) || (project == null)) {
            return;
        }

        long                tstart = System.currentTimeMillis();

        java.awt.Graphics2D gNP    =
            navigatedPanel.getBufferedImageGraphics();
        if (gNP == null) {  // panel not drawn on screen yet
            return;
        }

        // clear it
        gNP.setBackground(navigatedPanel.getBackgroundColor());
        gNP.fill(gNP.getClipBounds());

        mainRenderer.draw(gNP, atI);
        gNP.dispose();

        if (debug) {
            long tend = System.currentTimeMillis();
            System.out.println("NPController draw time = "
                               + (tend - tstart) / 1000.0 + " secs");
        }
        // copy buffer to the screen
        navigatedPanel.repaint();
    }



}


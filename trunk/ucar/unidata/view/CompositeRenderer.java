/*
 * $Id: CompositeRenderer.java,v 1.3 2005/12/30 14:15:39 jeffmc Exp $
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
import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;
import ucar.unidata.util.Debug;
import ucar.unidata.gis.shapefile.EsriShapefileRenderer;
import ucar.unidata.gis.mcidasmap.McidasMap;

import java.util.ArrayList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;


/**
 * Class CompositeRenderer
 *
 *
 * @author Unidata development team
 * @version %I%, %G%
 */
public class CompositeRenderer implements Renderer {

    /** List of renderers */
    ArrayList renderers = new ArrayList();

    /**
     * ctor
     *
     */
    public CompositeRenderer() {}



    /**
     * add a renderer
     *
     * @param r renderer
     */
    public void addRenderer(Renderer r) {
        renderers.add(r);
    }


    /**
     * add a renderer
     *
     * @param r renderer
     */
    public void removeRenderer(Renderer r) {
        renderers.remove(r);
    }

    /**
     * draw
     *
     * @param g graphcis
     * @param pixelAT transform
     */
    public void draw(java.awt.Graphics2D g,
                     java.awt.geom.AffineTransform pixelAT) {
        for (int i = 0; i < renderers.size(); i++) {
            ((Renderer) renderers.get(i)).draw(g, pixelAT);
        }
    }

    /**
     * Tell the Renderer to use the given projection from now on.
     *  @param project the projection to use.
     */
    public void setProjection(ucar.unidata.geoloc.ProjectionImpl project) {
        for (int i = 0; i < renderers.size(); i++) {
            ((Renderer) renderers.get(i)).setProjection(project);
        }
    }

    /**
     * Tell the Renderer to use the given color.
     *  @param color the Color to use.
     */
    public void setColor(java.awt.Color color) {
        for (int i = 0; i < renderers.size(); i++) {
            ((Renderer) renderers.get(i)).setColor(color);
        }
    }

    /**
     * Get the color
     * @return color
     */
    public java.awt.Color getColor() {
        return ((Renderer) renderers.get(0)).getColor();
    }

    /**
     * This allows application to automatically switch to some special area defined by the Renderer
     *  @return LatLonRect or null.
     */
    public ucar.unidata.geoloc.LatLonRect getPreferredArea() {
        return ((Renderer) renderers.get(0)).getPreferredArea();
    }
}


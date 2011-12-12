/*
 * $Id: GisFeatureRenderer.java,v 1.16 2005/05/13 18:29:33 jeffmc Exp $
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

package ucar.unidata.gis;



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.Debug;

import java.awt.Color;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;


/**
 * Superclass for rendering collections of GisFeatures.
 *
 * @author John Caron
 * @version $Id: GisFeatureRenderer.java,v 1.16 2005/05/13 18:29:33 jeffmc Exp $
 */
public abstract class GisFeatureRenderer
        implements ucar.unidata.view.Renderer {

    /** _more_ */
    private Color color = Color.blue;                 // default color of polylines

    /** _more_ */
    protected ProjectionImpl displayProject = null;   // the current display Projection

    /** _more_ */
    protected ArrayList shapeList = null;

    ////// this is what the subclasses have to implement (besides the constructor)

    /**
     * Preferred map area on opening for first time.
     * @return lat/lon bounding box that specifies preferred area.
     */
    public abstract LatLonRect getPreferredArea();

    /**
     * _more_
     * @return _more_
     */
    protected abstract java.util.List getFeatures();  // collection of AbstractGisFeature

    // what projection is the data in? set to null if no Projection (no conversion)
    // assumes data projection doesnt change

    /**
     * _more_
     * @return _more_
     */
    protected abstract ProjectionImpl getDataProjection();

    ///////////

    /**
     * _more_
     * @return _more_
     */
    public java.awt.Color getColor() {
        return color;
    }

    /**
     * _more_
     *
     * @param color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * _more_
     *
     * @param project
     */
    public void setProjection(ProjectionImpl project) {
        displayProject = project;
        shapeList      = null;
        //System.out.println("GisFeatureRenderer setProjection "+displayProject);

        //if (Debug.isSet("event.barf") && (displayProject instanceof LatLonProjection))
        //  throw new IllegalArgumentException();
    }

    /**
     * Draws all the features that are within the graphics clip rectangle,
     * using the previously set displayProjection.
     *
     * @param g the Graphics2D context on which to draw
     * @param pixelAT  transforms "Normalized Device" to Device coordinates
     */
    public void draw(java.awt.Graphics2D g, AffineTransform pixelAT) {
        g.setColor(color);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new java.awt.BasicStroke(0.0f));

        Rectangle2D clipRect = (Rectangle2D) g.getClip();
        Iterator    siter    = getShapes(g, pixelAT);
        while (siter.hasNext()) {
            Shape       s           = (Shape) siter.next();
            Rectangle2D shapeBounds = s.getBounds2D();
            if (shapeBounds.intersects(clipRect)) {
                g.draw(s);
            }
        }
    }

    // get the set of shapes to draw, convert projections if need be

    /**
     * _more_
     *
     * @param g
     * @param normal2device
     * @return _more_
     */
    protected Iterator getShapes(java.awt.Graphics2D g,
                                 AffineTransform normal2device) {
        if (shapeList != null) {
            return shapeList.iterator();
        }

        if (Debug.isSet("projection.LatLonShift")) {
            System.out.println(
                "projection.LatLonShift GisFeatureRenderer.getShapes called");
        }

        ProjectionImpl dataProject = getDataProjection();

        // a list of GisFeatureAdapter-s
        List featList = getFeatures();

        shapeList = new ArrayList(featList.size());

        Iterator iter = featList.iterator();


        while (iter.hasNext()) {
            AbstractGisFeature feature = (AbstractGisFeature) iter.next();
            Shape              shape;
            if (dataProject == null) {
                shape = feature.getShape();
            } else if (dataProject.isLatLon()) {
                // always got to run it through if its lat/lon
                shape = feature.getProjectedShape(displayProject);
                //            System.out.println("getShapes dataProject.isLatLon() "+displayProject);
            } else if (dataProject == displayProject) {
                shape = feature.getShape();
                //            System.out.println("getShapes dataProject == displayProject");
            } else {
                shape = feature.getProjectedShape(dataProject,
                                                  displayProject);
                //            System.out.println("getShapes dataProject != displayProject");
            }
            shapeList.add(shape);
        }



        return shapeList.iterator();
    }

}

/* Change History:
   $Log: GisFeatureRenderer.java,v $
   Revision 1.16  2005/05/13 18:29:33  jeffmc
   Clean up the odd copyright symbols

   Revision 1.15  2005/03/10 18:38:29  jeffmc
   jindent and javadoc

   Revision 1.14  2004/02/27 21:21:52  jeffmc
   Lots of javadoc warning fixes

   Revision 1.13  2004/01/29 17:35:20  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.12  2004/01/28 20:28:52  jeffmc
   A snapshot of some recent work that I want to get in before
   I make the sweeping change to run the jindent on everything

   Revision 1.11  2003/05/07 13:55:38  dmurray
   javadoc changes

   Revision 1.10  2003/04/08 14:01:44  caron
   nc2 version 2.1

   Revision 1.9  2001/04/30 23:38:22  caron
   debug

   Revision 1.8  2000/08/18 04:15:24  russ
   Licensed under GNU LGPL.

   Revision 1.7  2000/05/26 21:19:16  caron
   new GDV release

   Revision 1.6  2000/05/26 19:54:58  wier
   minor reformatting

   Revision 1.5  2000/05/16 22:38:01  caron
   factor GisFeatureRenderer

   Revision 1.4  2000/03/01 19:31:24  caron
   setProjection bug

   Revision 1.3  2000/02/17 20:18:02  caron
   make printing work for zoom resolution maps

   Revision 1.2  2000/02/11 01:24:42  caron
   add getDataProjection()

   Revision 1.1  2000/02/10 17:45:11  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/








/*
 * $Id: EsriShapefileRenderer.java,v 1.22 2005/12/30 15:38:55 jeffmc Exp $
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

package ucar.unidata.gis.shapefile;



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.*;
import ucar.unidata.gis.shapefile.EsriShapefile;

import java.awt.Color;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides a convenient interface to ESRI shapefiles by creating lists of
 * ucar.unidata.gis.AbstractGisFeature.  Java2D Shape or VisAD SampledSet
 * objects can be created from these.
 *
 * @author Russ Rew
 * @author John Caron
 * @version $Id: EsriShapefileRenderer.java,v 1.22 2005/12/30 15:38:55 jeffmc Exp $
 */
public class EsriShapefileRenderer
        extends ucar.unidata.gis.GisFeatureRendererMulti {

    /** _more_ */
    private static java.util.Map sfileHash = new HashMap();  // map of (filename -> EsriShapefileRenderer)

    /** _more_ */
    private static double defaultCoarseness = 0.0;           // expose later?

    /**
     * Use factory to obtain a EsriShapefileRenderer.  This caches the EsriShapefile for reuse.
     * <p>
     * Implementation note: should switch to weak references.
     *
     * @param filename
     * @return _more_
     */
    static public EsriShapefileRenderer factory(String filename) {
        if (sfileHash.containsKey(filename)) {
            return (EsriShapefileRenderer) sfileHash.get(filename);
        }

        try {
            EsriShapefileRenderer sfile = new EsriShapefileRenderer(filename);
            sfileHash.put(filename, sfile);
            return sfile;
        } catch (Exception ex) {
            //            System.err.println("EsriShapefileRenderer failed on " + filename
            //                               + "\n" + ex);
            //ex.printStackTrace();
            return null;
        }
    }

    /**
     * _more_
     *
     * @param stream
     * @return _more_
     */
    static public EsriShapefileRenderer factory(InputStream stream) {
        if (sfileHash.containsKey(stream)) {
            return (EsriShapefileRenderer) sfileHash.get(stream);
        }

        try {
            EsriShapefileRenderer sfile = new EsriShapefileRenderer(stream);
            sfileHash.put(stream, sfile);
            return sfile;
        } catch (Exception ex) {
            //            System.err.println("EsriShapefileRenderer failed on " + stream
            //                               + "\n" + ex);
            return null;
        }
    }

    ////////////////////////////////////////

    /** _more_ */
    private EsriShapefile esri = null;

    /** _more_ */
    private ProjectionImpl dataProject =
        new LatLonProjection("Cylindrical Equidistant");

    /**
     * _more_
     *
     * @param stream
     *
     * @throws IOException
     *
     */
    private EsriShapefileRenderer(InputStream stream) throws IOException {
        super();
        esri = new EsriShapefile(stream, null, defaultCoarseness);

        double avgD = getStats(esri.getFeatures().iterator());
        createFeatureSet(avgD);
        createFeatureSet(2 * avgD);
        createFeatureSet(3 * avgD);
        createFeatureSet(5 * avgD);
        createFeatureSet(8 * avgD);
    }

    /**
     * _more_
     *
     * @param filename
     *
     * @throws IOException
     *
     */
    private EsriShapefileRenderer(String filename) throws IOException {
        this(filename, defaultCoarseness);
    }

    /**
     * _more_
     *
     * @param filename
     * @param coarseness
     *
     * @throws IOException
     *
     */
    private EsriShapefileRenderer(String filename, double coarseness)
            throws IOException {
        super();
        esri = new EsriShapefile(filename, coarseness);

        double avgD = getStats(esri.getFeatures().iterator());
        createFeatureSet(2 * avgD);
        createFeatureSet(4 * avgD);
        createFeatureSet(8 * avgD);
        createFeatureSet(16 * avgD);
    }

    /**
     * _more_
     * @return _more_
     */
    public LatLonRect getPreferredArea() {
        Rectangle2D bb = esri.getBoundingBox();
        return new LatLonRect(new LatLonPointImpl(bb.getMinY(), bb.getMinX()),
                              bb.getHeight(), bb.getWidth());
    }

    /**
     * _more_
     * @return _more_
     */
    protected java.util.List getFeatures() {
        return esri.getFeatures();
    }

    /**
     * _more_
     * @return _more_
     */
    protected ProjectionImpl getDataProjection() {
        return dataProject;
    }

}


/* Change History:
   $Log: EsriShapefileRenderer.java,v $
   Revision 1.22  2005/12/30 15:38:55  jeffmc
   Remove some printlns

   Revision 1.21  2005/05/13 18:29:48  jeffmc
   Clean up the odd copyright symbols

   Revision 1.20  2005/03/10 18:38:53  jeffmc
   jindent and javadoc

   Revision 1.19  2004/02/27 21:22:43  jeffmc
   Lots of javadoc warning fixes

   Revision 1.18  2004/01/29 17:36:07  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.17  2003/05/07 13:55:39  dmurray
   javadoc changes

   Revision 1.16  2003/04/08 14:01:47  caron
   nc2 version 2.1

   Revision 1.15  2000/08/18 04:15:27  russ
   Licensed under GNU LGPL.

   Revision 1.14  2000/05/16 22:38:04  caron
   factor GisFeatureRenderer

   Revision 1.13  2000/02/17 20:15:59  caron
   tune resolution on zoom in

   Revision 1.12  2000/02/11 01:24:45  caron
   add getDataProjection()

   Revision 1.11  2000/02/10 17:45:16  caron
   add GisFeatureRenderer,GisFeatureAdapter

   Revision 1.10  2000/01/21 23:07:46  russ
   Add coarseness ShapefileShapeList constructors.  Make use of
   coarseness constructor using default of 1.0 to speed up rendering by a
   factor of 3.

   Revision 1.9  2000/01/05 16:04:59  russ
   Use particular instead of general feature type in projectShape()
   method, now that GisFeature interface has been simplified.

   Revision 1.8  1999/12/28 17:37:55  russ
   Oops, fixed bad import.

   Revision 1.7  1999/12/28 17:13:19  russ
   Eliminate unnecesssary dependence on Java2D.  Removed coarseness
   parameter for smaller coarser resolution maps (may add back in
   later).  Allow use of .zip files in constructor with bounding box.
   Made EsriFeature extend AbstractGisFeature for getShape() method.
   Have getGisParts() return iterator for list of GisPart.  Cosmetic
   changes to EsriShapefileRenderer, ShapefileShapeList.

   Revision 1.6  1999/12/16 22:57:35  caron
   gridded data viewer checkin

   Revision 1.5  1999/07/28 19:30:56  russ
   Adapted EsriShapefile to read from a DataInputStream instead of a
   RandomAccessFile.  Added URL constructor, so can read from a URL.
   Instead of using file length, read until EOF.  Still need to make
   independent of Java2D ...

   Removed java.awt.Dimension parameter from ShapefileShapeList
   constructor used in determining line segments to omit (just assume
   1000 pixel display).

   Removed unused PathIterator from EsriShapefileRenderer.

   Revision 1.4  1999/06/11 21:27:59  russ
   Cosmetic changes, preperatory to eliminating use of RandomAccessFile I/O.

   Revision 1.3  1999/06/03 01:43:57  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:24  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:43  caron
   startAgain

# Revision 1.10  1999/03/26  19:57:38  caron
# add SpatialSet; update javadocs
#
# Revision 1.9  1999/03/16  20:39:38  russ
# Deleted some unused fields.
#
# Revision 1.8  1999/03/12  16:01:01  russ
# Made draw method faster by testing for each shape whether its bounds
# intersects current clipping rectangle.
#
# Revision 1.7  1999/03/05  22:09:27  russ
# Don't draw over seam.
#
# Revision 1.6  1999/03/05  21:33:20  russ
# Made EsriShapefileRenderer.draw() use List of Shapes instead of List
# of GisFeatures.
# Fixed iterator bug of not invoking next().
#
# Revision 1.5  1999/03/03  23:13:45  russ
# Remove call to Shape.intersects(clipRect) in draw() method for large
# speedup.
#
# Revision 1.4  1999/03/02  17:35:58  russ
# Add comment for accumulating change histories.
*/








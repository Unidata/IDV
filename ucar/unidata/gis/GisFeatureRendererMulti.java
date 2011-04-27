/*
 * $Id: GisFeatureRendererMulti.java,v 1.8 2005/05/13 18:29:33 jeffmc Exp $
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
 * @version $Rev$
 */
public abstract class GisFeatureRendererMulti extends GisFeatureRenderer {

    /** _more_ */
    private static boolean useDiscretization = false;

    /** _more_ */
    private static double pixelMatch = 2.0;

    /**
     * _more_
     *
     * @param b
     */
    public static void setDiscretization(boolean b) {
        useDiscretization = b;
    }

    /**
     * _more_
     *
     * @param d
     */
    public static void setPixelMatch(double d) {
        pixelMatch = d;
    }

    /** _more_ */
    private ArrayList featSetList = null;                   // list of fetaureSets for progressive disclosure

    ////// this is what the subclasses have to implement (besides the constructor)

    /**
     * _more_
     * @return _more_
     */
    public abstract LatLonRect getPreferredArea();

    /**
     * _more_
     * @return _more_
     */
    protected abstract java.util.List getFeatures();        // collection of AbstractGisFeature

    /**
     * _more_
     * @return _more_
     */
    protected abstract ProjectionImpl getDataProjection();  // what projection is the data in?

    /**
     * Sets new projection for subsequent drawing.
     *
     * @param project the new projection
     */
    public void setProjection(ProjectionImpl project) {
        displayProject = project;

        if (featSetList == null) {
            return;
        }
        Iterator iter = featSetList.iterator();
        while (iter.hasNext()) {
            FeatureSet fs = (FeatureSet) iter.next();
            fs.newProjection = true;
        }
    }

    /**
     * _more_
     *
     * @param minDist
     */
    public void createFeatureSet(double minDist) {
        // make a FeatureSet out of this, defer actually creating the points
        FeatureSet fs = new FeatureSet(null, minDist);

        // add to the list of featureSets
        if (featSetList == null) {
            initFeatSetList();
        }
        featSetList.add(fs);
    }

    ////////////////////////////

    // get the set of shapes to draw.
    // we have to deal with both projections and resolution-dependence

    /**
     * _more_
     *
     * @param g
     * @param normal2device
     * @return _more_
     */
    protected Iterator getShapes(java.awt.Graphics2D g,
                                 AffineTransform normal2device) {
        long startTime = System.currentTimeMillis();

        if (featSetList == null) {
            initFeatSetList();
        }

        // which featureSet should we ue?
        FeatureSet fs = null;
        if (featSetList.size() == 1) {
            fs = (FeatureSet) featSetList.get(0);
        } else {
            // compute scale
            double scale = 1.0;
            try {
                AffineTransform world2device = g.getTransform();
                AffineTransform world2normal = normal2device.createInverse();
                world2normal.concatenate(world2device);
                scale = Math.max(Math.abs(world2normal.getScaleX()),
                                 Math.abs(world2normal.getShearX()));  // drawing or printing
                if (Debug.isSet("print.showTransform")) {
                    System.out.println("print.showTransform: " + world2normal
                                       + "\n scale = " + scale);
                }
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                System.out.println(
                    " GisRenderFeature: NoninvertibleTransformException on "
                    + normal2device);
            }
            if ( !displayProject.isLatLon()) {
                scale *= 111.0;                                        // km/deg
            }
            double minD = Double.MAX_VALUE;
            for (int i = 0; i < featSetList.size(); i++) {
                FeatureSet tryfs = (FeatureSet) featSetList.get(i);
                double     d     = Math.abs(scale * tryfs.minDist
                                            - pixelMatch);             // we want min features ~ 2 pixels
                if (d < minD) {
                    minD = d;
                    fs   = tryfs;
                }
            }
            if (Debug.isSet("Map.resolution")) {
                System.out.println("Map.resolution: scale = " + scale
                                   + " minDist = " + fs.minDist);
            }
        }

        // we may have deferred the actual creation of the points
        if (fs.featureList == null) {
            fs.createFeatures();
        }

        // ok, now see if we need to project
        if ( !displayProject.equals(fs.project)) {
            fs.setProjection(displayProject);
        } else {  // deal with LatLon
            if (fs.newProjection && displayProject.isLatLon()) {
                fs.setProjection(displayProject);
            }
        }
        fs.newProjection = false;

        if (Debug.isSet("timing.getShapes")) {
            long tookTime = System.currentTimeMillis() - startTime;
            System.out.println("timing.getShapes: " + tookTime * .001
                               + " seconds");
        }

        // so return it, already
        return fs.getShapes();
    }

    // make an ArrayList of Shapes from the given featureList and current display Projection

    /**
     * _more_
     *
     * @param featList
     * @return _more_
     */
    private ArrayList makeShapes(Iterator featList) {
        Shape          shape;
        ArrayList      shapeList   = new ArrayList();
        ProjectionImpl dataProject = getDataProjection();

        if (Debug.isSet("Map.draw")) {
            System.out.println("Map.draw: makeShapes with " + displayProject);
        }

        /*    if (Debug.isSet("bug.drawShapes")) {
              int count =0;
              // make each GisPart a seperate shape for debugging
        feats:while (featList.hasNext()) {
                AbstractGisFeature feature = (AbstractGisFeature) featList.next();
                java.util.Iterator pi = feature.getGisParts();
                while (pi.hasNext()) {
                  GisPart gp = (GisPart) pi.next();
                  int np = gp.getNumPoints();
                  GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, np);
                  double[] xx = gp.getX();
                  double[] yy = gp.getY();
                  path.moveTo((float) xx[0], (float) yy[0]);
                  if (count == 63)
                        System.out.println("moveTo x ="+xx[0]+" y= "+yy[0]);
                  for(int i = 1; i < np; i++) {
                    path.lineTo((float) xx[i], (float) yy[i]);
                    if (count == 63)
                        System.out.println("lineTo x ="+xx[i]+" y= "+yy[i]);
                  }
                  shapeList.add(path);
                  if (count == 63)
                    break feats;
                  count++;
                }
              }
              System.out.println("bug.drawShapes: #shapes =" +shapeList.size());
              return shapeList;
            }  */

        while (featList.hasNext()) {
            AbstractGisFeature feature = (AbstractGisFeature) featList.next();
            if (dataProject.isLatLon()) {  // always got to run it through if its lat/lon
                shape = feature.getProjectedShape(displayProject);
            } else if (dataProject == displayProject) {
                shape = feature.getShape();
            } else {
                shape = feature.getProjectedShape(dataProject,
                                                  displayProject);
            }

            shapeList.add(shape);
        }

        return shapeList;
    }

    /**
     * _more_
     */
    private void initFeatSetList() {
        featSetList = new ArrayList();
        featSetList.add(new FeatureSet(getFeatures(), 0.0));  // full resolution set
    }

    /**
     * Class FeatureSet
     *
     *
     * @author
     * @version %I%, %G%
     */
    private class FeatureSet {

        /** _more_ */
        List featureList = null;

        /** _more_ */
        double minDist;

        /** _more_ */
        ProjectionImpl project = null;

        /** _more_ */
        ArrayList shapeList = null;

        /** _more_ */
        boolean newProjection = true;

        /** _more_ */
        double centerLon = 0.0;

        /**
         * _more_
         *
         * @param featureList
         * @param minDist
         *
         */
        FeatureSet(List featureList, double minDist) {
            this.featureList = featureList;
            this.minDist     = minDist;
        }

        /**
         * _more_
         *
         * @param project
         */
        void setProjection(ProjectionImpl project) {
            this.project = project;
            shapeList    = makeShapes(featureList.iterator());

            if (project.isLatLon()) {  // why?
                LatLonProjection llproj = (LatLonProjection) project;
                centerLon = llproj.getCenterLon();
            }
        }

        /**
         * _more_
         * @return _more_
         */
        Iterator getShapes() {
            return shapeList.iterator();
        }

        /**
         * _more_
         */
        void createFeatures() {
            ProjectionPointImpl thisW = new ProjectionPointImpl();
            ProjectionPointImpl lastW = new ProjectionPointImpl();

            featureList = new ArrayList();

            Iterator iter =
                GisFeatureRendererMulti.this.getFeatures().iterator();  // this is the original, full resolution set
            while (iter.hasNext()) {
                AbstractGisFeature feature = (AbstractGisFeature) iter.next();
                FeatureMD          featMD  = new FeatureMD(minDist);

                Iterator           pi      = feature.getGisParts();
                while (pi.hasNext()) {
                    GisPart        gp   = (GisPart) pi.next();
                    FeatureMD.Part part = featMD.newPart(gp.getNumPoints());

                    int            np   = gp.getNumPoints();
                    double[]       xx   = gp.getX();
                    double[]       yy   = gp.getY();

                    part.set(xx[0], yy[0]);
                    for (int i = 1; i < np - 1; i++) {
                        part.setIfDistant(xx[i], yy[i]);
                    }

                    if (part.getNumPoints() > 1) {
                        part.set(xx[np - 1], yy[np - 1]);  // close polygons
                        part.truncateArray();
                        featMD.add(part);
                    }
                }                                          // loop over parts

                if (featSetList == null) {
                    initFeatSetList();
                }
                if (featMD.getNumParts() > 0) {
                    featureList.add(featMD);
                }
            }                                              // loop over featuures

            getStats(featureList.iterator());
        }  // createFeatures()

        /**
         * _more_
         *
         * @param d
         * @param n
         */
        private void discretizeArray(double[] d, int n) {
            if (minDist == 0.0) {
                return;
            }
            for (int i = 0; i < n; i++) {
                d[i] = (Math.rint(d[i] / minDist) * minDist) + minDist / 2;
            }
        }

    }  // FeatureSet inner class

    // these are derived Features based on a mimimum distance between points

    /**
     * Class FeatureMD
     *
     *
     * @author
     * @version %I%, %G%
     */
    private class FeatureMD extends AbstractGisFeature {

        /** _more_ */
        private ArrayList parts = new ArrayList();

        /** _more_ */
        private int total_pts = 0;

        /** _more_ */
        private double minDist;

        /** _more_ */
        private double minDist2;

        /**
         * _more_
         *
         * @param minDist
         *
         */
        FeatureMD(double minDist) {
            this.minDist = minDist;
            minDist2     = minDist * minDist;
        }

        /**
         * _more_
         *
         * @param part
         */
        void add(FeatureMD.Part part) {
            total_pts += part.getNumPoints();
            parts.add(part);
        }

        /**
         * _more_
         *
         * @param maxPts
         * @return _more_
         */
        FeatureMD.Part newPart(int maxPts) {
            return new FeatureMD.Part(maxPts);
        }

        /**
         * _more_
         *
         * @param d
         * @return _more_
         */
        private double discretize(double d) {
            if ( !useDiscretization || (minDist == 0.0)) {
                return d;
            }
            return (Math.rint(d / minDist) * minDist) + minDist / 2;
        }


        // implement GisFeature

        /**
         * _more_
         * @return _more_
         */
        public java.awt.geom.Rectangle2D getBounds2D() {
            return null;
        }

        /**
         * _more_
         * @return _more_
         */
        public int getNumPoints() {
            return total_pts;
        }

        /**
         * _more_
         * @return _more_
         */
        public int getNumParts() {
            return parts.size();
        }

        /**
         * _more_
         * @return _more_
         */
        public java.util.Iterator getGisParts() {
            return parts.iterator();
        }

        /**
         * Class Part
         *
         *
         * @author
         * @version %I%, %G%
         */
        class Part implements GisPart {

            /** _more_ */
            private int size;

            /** _more_ */
            private double[] wx;  // lat/lon coords

            /** _more_ */
            private double[] wy;

            // constructor

            /**
             * _more_
             *
             * @param maxPts
             *
             */
            Part(int maxPts) {
                wx       = new double[maxPts];
                wy       = new double[maxPts];
                size     = 0;
                minDist2 = minDist * minDist;
            }

            /**
             * _more_
             *
             * @param x
             * @param y
             */
            void set(double x, double y) {
                wx[size] = discretize(x);
                wy[size] = discretize(y);
                size++;
            }

            /**
             * _more_
             *
             * @param x
             * @param y
             */
            private void setNoD(double x, double y) {
                wx[size] = x;
                wy[size] = y;
                size++;
            }


            /**
             * _more_
             *
             * @param x
             * @param y
             */
            void setIfDistant(double x, double y) {
                x = discretize(x);
                y = discretize(y);
                double dx    = x - wx[size - 1];
                double dy    = y - wy[size - 1];
                double dist2 = dx * dx + dy * dy;
                if (dist2 >= minDist2) {
                    //   if ((x != wx[size-1]) || (y != wy[size-1]))
                    setNoD(x, y);
                }
            }

            /**
             * _more_
             */
            void truncateArray() {
                double[] x = new double[size];
                double[] y = new double[size];

                for (int i = 0; i < size; i++) {
                    x[i] = wx[i];  // arraycopy better?
                    y[i] = wy[i];  // arraycopy better?
                }
                wx = x;
                wy = y;
            }

            // implement GisPart

            /**
             * _more_
             * @return _more_
             */
            public int getNumPoints() {
                return size;
            }

            /**
             * _more_
             * @return _more_
             */
            public double[] getX() {
                return wx;
            }

            /**
             * _more_
             * @return _more_
             */
            public double[] getY() {
                return wy;
            }
        }
    }


    /**
     * _more_
     *
     * @param featList
     * @return _more_
     */
    protected double getStats(Iterator featList) {
        int                 total_pts   = 0;
        int                 total_parts = 0;
        int                 total_feats = 0;
        int                 cross_pts   = 0;
        double              avgD        = 0;
        double              minD        = Double.POSITIVE_INFINITY;
        double              maxD        = Double.NEGATIVE_INFINITY;

        ProjectionImpl      dataProject = getDataProjection();
        ProjectionPointImpl thisW       = new ProjectionPointImpl();
        ProjectionPointImpl lastW       = new ProjectionPointImpl();

        while (featList.hasNext()) {
            AbstractGisFeature feature = (AbstractGisFeature) featList.next();
            total_feats++;

            Iterator pi = feature.getGisParts();
            while (pi.hasNext()) {
                GisPart gp = (GisPart) pi.next();
                total_parts++;

                double[] xx = gp.getX();
                double[] yy = gp.getY();
                int      np = gp.getNumPoints();

                lastW.setLocation(xx[0], yy[0]);

                for (int i = 1; i < np; i++) {
                    thisW.setLocation(xx[i], yy[i]);
                    if ( !dataProject.crossSeam(thisW, lastW)) {
                        double dx   = (xx[i] - xx[i - 1]);
                        double dy   = (yy[i] - yy[i - 1]);
                        double dist = Math.sqrt(dx * dx + dy * dy);

                        total_pts++;
                        avgD += dist;
                        minD = Math.min(minD, dist);
                        maxD = Math.max(maxD, dist);
                    } else {
                        cross_pts++;
                    }

                    lastW.setLocation(xx[i], yy[i]);
                }
            }
        }

        avgD = (avgD / total_pts);
        if (Debug.isSet("Map.resolution")) {
            System.out.println("Map.resolution: total_feats = "
                               + total_feats);
            System.out.println(" total_parts = " + total_parts);
            System.out.println(" total_pts = " + total_pts);
            System.out.println(" cross_pts = " + cross_pts);
            System.out.println(" avg distance = " + avgD);
            System.out.println(" min distance = " + minD);
            System.out.println(" max distance = " + maxD);
        }

        return avgD;
    }
}

/* Change History:
   $Log: GisFeatureRendererMulti.java,v $
   Revision 1.8  2005/05/13 18:29:33  jeffmc
   Clean up the odd copyright symbols

   Revision 1.7  2005/03/10 18:38:29  jeffmc
   jindent and javadoc

   Revision 1.6  2004/02/27 21:21:52  jeffmc
   Lots of javadoc warning fixes

   Revision 1.5  2004/01/29 17:35:20  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.4  2003/05/07 13:55:38  dmurray
   javadoc changes

   Revision 1.3  2003/04/08 14:01:44  caron
   nc2 version 2.1

   Revision 1.2  2000/08/18 04:15:24  russ
   Licensed under GNU LGPL.

   Revision 1.1  2000/05/16 22:38:01  caron
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








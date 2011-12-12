/*
 * $Id: AbstractGisFeature.java,v 1.19 2006/08/22 19:57:07 dmurray Exp $
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

import ucar.visad.data.MapSet;

import visad.CoordinateSystem;
import visad.ErrorEstimate;
import visad.Gridded2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.UnionSet;
import visad.Unit;
import visad.VisADException;

import java.awt.Shape;

import java.util.List;
import java.util.ArrayList;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;


/**
 * Abstract class that implements common methods for concrete
 * implementations of GisFeature.
 *
 * @author Russ Rew
 * @author John Caron
 * @version $Id: AbstractGisFeature.java,v 1.19 2006/08/22 19:57:07 dmurray Exp $
 */

public abstract class AbstractGisFeature implements GisFeature {

    // subclasses must implement these methods

    /**
     * _more_
     * @return _more_
     */
    public abstract java.awt.geom.Rectangle2D getBounds2D();  // may be null

    /**
     * _more_
     * @return _more_
     */
    public abstract int getNumPoints();

    /**
     * _more_
     * @return _more_
     */
    public abstract int getNumParts();

    /**
     * _more_
     * @return _more_
     */
    public abstract java.util.Iterator getGisParts();

    /**
     * Convert this GisFeature to a java.awt.Shape, using the default
     * coordinate system, mapping gisFeature(x,y) -> screen(x,y).
     * LOOK STILL HAVE TO crossSeam()
     * @return shape corresponding to this feature.
     */
    public Shape getShape() {
        int                npts = getNumPoints();
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, npts);

        java.util.Iterator pi   = getGisParts();
        while (pi.hasNext()) {
            GisPart  gp = (GisPart) pi.next();
            double[] xx = gp.getX();
            double[] yy = gp.getY();
            int      np = gp.getNumPoints();
            if (np > 0) {
                path.moveTo((float) xx[0], (float) yy[0]);
            }
            for (int i = 1; i < np; i++) {
                path.lineTo((float) xx[i], (float) yy[i]);
            }
        }
        return path;
    }

    /**
     * Convert this GisFeature to a java.awt.Shape. The data coordinate system
     * is assumed to be (lat, lon), use the projection to transform points, so
     * project.latLonToProj(gisFeature(x,y)) -> screen(x,y).
     *
     * @param displayProject Projection to use.
     *
     * @return shape corresponding to this feature
     */
    public Shape getProjectedShape(ProjectionImpl displayProject) {
        LatLonPointImpl     workL = new LatLonPointImpl();
        ProjectionPointImpl lastW = new ProjectionPointImpl();
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                           getNumPoints());

        boolean showPts =
            ucar.unidata.util.Debug.isSet("projection.showPoints");

        java.util.Iterator pi = getGisParts();
        while (pi.hasNext()) {
            GisPart  gp = (GisPart) pi.next();
            double[] xx = gp.getX();
            double[] yy = gp.getY();
            for (int i = 0; i < gp.getNumPoints(); i++) {
                workL.set(yy[i], xx[i]);
                ProjectionPoint pt = displayProject.latLonToProj(workL);

                if (showPts) {
                    System.out.println("getProjectedShape 1 " + xx[i] + " "
                                       + yy[i] + " === " + pt.getX() + " "
                                       + pt.getY());
                    if (displayProject.crossSeam(pt, lastW)) {
                        System.out.println("***cross seam");
                    }
                }

                if ((i == 0) || displayProject.crossSeam(pt, lastW)) {
                    path.moveTo((float) pt.getX(), (float) pt.getY());
                } else {
                    path.lineTo((float) pt.getX(), (float) pt.getY());
                }

                lastW.setLocation(pt);
            }
        }
        return path;
    }

    /**
     * Convert this GisFeature to a java.awt.Shape. The data coordinate system
     * is in the coordinates of dataProject, and the screen is in the coordinates of
     * displayProject. So:
     * displayProject.latLonToProj( dataProject.projToLatLon(gisFeature(x,y))) -> screen(x,y).
     *
     * @param dataProject     data Projection to use.
     * @param displayProject  display Projection to use.
     * @return shape corresponding to this feature
     */
    public Shape getProjectedShape(ProjectionImpl dataProject,
                                   ProjectionImpl displayProject) {
        ProjectionPointImpl pt1   = new ProjectionPointImpl();
        ProjectionPointImpl lastW = new ProjectionPointImpl();
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                           getNumPoints());

        boolean showPts =
            ucar.unidata.util.Debug.isSet("projection.showPoints");

        java.util.Iterator pi = getGisParts();
        while (pi.hasNext()) {
            GisPart  gp = (GisPart) pi.next();
            double[] xx = gp.getX();
            double[] yy = gp.getY();
            for (int i = 0; i < gp.getNumPoints(); i++) {
                pt1.setLocation(xx[i], yy[i]);
                LatLonPoint     llpt = dataProject.projToLatLon(pt1);
                ProjectionPoint pt2  = displayProject.latLonToProj(llpt);

                if (showPts) {
                    System.out.println("getProjectedShape 2 " + xx[i] + " "
                                       + yy[i] + " === " + pt2.getX() + " "
                                       + pt2.getY());
                    if (displayProject.crossSeam(pt2, lastW)) {
                        System.out.println("***cross seam");
                    }
                }

                if ((i == 0) || displayProject.crossSeam(pt2, lastW)) {
                    path.moveTo((float) pt2.getX(), (float) pt2.getY());
                } else {
                    path.lineTo((float) pt2.getX(), (float) pt2.getY());
                }

                lastW.setLocation(pt2);
            }
        }
        return path;
    }


    private int pointCnt = 0;

    public int getPointCount() {
	return pointCnt;
    }

    /**
     * Convert a GisFeature to a visad.SampledSet, which is either a
     * single Gridded2DSet (if there is only one part to the feature)
     * or a UnionSet of Gridded2DSet (if there are multiple parts).
     * Each Gridded2DSet is a sequence of line segments that is
     * supposed to be drawn as a continuous line.
     *
     * @return UnionSet of Gridded2DSet corresponding to this feature.
     */

    public SampledSet getMapLines() {
	return getMapLines(null);
    }

    /**
     * Convert a GisFeature to a visad.SampledSet, which is either a
     * single Gridded2DSet (if there is only one part to the feature)
     * or a UnionSet of Gridded2DSet (if there are multiple parts).
     * Each Gridded2DSet is a sequence of line segments that is
     * supposed to be drawn as a continuous line.
     *
     * @return UnionSet of Gridded2DSet corresponding to this feature.
     */
    public SampledSet getMapLines(Rectangle2D bbox) {
	pointCnt = 0;
        if ((getNumParts() == 0) || (getNumPoints() == 0)) {
            return null;
        }
        SampledSet     maplines    = null;
	List<Gridded2DSet> lines = new ArrayList<Gridded2DSet>();

        try {
            RealTupleType coordMathType = RealTupleType.SpatialEarth2DTuple;
            java.util.Iterator pi = getGisParts();
            while (pi.hasNext()) {
                GisPart   gp   = (GisPart) pi.next();
                int       np   = gp.getNumPoints();
                double[]  xx   = gp.getX();
                double[]  yy   = gp.getY();
		if(bbox!=null) {
		    boolean inBox = false;
		    for(int i=0;i<np;i++) {
			if(bbox.contains(xx[i],yy[i])) {
			    inBox = true;
			    break;
			} 
		    }
		    if(!inBox) continue;
		}

		pointCnt +=np;
                float[][] part = new float[2][np];
                for (int i = 0; i < np; i++) {
                    part[0][i] = (float) xx[i];
                    part[1][i] = (float) yy[i];
                }
                lines.add(new MapSet(coordMathType, part, np,
				     (CoordinateSystem) null, (Unit[]) null,
				     (ErrorEstimate[]) null, false));  
            }

	    if(lines.size()==0) return null;
	    Gridded2DSet[] latLonLines = (Gridded2DSet[]) lines.toArray(new Gridded2DSet[lines.size()]);
            if (latLonLines.length > 1) {
                maplines = new UnionSet(coordMathType, latLonLines,
                                        (CoordinateSystem) null,
                                        (Unit[]) null,
                                        (ErrorEstimate[]) null, false);  // no copy
            } else {
                maplines = latLonLines[0];
            }
        } catch (visad.VisADException e) {
            e.printStackTrace();
            System.out.println("numParts = " + getNumParts());
        }
        return maplines;
    }

}  // AbstractGisFeature

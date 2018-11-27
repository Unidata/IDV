/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;



import java.awt.geom.Rectangle2D;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.List;


/**
 * Provides a convenient interface to ESRI shapefiles by creating lists of
 * ucar.unidata.gis.AbstractGisFeature.  Java2D Shape or VisAD SampledSet
 * objects can be created from these.
 *
 * @author Russ Rew
 * @author John Caron
 */
public class EsriShapefileRenderer extends ucar.unidata.gis
    .GisFeatureRendererMulti {

    /** _more_ */
    private static java.util.Map sfileHash = new HashMap();  // map of (filename -> EsriShapefileRenderer)

    /** _more_ */
    private static double defaultCoarseness = 0.0;  // expose later?

    /**
     * Use factory to obtain a EsriShapefileRenderer.  This caches the EsriShapefile for reuse.
     * <p>
     * Implementation note: should switch to weak references.
     *
     * @param filename the filename
     * @return the esri shapefile renderer
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
     * Factory.
     *
     * @param stream the stream
     * @return the esri shapefile renderer
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
     * Instantiates a new esri shapefile renderer.
     *
     * @param stream the stream
     * @throws IOException Signals that an I/O exception has occurred.
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
     * Instantiates a new esri shapefile renderer.
     *
     * @param filename the filename
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private EsriShapefileRenderer(String filename) throws IOException {
        this(filename, defaultCoarseness);
    }

    /**
     * Instantiates a new esri shapefile renderer.
     *
     * @param filename the filename
     * @param coarseness the coarseness
     * @throws IOException Signals that an I/O exception has occurred.
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
     * {@inheritDoc}
     */
    public LatLonRect getPreferredArea() {
        Rectangle2D bb = esri.getBoundingBox();
        return new LatLonRect(new LatLonPointImpl(bb.getMinY(),
                bb.getMinX()), bb.getHeight(), bb.getWidth());
    }

    /**
     * {@inheritDoc}
     */
    protected List getFeatures() {
        return esri.getFeatures();
    }


    /**
     * {@inheritDoc}
     */
    protected ProjectionImpl getDataProjection() {
        return dataProject;
    }

}

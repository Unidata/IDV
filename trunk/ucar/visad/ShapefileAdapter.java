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

package ucar.visad;


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;

import ucar.unidata.gis.GisFeature;

import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.EsriShapefile;

import ucar.unidata.idv.control.drawing.ShapeGlyph;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.data.MapSet;

import ucar.visad.quantities.CommonUnits;



import visad.*;



import java.awt.geom.Rectangle2D;

import java.io.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;


/**
 * Provides support for ShapeFiles in VisAD.
 *
 * @author Don Murray?
 */
public class ShapefileAdapter {

    /** map xml tag */
    public static final String TAG_POLYGON = "polygon";

    /** map xml attribute */
    public static final String ATTR_POINTS = "points";


    /** The db file if exists */
    private DbaseFile dbFile;

    /** shapefile */
    protected EsriShapefile shapefile;

    /** resulting maplines */
    protected UnionSet mapLines;

    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param filename name of ESRI shapefile (typically has ".shp"
     *        extension)
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(String filename)
            throws IOException, VisADException {
        this(filename, null, 0.0);
    }

    /**
     *
     * Read an ESRI shapefile from a URL and extract all features into
     * an in-memory structure.
     *
     *
     * @throws IOException
     * @throws VisADException
     * @param url URL of ESRI shapefile
     */
    public ShapefileAdapter(URL url) throws IOException, VisADException {
        this(url.toString(), null, 0.0);
    }

    /**
     *
     * Read an ESRI shapefile and extract the subset of features that have
     * bounding boxes that intersect a specified bounding box
     *
     * @param url URL of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(URL url, Rectangle2D bBox)
            throws IOException, VisADException {
        this(url.toString(), bBox, 0.0);
    }

    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param filename name of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(String filename, Rectangle2D bBox)
            throws IOException, VisADException {
        this(filename, bBox, 0.0);
    }

    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param filename name of ESRI shapefile (typically has ".shp"
     *        extension)
     * @param coarseness resolution vs. speed parameter.  0.0 for best
     * resolution, 0.1 for good resolution with 10:1 zooms, etc.
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(String filename, double coarseness)
            throws IOException, VisADException {
        this(filename, null, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile from a URL and extract all features into
     * an in-memory structure.
     *
     * @param url URL of ESRI shapefile
     * @param coarseness resolution vs. speed parameter.  0.0 for best
     * resolution, 0.1 for good resolution with 10:1 zooms, etc.
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(URL url, double coarseness)
            throws IOException, VisADException {
        this(url, null, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile and extract the subset of features that have
     * bounding boxes that intersect a specified bounding box
     *
     * @param url URL of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness resolution vs. speed parameter.  0.0 for best
     * resolution, 0.1 for good resolution with 10:1 zooms, etc.
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(URL url, Rectangle2D bBox, double coarseness)
            throws IOException, VisADException {
        this(url.toString(), bBox, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param filename name of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness resolution vs. speed parameter.  0.0 for best
     * resolution, 0.1 for good resolution with 10:1 zooms, etc.
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(String filename, Rectangle2D bBox,
                            double coarseness)
            throws IOException, VisADException {
        readSource(filename, null, bBox, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param iStream input from which to read
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(InputStream iStream)
            throws IOException, VisADException {
        this(iStream, null, 0.0f);
    }

    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param iStream input from which to read
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(InputStream iStream, Rectangle2D bBox)
            throws IOException, VisADException {
        this(iStream, bBox, 0.0);
    }


    /**
     * constructor
     *
     * @param iStream Inputstream
     * @param filename Filename where we got the input stream
     *
     * @throws IOException On badness
     * @throws VisADException On badness
     */
    public ShapefileAdapter(InputStream iStream, String filename)
            throws IOException, VisADException {
        readSource(filename, iStream, null, 0.0);
    }



    /**
     * _more_
     *
     * @param iStream _more_
     * @param filename _more_
     * @param dBox _more_
     * @param coarseness _more_
     *
     * @throws IOException _more_
     * @throws VisADException _more_
     */
    public ShapefileAdapter(InputStream iStream, String filename,
                            Rectangle2D dBox, double coarseness)
            throws IOException, VisADException {
        readSource(filename, iStream, dBox, coarseness);
    }


    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure.
     *
     * @param iStream input from which to read
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness of plotting, 0.0 for best resolution, 0.1 for
     * good resoution at 10:1 zoom, etc.  Higher values make things
     * display a bit faster.
     *
     * @throws IOException
     * @throws VisADException
     */
    public ShapefileAdapter(InputStream iStream, Rectangle2D bBox,
                            double coarseness)
            throws IOException, VisADException {
        this(new EsriShapefile(iStream, bBox, coarseness));
    }


    /**
     *
     * From an ESRI shapefile extract all features into an in-memory
     * structure.
     *
     * @param shapefile input from which to extract features
     *
     * @throws VisADException
     */
    public ShapefileAdapter(EsriShapefile shapefile) throws VisADException {
        mapLines = makeSet(doRead(shapefile, null));
    }


    /**
     * Read the shapefile from the given iStream
     *
     * @param name Filename or url
     * @param iStream The input stream to read from
     * @param bBox The bounding box to select on. May be null.
     * @param coarseness The stride
     *
     * @throws IOException On badness
     * @throws VisADException On badness
     */
    private void readSource(String name, InputStream iStream,
                            Rectangle2D bBox, double coarseness)
            throws IOException, VisADException {
        try {
            List   sets   = null;

            String lcName = name.toLowerCase();
            if (lcName.endsWith(".gsf") || lcName.endsWith(".nws")
                    || lcName.endsWith(".cia") || lcName.endsWith(".usg")
                    || lcName.endsWith(".ncp") || lcName.endsWith(".rfc")
                    || lcName.endsWith(".cpc")) {
                sets = doReadSSF(name, iStream);
            } else if (name.endsWith(".xml")) {
                sets = doReadXml(name, iStream);
            } else if (name.endsWith(".xgrf")) {
                sets = doReadXml(name, iStream);
            } else {
                try {
                    if (iStream == null) {
                        iStream =
                            new DataInputStream((new URL(name)).openStream());
                    }
                } catch (java.net.MalformedURLException exc) {
                    iStream = new FileInputStream(name);
                }
                sets = doRead(new EsriShapefile(iStream, null, coarseness),
                              bBox);
            }
            mapLines = makeSet(sets);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }



    /**
     * Utility to make a set
     *
     * @param sets List of sets
     *
     *
     * @return The set of sets
     * @throws VisADException On badness
     */
    public static UnionSet makeSet(List sets) throws VisADException {
        if (sets == null) {
            return null;
        }
        SampledSet[] ss = new SampledSet[sets.size()];
        System.arraycopy(sets.toArray(), 0, ss, 0, sets.size());
        if (ss.length > 0) {
            return new UnionSet(ss[0].getType(), ss, null, null, null, false);
        } else {
            return null;
        }
    }




    /**
     * Read the xml format
     *
     * @param name filename or url
     * @param iStream input stream
     *
     * @return List of point sets
     */
    private List doReadXml(String name, InputStream iStream) {
        List sets = new ArrayList();
        try {
            if (iStream == null) {
                iStream = IOUtil.getInputStream(name);
            }
            String  xml  = IOUtil.readContents(iStream);
            Element root = XmlUtil.getRoot(xml);
            if (root == null) {
                return sets;
            }
            NodeList elements = XmlUtil.getElements(root);
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);
                if ( !XmlUtil.hasAttribute(child, ATTR_POINTS)) {
                    continue;
                }
                double[] points =
                    Misc.parseDoubles(XmlUtil.getAttribute(child,
                        ATTR_POINTS));


                boolean isRect = false;
                if ( !child.getTagName().equals(TAG_POLYGON)) {
                    if (child.getTagName().equals(ShapeGlyph.TAG_SHAPE)) {
                        if (XmlUtil.getAttribute(child,
                                ShapeGlyph.ATTR_SHAPETYPE).toLowerCase()
                                    .equals("rectangle")) {
                            isRect = true;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }


                if (XmlUtil.hasAttribute(child, "coordtype")) {
                    String coord = XmlUtil.getAttribute(child, "coordtype");
                    if ( !coord.startsWith("LATLON")) {
                        continue;
                    }
                    if (coord.equals("LATLONALT")) {
                        double[] tmp    = new double[2 * points.length / 3];
                        int      tmpCnt = 0;
                        for (int ptIdx = 0; ptIdx < points.length;
                                ptIdx += 3) {
                            tmp[tmpCnt++] = points[ptIdx];
                            tmp[tmpCnt++] = points[ptIdx + 1];
                        }
                        points = tmp;
                    }
                }

                RealTupleType coordMathType =
                    new RealTupleType(RealType.Longitude, RealType.Latitude);
                float[][] part = new float[2][points.length / 2];
                for (int ptIdx = 0; ptIdx < points.length / 2; ptIdx++) {
                    part[1][ptIdx] = (float) points[ptIdx * 2];
                    part[0][ptIdx] = (float) points[ptIdx * 2 + 1];
                }

                if (isRect) {
                    part = ShapeGlyph.makeRectangle(part);
                }

                MapSet mapSet = new MapSet(coordMathType, part,
                                           part[0].length,
                                           (CoordinateSystem) null,
                                           (Unit[]) null,
                                           (ErrorEstimate[]) null,
                                           false /* no copy */);
                sets.add(mapSet);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        return sets;
    }

    /**
     * Read the xml format
     *
     * @param name filename or url
     * @param iStream input stream
     *
     * @return List of point sets
     */
    private List doReadGmap(String name, InputStream iStream) {
        List sets = new ArrayList();
        try {
            if (iStream == null) {
                iStream = IOUtil.getInputStream(name);
            }
            DataInputStream dis     = new DataInputStream(iStream);
            short           numBlks = dis.readShort();
            System.err.println("numBlks:" + numBlks);
            dis.skipBytes(252);

            for (int blockIdx = 0; blockIdx < numBlks; blockIdx++) {
                short numSegments = dis.readShort();
                System.err.println("segs=" + numSegments);
                dis.skipBytes(4);
                for (int segIdx = 0; segIdx < numSegments; segIdx++) {
                    int nPts = dis.readShort();
                    System.err.println("  pts=" + nPts);
                    dis.skipBytes(4 * 5);
                    for (int ptIdx = 0; ptIdx < nPts; ptIdx++) {
                        float lat = Float.intBitsToFloat(dis.readShort());
                        float lon = Float.intBitsToFloat(dis.readShort());
                        //                        System.err.println("     lat:" + lat+"/"+lon);
                    }

                    //                    if(true)
                    //                        break;
                }
            }


            double[] points = new double[] { -107, 40 };

            RealTupleType coordMathType =
                new RealTupleType(RealType.Longitude, RealType.Latitude);
            float[][] part = new float[2][points.length / 2];
            for (int ptIdx = 0; ptIdx < points.length / 2; ptIdx++) {
                part[1][ptIdx] = (float) points[ptIdx * 2];
                part[0][ptIdx] = (float) points[ptIdx * 2 + 1];
            }
            sets.add(new MapSet(coordMathType, part, points.length / 2,
                                (CoordinateSystem) null, (Unit[]) null,
                                (ErrorEstimate[]) null, false /* no copy */));
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return sets;
    }


    /**
     * Read the xml format
     *
     * @param name filename or url
     * @param iStream input stream
     *
     * @return List of point sets
     */
    private List doReadSSF(String name, InputStream iStream) {
        List sets = new ArrayList();
        try {
            if (iStream == null) {
                iStream = IOUtil.getInputStream(name);
            }
            List toks = StringUtil.split(IOUtil.readContents(iStream), " ",
                                         true, true);
            int xcnt = 0;
            int cnt  = 0;
            while (cnt < toks.size()) {
                int size = Integer.parseInt((String) toks.get(cnt));
                cnt += 5;
                double[] points = new double[size];
                for (int i = 0; i < size; i++) {
                    points[i] = Double.parseDouble((String) toks.get(cnt));
                    cnt++;
                }
                RealTupleType coordMathType =
                    new RealTupleType(RealType.Longitude, RealType.Latitude);
                float[][] part = new float[2][points.length / 2];
                for (int ptIdx = 0; ptIdx < points.length / 2; ptIdx++) {
                    part[1][ptIdx] = (float) points[ptIdx * 2];
                    part[0][ptIdx] = (float) points[ptIdx * 2 + 1];
                }
                xcnt++;
                sets.add(new MapSet(coordMathType, part, points.length / 2,
                                    (CoordinateSystem) null, (Unit[]) null,
                                    (ErrorEstimate[]) null,
                                    false /* no copy */));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return sets;
    }



    /**
     *     Read the shapefile
     *
     *     @param shapefile shapefile
     * @param bbox _more_
     *
     *     @return List of point sets
     */
    private List doRead(EsriShapefile shapefile, Rectangle2D bbox) {



        this.shapefile = shapefile;
        List               features = shapefile.getFeatures();
        java.util.Iterator si       = features.iterator();
        dbFile = shapefile.getDbFile();
        List s0       = new ArrayList();


        int  pointCnt = 0;
        for (int i = 0; si.hasNext(); i++) {
            EsriShapefile.EsriFeature gf =
                (EsriShapefile.EsriFeature) si.next();
            SampledSet mapLines = gf.getMapLines(bbox);
            pointCnt += gf.getPointCount();
            if (mapLines != null) {
                s0.add(mapLines);
            }
        }
        return s0;
    }

    /**
     * What is the feature type
     *
     * @return Feature type
     */
    public int getFeatureType() {
        if (shapefile != null) {
            return shapefile.getFeatureType();
        }
        return EsriShapefile.POLYGON;
    }


    /**
     * Get the db file. May be null.
     *
     * @return db file
     */
    public DbaseFile getDbFile() {
        return dbFile;
    }


    /**
     * getData creates a VisAD UnionSet type with the MathType
     * specified thru one of the other methods.  By default,
     * the MathType is a RealTupleType of Latitude,Longitude,
     * so the UnionSet (a union of Gridded2DSets) will have
     * lat/lon values.  Each Gridded2DSet is a line segment that
     * is supposed to be drawn as a continuous line.
     * The UnionSet is null if there are no maplines in the domain
     * of the display.
     * @return  the data as a UnionSet
     */
    public UnionSet getData() {
        return mapLines;
    }

    /**
     * Test this out
     *
     * @param args filename
     *
     * @throws Exception  problem
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("need to supply a filename");
            System.exit(1);
        }
        ShapefileAdapter sfa  = new ShapefileAdapter(args[0]);
        UnionSet         data = sfa.getData();
        //System.out.println(data);
        visad.python.JPythonMethods.dumpTypes(data);
    }
}

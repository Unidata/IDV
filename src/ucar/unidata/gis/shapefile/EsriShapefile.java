/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for Atmospheric Research
 * Copyright 2010- Jeff McWhirter
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
 * 
 */

package ucar.unidata.gis.shapefile;


import ucar.unidata.gis.AbstractGisFeature;
import ucar.unidata.gis.GisFeature;
import ucar.unidata.gis.GisPart;
import ucar.unidata.io.BeLeDataInputStream;
import ucar.unidata.util.IOUtil;



import java.awt.geom.Rectangle2D;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;



/**
 * EsriShapefile.java
 *
 * Encapsulates details of ESRI Shapefile format, documented at
 * http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf
 *
 * @author Russ Rew
 * @version $Revision: 1.27 $  $Date: 2005/05/13 18:29:48 $
 */
public class EsriShapefile {

    /** _more_          */
    private boolean debug = false;

    /** count total number of points    */
    private int NUMPOINTS = 0;

    /** shapefile magic number */
    public final static int SHAPEFILE_CODE = 9994;  // shapefile magic number

    // these are only shape types handled by this package, so far

    /** null shape */
    public final static int NULL = 0;

    /** point shape */
    public final static int POINT = 1;

    /** polyline shape */
    public final static int POLYLINE = 3;

    /** polygon shape */
    public final static int POLYGON = 5;

    /** multipoint shape */
    public final static int MULTIPOINT = 8;

    // Eventually we should handle these new types also (is anyone using these?)

    /** point with Z */
    public final static int POINTZ = 11;

    /** polyline with z */
    public final static int POLYLINEZ = 13;

    /** polygon with z */
    public final static int POLYGONZ = 15;

    /** multipoint with z */
    public final static int MULTIPOINTZ = 18;

    /** Measured point */
    public final static int POINTM = 21;

    /** Measured polyline */
    public final static int POLYLINEM = 23;

    /** Measured polygon */
    public final static int POLYGONM = 25;

    /** Measured multi point */
    public final static int MULTIPOINTM = 28;

    /** muti patch */
    public final static int MULTIPATCH = 31;

    /** the feature type */
    private int featureType;


    /** the shapefile data stream */
    private BeLeDataInputStream bdis;

    /** bytes in file, according to header */
    private int fileBytes;

    /** so far, in bytes */
    private int bytesSeen = 0;

    /** version of shapefile format (currently 1000) */
    private int version;

    /** file shape type */
    private int fileShapeType;  // not used here

    /** EsriFeatures as List */
    private ArrayList<GisFeature> features;  // EsriFeatures in List

    /** bounds from shapefile */
    private Rectangle2D listBounds;  // bounds from shapefile

    /** resolution computed from coarseness */
    private double resolution;  // computed from coarseness

    /** the Dbase file */
    private DbaseFile dbFile;

    /** the projection file */
    private ProjFile prjFile;

    /** default coarseness */
    private final static double defaultCoarseness = 0.0;

    /**
     * Relative accuracy of plotting.  Larger means coarser but
     * faster, 0.0 is all available resolution.  Anything less than 1
     * is wasted sub-pixel plotting, but retains quality for closer
     * zooming.  Anything over about 2.0 is ugly.  1.50 makes things
     * faster than 1.0 at the cost of barely discernible ugliness, but
     * for best quality (without zooms), set to 1.0.  If you still
     * want quality at 10:1 zooms, set to 1/10, etc.
     */
    private double coarseness = defaultCoarseness;



    /**
     *
     * Read an ESRI shapefile and extract all features into
     * an in-memory structure.
     *
     * @param filename name of ESRI shapefile (typically has ".shp"
     *        extension)
     *
     * @throws IOException
     */
    public EsriShapefile(String filename) throws IOException {
        this(filename, null);
    }

    /**
     *
     *    Read an ESRI shapefile from a URL and extract all features into
     * an in-memory structure.
     *
     * @param url URL of ESRI shapefile
     *
     * @throws IOException
     */
    public EsriShapefile(URL url) throws IOException {
        this(url, null);
    }


    /**
     *
     * Read an ESRI shapefile and extract all features into
     * an in-memory structure, with control of time versus resolution.
     *
     * @param filename name of ESRI shapefile (typically has ".shp"
     *        extension)
     * @param coarseness to tradeoff plot quality versus speed.
     *
     * @throws IOException
     */
    public EsriShapefile(String filename, double coarseness)
            throws IOException {
        this(filename, null, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile from a URL and extract all features into
     * an in-memory structure, with control of time versus resolution.
     *
     * @param url URL of ESRI shapefile
     * @param coarseness to tradeoff plot quality versus speed.
     *
     * @throws IOException
     */
    public EsriShapefile(URL url, double coarseness) throws IOException {
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
     * @param coarseness to tradeoff plot quality versus speed.
     *
     * @throws IOException
     */
    public EsriShapefile(URL url, Rectangle2D bBox, double coarseness)
            throws IOException {
        this(new DataInputStream(url.openStream()), bBox, coarseness);
    }


    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure, with control of time versus resolution.
     *
     * @param filename name of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness to tradeoff plot quality versus speed.
     *
     * @throws IOException
     */
    public EsriShapefile(String filename, Rectangle2D bBox, double coarseness)
            throws IOException {
        this(new FileInputStream(filename), bBox, coarseness);
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
     */
    public EsriShapefile(URL url, Rectangle2D bBox) throws IOException {
        this(new DataInputStream(url.openStream()), bBox, 0.0f);
    }


    /**
     *
     * Read an ESRI shapefile and extract the subset of features that have
     * bounding boxes that intersect a specified bounding box.
     *
     * @param filename name of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     *
     * @throws IOException
     */
    public EsriShapefile(String filename, Rectangle2D bBox)
            throws IOException {
        this(new FileInputStream(filename), bBox, 0.0f);
    }


    /**
     *
     * Read an ESRI shapefile and extract the subset of features that
     * have bounding boxes that intersect a specified bounding box,
     * with control of time versus resolution.
     *
     * @param iStream input from which to read
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness
     *
     * @throws IOException
     */
    public EsriShapefile(InputStream iStream, Rectangle2D bBox,
                         double coarseness)
            throws IOException {

        BufferedInputStream bin = new BufferedInputStream(iStream);

        if (coarseness < 0.0f) {
            this.coarseness = defaultCoarseness;
        } else {
            this.coarseness = coarseness;
        }
        if (isZipStream(bin)) {
            BeLeDataInputStream dbInputStream = null;
            byte[]              shapeBytes    = null;
            ZipInputStream      zin           = new ZipInputStream(bin);
            ZipEntry            ze            = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.endsWith(".shp")) {
                    // hold bytes until we've read the other stuff
                    shapeBytes = IOUtil.readBytes(zin, null, false);
                    /*
                    bdis = new BeLeDataInputStream(zin);
                    Init(bBox);
                    if ((dbFile != null) && (prjFile != null)) {
                        return;
                    }
                    */
                } else if (name.endsWith(".dbf")) {
                    dbFile = new DbaseFile(new BeLeDataInputStream(zin));
                    dbFile.loadHeader();
                    dbFile.loadData();
                    if ((shapeBytes != null) && (prjFile != null)) {
                        break;
                    }
                } else if (name.endsWith(".prj")) {
                    try {
                        prjFile = new ProjFile(zin);
                        if (debug) {
                            System.err.println("Make prj file: " + prjFile);
                        }
                    } catch (Exception e) {
                        prjFile = null;
                        break;
                    }
                    if ((shapeBytes != null) && (dbFile != null)) {
                        break;
                    }
                } else {
                    zin.closeEntry();
                }
            }
            if (shapeBytes == null) {
                throw new IOException("no .shp entry found in zipped input");
            } else {
                bdis = new BeLeDataInputStream(
                    new ByteArrayInputStream(shapeBytes));
                Init(bBox);
            }
        } else {
            bdis = new BeLeDataInputStream(bin);
            Init(bBox);
        }
    }


    /**
     * Get the Dbase file object
     *
     * @return  the DbaseFile object; may be null
     */
    public DbaseFile getDbFile() {
        return dbFile;
    }

    /**
     * Get the projection file
     *
     * @return  the projection file; may be null
     */
    public ProjFile getProjFile() {
        return prjFile;
    }

    /**
     * Is this a Zip stream?
     *
     * @param is
     * @return  true if it is
     *
     * @throws IOException
     */
    static boolean isZipStream(InputStream is) throws IOException {
        is.mark(5);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        is.reset();
        if ((c1 == 'P') && (c2 == 'K') && (c3 == 0x03) && (c4 == 0x04)) {
            return true;
        }
        return false;
    }

    /**
     * Initialize with the bounding box
     *
     * @param bBox  bounding box in shape coordinates
     *
     * @throws IOException  problem reading from the file
     */
    private void Init(Rectangle2D bBox) throws IOException {
        int fileCode = readInt();
        if (fileCode != SHAPEFILE_CODE) {
            throw (new IOException("Not a shapefile"));
        }
        skipBytes(20);  // 5 unused ints
        fileBytes     = 2 * readInt();
        version       = readLEInt();
        fileShapeType = readLEInt();
        listBounds    = readBoundingBox();
        //      bBox = null;
        // if no bounds specified, use shapefile bounds
        if (bBox == null) {
            bBox = listBounds;
        }

        double xu = listBounds.getMaxX();
        double yu = listBounds.getMaxY();
        double xl = listBounds.getMinX();
        double yl = listBounds.getMinY();
        double w  = 1000;  // for resolution, just assume 1000x1000 display
        double h  = 1000;
        resolution = 1.0
                     / (coarseness
                        * Math.min(Math.abs(xu - xl) / w,
                                   Math.abs(yu - yl) / h));

        //      System.err.println("coarseness:" + coarseness +" resolution:" + resolution);

        skipBytes(32);  // skip to start of first record header

        /* Read through file, filtering out features that don't
           intersect bounding box. */
        features = new ArrayList<GisFeature>();


        while (bytesSeen < fileBytes) {
            GisFeature  gf       = nextFeature();
            Rectangle2D gfBounds = gf.getBounds2D();
            if ((gfBounds == null) || (gfBounds.getWidth() == 0)
                    || (gfBounds.getHeight() == 0)
                    || gfBounds.intersects(bBox)) {
                features.add(gf);
                //              if(features.size()>10) break;
            }
        }
        //System.err.println("features:" + features.size() + " num points:"
        //                   + NUMPOINTS);
    }



    /**
     * Return percent of file read, so far.
     *
     * @return percent of file read, so far.
     */
    public double percentRead() {
        return (double) bytesSeen / (double) fileBytes;
    }

    /**
     * @return number of features in shapefile
     */
    public int getNumFeatures() {
        return features.size();
    }

    /**
     * @return number of features in shapefile
     * @deprecated
     */
    public int numShapes() {
        return features.size();
    }

    /**
     * Read the bounding box for the file or shape
     * @return the bounding box as a rectangle
     *
     * @throws IOException
     */
    private Rectangle2D readBoundingBox() throws IOException {

        // TODO: convert bounding box to lat/lon if projFile != null
        double xMin   = readLEDouble();
        double yMin   = readLEDouble();
        double xMax   = readLEDouble();
        double yMax   = readLEDouble();
        double width  = xMax - xMin;
        double height = yMax - yMin;

        return new Rectangle2D.Double(xMin, yMin, width, height);
    }

    /**
     * Read the next feature
     * @return  the next feature
     *
     * @throws IOException problem reading the feature
     */
    private EsriFeature nextFeature() throws IOException {


        int recordNumber  = readInt();  // starts at 1, not 0
        int contentLength = readInt();  // in 16-bit words
        featureType = readLEInt();

        //        System.err.println("type:" + featureType);

        switch (featureType) {

          case EsriShapefile.NULL :   // placeholder
              return new EsriNull();

          case EsriShapefile.POINT :  // point data
              // System.err.println ("point");
              return new EsriPoint();

          case EsriShapefile.POINTZ :  // point data
              // System.err.println ("point");
              return new EsriPointZ();

          case EsriShapefile.MULTIPOINT :  // multipoint, only 1 part
              // System.err.println ("multi point");
              return new EsriMultipoint();

          case EsriShapefile.POLYLINE :  // arcs
              // System.err.println ("polyline");
              return new EsriPolyline();

          case EsriShapefile.POLYLINEZ :  // arcs
              // System.err.println ("polyline");
              return new EsriPolylineZ();

          case EsriShapefile.POLYGON :  // polygon
              // System.err.println ("polygon");
              return new EsriPolygon();

          case EsriShapefile.POLYGONZ :  // polygon
              // System.err.println ("polygon");
              return new EsriPolygonZ();

          default :
              throw new IOException("can't handle shapefile shape type "
                                    + featureType);
        }
    }

    /**
     * Read a little endian int
     * @return the value
     *
     * @throws IOException
     */
    private int readLEInt() throws IOException {
        bytesSeen += 4;
        return bdis.readLEInt();
    }

    /**
     * Read a 4 byte integer
     * @return the value
     *
     * @throws IOException
     */
    private int readInt() throws IOException {
        bytesSeen += 4;
        return bdis.readInt();
    }

    /**
     * Read a little endian double
     * @return the value
     *
     * @throws IOException
     */
    private double readLEDouble() throws IOException {
        bytesSeen += 8;
        return bdis.readLEDouble();
    }

    /**
     * Read an array of little endian doubles
     *
     * @param d  the output array
     * @param n  the number of elements
     *
     * @throws IOException
     */
    private void readLEDoubles(double[] d, int n) throws IOException {
        bdis.readLEDoubles(d, n);
        bytesSeen += 8 * n;
    }

    /**
     * Skip bytes
     *
     * @param n the number of bytes to skip
     *
     * @throws IOException
     */
    private void skipBytes(int n) throws IOException {
        bdis.skip(n);
        bytesSeen += n;
    }

    /**
     * Returns shapefile format version (currently 1000)
     *
     * @return version, as stored in shapefile.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get bounding box, according to file (not computed from features)
     *
     * @return bounding box for shapefilew, as stored in header.
     */
    public Rectangle2D getBoundingBox() {
        return listBounds;
    }

    /**
     * Get a List of all the GisFeatures in the shapefile.  This is
     * very fast after the constructor has been called, since it is
     * created during construction.
     *
     * @return a List of features
     */
    public java.util.List getFeatures() {
        return features;
    }

    /**
     * Get a List of all the features in the shapefile that intersect
     * the specified bounding box.  This requires testing every
     * feature in the List created at construction, so it's faster to
     * just give a bounding box o the constructor if you will only do
     * this once.
     *
     * @param bBox specifying the bounding box with which all
     * the returned features bounding boxes have a non-empty
     * intersection.
     *
     * @return a new list of features in the shapefile whose bounding
     * boxes intersect the specified bounding box.
     */
    public java.util.List getFeatures(Rectangle2D bBox) {
        if (bBox == null) {
            return features;
        }
        List list = new ArrayList();
        for (Iterator i = features.iterator(); i.hasNext(); ) {
            EsriFeature gf = (EsriFeature) i.next();
            if (gf.getBounds2D().intersects(bBox)) {
                list.add(gf);
            }
        }
        return list;
    }

    /**
     * EsriFeature.java
     *
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public abstract class EsriFeature extends AbstractGisFeature {

        /** bounds of this feature */
        protected Rectangle2D bounds;

        /** number of points */
        protected int numPoints;

        /** number of parts */
        protected int numParts;

        /** the list of parts */
        protected List partsList = new ArrayList();


        // private int recordNumber;
        // Together these can be used to access more info about feature, 
        // using the associated .dbf Dbase file.
        // TODO: extend interface to permit access to this info

        /**
         * read the points
         *
         * @throws IOException on badness
         */
        protected void readNumPoints() throws IOException {
            numPoints = readLEInt();
            NUMPOINTS += numPoints;
        }


        /**
         * Get bounding rectangle for this feature.
         *
         * @return bounding rectangle for this feature.
         */
        public Rectangle2D getBounds2D() {
            return bounds;
        }

        /**
         * Get total number of points in all parts of this feature.
         *
         * @return total number of points in all parts of this feature.
         */
        public int getNumPoints() {
            return numPoints;
        }

        /**
         * Get number of parts comprising this feature.
         *
         * @return number of parts comprising this feature.
         */
        public int getNumParts() {
            return numParts;
        }

        /**
         * Get the parts of this feature, in the form of an iterator.
         *
         * @return the iterator over the parts of this feature.  Each part
         * is a GisPart.
         */
        public java.util.Iterator getGisParts() {
            return partsList.iterator();
        }

    }  // EsriFeature


    /** buffer for points input */
    private double[] xyPoints = new double[100];  // buffer for points input

    /**
     * Discretize elements of array to a lower resolution.  For
     * example, if resolution = 100., the value 3.14159265358979 will
     * be changed to 3.14.
     *
     * @param d array of values to discretize to lower resolution
     * @param n number of values in array to discretize
     */
    private void discretize(double[] d, int n) {
        if (coarseness == 0.0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            d[i] = (Math.rint(resolution * d[i]) / resolution);
        }
    }

    /**
     * Represents a Polygon in an ESRI shapefile as a List of
     * GisParts.  A Polygon is just an ordered set of vertices of 1 or
     * more parts, where a part is a closed connected sequence of
     * points.  A state boundary might be represented by a Polygon,
     * for example, where each part might be the main part or islands.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriPolygon extends EsriFeature {

        /**
         * Create a new EsriPolygon
         *
         * @throws java.io.IOException
         *
         */
        public EsriPolygon() throws java.io.IOException {
            bounds   = readBoundingBox();
            numParts = readLEInt();
            readNumPoints();
            int[] parts = new int[numParts + 1];
            for (int j = 0; j < numParts; j++) {
                parts[j] = readLEInt();
            }
            parts[numParts] = numPoints;

            if (xyPoints.length < 2 * numPoints) {
                xyPoints = new double[2 * numPoints];
            }
            readLEDoubles(xyPoints, 2 * numPoints);
            discretize(xyPoints, 2 * numPoints);  // overwrites xyPoints

            /* numPoints is reduced by removing dupl. discretized points */
            numPoints = 0;
            int ixy = 0;
            int numPartsLeft = 0;  // may be < numParts after eliminating 1-point parts
            for (int part = 0; part < numParts; part++) {
                int pointsInPart = parts[part + 1] - parts[part];
                /* remove duplicate discretized points in part constructor */
                GisPart gp = new EsriPart(pointsInPart, xyPoints, ixy);
                /* Only add a part if it has 2 or more points, after duplicate
                   point removal */
                if (gp.getNumPoints() > 1) {
                    partsList.add(gp);
                    numPoints += gp.getNumPoints();
                    numPartsLeft++;
                }
                ixy += 2 * pointsInPart;
            }
            numParts = numPartsLeft;
        }
    }  // EsriPolygon




    /**
     * Represents a PolygonZ in an ESRI shapefile as a List of
     * GisParts.  A PolygonZ is just an ordered set of vertices of 1 or
     * more parts, where a part is a closed connected sequence of
     * points.
     *
     * Note: This reads the MRANGE and MPOINTS every time though, according to:
     * http://en.wikipedia.org/wiki/Shapefile
     * these fields are optional but how does one tell?
     *
     * @author Jeff McWhirter
     */
    public class EsriPolygonZ extends EsriFeature {

        /**
         * Create a new EsriPolygonZ
         *
         * @throws java.io.IOException
         *
         */
        public EsriPolygonZ() throws java.io.IOException {
            bounds   = readBoundingBox();
            numParts = readLEInt();
            readNumPoints();
            int[] parts = new int[numParts + 1];
            for (int j = 0; j < numParts; j++) {
                parts[j] = readLEInt();
            }
            parts[numParts] = numPoints;

            //Mandatory: MBR, Number of parts, Number of points, Parts, Points, Z range, Z array
            if (xyPoints.length < 2 * numPoints) {
                xyPoints = new double[2 * numPoints];
            }


            readLEDoubles(xyPoints, 2 * numPoints);

            double[] zRange  = { 0, 0 };
            double[] zPoints = new double[numPoints];

            readLEDoubles(zRange, 2);
            readLEDoubles(zPoints, numPoints);

            //Try reading this again to pick up the mrange and mpoints
            //NOTE: the mrange/mpoints are optional but I don't know how to determine that
            //            readLEDoubles(zRange, 2);
            //            readLEDoubles(zPoints, numPoints);

            discretize(xyPoints, 2 * numPoints);  // overwrites xyPoints

            /* numPoints is reduced by removing dupl. discretized points */
            numPoints = 0;
            int ixy = 0;
            int numPartsLeft = 0;  // may be < numParts after eliminating 1-point parts
            for (int part = 0; part < numParts; part++) {
                int pointsInPart = parts[part + 1] - parts[part];
                /* remove duplicate discretized points in part constructor */
                GisPart gp = new EsriPart(pointsInPart, xyPoints, ixy);
                /* Only add a part if it has 2 or more points, after duplicate
                   point removal */
                if (gp.getNumPoints() > 1) {
                    partsList.add(gp);
                    numPoints += gp.getNumPoints();
                    numPartsLeft++;
                }
                ixy += 2 * pointsInPart;
            }
            numParts = numPartsLeft;
        }
    }  // EsriPolygon


    /**
     * Represents a Polyline in an ESRI shapefile as a List of
     * GisParts.  A Polyline is just an ordered set of vertices of 1
     * or more parts, where a part is a connected sequence of points.
     * A river including its tributaries might be represented by a
     * Polyine, for example, where each part would be a branch of the
     * river.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriPolyline extends EsriFeature {

        /**
         * Create a new EsriPolyline
         *
         * @throws java.io.IOException
         *
         */
        public EsriPolyline() throws java.io.IOException {
            bounds   = readBoundingBox();
            numParts = readLEInt();
            readNumPoints();
            int[] parts = new int[numParts + 1];
            for (int j = 0; j < numParts; j++) {
                parts[j] = readLEInt();
            }
            parts[numParts] = numPoints;

            if (xyPoints.length < 2 * numPoints) {
                xyPoints = new double[2 * numPoints];
            }
            readLEDoubles(xyPoints, 2 * numPoints);
            discretize(xyPoints, 2 * numPoints);  // overwrites xyPoints

            /* numPoints is reduced by removing dupl. discretized points */
            numPoints = 0;
            int ixy = 0;
            int numPartsLeft = 0;  // may be < numParts after eliminating 1-point parts
            for (int part = 0; part < numParts; part++) {
                int pointsInPart = parts[part + 1] - parts[part];
                /* remove duplicate discretized points in part constructor */
                GisPart gp = new EsriPart(pointsInPart, xyPoints, ixy);
                /* Only add a part if it has 2 or more points, after duplicate
                   point removal */
                if (gp.getNumPoints() > 1) {
                    partsList.add(gp);
                    numPoints += gp.getNumPoints();
                    numPartsLeft++;
                }
                ixy += 2 * pointsInPart;
            }
            numParts = numPartsLeft;
        }
    }  // EsriPolyline



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Aug 3, '11
     * @author         Enter your name here...    
     */
    public class EsriPolylineZ extends EsriFeature {

        /**
         * Create a new EsriPolyline
         *
         * @throws java.io.IOException
         *
         */
        public EsriPolylineZ() throws java.io.IOException {
            bounds   = readBoundingBox();
            numParts = readLEInt();
            readNumPoints();
            int[] parts = new int[numParts + 1];
            for (int j = 0; j < numParts; j++) {
                parts[j] = readLEInt();
            }
            parts[numParts] = numPoints;

            if (xyPoints.length < 2 * numPoints) {
                xyPoints = new double[2 * numPoints];
            }
            readLEDoubles(xyPoints, 2 * numPoints);

            double[] zRange  = { 0, 0 };
            double[] zPoints = new double[numPoints];

            readLEDoubles(zRange, 2);
            readLEDoubles(zPoints, numPoints);

            //Try reading this again to pick up the mrange and mpoints
            //NOTE: the mrange/mpoints are optional but I don't know how to determine that
            //            readLEDoubles(zRange, 2);
            //            readLEDoubles(zPoints, numPoints);


            discretize(xyPoints, 2 * numPoints);  // overwrites xyPoints

            /* numPoints is reduced by removing dupl. discretized points */
            numPoints = 0;
            int ixy = 0;
            int numPartsLeft = 0;  // may be < numParts after eliminating 1-point parts
            for (int part = 0; part < numParts; part++) {
                int pointsInPart = parts[part + 1] - parts[part];
                /* remove duplicate discretized points in part constructor */
                GisPart gp = new EsriPart(pointsInPart, xyPoints, ixy);
                /* Only add a part if it has 2 or more points, after duplicate
                   point removal */
                if (gp.getNumPoints() > 1) {
                    partsList.add(gp);
                    numPoints += gp.getNumPoints();
                    numPartsLeft++;
                }
                ixy += 2 * pointsInPart;
            }
            numParts = numPartsLeft;
        }
    }  // EsriPolyline


    /**
     * Represents a Multipoint in an ESRI shapefile.  A
     * Multipoint is a set of 2D points.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriMultipoint extends EsriFeature {

        /**
         * Create a new EsriMultipoint
         *
         * @throws java.io.IOException
         *
         */
        public EsriMultipoint() throws java.io.IOException {
            bounds = readBoundingBox();
            readNumPoints();
            if (xyPoints.length < 2 * numPoints) {
                xyPoints = new double[2 * numPoints];
            }
            readLEDoubles(xyPoints, 2 * numPoints);
            discretize(xyPoints, 2 * numPoints);
            GisPart gp = new EsriPart(numPoints, xyPoints, 0);
            partsList.add(gp);
        }
    }  // EsriMultipoint


    /**
     * Represents a single point in an ESRI shapefile.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriPoint extends EsriFeature {

        /**
         * Create a new EsriPoint
         *
         * @throws java.io.IOException
         *
         */
        public EsriPoint() throws java.io.IOException {
            numPoints = 1;
            NUMPOINTS++;
            readLEDoubles(xyPoints, 2 * numPoints);
            discretize(xyPoints, 2 * numPoints);
            GisPart gp = new EsriPart(numPoints, xyPoints, 0);
            partsList.add(gp);
            numParts = 1;
            bounds = new Rectangle2D.Double(xyPoints[0], xyPoints[1], 0., 0.);
        }
    }  // EsriPoint


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Aug 3, '11
     * @author         Enter your name here...    
     */
    public class EsriPointZ extends EsriFeature {

        /**
         * Create a new EsriPoint
         *
         * @throws java.io.IOException
         *
         */
        public EsriPointZ() throws java.io.IOException {
            numPoints = 1;
            NUMPOINTS++;
            readLEDoubles(xyPoints, 2 * numPoints);
            discretize(xyPoints, 2 * numPoints);
            //Read the extra M
            double  M  = readLEDouble();
            GisPart gp = new EsriPart(numPoints, xyPoints, 0);
            partsList.add(gp);
            numParts = 1;
            bounds = new Rectangle2D.Double(xyPoints[0], xyPoints[1], 0., 0.);
        }
    }  // EsriPoint


    /**
     * A NULL shape in an ESRI shapefile.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriNull extends EsriFeature {

        /**
         * Create a null feature
         *
         */
        public EsriNull() {
            numPoints = 0;
        }
    }  // EsriNull

    /**
     * Implementation of GisPart for Esri specific features, x and y are
     * converted to lon/lat if a ProjFile is available
     */
    class EsriPart implements GisPart {

        /** number of points */
        private int numPoints = 0;

        /** the x values */
        private double[] x;

        /** the y values */
        private double[] y;

        /**
         * Construct an EsriPart by eliding duplicates from array
         * representing points.
         *
         * @param num number of input points to use
         * @param xyPoints array containing consecutive (x,y) pair for
         * each point.
         * @param xyOffset index in array from which to start
         */
        public EsriPart(int num, double[] xyPoints, int xyOffset) {
            double xi, yi;
            /* In first pass over data, just count nonduplicated points */
            int ixy = xyOffset;
            numPoints = 1;
            double xx = xyPoints[ixy++];
            double yy = xyPoints[ixy++];
            for (int i = 1; i < num; i++) {
                xi = xyPoints[ixy++];
                yi = xyPoints[ixy++];
                if ((xi != xx) || (yi != yy)) {
                    numPoints++;
                    xx = xi;
                    yy = yi;
                }
            }

            /* second pass: store nonduplicated points */
            x   = new double[numPoints];
            y   = new double[numPoints];
            ixy = xyOffset;
            int j = 0;
            x[j] = xyPoints[ixy++];
            y[j] = xyPoints[ixy++];
            xx   = x[j];
            yy   = y[j];
            for (int i = 1; i < num; i++) {
                xi = xyPoints[ixy++];
                yi = xyPoints[ixy++];
                if ((xi != xx) || (yi != yy)) {
                    j++;
                    x[j] = xi;
                    y[j] = yi;
                    xx   = x[j];
                    yy   = y[j];
                }
            }
            double[][] xy = makeLatLon(new double[][] {
                x, y
            });
            x = xy[0];
            y = xy[1];

        }

        /**
         * Get the number of points
         * @return the number of points
         */
        public int getNumPoints() {
            return numPoints;
        }

        /**
         * Get the X values
         * @return  the x values
         */
        public double[] getX() {
            return x;
        }

        /**
         * Get the Y values
         * @return the Y values
         */
        public double[] getY() {
            return y;
        }

        /**
         * Convert xy to lat/lon if we have a ProjFile
         *
         * @param xy  the xy values
         *
         * @return  the lat/lon values if there is a proj file
         */
        private double[][] makeLatLon(double[][] xy) {
            if (getProjFile() == null) {
                return xy;
            }
            return getProjFile().convertToLonLat(xy);
        }

    }

    /**
     * Get the feature type
     *
     * @return  the type
     */
    public int getFeatureType() {
        return featureType;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws IOException _more_
     */
    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            System.err.println(arg);
            EsriShapefile shapefile = new EsriShapefile(arg);
        }
    }


}

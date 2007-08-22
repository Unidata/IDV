/*
 * $Id: EsriShapefile.java,v 1.27 2005/05/13 18:29:48 jeffmc Exp $
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



import java.awt.geom.Rectangle2D;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ucar.unidata.gis.AbstractGisFeature;
import ucar.unidata.gis.GisFeature;
import ucar.unidata.gis.GisPart;
import ucar.unidata.io.BeLeDataInputStream;


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

    /** _more_ */
    public final static int SHAPEFILE_CODE = 9994;  // shapefile magic number

    // these are only shape types handled by this package, so far

    /** _more_ */
    public final static int NULL = 0;

    /** _more_ */
    public final static int POINT = 1;

    /** _more_ */
    public final static int POLYLINE = 3;

    /** _more_ */
    public final static int POLYGON = 5;

    /** _more_ */
    public final static int MULTIPOINT = 8;

    // Eventually we should handle these new types also (is anyone using these?)

    /** _more_ */
    public final static int POINTZ = 11;

    /** _more_ */
    public final static int POLYLINEZ = 13;

    /** _more_ */
    public final static int POLYGONZ = 15;

    /** _more_ */
    public final static int MULTIPOINTZ = 18;

    /** _more_ */
    public final static int POINTM = 21;

    /** _more_ */
    public final static int POLYLINEM = 23;

    /** _more_ */
    public final static int POLYGONM = 25;

    /** _more_ */
    public final static int MULTIPOINTM = 28;

    /** _more_ */
    public final static int MULTIPATCH = 31;

    /** _more_          */
    private int featureType;


    /** _more_ */
    private BeLeDataInputStream bdis;               // the shapefile data stream

    /** _more_ */
    private int fileBytes;                          // bytes in file, according to header

    /** _more_ */
    private int bytesSeen = 0;                      // so far, in bytes.

    /** _more_ */
    private int version;                            // of shapefile format (currently 1000)

    /** _more_ */
    private int fileShapeType;                      // not used here

    /** _more_ */
    private ArrayList features;                     // EsriFeatures in List

    /** _more_ */
    private Rectangle2D listBounds;                 // bounds from shapefile

    /** _more_ */
    private double resolution;                      // computed from coarseness

    /** _more_          */
    private DbaseFile dbFile;


    /** _more_ */
    private final static double defaultCoarseness = 0.0;
    /*
     * Relative accuracy of plotting.  Larger means coarser but
     * faster, 0.0 is all available resolution.  Anything less than 1
     * is wasted sub-pixel plotting, but retains quality for closer
     * zooming.  Anything over about 2.0 is ugly.  1.50 makes things
     * faster than 1.0 at the cost of barely discernible ugliness, but
     * for best quality (without zooms), set to 1.0.  If you still
     * want quality at 10:1 zooms, set to 1/10, etc.  */

    /** _more_ */
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
     * Read an ESRI shapefile from a URL and extract all features into
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
    public EsriShapefile(InputStream iStream, Rectangle2D bBox, double coarseness)
            throws IOException {

        BufferedInputStream bin = new BufferedInputStream(iStream);

        if (coarseness < 0.0f) {
            this.coarseness = defaultCoarseness;
        } else {
            this.coarseness = coarseness;
        }
        if (isZipStream(bin)) {
            BeLeDataInputStream dbInputStream = null;
            ZipInputStream      zin           = new ZipInputStream(bin);
            ZipEntry            ze            = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.endsWith(".shp")) {
                    bdis = new BeLeDataInputStream(zin);
                    Init(bBox);
                    if (dbFile != null) {
                        return;
                    }
                } else if (name.endsWith(".dbf")) {
                    dbFile = new DbaseFile(new BeLeDataInputStream(zin));
                    dbFile.loadHeader();
                    dbFile.loadData();
                    if (bdis != null) {
                        return;
                    }
                } else {
                    zin.closeEntry();
                }
            }
            if (bdis == null) {
                throw new IOException("no .shp entry found in zipped input");
            }
        } else {
            bdis = new BeLeDataInputStream(bin);
            Init(bBox);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DbaseFile getDbFile() {
        return dbFile;
    }

    /**
     * _more_
     *
     * @param is
     * @return _more_
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
     * _more_
     *
     * @param bBox
     *
     * @throws IOException
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

        // if no bounds specified, use shapefile bounds
        if (bBox == null) {
            bBox = listBounds;
        }

        double xu = bBox.getMaxX();
        double yu = bBox.getMaxY();
        double xl = bBox.getMinX();
        double yl = bBox.getMinY();
        double w  = 1000;  // for resolution, just assume 1000x1000 display
        double h  = 1000;
        resolution = 1.0
                     / (coarseness
                        * Math.min(Math.abs(xu - xl) / w,
                                   Math.abs(yu - yl) / h));

        skipBytes(32);  // skip to start of first record header

        /* Read through file, filtering out features that don't
           intersect bounding box. */
        features = new ArrayList();

        while (bytesSeen < fileBytes) {
            GisFeature  gf       = nextFeature();
            Rectangle2D gfBounds = gf.getBounds2D();
            if ((gfBounds == null) || (gfBounds.getWidth() == 0)
                    || (gfBounds.getHeight() == 0)
                    || gfBounds.intersects(bBox)) {
                features.add(gf);
            }
        }
        //      System.err.println ("features:" + features.size());
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
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    private Rectangle2D readBoundingBox() throws IOException {

        double xMin   = readLEDouble();
        double yMin   = readLEDouble();
        double xMax   = readLEDouble();
        double yMax   = readLEDouble();
        double width  = xMax - xMin;
        double height = yMax - yMin;

        return new Rectangle2D.Double(xMin, yMin, width, height);
    }

    /**
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    private EsriFeature nextFeature() throws IOException {

        int recordNumber  = readInt();  // starts at 1, not 0
        int contentLength = readInt();  // in 16-bit words
        featureType = readLEInt();


        switch (featureType) {

          case EsriShapefile.NULL :        // placeholder
              return new EsriNull();

          case EsriShapefile.POINT :       // point data
              //      System.err.println ("point");
              return new EsriPoint();

          case EsriShapefile.MULTIPOINT :  // multipoint, only 1 part
              //      System.err.println ("multi point");
              return new EsriMultipoint();

          case EsriShapefile.POLYLINE :    // arcs
              //      System.err.println ("polyline");
              return new EsriPolyline();

          case EsriShapefile.POLYGON :     // polygon
              //      System.err.println ("polygon");
              return new EsriPolygon();

          default :
              throw new IOException("can't handle shapefile shape type "
                                    + featureType);
        }
    }

    /**
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    private int readLEInt() throws IOException {
        bytesSeen += 4;
        return bdis.readLEInt();
    }

    /**
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    private int readInt() throws IOException {
        bytesSeen += 4;
        return bdis.readInt();
    }

    /**
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    private double readLEDouble() throws IOException {
        bytesSeen += 8;
        return bdis.readLEDouble();
    }

    /**
     * _more_
     *
     * @param d
     * @param n
     *
     * @throws IOException
     */
    private void readLEDoubles(double[] d, int n) throws IOException {
        bdis.readLEDoubles(d, n);
        bytesSeen += 8 * n;
    }

    /**
     * _more_
     *
     * @param n
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

        /** _more_ */
        protected Rectangle2D bounds;

        /** _more_ */
        protected int numPoints;

        /** _more_ */
        protected int numParts;

        /** _more_ */
        protected List partsList = new ArrayList();


        // private int recordNumber;
        // Together these can be used to access more info about feature, 
        // using the associated .dbf Dbase file.
        // TODO: extend interface to permit access to this info

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


    /** _more_ */
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
         * _more_
         *
         * @throws java.io.IOException
         *
         */
        public EsriPolygon() throws java.io.IOException {
            bounds    = readBoundingBox();
            numParts  = readLEInt();
            numPoints = readLEInt();
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
            int ixy          = 0;
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
         * _more_
         *
         * @throws java.io.IOException
         *
         */
        public EsriPolyline() throws java.io.IOException {
            bounds    = readBoundingBox();
            numParts  = readLEInt();
            numPoints = readLEInt();
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
            int ixy          = 0;
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
         * _more_
         *
         * @throws java.io.IOException
         *
         */
        public EsriMultipoint() throws java.io.IOException {

            bounds = readBoundingBox();
            int numPoints = readLEInt();
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
         * _more_
         *
         * @throws java.io.IOException
         *
         */
        public EsriPoint() throws java.io.IOException {
            numPoints = 1;
            readLEDoubles(xyPoints, 2 * numPoints);
            discretize(xyPoints, 2 * numPoints);
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
         * _more_
         *
         */
        public EsriNull() {
            int numPoints = 0;
        }
    }  // EsriNull

    /**
     * Class EsriPart
     *
     *
     * @author
     * @version %I%, %G%
     */
    class EsriPart implements GisPart {

        /** _more_ */
        private int numPoints = 0;

        /** _more_ */
        private double[] x;

        /** _more_ */
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
        }

        /**
         * _more_
         * @return _more_
         */
        public int getNumPoints() {
            return numPoints;
        }

        /**
         * _more_
         * @return _more_
         */
        public double[] getX() {
            return x;
        }

        /**
         * _more_
         * @return _more_
         */
        public double[] getY() {
            return y;
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getFeatureType() {
        return featureType;
    }

}



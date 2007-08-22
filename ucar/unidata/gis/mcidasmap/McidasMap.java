/*
 * $Id: McidasMap.java,v 1.6 2005/05/13 18:29:44 jeffmc Exp $
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

package ucar.unidata.gis.mcidasmap;



import java.awt.*;
import java.awt.geom.*;

import java.io.*;

import java.util.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.*;
import ucar.unidata.util.Resource;


/**
 * A renderer for McIDAS OUTL* map files
 * @author Don Murray
 * @version $Id: McidasMap.java,v 1.6 2005/05/13 18:29:44 jeffmc Exp $
 */

public class McidasMap extends ucar.unidata.gis.GisFeatureRenderer {

    /** _more_ */
    private ArrayList gisList;

    /** _more_ */
    private ArrayList partList = null;

    /** _more_ */
    private int total_pts = 0;

    /** _more_ */
    private McidasMapFeature mcidasMapFeature = null;

    /** _more_ */
    private String mapName;

    /** _more_ */
    private boolean debug     = false,
                    debugTime = false;

    /** _more_ */
    private LatLonRect defaultLLBB = null;

    /** _more_ */
    private ProjectionImpl dataProject =
        new LatLonProjection("Cylindrical Equidistant");

    /** _more_ */
    private int MAX_SEGMENTS = 100000;  // docs say 1000, but they lie!

    // constructor

    /**
     * _more_
     *
     * @param mapName
     *
     */
    public McidasMap(String mapName) {

        this.mapName = mapName;
        readMcidasMap();

    }

    // read in lat/lon points one time for this class

    /**
     * _more_
     * @return _more_
     */
    private boolean readMcidasMap() {

        java.io.DataInputStream dis;
        java.io.InputStream     is   = null;
        long                    secs = System.currentTimeMillis();

        is = Resource.getFileResource(null, mapName);

        if (is == null) {
            System.err.println("McidasMap read failed on resource "
                               + mapName);
            return false;
        } else {
            dis = new java.io.DataInputStream(new BufferedInputStream(is));
        }

        // need an AbstractGisFeature for visad
        mcidasMapFeature = new McidasMapFeature();
        // need an ArrayList of AbstractGisFeature's for GisFeatureRenderer
        gisList = new ArrayList();
        gisList.add(mcidasMapFeature);

        partList = new ArrayList();

        int numSegments;
        try {
            numSegments = dis.readInt();
        } catch (IOException e) {
            return false;
        }

        if ((numSegments <= 0) || (numSegments > MAX_SEGMENTS)) {
            return false;
        }

        int position = 4;  // position in the file (in bytes);

        // read in the directories
        for (int j = 0; j < numSegments; j++) {
            try {
                /* Each segement directory has 6 words:
                     0 - min lat
                     1 - max lat
                     2 - min lon
                     3 - max lon
                     4 - pointer (words) to start of data for segment
                     5 - number of words to read for the segment
                         (should be even lat/lon)
                */
                float miny  = dis.readInt() / 10000.f;
                float maxy  = dis.readInt() / 10000.f;
                float minx  = dis.readInt() / 10000.f;
                float maxx  = dis.readInt() / 10000.f;
                int   start = dis.readInt();
                int   npts  = dis.readInt();
                position += 24;  // (6 words * 4 bytes/word);
                if (start < 0) {
                    continue;
                }
                if ((npts < 0) || (npts % 2 != 0)) {
                    continue;
                }
                npts = npts / 2;

                McidasMapPart run = new McidasMapPart(npts, minx, maxx, miny,
                                                      maxy, start);
                partList.add(run);
                total_pts += npts;

            } catch (EOFException ex) {
                break;
            } catch (Exception ex) {
                System.err.println("McidasMap exception " + ex);
                break;
            }
        }

        // now read in the data
        for (int i = 0; i < partList.size(); i++) {
            McidasMapPart part     = (McidasMapPart) partList.get(i);
            int           start    = part.getStartingWord();
            int           numPairs = part.getNumPoints();
            int           skipByte = (start * 4) - position;
            try {
                dis.skipBytes(skipByte);
                position += skipByte;
            } catch (Exception e) {
                break;
            }

            try {
                for (int l = 0; l < numPairs; l++) {
                    part.wy[l] = dis.readInt() / 10000.;
                    position   += 4;
                    part.wx[l] = -(double) dis.readInt() / 10000.;  //file is west positive
                    position += 4;
                }
            } catch (IOException e) {
                return false;
            }

        }

        try {
            is.close();
        } catch (Exception ex) {}

        if (debugTime) {
            secs = System.currentTimeMillis() - secs;
            System.out.println("McidasMap read file: " + secs * .001
                               + " seconds");
        }
        return true;
    }

    /**
     * Class McidasMapFeature
     *
     *
     * @author
     * @version %I%, %G%
     */
    private class McidasMapFeature extends AbstractGisFeature {

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
            return partList.size();
        }

        /**
         * _more_
         * @return _more_
         */
        public java.util.Iterator getGisParts() {
            return partList.iterator();
        }
    }

    /**
     * Class McidasMapPart
     *
     *
     * @author
     * @version %I%, %G%
     */
    private class McidasMapPart implements GisPart {

        /** _more_ */
        int npts, start;

        /** _more_ */
        double[] wx;                            // lat/lon coords

        /** _more_ */
        double[] wy;

        /** _more_ */
        private double minx, miny, maxx, maxy;  // ??

        // constructor

        /**
         * _more_
         *
         * @param npts
         * @param minx
         * @param maxx
         * @param miny
         * @param maxy
         * @param start
         *
         */
        McidasMapPart(int npts, double minx, double maxx, double miny,
                      double maxy, int start) {
            this.npts  = npts;
            this.minx  = minx;
            this.maxx  = maxx;
            this.miny  = miny;
            this.maxy  = maxy;
            this.start = start;

            wx         = new double[npts];
            wy         = new double[npts];
        }

        /**
         * _more_
         * @return _more_
         */
        public int getStartingWord() {
            return start;
        }

        // implement GisPart

        /**
         * _more_
         * @return _more_
         */
        public int getNumPoints() {
            return npts;
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

    // GisFeatureRenderer abstract methods

    /**
     * _more_
     * @return _more_
     */
    public LatLonRect getPreferredArea() {
        return defaultLLBB;
    }

    /**
     * _more_
     * @return _more_
     */
    protected java.util.List getFeatures() {
        return gisList;
    }

    /**
     * _more_
     * @return _more_
     */
    protected ProjectionImpl getDataProjection() {
        return dataProject;
    }

    /**
     * _more_
     * @return _more_
     */
    public AbstractGisFeature getMcidasMap() {
        return mcidasMapFeature;
    }

}








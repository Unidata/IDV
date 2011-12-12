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



import visad.*;

import java.io.*;

import java.net.URL;

import java.util.StringTokenizer;
import java.util.Vector;


/**
 * VisAD Adapter for Zebra ASCII map files
 *
 * @author MetApps Development Team
 * @version $Revision: 1.4 $ $Date: 2005/05/13 18:34:07 $
 */
public class ZebraAsciiMapAdapter {

    /** Set of map lines */
    private UnionSet maplines;

    /** reader for the data */
    private BufferedReader reader;

    /**
     * Construct the adapter from the filename specification.
     *
     * @param filename  path to file
     *
     * @throws VisADException  couldn't create the data object
     * @throws IOException problem reading in file
     */
    public ZebraAsciiMapAdapter(String filename)
            throws IOException, VisADException {
        this(new FileInputStream(filename));
    }

    /**
     * Construct the adapter from the URL location
     *
     * @param url  URL path to file
     *
     * @throws VisADException  couldn't create the data object
     * @throws IOException problem reading in file
     */
    public ZebraAsciiMapAdapter(URL url) throws IOException, VisADException {
        this(url.openStream());
    }

    /**
     * Construct the adapter from an input stream
     *
     * @param is  InputStream for the data
     *
     * @throws VisADException  couldn't create the data object
     * @throws IOException problem reading in file
     */
    public ZebraAsciiMapAdapter(InputStream is)
            throws IOException, VisADException {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Get the Data object.
     *
     * @return  UnionSet of Gridded2DSets of map lines.
     *
     * @throws VisADException  can't create the VisAD data objects
     */
    public UnionSet getData() throws VisADException {

        Vector sets = new Vector();
        try {
            int    i    = 0;
            String line = reader.readLine();  // grab first line
            while (line != null) {

                PolyLineHeader ph        = new PolyLineHeader(line);
                int            numPoints = ph.getNumPoints();
                float[][]      points    = new float[2][numPoints];
                int            curPoint  = 0;
                while (curPoint < numPoints) {
                    line = reader.readLine();
                    StringTokenizer tok = new StringTokenizer(line);
                    while (tok.hasMoreTokens()) {
                        points[0][curPoint] =
                            Float.parseFloat(tok.nextToken());
                        points[1][curPoint] =
                            Float.parseFloat(tok.nextToken());
                        curPoint++;
                    }
                }
                Gridded2DSet set =
                    new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                                     points, numPoints);
                sets.add(set);
                line = reader.readLine();
            }
            if ( !sets.isEmpty()) {

                Gridded2DSet[] basemaplines = new Gridded2DSet[sets.size()];
                sets.copyInto(basemaplines);

                maplines = new UnionSet(basemaplines);
            }

        } catch (IOException ie) {
            ie.printStackTrace();
        }

        return maplines;
    }

    /** class for defining the header information */
    private class PolyLineHeader {

        /** number of points */
        private int numPoints;

        /** minimum Latitude */
        private float minLat;

        /** maximum Latitude */
        private float maxLat;

        /** minimum longitude */
        private float minLon;

        /** maximum longitude */
        private float maxLon;

        /** fill the shape */
        private boolean fill = false;

        /**
         * Construct a PolyLineHeader
         *
         * @param header  header to decode
         *
         */
        public PolyLineHeader(String header) {
            StringTokenizer tok = new StringTokenizer(header);
            numPoints = Integer.parseInt(tok.nextToken()) / 2;
            maxLat    = Float.parseFloat(tok.nextToken());
            minLat    = Float.parseFloat(tok.nextToken());
            maxLon    = Float.parseFloat(tok.nextToken());
            minLon    = Float.parseFloat(tok.nextToken());

            fill      = tok.hasMoreTokens();
        }

        /**
         * Get the number of points
         * @return number of points
         */
        public int getNumPoints() {
            return numPoints;
        }

    }


    /**
     * Test by running  java ucar.visad.ZebraAsciiMapAdapter <mapfile>
     *
     * @param args  mapfile locations (filenames
     *
     * @throws Exception  if there is a problem
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("must supply filename");
            System.exit(1);
        }
        ZebraAsciiMapAdapter zama = new ZebraAsciiMapAdapter(args[0]);
        visad.python.JPythonMethods.dumpTypes(zama.getData());
    }

}

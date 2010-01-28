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

package ucar.unidata.data.radar;


import ucar.nc2.dt.RadialDatasetSweep;

import java.io.IOException;

import java.util.HashMap;


/**
 * Class CDMRadarSweepDB holds things
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CDMRadarSweepDB {

    /** The table */
    private HashMap sweepTableHash = null;

    /**
     * init a CDM radar sweep table
     *
     * @param s sweep
     * @param sIdx _more_
     * @param b _more_
     *
     * @throws IOException On badness
     */
    public CDMRadarSweepDB(float[] s, int[] sIdx, float b)
            throws IOException {
        if (sweepTableHash == null) {
            readSweepTable(s, sIdx, b);
        }
    }

    /**
     * get the ray index
     *
     * @param idx rayIndex
     *
     * @return Ray object
     */
    public Ray get(int idx) {
        return (Ray) sweepTableHash.get(Integer.toString(idx));
    }


    /**
     * calculate each ray in a sweep to construct a hash map table
     *
     * @param s radialDatasetSweep object
     * @param sIdx _more_
     * @param beamWidth _more_
     *
     * @throws IOException On badness
     */
    private void readSweepTable(float[] s, int[] sIdx, float beamWidth)
            throws IOException {
        sweepTableHash = new HashMap();

        int   i, iazim;
        int   numberOfRay = s.length;
        float res;
        if (s == null) {
            return;
        }

        res = 360.0f / numberOfRay;
        /* Check that this makes sense with beam width. */
        if ((res > 2 * beamWidth) && (beamWidth != 0)) {

            res = beamWidth;
        }
        float[] _azimuths = s;
        for (i = 0; i < numberOfRay; i++) {
            float azi = 0.0f;
            //  try {
            azi = _azimuths[i];
            if ( !Float.isNaN(azi)) {
                iazim = (int) (azi / res + res / 2.0);  /* Centered on bin. */
                //  } catch (IOException e) {
                //     e.printStackTrace();
                //     iazim = 0;
                //  }
                if (iazim >= numberOfRay) {
                    iazim -= numberOfRay;
                }

                Ray r = new Ray();
                r.index    = Integer.toString(iazim);
                r.rayIndex = sIdx[i];
                r.azimuth  = azi;

                /*      fprintf(stderr,"ray# %d, azim %f, iazim %d\n", ray->h.ray_num, ray->h.azimuth, iazim); */
                sweepTableHash.put(r.index, r);
            }
        }

    }

    /**
     * Class Ray represents a ray
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class Ray {

        /** azi angle index, such 255 */
        public String index;

        /** number index, 0 to 360+/- */
        public int rayIndex;

        /** azimuth angle, 0 to 360+/- degree */
        public float azimuth;

        /**
         * get the string object of rayIndex
         *
         * @return rayIndex string
         */
        public String toString() {
            return Integer.toString(rayIndex);
        }
    }

}

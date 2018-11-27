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

package ucar.unidata.data.radar;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Class CDMRadarSweepDB holds things.
 *
 */
public class CDMRadarSweepDB {

    /** The table. */
    private Map<Integer,Ray> sweepTableHash = null;

    /**
     * init a CDM radar sweep table.
     *
     * @param s
     *          sweep
     * @param sIdx
     *          s index
     * @param b
     *          beam
     * @throws IOException
     *           On badness
     */
    public CDMRadarSweepDB(float[] s, int[] sIdx, float b)
            throws IOException {
        if (sweepTableHash == null) {
            readSweepTable(s, sIdx, b);
        }
    }

    /**
     * get the ray index.
     *
     * @param idx
     *          rayIndex
     * @return Ray object
     */
    public Ray get(int idx) {
        return (Ray) sweepTableHash.get(idx);
    }

    /**
     * calculate each ray in a sweep to construct a hash map table.
     *
     * @param s
     *          radialDatasetSweep object
     * @param sIdx
     *          s index
     * @param beamWidth
     *          beam width
     * @throws IOException
     *           On badness
     */
    private void readSweepTable(float[] s, int[] sIdx, float beamWidth)
            throws IOException {
        sweepTableHash = new HashMap<Integer,Ray>();

        int   i, iazim;
        int   numberOfRay = s.length;
        float res;
        if (s == null) {
            return;
        }

        res = 360.0f / numberOfRay;
        /* Check that this makes sense with beam width. */
        if (beamWidth != 0) {
            res = beamWidth;
        }
        float[] _azimuths = s;
        for (i = 0; i < numberOfRay; i++) {
            float azi = 0.0f;
            // try {
            azi = _azimuths[i];
            if ( !Float.isNaN(azi)) {
                iazim = Math.round (azi + res / 2.0f);  /* Centered on bin. */
                // } catch (IOException e) {
                // e.printStackTrace();
                // iazim = 0;
                // }
                if (iazim >= numberOfRay) {
                    iazim -= numberOfRay;
                }

                Ray r = new Ray();
                r.index    = iazim;
                r.rayIndex = sIdx[i];
                r.azimuth  = azi;
                //System.out.println("I " + i + " INDEX " + iazim + " AZI " + azi + "\n");
                /*
                 * fprintf(stderr,"ray# %d, azim %f, iazim %d\n", ray->h.ray_num,
                 * ray->h.azimuth, iazim);
                 */
                if(r.index < 360)
                    sweepTableHash.put(r.index, r);
            }
        }

    }

    /**
     * Class Ray represents a ray.
     *
     */
    public class Ray {

        /** azi angle index, such 255. */
        public int index;

        /** number index, 0 to 360+/-. */
        public int rayIndex;

        /** azimuth angle, 0 to 360+/- degree. */
        public float azimuth;

        /**
         * get the string object of rayIndex.
         *
         * @return rayIndex string
         */
        public String toString() {
            return Integer.toString(rayIndex);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime  = 31;
            int       result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + Float.floatToIntBits(azimuth);
            result = prime * result + index;
            result = prime * result + rayIndex;
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Ray other = (Ray) obj;
            if ( !getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (Float.floatToIntBits(azimuth)
                    != Float.floatToIntBits(other.azimuth)) {
                return false;
            }
            if (index != other.index) {
                return false;
            }
            if (rayIndex != other.rayIndex) {
                return false;
            }
            return true;
        }

        /**
         * For equals/hashcode
         *
         * @return CDMRadarSweepDB
         */
        private CDMRadarSweepDB getOuterType() {
            return CDMRadarSweepDB.this;
        }
    }

}

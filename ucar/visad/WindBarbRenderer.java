package ucar.visad;

import visad.CommonUnit;
import visad.Unit;
import visad.UnitException;
import visad.bom.BarbRendererJ3D;

    
    /**
     * Custom barb renderer
     *
     *
     * @author IDV Development Team
     */
    public class WindBarbRenderer extends BarbRendererJ3D {

        private Unit speedUnit = CommonUnit.meterPerSecond;
        
        /**
         * Ctor
         */
        public WindBarbRenderer() {
        	this(CommonUnit.meterPerSecond);
        }

        /**
         * Ctor
         */
        public WindBarbRenderer(Unit speedUnit) {
            super();
        	this.speedUnit = speedUnit;
        }

        /**
         * Make the barb
         *
         * @param south true if southern hemisphere
         * @param x  x position
         * @param y  y position
         * @param z  z position
         * @param scale  scale factor
         * @param pt_size  spacing
         * @param f0  u component
         * @param f1  v component
         * @param vx  line x points
         * @param vy  line y points
         * @param vz  line z points
         * @param numv  num line points
         * @param tx triangle x points
         * @param ty triangle y points
         * @param tz triangle z points
         * @param numt triangles
         *
         * @return stuff
         */
        public float[] makeVector(boolean south, float x, float y, float z,
                                  float scale, float pt_size, float f0,
                                  float f1, float[] vx, float[] vy,
                                  float[] vz, int[] numv, float[] tx,
                                  float[] ty, float[] tz, int[] numt) {
            // WindBarb.makeBarb(New) always expects f0 and f1 to be in m/s
            Unit u = speedUnit;
            if ((u != null) && Unit.canConvert(u, CommonUnit.meterPerSecond)
                    && !u.equals(CommonUnit.meterPerSecond)) {
                try {
                    // convert meters per second to knots
                    f0 = (float) CommonUnit.meterPerSecond.toThis(f0, u);
                    f1 = (float) CommonUnit.meterPerSecond.toThis(f1, u);
                } catch (UnitException ue) {}
            }
            return WindBarb.makeBarbNew(south, x, y, z, scale, pt_size, f0,
                                        f1, vx, vy, vz, numv, tx, ty, tz,
                                        numt);
        }

        /**
         * Clone this
         *
         * @return a new one
         */
        public Object clone() {
            return new WindBarbRenderer();
        }

    }

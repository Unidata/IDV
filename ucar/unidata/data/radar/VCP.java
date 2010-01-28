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



/**
 * Static class for describing Volume Coverage Patterns (VCP)
 * for Archive Level II data files.  Will probably change in the
 * future.
 *
 * Adapted with permission from the Java Iras software developed
 * by David Priegnitz at NSSL.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.11 $ $Date: 2006/12/01 20:42:39 $
 */
public class VCP {

    /** Angles for VCP 11 */
    private static final double[] vcp_11_angles = {
        0.5, 1.5, 2.4, 3.4, 4.3, 5.3, 6.2, 7.5, 8.7, 10.0, 12.0, 14.0, 16.7,
        19.5
    };

    /** Angles for VCP 12 */
    private static final double[] vcp_12_angles = {
        0.5, 0.9, 1.3, 1.8, 2.4, 3.1, 4.0, 5.1, 6.4, 8.0, 10.0, 12.5, 15.6,
        19.5
        //0.5, 0.5, 0.9, 0.9, 1.3, 1.3, 1.8, 2.4, 3.1, 4.0, 5.1, 6.4, 8.0, 10.0, 12.5, 15.6, 19.5
    };

    /** Angles for VCP 21 */
    private static final double[] vcp_21_angles = {
        0.5, 1.5, 2.4, 3.4, 4.3, 6.0, 9.9, 14.6, 19.5
    };

    /** Angles for VCP 31 */
    private static final double[] vcp_31_angles = { 0.5, 1.5, 2.5, 3.5, 4.5 };

    /** Angles for VCP 32 */
    private static final double[] vcp_32_angles = { 0.5, 1.5, 2.5, 3.5, 4.5 };

    /** Angles for VCP 121 */
    private static final double[] vcp_121_angles = {
        0.5, 1.5, 2.4, 3.4, 4.3, 6.0, 9.9, 14.6, 19.5
        //0.5, 0.5, 0.5, 0.5, 1.45, 1.45, 1.45, 1.45, 2.4, 2.4, 2.4, 3.35, 3.35, 3.35, 4.3, 4.3, 6.0, 9.9, 14.6, 19.5
    };

    /** Angles for VCP 300 */
    private static final double[] vcp_300_angles = { 0.5, 2.4, 9.9 };

    /**
     * Constructor.
     */
    public VCP() {}

    /**
     * Get the number of cuts for a particular VCP value
     *
     * @param vcp value for VCP
     * @return number of cuts
     */
    public static int getNumCuts(int vcp) {

        switch (vcp) {

          case 11 :

              return vcp_11_angles.length;

          case 12 :

              return vcp_12_angles.length;

          case 21 :

              return vcp_21_angles.length;

          case 31 :

              return vcp_31_angles.length;

          case 32 :

              return vcp_32_angles.length;

          case 121 :

              return vcp_121_angles.length;

          case 300 :

              return vcp_300_angles.length;

          default :

              return 0;
        }

    }

    /**
     * Get the elevation angle for a given VCP and cut.
     *
     * @param vcp VCP value
     * @param cut cut index
     * @return  elevation angle
     */
    public static float getCutAngle(int vcp, int cut) {

        switch (vcp) {

          case 11 :

              return ((float) vcp_11_angles[cut]);

          case 12 :

              return ((float) vcp_12_angles[cut]);

          case 21 :

              return ((float) vcp_21_angles[cut]);

          case 31 :

              return ((float) vcp_31_angles[cut]);

          case 32 :

              return ((float) vcp_32_angles[cut]);

          case 121 :

              return ((float) vcp_121_angles[cut]);

          case 300 :

              return ((float) vcp_300_angles[cut]);

          default :

              return ((float) -99.9);

        }

    }

    /**
     * Get the elevation angles for a given VCP.
     *
     * @param vcp  VCP value
     * @return array of elevation angle values
     */
    public static double[] getAngles(int vcp) {

        switch (vcp) {

          case 11 :

              return (double[]) vcp_11_angles.clone();

          case 12 :

              return (double[]) vcp_12_angles.clone();

          case 21 :

              return (double[]) vcp_21_angles.clone();

          case 31 :

              return (double[]) vcp_31_angles.clone();

          case 32 :

              return (double[]) vcp_32_angles.clone();

          case 121 :

              return (double[]) vcp_121_angles.clone();

          case 300 :

              return (double[]) vcp_300_angles.clone();

          default :

              return (double[]) null;

        }
    }
}

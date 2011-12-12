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

package ucar.visad.quantities;



import visad.*;


/**
 * Provides support for the latent heat of evaporation.
 *
 * @author Steven R. Emmerson
 * @version $Id: LatentHeatOfEvaporation.java,v 1.9 2005/05/13 18:35:40 jeffmc Exp $
 */
public class LatentHeatOfEvaporation extends ScalarQuantity {

    /** RealType for this quantity */
    private static RealType realType;

    /** a Real value */
    private static Real real;

    /**
     * Constructs from nothing.
     *
     * @throws UnitException
     * @throws VisADException
     */
    private LatentHeatOfEvaporation() throws UnitException, VisADException {
        super(getRealType());
    }

    /**
     * Obtains the RealType associated with this class.
     *
     * @return                  The RealType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealType getRealType() throws VisADException {

        if (realType == null) {
            synchronized (LatentHeatOfEvaporation.class) {
                if (realType == null) {
                    realType = RealType.getRealType(
                        "LatentHeatOfEvaporation",
                        SI.meter.divide(SI.second).pow(2), (Set) null);
                }
            }
        }

        return realType;
    }

    /**
     * Creates a Real corresponding to the latent heat of evaporation.
     *
     * @return                  A Real corresponding to the latent heat of
     *                          evaporation.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static Real newReal() throws VisADException {

        if (real == null) {
            synchronized (LatentHeatOfEvaporation.class) {
                if (real == null) {
                    try {

                        /*
                         * Value (converted to SI) from "Introduction to
                         * Theoretical "Meteorology" by Seymour L. Hess,
                         * 1985; Robert E. Krieger Publishing Company; ISBN
                         * 0-88275-857-8.
                         */
                        real = new Real(getRealType(), 2500775.64);
                    }
                    /*
                     * Can't happen because the above unit expression is valid.
                     */
                    catch (UnitException e) {}
                }
            }
        }

        return real;
    }
}

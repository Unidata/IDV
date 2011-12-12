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
 * Provides support for the dry air gas constant.
 *
 * @author Steven R. Emmerson
 * @version $Id: SpecificHeatCapacityOfDryAirAtConstantVolume.java,v 1.9 2005/05/13 18:35:45 jeffmc Exp $
 */
public class SpecificHeatCapacityOfDryAirAtConstantVolume extends SpecificHeatCapacityOfDryAir {

    /**
     * The single instance of the RealType.
     */
    private static RealType realType;

    /**
     * The single instance of the Real.
     */
    private static Real real;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SpecificHeatCapacityOfDryAirAtConstantVolume()
            throws VisADException {
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
            synchronized (
                SpecificHeatCapacityOfDryAirAtConstantVolume.class) {
                if (realType == null) {
                    realType = RealType.getRealType(
                        "SpecificHeatCapacityOfDryAirAtConstantVolume",
                        getDefaultSIUnit(), getDefaultSet());
                }
            }
        }

        return realType;
    }

    /**
     * Creates a Real from nothing.
     * @return                  The dry air gas constant.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static Real newReal() throws VisADException {

        if (real == null) {
            synchronized (
                SpecificHeatCapacityOfDryAirAtConstantVolume.class) {
                if (real == null) {

                    /*
                     * Value (converted to SI) from Appendix 1 of "Introduction
                     * to Theoretical "Meteorology" by Seymour L. Hess, 1985;
                     * Robert E. Krieger Publishing Company; ISBN 0-88275-857-8
                     */
                    real = new Real(getRealType(), 715.9428);
                }
            }
        }

        return real;
    }
}

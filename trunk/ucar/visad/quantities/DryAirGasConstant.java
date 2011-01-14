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


import visad.Real;



import visad.RealTupleType;

import visad.RealType;

import visad.SI;

import visad.VisADException;


/**
 * Provides support for the dry air gas constant.
 *
 * @author Steven R. Emmerson
 * @version $Id: DryAirGasConstant.java,v 1.9 2005/05/13 18:35:38 jeffmc Exp $
 */
public class DryAirGasConstant extends GasConstant {

    /**
     * The single instance.
     */
    private static DryAirGasConstant INSTANCE;

    /**
     * The single instance of the Real.
     */
    private static Real dryAirGasConstant;

    static {
        try {
            INSTANCE = new DryAirGasConstant();
            dryAirGasConstant = new Real(
                (RealType) INSTANCE.getRealTupleType().getComponent(0),
                287.04, SI.meter.divide(SI.second).pow(2).divide(SI.kelvin));
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private DryAirGasConstant() throws VisADException {
        this("DryAirGasConstant");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected DryAirGasConstant(String name) throws VisADException {
        super(name);
    }

    /**
     * Obtains the RealType associated with this class.
     *
     * @return                  The RealType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealType getRealType() throws VisADException {
        return (RealType) dryAirGasConstant.getType();
    }

    /**
     * Obtains the RealTupleType associated with this class.
     *
     * @return                  The RealTupleType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealTupleType getRealTupleType() throws VisADException {

        return INSTANCE.realTupleType();
    }

    /**
     * Creates a Real from nothing.
     * @return                  The dry air gas constant.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static Real newReal() throws VisADException {

        return dryAirGasConstant;
    }
}

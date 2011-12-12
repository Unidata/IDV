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



import visad.RealTupleType;

import visad.RealType;

import visad.VisADException;


/**
 * Provides support for the quantity of atmospheric temperature.
 *
 * @author Steven R. Emmerson
 * @version $Id: InSituAirTemperature.java,v 1.8 2005/05/13 18:35:40 jeffmc Exp $
 */
public class InSituAirTemperature extends AirTemperature {

    /**
     * The single instance.
     */
    private static InSituAirTemperature INSTANCE;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private InSituAirTemperature() throws VisADException {
        this("InSituAirTemperature");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private InSituAirTemperature(String name) throws VisADException {
        super(name);
    }

    /**
     * Obtains the RealType associated with this class.
     *
     * @return                  The RealType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealType getRealType() throws VisADException {
        return (RealType) getRealTupleType().getComponent(0);
    }

    /**
     * Obtains the RealTupleType associated with this class.
     *
     * @return                  The RealTupleType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealTupleType getRealTupleType() throws VisADException {

        if (INSTANCE == null) {
            synchronized (InSituAirTemperature.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InSituAirTemperature();
                }
            }
        }

        return INSTANCE.realTupleType();
    }
}

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
 * Provides support for the quantity of dew point temperature.
 *
 * @author Steven R. Emmerson
 * @version $Id: DewPoint.java,v 1.8 2005/05/13 18:35:38 jeffmc Exp $
 */
public class DewPoint extends AirTemperature {

    /**
     * The single instance.
     */
    private static DewPoint INSTANCE;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private DewPoint() throws VisADException {
        super("DewPoint");
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
            synchronized (DewPoint.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DewPoint();
                }
            }
        }

        return INSTANCE.realTupleType();
    }
}

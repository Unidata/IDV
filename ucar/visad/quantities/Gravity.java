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

import visad.UnitException;

import visad.VisADException;


/**
 * Provides support for the quantity of gravity.
 *
 * @author Steven R. Emmerson
 * @version $Id: Gravity.java,v 1.9 2005/05/13 18:35:40 jeffmc Exp $
 */
public class Gravity extends Acceleration {

    /**
     * The single instance.
     */
    private static Gravity INSTANCE;

    /**
     * The default gravity value.
     */
    private static Real gravity = null;

    /**
     * Constructs from nothing.
     *
     * @throws VisADException
     */
    private Gravity() throws VisADException {
        this("Gravity");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for this quantity.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected Gravity(String name) throws VisADException {
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
            synchronized (Gravity.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Gravity();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates a Real corresponding to the standard acceleration of gravity.
     *
     * @return                  A Real corresponding to standard gravity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static Real newReal() throws VisADException {

        if (gravity == null) {
            synchronized (Gravity.class) {
                if (gravity == null) {
                    try {

                        /*
                         * See <http://physics.nist.gov/cgi-bin/cuu> for the
                         * numerical value used below.
                         */
                        gravity = new Real(getRealType(), 9.80665,
                                           SI.meter.divide(SI.second.pow(2)));
                    }
                    /*
                     * Can't happen because the above unit expression is valid.
                     */
                    catch (UnitException e) {}
                }
            }
        }

        return gravity;
    }
}

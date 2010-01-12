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

package ucar.visad;


import visad.*;



import java.lang.reflect.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for tuple quantities.  This class uses the
 * <em>decorator</em> design pattern to add convenience methods to its
 * superclass and to form the root of a hierarchy of classes that serve to
 * encapsulate knowledge about scientific quantities that either have multiple
 * components or a reference coordinate system.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.16 $ $Date: 2005/05/13 18:34:05 $
 */
public abstract class TupleQuantity extends Quantity {

    /*
     * Fields:
     */

    /** scalar quantities that make up the tuple */
    private final ScalarQuantity[] scalars;

    /*
     * Constructors:
     */

    /**
     * Constructs from a name and scalar quantities.    The
     * default coordinate system transformation will be <code>null</code>.
     *
     * @param name              The name for the tuple quantity.
     * @param scalars           The scalar quantities.
     * @throws VisADException   VisAD failure.
     * @see #TupleQuantity(String, ScalarQuantity[], CoordinateSystem)
     */
    protected TupleQuantity(String name, ScalarQuantity[] scalars)
            throws VisADException {
        this(name, scalars, (CoordinateSystem) null);
    }

    /**
     * Constructs from a name, scalar quantities, and a default coordinate
     * system transformation.  The default domain set will be <code>null</code>.
     *
     * @param name              The name for the tuple quantity.
     * @param scalars           The scalar quantitites.
     * @param coordSys          The default coordinate system transformation.
     *                          May be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @see #TupleQuantity(String, ScalarQuantity[], CoordinateSystem,
     *                     visad.Set)
     */
    protected TupleQuantity(String name, ScalarQuantity[] scalars,
                            CoordinateSystem coordSys)
            throws VisADException {
        this(name, scalars, coordSys, (visad.Set) null);
    }

    /**
     * Constructs from a name, scalar quantities, a default coordinate system
     * transformation, and a default domain set.  This is the most general
     * constructor.
     *
     * @param name              The name for the tuple quantity.
     * @param scalars           The scalar quantitites.
     * @param coordSys          The default coordinate system transformation.
     *                          May be <code>null</code>.
     * @param domain            The default domain set.  May be
     *                          <code>null</code>.
     * @throws TypeException    The quantity already exists.
     * @throws VisADException   VisAD failure.
     */
    protected TupleQuantity(String name, ScalarQuantity[] scalars,
                            CoordinateSystem coordSys, visad.Set domain)
            throws TypeException, VisADException {

        super(name,
              new RealTupleType(getRealTypes(scalars), coordSys, domain));

        this.scalars = scalars;
    }

    /*
     * Class methods:
     */

    /**
     * Returns an array of VisAD {@link RealType}s corresponding to an array of
     * scalar quantitities.
     *
     * @param scalars           An array of the scalar quantities.
     * @return                  A corresponding aray of VisAD {@link
     *                          RealType}s
     */
    protected static RealType[] getRealTypes(ScalarQuantity[] scalars) {

        RealType[] realTypes = new RealType[scalars.length];

        for (int i = 0; i < scalars.length; ++i) {
            realTypes[i] = scalars[i].getRealType();
        }

        return realTypes;
    }

    /*
     * Public instance methods:
     */
}

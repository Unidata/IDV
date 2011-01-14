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


import ucar.visad.*;

import visad.*;



import java.util.Hashtable;


/**
 * Provides support for scientific constants.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:34:02 $
 */
public class Constants {

    /*
     * Fields:
     */

    /**
     * The single instance of this class.
     */
    private static Constants instance;

    static {
        try {
            instance = new Constants(null);
        } catch (Exception e) {
            System.err.println(
                "Constants.<clinit>: Couldn't initialize class: " + e);
            System.exit(1);
        }
    }

    /** table for constants */
    private Hashtable table = new Hashtable();

    /** parent for this */
    private Constants parent;

    /*
     * Constructors:
     */

    /**
     * Constructs from a parent constant table or <code>null</code>.  Protected
     * to prevent use.  The parent table will be searched if the constant is not
     * found in this instances's table.
     *
     * @param parent          The parent constant table or <code>null</code>.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected Constants(Constants parent) throws VisADException {
        this.parent = parent;
    }

    /*
     * Class methods:
     */

    /**
     * Returns an instance of this class.
     *
     * @return                  An instance of this class.
     */
    public static Constants instance() {
        return instance;
    }

    /*
     * Instance methods:
     */

    /**
     * Stores a constant into the table.
     *
     * @param name              The name of the constant.
     * @param value             The value of the constant.
     * @return                  The value previously stored under the name.
     *                          May be <code>null</code>.
     */
    public Real put(String name, Real value) {
        return (Real) table.put(name, value);
    }

    /**
     * Returns the constant with a given name.
     *
     * @param name              The name of the constant.
     * @return                  The constant with the given name.  May be
     *                          <code>null</code> if not found.
     */
    public Real get(String name) {

        Real value = (Real) table.get(name);

        if ((value == null) && (parent != null)) {
            value = parent.get(name);
        }

        return value;
    }
}

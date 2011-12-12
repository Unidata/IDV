/*
 * $Id: ObjectClass.java,v 1.7 2005/05/13 18:33:53 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.xml;



/**
 * Holds an object (possibly null) and the Class of the object.
 *
 * @author IDV development team
 * @version $Revision: 1.7 $Date: 2005/05/13 18:33:53 $
 */

public class ObjectClass {

    /** The object */
    Object object;

    /** The class */
    Class type;

    /**
     *  Create an ObjectClass with the given object. Get the class from the object itself (if non-null).
     *  If null the use Object.class.
     *
     *  @param object The object.
     */
    public ObjectClass(Object object) {
        this.object = object;
        if (object == null) {
            type = Object.class;
        } else {
            type = object.getClass();
        }
    }

    /**
     *  Create an ObjectClass with the given object and class.
     *
     *
     *  @param object The object.
     *  @param theClass The object's class.
     */

    public ObjectClass(Object object, Class theClass) {
        this.object = object;
        this.type   = theClass;
    }

    /**
     *  Override toString.
     *  @return The String representation of this object.
     */
    public String toString() {
        return "ObjectClass  class=" + ((type != null)
                                        ? type.getName()
                                        : " null type ") + " object="
                                        + object;
    }


}






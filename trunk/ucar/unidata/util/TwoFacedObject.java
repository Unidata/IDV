/**
 * $Id: TwoFacedObject.java,v 1.20 2006/06/23 20:17:32 dmurray Exp $
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


package ucar.unidata.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;


/**
 * A generic Object wrapper that holds two objects.  The first
 * is used as the label and for comparisons in lists.
 *
 * @author Metapps development team
 * @version $Revision: 1.20 $ $Date: 2006/06/23 20:17:32 $
 */
public class TwoFacedObject implements Comparable {

    /** debug flag */
    public static boolean debug = false;

    /** The label object */
    Object label;

    /** The id object */
    Object id;

    /**
     * Default constructor with null id and label
     */
    public TwoFacedObject() {}


    /**
     * Create a TwoFacedObject where both id and label are identical.
     *
     * @param label  label and object
     */
    public TwoFacedObject(Object label) {
        this(label, label);
    }


    /**
     * Create a TwoFacedObject with label and integer id
     *
     * @param label  label
     * @param id id
     */
    public TwoFacedObject(Object label, int id) {
        this(label, new Integer(id));
    }

    /**
     * Create a TwoFacedObject.
     *
     * @param label  object to use for labeling.
     * @param id  other face of this object
     */
    public TwoFacedObject(Object label, Object id) {
        this.label = label;
        this.id    = id;
    }

    /**
     * String representation of this object.
     * @return toString() method of label.
     */
    public String toString() {
        return label.toString();
    }

    /**
     * See if this TwoFacedObject is equal to another object.
     *
     * @param other
     * @return true if both are TwoFacedObjects and their id-s are equal
     */
    public boolean equals(Object other) {
        if ( !(other instanceof TwoFacedObject)) {
            return false;
        }
        TwoFacedObject that = (TwoFacedObject) other;
        return Misc.equals(id, that.id);
    }


    /**
     * Get the hashcode for this TwoFacedObject
     *
     * @return the hashcode
     */
    public int hashCode() {
        if (id == null) {
            return 0;
        }
        return id.hashCode();
    }


    /**
     * Get the Id object.
     * @return the id for this TwoFacedObject.
     */
    public Object getId() {
        return id;
    }

    /**
     * Sets the value of  the Id object.
     *
     * @param newId
     */
    public void setId(Object newId) {
        id = newId;
    }

    /**
     * Get the Label object.
     * @return the label for this TwoFacedObject.
     */
    public Object getLabel() {
        return label;
    }

    /**
     * Sets the value of  the Label object.
     *
     * @param newLabel
     */
    public void setLabel(Object newLabel) {
        label = newLabel;
    }

    /**
     * Get a String representation for the id of this TwoFacedObject.
     * @param  o object in question.
     * @return the toString() of the id if this is a TwoFacedObject,
     *         otherwise the toString() method of o.
     */
    public static String getIdString(Object o) {
        Object id = getIdObject(o);
        if (id == null) {
            return null;
        }
        return id.toString();
    }

    /**
     * Get the id object of this possible TwoFacedObject.
     * @param  o object in question.
     * @return the id if this is a TwoFacedObject, otherwise o.
     */
    public static Object getIdObject(Object o) {
        if (o instanceof TwoFacedObject) {
            return ((TwoFacedObject) o).getId();
        }
        return o;
    }


    /**
     * Create a list of tfos from the given int ids and names
     *
     * @param ids ids
     * @param names names
     *
     * @return list of tfos
     */
    public static List createList(int[] ids, String[] names) {
        List l = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            l.add(new TwoFacedObject(names[i], ids[i]));
        }
        return l;
    }



    /**
     * Create a list of tfos from the given int ids and names
     *
     * @param ids ids
     * @param names names
     *
     * @return list of tfos
     */
    public static List createList(String[] ids, String[] names) {
        List l = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            l.add(new TwoFacedObject(names[i], ids[i]));
        }
        return l;
    }

    /**
     * Create a list of ids from the given list of objects.
     * For any TwoFaceObject in objects, the id is added to
     * the resulting list, otherwise the object is added
     *
     * @param objects  list of objects
     *
     * @return list of tfos
     */
    public static List getIdList(List objects) {
        List l = new ArrayList();
        for (int i = 0; i < objects.size(); i++) {
            Object o = objects.get(i);
            if (o instanceof TwoFacedObject) {
                l.add(((TwoFacedObject) o).getId());
            } else {
                l.add(o);
            }
        }
        return l;
    }

    /**
     * Finf the tfo with the given id in the list
     *
     * @param id id to look for
     * @param l list of tfos
     *
     * @return the tfo or null if none found
     */
    public static TwoFacedObject findId(Object id, List l) {
        for (int i = 0; i < l.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject) l.get(i);
            if (Misc.equals(id, tfo.getId())) {
                return tfo;
            }
        }
        return null;
    }


    /**
     * Find the label for the object in the list
     *
     * @param id  object to search for
     * @param l   list of TwoFacedObjects objects
     *
     * @return the label
     */
    public static String findLabel(Object id, List l) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < l.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject) l.get(i);
            if (Misc.equals(id, tfo.getId())) {
                return tfo.toString();
            }
        }
        return null;
    }


    public static boolean  contains(List objects, Object value) {
        for (Object o:objects) {
            if(o instanceof TwoFacedObject) {
                o = ((TwoFacedObject)o).getId();
            }
            if(Misc.equals(o, value)) return true;
        }
        return false;
    }



    /**
     * Get a String representation for the objects in te lsit
     * @param  objects objects in question.
     * @return List of fbhe toString() of the id if these are TwoFacedObjects,
     *         otherwise the toString() method of o.
     */
    public static List getIdStrings(List objects) {
        List result = new ArrayList();
        for (int i = 0; i < objects.size(); i++) {
            result.add(getIdString(objects.get(i)));
        }
        return result;
    }

    /**
     * Compare this object to another.
     * @param o object in question.
     * @return spec from Comparable interface.
     */
    public int compareTo(Object o) {
        if ((id != null) && (id instanceof Comparable)
                && (o instanceof TwoFacedObject)) {
            return ((Comparable) id).compareTo(((TwoFacedObject) o).id);
        }
        return toString().compareTo(o.toString());
    }




    public static void sort(List list) {
	Comparator comp = new Comparator() {
		public int compare(Object o1,Object o2 ) {
		    return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());

		}
	    };
	Collections.sort(list, comp);
	
    }


}


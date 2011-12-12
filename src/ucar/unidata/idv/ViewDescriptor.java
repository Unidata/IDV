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

package ucar.unidata.idv;


import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.List;


/**
 * This class has never been fully fleshed out (in part because it
 * has done what we need it to do. It is meant to be a means of identifying
 * different types and instances of {@link ViewManager}s
 *
 * @author IDV Development Team
 */

public class ViewDescriptor {

    /** Type identifier for 3d view managers */
    public static String TYPE_MAIN3D = "ucar.unidata.idv.MapViewManager";

    /** Type identifier for 2d view managers */
    public static String TYPE_MAIN2D = "ucar.unidata.idv.MapViewManager";




    /**
     *   Represents the active view manager
     *   We keep the old name, MAIN3D, around for legacy reasons?
     */
    public static final ViewDescriptor LASTACTIVE =
        new ViewDescriptor("MAIN3D");


    /**
     *   Instance for 3d view managers
     *   @deprecated Use LASTACTIVE
     */
    public static final ViewDescriptor NAME_MAIN3D = LASTACTIVE;



    /**
     *   Instance for 3d view managers
     *   @deprecated Use LASTACTIVE
     */
    public static final ViewDescriptor NAME_LASTACTIVE_3D = LASTACTIVE;

    /** The name */
    private String name;


    /** List of ViewManager class names we are for */
    private List classNames = new ArrayList();


    /**
     * Create this descriptor. Make a unique name.
     */
    public ViewDescriptor() {
        this.name = "view_" + Misc.getUniqueId();
    }

    /**
     * Create this descriptor with the given name.
     *
     * @param name THe name
     *
     */
    public ViewDescriptor(String name) {
        this.name = name;
    }

    /**
     * Copy ctor
     *
     * @param that Object to copy from
     */
    public ViewDescriptor(ViewDescriptor that) {
        this.name       = that.name;
        this.classNames = that.classNames;
    }



    /**
     * The to string method
     *
     * @return The name
     */
    public String toString() {
        return "(" + name + " " + classNames + ")";
    }

    /**
     * Does this view descriptor equal the given obj
     *
     * @param obj The object
     * @return Equals the obj
     */
    public boolean equals(Object obj) {
        if (obj instanceof ViewDescriptor) {
            ViewDescriptor that = (ViewDescriptor) obj;
            return Misc.equals(name, that.name)
                   && Misc.equals(classNames, that.classNames);
        }
        return false;
    }

    /**
     * Does this view descriptor equal the given obj
     *
     * @param that The object
     * @return Equals the obj
     */
    public boolean nameEquals(ViewDescriptor that) {
        return Misc.equals(name, that.name);
    }

    /**
     * Override hashcode. Use the name's hashcode
     *
     * @return The hashcode
     */
    public int hashCode() {
        //        return name.hashCode() ^ Misc.hashcode(classNames);
        return name.hashCode();
    }


    /**
     * Get the name
     *
     * @return The name of this ViewDescriptor
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name
     *
     * @param n The new name
     */
    public void setName(String n) {
        name = n;
    }

    /**
     * Set the Classes property.
     *
     * @param value The new value for Classes
     */
    public void setClassNames(List value) {
        classNames = value;
    }

    /**
     * Get the Classes property.
     *
     * @return The Classes
     */
    public List getClassNames() {
        return classNames;
    }


}

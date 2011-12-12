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

import java.util.List;



/**
 * This class is used to keep track of the files and the data sources
 * that a user has loaded. It is abstract, with derived classes
 * {@link FileHistory} and {@link DataSourceHistory}
 * These history classes are used to show a history list in the File
 * menu so, once the user has loaded some file, they can easily reload the file.
 *
 * @author IDV development team
 */


public abstract class History {

    /**
     *  The name property.
     */
    private String name;

    /**
     *  The alias property.
     */
    private String alias = "";


    /**
     * Parameterless ctor for xml encoding/decoding
     *
     */
    public History() {}


    /**
     * Create this History object
     *
     * @param name The name
     *
     */
    public History(String name) {
        this.name = name;
    }

    /**
     *  Does this object have an alias.
     *
     *  @return Has a non-null, non-zero length alias.
     */
    public boolean hasAlias() {
        return (alias != null) && (alias.length() > 0);
    }


    /**
     *  Create the data source (or load the file) represented by this object.
     * This is actually implemented  in the derived classes.
     *
     *  @param idv The idv
     *  @return true if this was successful.
     */
    public abstract boolean process(IntegratedDataViewer idv);


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }


    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the Alias property.
     *
     *  @param value The new value for Alias
     */
    public void setAlias(String value) {
        alias = value;
    }

    /**
     *  Get the Alias property.
     *
     *  @return The Alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Override the toString method
     *
     * @return String representation of this object
     */
    public String toString() {
        if ((alias != null) && (alias.length() > 0)) {
            return alias + " " + name;
        }
        return name;
    }

    /**
     * Override the hashcode  method. Use the name member
     *
     * @return The hashcode
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Override the equals method
     *
     * @param o The other object
     * @return Is equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof History)) {
            return false;
        }
        return Misc.equals(name, ((History) o).name);
    }


    /**
     *  Search the given list for a PC object that has the given alias.
     *
     *  @param alias The alias to serach for.
     *  @param historyList The list of History objects
     *  @param theClass If non-null then only find History objects of the given theClass
     *  @return The PC object in the list with the given alias or null if none found.
     */
    public static History findWithAlias(String alias, List historyList,
                                        Class theClass) {
        if (alias.length() == 0) {
            return null;
        }
        for (int i = 0; i < historyList.size(); i++) {
            History history = (History) historyList.get(i);
            if (Misc.equals(history.getAlias(), alias)) {
                if (theClass != null) {
                    if ( !theClass.equals(history.getClass())) {
                        continue;
                    }
                }
                return history;
            }
        }
        return null;
    }




}

/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.util.Date;
import java.util.List;


/**
 * Class FilesInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class User {

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private boolean admin = false;

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param admin _more_
     */
    public User(String id, String name, boolean admin) {
        this.id    = id;
        this.name  = name;
        this.admin = admin;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return Misc.hashcode(id) ^ Misc.hashcode(name) ^ (admin
                ? 1
                : 2);
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        User that = (User) o;
        return Misc.equals(this.id, that.id);
    }



    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Admin property.
     *
     * @param value The new value for Admin
     */
    public void setAdmin(boolean value) {
        admin = value;
    }

    /**
     * Get the Admin property.
     *
     * @return The Admin
     */
    public boolean getAdmin() {
        return admin;
    }



}


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

import java.util.ArrayList;

import java.util.Date;
import java.util.List;




/**
 * Class RadarInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Group extends Entry {


    /** _more_ */
    public static final String IDDELIMITER = "/";


    /** _more_ */
    private List<Group> children = new ArrayList<Group>();


    /** _more_ */
    private boolean isDummy = false;

    /**
     * _more_
     *
     * @param handler _more_
     * @param isDummy _more_
     */
    public Group(TypeHandler handler, boolean isDummy) {
        super("", handler);
        this.isDummy = isDummy;
        setName("Search Results");
        setDescription("");
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param typeHandler _more_
     */
    public Group(String id, TypeHandler typeHandler) {
        super(id, typeHandler);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Entry clone() {
        Entry newEntry = new Group(getId(), getTypeHandler());
        initClone(newEntry);
        return newEntry;
    }

    /**
     * _more_
     *
     * @param newEntry _more_
     */
    protected void initClone(Entry newEntry) {
        Group newGroup = (Group) newEntry;
        newGroup.isDummy  = isDummy;
        newGroup.children = children;
        super.initClone(newEntry);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullName() {
        if (getParentGroup() != null) {
            return getParentGroup().getFullName() + "/" + getName();
        }
        return getName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDummy() {
        return isDummy;
    }


}


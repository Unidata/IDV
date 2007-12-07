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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class Entry _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GenericEntry extends Entry {

    Hashtable attributes  = new Hashtable();

    /**
     * _more_
     *
     *
     * @param name _more_
     * @param description _more_
     * @param type _more_
     * @param group _more_
     * @param file _more_
     * @param date _more_
     */
    public GenericEntry(String id, String type, String name, String description, 
                     Group group, User user, String file, long date) {
        super(id,type, name, description, group, user,file, date, date,date);
    }



    /**
     * _more_
     *
     *
     *
     * @param name _more_
     * @param description _more_
     * @param type _more_
     * @param group _more_
     * @param file _more_
     * @param station _more_
     * @param product _more_
     * @param date _more_
     * @param startDate _more_
     * @param endDate _more_
     */
    public GenericEntry(String id, String type,
                        String name, String description, 
                     Group group, User user, String file, long createDate, long startDate, long endDate) {
        super(id, type, name, description, group, user,file, createDate, startDate,endDate);
    }

    public Object get(String name) {
        return attributes.get(name);
    }

    public void put(String name, Object value) {
        attributes.put(name,value);
    }



}


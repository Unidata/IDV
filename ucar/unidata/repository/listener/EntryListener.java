/**
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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository.listener;

import ucar.unidata.repository.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class EntryListener implements Constants {


    /** _more_ */
    private Repository repository;

    /** _more_ */
    private String id;


    private List<Filter> filters = new ArrayList<Filter>();



    public EntryListener() {
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     */
    public EntryListener(Repository repository, String id) {
        this.id  =id;
        this.repository = repository;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    protected Repository getRepository() {
        return repository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    protected void handleError(String message, Exception exc) {
        //TODO: What to do with errors
        throw new RuntimeException(exc);
    }

    public void addFilter(Filter filter) {
        synchronized(filters) {
            filters.add(filter);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean checkEntry(Entry entry) {
        for (Filter filter: filters) {
            boolean ok    = filter.checkEntry(entry);
            if(!ok) return false;
        }
        entryMatched(entry);
        return true;
    }

    protected void entryMatched(Entry entry) {
    }

/**
Set the Filters property.

@param value The new value for Filters
**/
public void setFilters (List<Filter> value) {
	filters = value;
}

/**
Get the Filters property.

@return The Filters
**/
public List<Filter> getFilters () {
	return filters;
}




}


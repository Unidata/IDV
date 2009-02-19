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
import java.util.Date;
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


    /** _more_          */
    private String userId;

    /** _more_          */
    private User user;

    /** _more_          */
    private boolean active = true;

    /** _more_ */
    private List<Filter> filters = new ArrayList<Filter>();



    /** _more_          */
    private Date fromDate;

    /** _more_          */
    private Date toDate;


    /**
     * _more_
     */
    public EntryListener() {}


    /**
     * _more_
     *
     * @param repository _more_
     * @param user _more_
     */
    public EntryListener(Repository repository, User user) {
        this.repository = repository;
        this.user       = user;
        if (user != null) {
            this.userId = user.getId();
        }
        this.id = repository.getGUID();
    }


    /**
     * _more_
     *
     * @param repository _more_
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    protected void handleError(String message, Exception exc) {
        //TODO: What to do with errors
        throw new RuntimeException(exc);
    }

    /**
     * _more_
     *
     * @param filter _more_
     */
    public void addFilter(Filter filter) {
        synchronized (filters) {
            filters.add(filter);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean checkEntry(Entry entry) throws Exception {
        System.err.println("checking entry");

        if ( !getActive()) {
            System.err.println("not activey");
            return false;
        }
        if (fromDate != null) {
            Date now = new Date();
            if (now.getTime() < fromDate.getTime()) {
                return false;
            }
        }

        if (toDate != null) {
            Date now = new Date();
            if (now.getTime() > toDate.getTime()) {
                return false;
            }
        }



        Request request = new Request(repository, getUser());
        if ( !repository.getAccessManager().canDoAction(request, entry,
                Permission.ACTION_VIEW)) {
            System.err.println("can't view");
            return false;
        }


        for (Filter filter : filters) {
            boolean ok = filter.checkEntry(entry);
            if ( !ok) {
                System.err.println("filter not OK");
                return false;
            }
        }
        System.err.println("Matched");
        entryMatched(entry);
        return true;
    }

    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void entryMatched(Entry entry) {}

    /**
     * Set the Filters property.
     *
     * @param value The new value for Filters
     */
    public void setFilters(List<Filter> value) {
        filters = value;
    }

    /**
     * Get the Filters property.
     *
     * @return The Filters
     */
    public List<Filter> getFilters() {
        return filters;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public User getUser() throws Exception {
        if (user == null) {
            if (repository != null) {
                user = repository.getUserManager().findUser(userId, true);
            }
        }
        return user;
    }


    /**
     *  Set the UserId property.
     *
     *  @param value The new value for UserId
     */
    public void setUserId(String value) {
        userId = value;
    }

    /**
     *  Get the UserId property.
     *
     *  @return The UserId
     */
    public String getUserId() {
        return userId;
    }


    /**
     *  Set the Active property.
     *
     *  @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     *  Get the Active property.
     *
     *  @return The Active
     */
    public boolean getActive() {
        return active;
    }

    /**
     *  Set the FromDate property.
     *
     *  @param value The new value for FromDate
     */
    public void setFromDate(Date value) {
        fromDate = value;
    }

    /**
     *  Get the FromDate property.
     *
     *  @return The FromDate
     */
    public Date getFromDate() {
        return fromDate;
    }

    /**
     *  Set the ToDate property.
     *
     *  @param value The new value for ToDate
     */
    public void setToDate(Date value) {
        toDate = value;
    }

    /**
     *  Get the ToDate property.
     *
     *  @return The ToDate
     */
    public Date getToDate() {
        return toDate;
    }


}


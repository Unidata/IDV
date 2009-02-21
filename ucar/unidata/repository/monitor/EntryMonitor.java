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



package ucar.unidata.repository.monitor;


import ucar.unidata.repository.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
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
public class EntryMonitor implements Constants {


    /** _more_ */
    private String id;

    private String name = "";

    /** _more_ */
    private Repository repository;


    /** _more_          */
    private String userId;

    /** _more_          */
    private User user;

    /** _more_          */
    private boolean enabled = true;

    /** _more_ */
    private List<Filter> filters = new ArrayList<Filter>();

    private List<MonitorAction> actions = new ArrayList<MonitorAction>();


    /** _more_          */
    private Date fromDate;

    /** _more_          */
    private Date toDate;

    private boolean editable = true;

    public static final String ARG_ADD_ACTION = "addaction";
    public static final String ARG_DELETE_ACTION = "deleteaction";
    public static final String ARG_DELETE_ACTION_CONFIRM = "deleteactionconfirm";



    /**
     * _more_
     */
    public EntryMonitor() {}


    /**
     * _more_
     *
     * @param repository _more_
     * @param user _more_
     */
    public EntryMonitor(Repository repository, User user,String name,boolean editable) {
        this.repository = repository;
        this.name = name;
        this.editable = editable;
        this.user       = user;
        if (user != null) {
            this.userId = user.getId();
        }
        this.id = repository.getGUID();
        fromDate = new Date();
        toDate = new Date(fromDate.getTime()+(long)DateUtil.daysToMillis(7));
    }



    public static EntryMonitor findMonitor(List<EntryMonitor> monitors, String id) {
        for(EntryMonitor monitor: monitors) {
            if(Misc.equals(monitor.getId(),id)) {
                return monitor;
            }
        }
        return null;
    }

    public static List<EntryMonitor> getEditable(List<EntryMonitor> monitors) {
        Class[]actionClasses = {EmailAction.class,
                                TwitterAction.class};
        String[]actionNames = {"Send an email",
                               "Ping Twitter"};



        List<EntryMonitor> result = new ArrayList<EntryMonitor>();
        for(EntryMonitor monitor: monitors) {
            if(monitor.getEditable()) {
                result.add(monitor);
            }
        }
        return result;
    }


    public void applyEditForm(Request request) throws Exception {
        setName(request.getString(ARG_MONITOR_NAME,getName()));
        setEnabled(request.get(ARG_MONITOR_ENABLED,false));
        Date[] dateRange = request.getDateRange(ARG_MONITOR_FROMDATE, ARG_MONITOR_TODATE,
                               new Date());
        fromDate = dateRange[0];
        toDate = dateRange[1];

        for(MonitorAction action: actions) {
            action.applyEditForm(request, this);
        }
    }

    public void addToEditForm(Request request,StringBuffer sb) throws Exception {
        StringBuffer stateSB = new StringBuffer();

        stateSB.append(HtmlUtil.formTable());
        stateSB.append(HtmlUtil.formEntry(getRepository().msgLabel("Name"),
                                     HtmlUtil.input(ARG_MONITOR_NAME,getName(),HtmlUtil.SIZE_70)));
        stateSB.append(HtmlUtil.formEntry(getRepository().msgLabel("Enabled"),
                                     HtmlUtil.checkbox(ARG_MONITOR_ENABLED,"true",getEnabled())));

        stateSB.append(
                  HtmlUtil.formEntry(
                                     getRepository().msgLabel("Valid Date Range"),
                                     
                                     getRepository().makeDateInput(
                                                                   request, ARG_MONITOR_FROMDATE, "monitorform", getFromDate()) +
                                     " " + getRepository().msg("To") +" " +
                                     getRepository().makeDateInput(
                                                                   request, ARG_MONITOR_TODATE, "monitorform", getToDate())));




        stateSB.append(HtmlUtil.formTableClose());


        StringBuffer searchSB = new StringBuffer();
        addSearchToEditForm(request, searchSB);

        StringBuffer actionsSB = new StringBuffer();
        for(MonitorAction action: actions) {
            action.addToEditForm(this,actionsSB);
        }

        sb.append(HtmlUtil.makeShowHideBlock("Settings",stateSB.toString(),true));
        sb.append(HtmlUtil.makeShowHideBlock("Search Criteria",searchSB.toString(),false));
        sb.append(HtmlUtil.makeShowHideBlock("Actions",actionsSB.toString(),false));
        sb.append(HtmlUtil.p());

    }

    public void addSearchToEditForm(Request request,StringBuffer sb) throws Exception {
        searchSB.append(HtmlUtil.formTable());
        HashSet<String> filterMap = new HashSet<String>();
        for(Filter filter: filters) {
            filterMap.put(filter.getField());
        }

                


        searchSB.append(HtmlUtil.formTableClose());        

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

        if ( !getEnabled()) {
            System.err.println("not enabledy");
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
        entryMatched(entry);
        return true;
    }


    protected void entryMatched(Entry entry) {
        System.err.println("Matched");
        for(MonitorAction action: actions) {
            action.entryMatched(this,entry);
        }

    }


    public void addAction(MonitorAction action) {
        actions.add(action);
    }

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
     *  Set the Enabled property.
     *
     *  @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     *  Get the Enabled property.
     *
     *  @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
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

    /**
       Set the Actions property.

       @param value The new value for Actions
    **/
    public void setActions (List<MonitorAction> value) {
	actions = value;
    }

    /**
       Get the Actions property.

       @return The Actions
    **/
    public List<MonitorAction> getActions () {
	return actions;
    }


    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
    }


    /**
       Set the Editable property.

       @param value The new value for Editable
    **/
    public void setEditable (boolean value) {
	editable = value;
    }

    /**
       Get the Editable property.

       @return The Editable
    **/
    public boolean getEditable () {
	return editable;
    }




    public boolean equals(Object o) {
        if(!(o instanceof EntryMonitor)) return false;
        EntryMonitor that  = (EntryMonitor)o;
        return this.id.equals(that.id);
    }

}


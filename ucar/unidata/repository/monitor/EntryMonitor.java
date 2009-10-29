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
import ucar.unidata.repository.output.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
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
    public static final String ARG_CLEARERROR = "monitor_clearerror";

    /** _more_ */
    private String lastError;

    /** _more_ */
    private String id;

    /** _more_ */
    private String name = "";

    /** _more_ */
    private Repository repository;


    /** _more_ */
    private String userId;

    /** _more_ */
    private User user;

    /** _more_ */
    private Request request;

    /** _more_ */
    private boolean enabled = true;

    /** _more_ */
    private List<Filter> filters = new ArrayList<Filter>();

    /** _more_ */
    private List<MonitorAction> actions = new ArrayList<MonitorAction>();


    /** _more_ */
    private Date fromDate;

    /** _more_ */
    private Date toDate;

    /** _more_ */
    private boolean editable = true;

    /** _more_ */
    public static final String ARG_ADD_ACTION = "addaction";

    /** _more_ */
    public static final String ARG_DELETE_ACTION = "deleteaction";

    /** _more_ */
    public static final String ARG_DELETE_ACTION_CONFIRM =
        "deleteactionconfirm";




    /**
     * _more_
     */
    public EntryMonitor() {}


    /**
     * _more_
     *
     * @param repository _more_
     * @param user _more_
     * @param name _more_
     * @param editable _more_
     */
    public EntryMonitor(Repository repository, User user, String name,
                        boolean editable) {
        this.repository = repository;
        this.name       = name;
        this.editable   = editable;
        this.user       = user;
        if (user != null) {
            this.userId = user.getId();
        }
        this.id  = repository.getGUID();
        fromDate = new Date();
        toDate = new Date(fromDate.getTime()
                          + (long) DateUtil.daysToMillis(7));
    }





    /**
     * _more_
     *
     * @param monitors _more_
     * @param id _more_
     *
     * @return _more_
     */
    public static EntryMonitor findMonitor(List<EntryMonitor> monitors,
                                           String id) {
        for (EntryMonitor monitor : monitors) {
            if (Misc.equals(monitor.getId(), id)) {
                return monitor;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSearchSummary() {
        StringBuffer sb = new StringBuffer();
        if (filters.size() == 0) {
            return "None";
        }
        for (int i = 0; i < filters.size(); i++) {
            if (i > 0) {
                sb.append(" AND<br>");
            }
            sb.append(getSearchSummary(filters.get(i)));
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionSummary() {
        StringBuffer sb = new StringBuffer();
        if (actions.size() == 0) {
            return "None";
        }
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(actions.get(i).getSummary(this));
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {

        if (request.get(ARG_CLEARERROR, false)) {
            lastError = "";
        }

        setName(request.getString(ARG_MONITOR_NAME, getName()));
        setEnabled(request.get(ARG_MONITOR_ENABLED, false));
        Date[] dateRange = request.getDateRange(ARG_MONITOR_FROMDATE,
                               ARG_MONITOR_TODATE, new Date());
        fromDate = dateRange[0];
        toDate   = dateRange[1];

        for (MonitorAction action : actions) {
            action.applyEditForm(request, this);
        }
        filters = new ArrayList();
        for (int i = 0; i < Filter.FIELD_TYPES.length; i++) {
            applyEditFilterField(request, Filter.FIELD_TYPES[i]);
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToEditForm(Request request, StringBuffer sb)
            throws Exception {
        StringBuffer stateSB = new StringBuffer();

        stateSB.append(HtmlUtil.formTable());
        stateSB.append(HtmlUtil.formEntry(getRepository().msgLabel("Name"),
                                          HtmlUtil.input(ARG_MONITOR_NAME,
                                              getName(), HtmlUtil.SIZE_70)));
        stateSB.append(
            HtmlUtil.formEntry(
                getRepository().msgLabel("Enabled"),
                HtmlUtil.checkbox(
                    ARG_MONITOR_ENABLED, "true", getEnabled())));

        stateSB.append(
            HtmlUtil.formEntry(
                getRepository().msgLabel("Valid Date Range"),
                getRepository().makeDateInput(
                    request, ARG_MONITOR_FROMDATE, "monitorform",
                    getFromDate()) + " " + getRepository().msg("To") + " "
                                   + getRepository().makeDateInput(
                                       request, ARG_MONITOR_TODATE,
                                       "monitorform", getToDate())));




        stateSB.append(HtmlUtil.formTableClose());


        StringBuffer searchSB = new StringBuffer();
        addSearchToEditForm(request, searchSB);

        StringBuffer actionsSB = new StringBuffer();
        for (MonitorAction action : actions) {
            action.addToEditForm(this, actionsSB);
        }

        sb.append(HtmlUtil.makeShowHideBlock("Settings", stateSB.toString(),
                                             true));

        if ((getLastError() != null) && (getLastError().length() > 0)) {
            StringBuffer errorSB = new StringBuffer();
            errorSB.append(HtmlUtil.checkbox(ARG_CLEARERROR, "true", true));
            errorSB.append(" ");
            errorSB.append(getRepository().msg("Clear error"));
            errorSB.append(HtmlUtil.pre(getLastError()));
            sb.append(
                HtmlUtil.makeShowHideBlock(
                    HtmlUtil.span(
                        getRepository().msg("Error"),
                        HtmlUtil.cssClass("errorlabel")), errorSB.toString(),
                            true));
        }




        sb.append(HtmlUtil.makeShowHideBlock("Search Criteria",
                                             searchSB.toString(), false));
        sb.append(HtmlUtil.makeShowHideBlock("Actions", actionsSB.toString(),
                                             false));
        sb.append(HtmlUtil.p());

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "Montior" + name + " filters:" + filters;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addSearchToEditForm(Request request, StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtil.formTable());
        Hashtable<String, Filter> filterMap = new Hashtable<String, Filter>();
        for (Filter filter : filters) {
            filterMap.put(filter.getField(), filter);
        }

        for (int i = 0; i < Filter.FIELD_TYPES.length; i++) {
            addFilterField(Filter.FIELD_TYPES[i], filterMap, sb);
        }


        sb.append(HtmlUtil.formTableClose());

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @throws Exception _more_
     */
    private void applyEditFilterField(Request request, String what)
            throws Exception {
        boolean doNot = request.get(what + "_not", false);
        if (what.equals(ARG_AREA)) {
            double[] bbox = new double[] {
                                request.get(ARG_AREA + "_south",
                                            Entry.NONGEO),
                                request.get(ARG_AREA + "_north",
                                            Entry.NONGEO),
                                request.get(ARG_AREA + "_east", Entry.NONGEO),
                                request.get(ARG_AREA + "_west",
                                            Entry.NONGEO) };

            if ((bbox[0] != Entry.NONGEO) || (bbox[1] != Entry.NONGEO)
                    || (bbox[2] != Entry.NONGEO)
                    || (bbox[3] != Entry.NONGEO)) {
                addFilter(new Filter(what, bbox, doNot));
            }
            return;
        }



        if ( !request.defined(what)) {
            return;
        }
        if (what.equals(ARG_FILESUFFIX)) {
            List<String> suffixes = StringUtil.split(request.getString(what,
                                        ""), ",", true, true);
            addFilter(new Filter(what, suffixes, doNot));
        } else if (what.equals(ARG_TEXT)) {
            addFilter(new Filter(what, request.getString(what, "").trim(),
                                 doNot));
        } else if (what.equals(ARG_USER)) {
            List<String> users = StringUtil.split(request.getString(what,
                                     ""), ",", true, true);
            addFilter(new Filter(what, users, doNot));
        } else if (what.equals(ARG_ANCESTOR)) {
            String ancestorName = request.getString(ARG_ANCESTOR, "");
            Entry entry = getRepository().getEntryManager().findGroupFromName(
                              ancestorName, getUser(), false);
            if (entry == null) {
                addFilter(new Filter(what, "", doNot));
            } else {
                addFilter(new Filter(what, entry.getId(), doNot));
            }
        } else if (what.equals(ARG_TYPE)) {
            List types = request.get(ARG_TYPE, new ArrayList());
            addFilter(new Filter(what, types, doNot));
        }
    }


    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public boolean nameMatch(String s1, String s2) {
        //TODO: We need to have a StringMatcher object
        if (s1.endsWith("%")) {
            s1 = s1.substring(0, s1.length() - 1);
            return s2.startsWith(s1);
        }
        if (s1.startsWith("%")) {
            s1 = s1.substring(1);
            return s2.endsWith(s1);
        }
        return s2.equals(s1);
    }



    /**
     * _more_
     *
     * @param what _more_
     * @param filterMap _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void addFilterField(String what,
                                Hashtable<String, Filter> filterMap,
                                StringBuffer sb)
            throws Exception {
        Filter  filter = filterMap.get(what);
        boolean doNot  = ((filter == null)
                          ? false
                          : filter.getDoNot());
        String notCbx = HtmlUtil.checkbox(what + "_not", "true", doNot)
                        + HtmlUtil.space(1) + getRepository().msg("Not");

        if (what.equals(ARG_FILESUFFIX)) {
            List<String> suffixes = ((filter == null)
                                     ? (List) new ArrayList()
                                     : (List) filter.getValue());
            sb.append(
                HtmlUtil.formEntry(
                    getRepository().msgLabel("File Suffix"),
                    HtmlUtil.input(
                        what, StringUtil.join(",", suffixes),
                        " size=\"60\" ") + notCbx));
        } else if (what.equals(ARG_TEXT)) {
            sb.append(HtmlUtil.formEntry(getRepository().msgLabel("Text"),
                                         HtmlUtil.input(what,
                                             ((filter == null)
                    ? ""
                    : filter.getValue()
                        .toString()), " size=\"60\" ") + notCbx));
        } else if (what.equals(ARG_USER)) {
            List<String> users = ((filter == null)
                                  ? (List) new ArrayList()
                                  : (List) filter.getValue());
            sb.append(HtmlUtil.formEntry(getRepository().msgLabel("Users"),
                                         HtmlUtil.input(what,
                                             StringUtil.join(",", users),
                                             " size=\"60\" ") + notCbx));
        } else if (what.equals(ARG_ANCESTOR)) {
            String id = (String) ((filter == null)
                                  ? ""
                                  : filter.getValue());
            Group group =
                (Group) getRepository().getEntryManager().getEntry(null, id);

            String select = OutputHandler.getGroupSelect(getRequest(), what);
            sb.append(
                HtmlUtil.formEntry(
                    getRepository().msgLabel("Ancestor Group"),
                    HtmlUtil.input(what, ((group == null)
                                          ? ""
                                          : group.getFullName()), HtmlUtil.id(
                                          what) + HtmlUtil.attr(
                                          HtmlUtil.ATTR_SIZE, "60")) + select
                                              + notCbx));


        } else if (what.equals(ARG_AREA)) {
            double[] values = ((filter == null)
                               ? new double[] { Entry.NONGEO, Entry.NONGEO,
                    Entry.NONGEO, Entry.NONGEO }
                               : (double[]) filter.getValue());
            String latLonForm = HtmlUtil.makeLatLonBox(ARG_AREA,
                                    (values[0] != Entry.NONGEO)
                                    ? values[0]
                                    : Double.NaN, (values[1] != Entry.NONGEO)
                    ? values[1]
                    : Double.NaN, (values[2] != Entry.NONGEO)
                                  ? values[2]
                                  : Double.NaN, (values[3] != Entry.NONGEO)
                    ? values[3]
                    : Double.NaN);

            sb.append(HtmlUtil.formEntry(getRepository().msgLabel("Area"),
                                         latLonForm));
        } else if (what.equals(ARG_TYPE)) {
            List<TypeHandler> typeHandlers =
                getRepository().getTypeHandlers();
            List         tmp   = new ArrayList();
            List<String> types = (List<String>) ((filter == null)
                    ? new ArrayList()
                    : filter.getValue());
            for (TypeHandler typeHandler : typeHandlers) {
                if (typeHandler.getType().equals(TYPE_ANY)) {
                    continue;
                }
                tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                           typeHandler.getType()));
            }
            String typeSelect = HtmlUtil.select(ARG_TYPE, tmp, types,
                                    " MULTIPLE SIZE=4 ");
            sb.append(HtmlUtil.formEntry(getRepository().msgLabel("Type"),
                                         typeSelect + notCbx));
        }
    }



    /**
     * _more_
     *
     * @param filter _more_
     *
     * @return _more_
     */
    private Group getGroup(Filter filter) {
        try {
            Group group = (Group) filter.getProperty("ancestor");
            if (group != null) {
                return group;
            }
            group = (Group) getRepository().getEntryManager().getEntry(null,
                    (String) filter.getValue());
            filter.putProperty("ancestor", group);
            return group;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param filter _more_
     *
     * @return _more_
     */
    private String getSearchSummary(Filter filter) {
        String desc  = "";
        String value = null;
        String what  = filter.getField();
        if (what.equals(ARG_FILESUFFIX)) {
            desc = "file suffix";
        } else if (what.equals(ARG_TEXT)) {
            desc = "name/description";
        } else if (what.equals(ARG_USER)) {
            desc = "user";
        } else if (what.equals(ARG_AREA)) {
            desc = "area";
        } else if (what.equals(ARG_ANCESTOR)) {
            desc = "ancestor";
            Group group = getGroup(filter);
            value = ((group == null)
                     ? "_undefined_"
                     : group.getFullName());
        } else if (what.equals(ARG_TYPE)) {
            desc = "type";
        } else {
            desc = "Unknown";
        }

        if (value == null) {
            if (filter.getValue() instanceof List) {
                value = HtmlUtil.quote(StringUtil.join("\" OR \"",
                        (List) filter.getValue()));
            } else {
                value = HtmlUtil.quote(filter.getValue().toString());
            }
        }
        return HtmlUtil.italics(desc) + " " + (filter.getDoNot()
                ? "!"
                : "") + "= (" + value + ")";
    }

    /**
     * _more_
     *
     * @param dummy _more_
     */
    public void setRepository(String dummy) {}


    /**
     * _more_
     *
     * @param repository _more_
     */
    protected void setRepository(Repository repository) {
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
        lastError = message + "<br>" + exc + "<br>" + ((exc != null)
                ? LogUtil.getStackTrace(exc)
                : "");
        getRepository().getLogManager().logError(message, exc);
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    protected void logInfo(String message) {
        getRepository().getLogManager().logInfo(message);
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
     * @return _more_
     */
    public boolean isActive() {
        if ( !getEnabled()) {
            return false;
        }
        //Must have at least one date
        if ((fromDate == null) && (toDate == null)) {
            return false;
        }
        Date now = new Date();

        if (fromDate != null) {
            if (now.getTime() < fromDate.getTime()) {
                return false;
            }
        }

        if (toDate != null) {
            if (now.getTime() > toDate.getTime()) {
                return false;
            }
        }
        return true;
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
        if ( !isActive()) {
            return false;
        }

        if (filters.size() == 0) {
            return false;
        }

        //        System.err.println(getName() + " checking entry:" + entry.getName());

        if ( !okToView(entry)) {
            //            System.err.println("can't view");
            return false;
        }



        for (Filter filter : filters) {
            boolean ok = checkEntry(filter, entry);
            //            System.err.println("Checking " + ok + " filter=" + filter);
            if ( !ok) {
                //                System.err.println("filter not OK");
                return false;
            }
        }
        entryMatched(entry);
        return true;
    }


    /**
     * _more_
     *
     *
     * @param filter _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean checkEntry(Filter filter, Entry entry) throws Exception {
        boolean ok    = false;
        String  field = filter.getField();
        Object  value = filter.getValue();
        boolean doNot = filter.getDoNot();
        if (field.equals(ARG_TYPE)) {
            List<String> types = (List<String>) value;
            ok = types.contains(entry.getTypeHandler().getType());
        } else if (field.equals(ARG_NAME)) {
            ok = nameMatch(value.toString(), entry.getName());
        } else if (field.equals(ARG_DESCRIPTION)) {
            ok = nameMatch(value.toString(), entry.getDescription());
        } else if (field.equals(ARG_FILESUFFIX)) {
            List<String> suffixes = (List<String>) value;
            String       path     = entry.getResource().getPath();
            if (path != null) {
                for (String suffix : suffixes) {
                    if (IOUtil.hasSuffix(path, suffix)) {
                        ok = true;
                        break;
                    }
                }
            }
        } else if (field.equals(ARG_TEXT)) {
            ok = nameMatch(value.toString(), entry.getDescription())
                 || nameMatch(value.toString(), entry.getName());
        } else if (field.equals(ARG_ANCESTOR)) {
            Group ancestor = getGroup(filter);
            if (ancestor != null) {
                Group parent = entry.getParentGroup();
                while (parent != null) {
                    if (ancestor.equals(parent)) {
                        ok = true;
                        break;
                    }
                    parent = parent.getParentGroup();
                }
            }

        } else if (field.equals(ARG_AREA)) {
            //            System.err.println ("got area filter");
            double[] bbox    = (double[]) filter.getValue();
            boolean  okSouth = true,
                     okNorth = true,
                     okEast  = true,
                     okWest  = true;
            if (bbox[0] != Entry.NONGEO) {
                okSouth = entry.hasSouth() && (entry.getSouth() >= bbox[0]);
            }
            if (bbox[1] != Entry.NONGEO) {
                okNorth = entry.hasNorth() && (entry.getNorth() <= bbox[1]);
            }
            if (bbox[2] != Entry.NONGEO) {
                okEast = entry.hasEast() && (entry.getEast() <= bbox[2]);
            }
            if (bbox[3] != Entry.NONGEO) {
                okWest = entry.hasWest() && (entry.getWest() >= bbox[3]);
            }
            //            System.err.println (okWest +" " + okEast +" " +  okNorth +" " + okSouth);
            ok = okWest && okEast && okNorth && okSouth;
        } else if (field.equals(ARG_USER)) {
            List<String> users = (List<String>) value;
            ok = users.contains(entry.getUser().getId());
        } else if (field.equals(ARG_WAIT)) {
            ok = true;
        } else {
            int match = entry.getTypeHandler().matchValue(field, value,
                            entry);
            if (match == TypeHandler.MATCH_FALSE) {
                ok = false;
            } else if (match == TypeHandler.MATCH_TRUE) {
                ok = true;
            } else {
                System.err.println("unknown field:" + field);
                return true;
            }
        }
        if (doNot) {
            return !ok;
        }
        return ok;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getRequest() throws Exception {
        if (request == null) {
            request = new Request(repository, getUser());
        }
        return request;
    }

    /**
     * _more_
     *
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean okToAddNew(Group group) throws Exception {
        if (group == null) {
            return false;
        }
        return getRepository().getAccessManager().canDoAction(getRequest(),
                group, Permission.ACTION_NEW);
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
    public boolean okToView(Entry entry) throws Exception {
        if (entry == null) {
            return false;
        }
        return getRepository().getAccessManager().canDoAction(getRequest(),
                entry, Permission.ACTION_VIEW);
    }




    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void entryMatched(final Entry entry) {
        Misc.run(new Runnable() {
            public void run() {
                try {
                    entryMatchedInner(entry);
                } catch (Exception exc) {
                    handleError("Error handle entry matched", exc);
                }
            }
        });
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void entryMatchedInner(Entry entry) {
        System.err.println(getName() + " matched entry: " + entry);
        for (MonitorAction action : actions) {
            action.entryMatched(this, entry);
        }
    }


    /**
     * _more_
     *
     * @param action _more_
     */
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
     *  Set the Actions property.
     *
     *  @param value The new value for Actions
     */
    public void setActions(List<MonitorAction> value) {
        actions = value;
    }

    /**
     *  Get the Actions property.
     *
     *  @return The Actions
     */
    public List<MonitorAction> getActions() {
        return actions;
    }


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
     *  Set the Editable property.
     *
     *  @param value The new value for Editable
     */
    public void setEditable(boolean value) {
        editable = value;
    }

    /**
     *  Get the Editable property.
     *
     *  @return The Editable
     */
    public boolean getEditable() {
        return editable;
    }




    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof EntryMonitor)) {
            return false;
        }
        EntryMonitor that = (EntryMonitor) o;
        return this.id.equals(that.id);
    }


    /**
     *  Set the LastError property.
     *
     *  @param value The new value for LastError
     */
    public void setLastError(String value) {
        this.lastError = value;
    }

    /**
     *  Get the LastError property.
     *
     *  @return The LastError
     */
    public String getLastError() {
        return this.lastError;
    }


}


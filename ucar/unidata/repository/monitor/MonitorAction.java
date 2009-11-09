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
import ucar.unidata.repository.auth.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public abstract class MonitorAction implements Constants {

    /** _more_          */
    public static final String macroTooltip =
        "macros: ${entryid} ${resourcepath} ${resourcename} ${fileextension} ${from_day}  ${from_month} ${from_year} ${from_monthname}  <br>"
        + "${to_day}  ${to_month} ${to_year} ${to_monthname}";



    /** _more_ */
    private String id;


    /**
     * _more_
     */
    public MonitorAction() {}


    /**
     * _more_
     *
     * @param id _more_
     */
    public MonitorAction(String id) {
        this.id = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getActionName();

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return getActionName();
    }

    /**
     * _more_
     *
     * @param prefix _more_
     *
     * @return _more_
     */
    protected String getArgId(String prefix) {
        return prefix + "_" + id;
    }

    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     */
    public void addToEditForm(EntryMonitor monitor, StringBuffer sb) {}

    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {}


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {}



    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }




}


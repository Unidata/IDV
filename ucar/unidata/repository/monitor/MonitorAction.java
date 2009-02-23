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



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public abstract class MonitorAction implements Constants {

    /** _more_          */
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
     * @return _more_
     */
    public String getSummary() {
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


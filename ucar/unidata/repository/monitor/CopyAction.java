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
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.InputStream;

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
public class CopyAction extends MonitorAction {

    /** _more_ */
    public static final String ARG_SUBGROUP = "subgroup";


    /** _more_ */
    private String parentGroupId;

    /** _more_ */
    private String subGroup = "";

    /** _more_ */
    private Group group;

    /**
     * _more_
     */
    public CopyAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public CopyAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "Copy Action";
    }

    /**
     * _more_
     *
     * @param entryMonitor _more_
     *
     * @return _more_
     */
    private Group getGroup(EntryMonitor entryMonitor) {
        try {
            if (group == null) {
                group =
                    (Group) entryMonitor.getRepository().getEntryManager()
                        .findGroup(null, parentGroupId);
            }
            return group;
        } catch (Exception exc) {
            return null;
        }
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        Group group = getGroup(entryMonitor);
        if (group == null) {
            return "Copy entry: Error bad group";
        }
        return "Copy entry to:" + group.getName();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.parentGroupId = request.getString(getArgId(ARG_GROUP)
                + "_hidden", "");
        this.group    = null;
        this.subGroup = request.getString(getArgId(ARG_SUBGROUP), "").trim();
    }


    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     */
    public void addToEditForm(EntryMonitor monitor, StringBuffer sb) {
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.colspan("Copy Action", 2));
        try {
            Group  group      = getGroup(monitor);
            String errorLabel = "";
            if ((group != null) && !monitor.okToAddNew(group)) {
                errorLabel = HtmlUtil.span(
                    monitor.getRepository().msg(
                        "You cannot add to the group"), HtmlUtil.cssClass(
                        HtmlUtil.CLASS_ERRORLABEL));
            }
            String groupName = ((group != null)
                                ? group.getFullName()
                                : "");
            String inputId   = getArgId(ARG_GROUP);
            String select =
                monitor.getRepository().getHtmlOutputHandler().getSelect(
                    null, inputId,
                    HtmlUtil.img(
                        monitor.getRepository().iconUrl(
                            ICON_FOLDER_OPEN)) + HtmlUtil.space(1)
                                + monitor.getRepository().msg(
                                    "Select"), false, "");
            sb.append(HtmlUtil.hidden(inputId + "_hidden", parentGroupId,
                                      HtmlUtil.id(inputId + "_hidden")));
            sb.append(
                HtmlUtil.formEntry(
                    "Group:",
                    HtmlUtil.disabledInput(
                        inputId, groupName,
                        HtmlUtil.SIZE_60 + HtmlUtil.id(inputId)) + select));
            sb.append(
                HtmlUtil.formEntry(
                    "Sub-Group Template:",
                    HtmlUtil.input(
                        getArgId(ARG_SUBGROUP), subGroup, HtmlUtil.SIZE_60)));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        sb.append(HtmlUtil.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
        try {
            Group group = getGroup(monitor);
            if (group == null) {
                return;
            }
        } catch (Exception exc) {
            monitor.handleError("Error posting to LDM", exc);
        }
    }


    /**
     *  Set the ParentGroupId property.
     *
     *  @param value The new value for ParentGroupId
     */
    public void setParentGroupId(String value) {
        this.parentGroupId = value;
    }

    /**
     *  Get the ParentGroupId property.
     *
     *  @return The ParentGroupId
     */
    public String getParentGroupId() {
        return this.parentGroupId;
    }

    /**
     *  Set the SubGroup property.
     *
     *  @param value The new value for SubGroup
     */
    public void setSubGroup(String value) {
        this.subGroup = value;
    }

    /**
     *  Get the SubGroup property.
     *
     *  @return The SubGroup
     */
    public String getSubGroup() {
        return this.subGroup;
    }



}


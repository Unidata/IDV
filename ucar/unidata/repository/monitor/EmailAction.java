/**
 * $Id: ,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.StringUtil;


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
public class EmailAction extends PasswordAction {


    /**
     * _more_
     */
    public EmailAction() {}


    /**
     * _more_
     * @param id _more_
     * @param remoteUserId _more_
     */
    public EmailAction(String id, String remoteUserId) {
        super(id, remoteUserId, (String) null);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "Email Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Send an email to " + getRemoteUserId();
    }

    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     */
    public void addToEditForm(EntryMonitor monitor, StringBuffer sb) {
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.colspan("Send an email", 2));

        sb.append(HtmlUtil.formEntry("Email address",
                                     HtmlUtil.input(getArgId(ARG_ACTION_ID),
                                         getRemoteUserId(),
                                         HtmlUtil.SIZE_60)));
        sb.append(
            HtmlUtil.formEntryTop(
                "Message",
                HtmlUtil.textArea(
                    getArgId(ARG_ACTION_MESSAGE), getMessageTemplate(), 5,
                    60)));
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
            String from = monitor.getUser().getEmail();
            if ((from == null) || (from.trim().length() == 0)) {
                from = monitor.getRepository().getProperty(PROP_ADMIN_EMAIL,
                        "");
            }

            try {
                for (String to : StringUtil.split(getRemoteUserId(), ",",
                        true, true)) {
                    monitor.getRepository().getLogManager().logInfo(
                        "Monitor:" + this + " sending mail to: " + to);
                    String message = getMessage(monitor, entry);
                    monitor.getRepository().getAdmin().sendEmail(to, from,
                            "New Entry", message, false);
                }
            } catch (Exception exc) {
                monitor.handleError("Error sending email to "
                                    + getRemoteUserId() + " from:"
                                    + from, exc);
            }
        } catch (Exception exc2) {
            monitor.handleError("Error:", exc2);
        }
    }



}


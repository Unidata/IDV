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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */




package ucar.unidata.repository.monitor;


import ucar.unidata.repository.*;

import ucar.unidata.util.HtmlUtil;


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
    public EmailAction() {
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param user _more_
     * @param remoteUserId _more_
     */
    public EmailAction(String remoteUserId) {
        super(remoteUserId, (String) null);
    }



    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
        try {
            String url =
                HtmlUtil.url(monitor.getRepository().URL_ENTRY_SHOW.getFullUrl(),
                             ARG_ENTRYID, entry.getId());
            StringBuffer message = new StringBuffer("New entry:"
                                       + entry.getFullName() + "\n" + url);
            String userId = getRemoteUserId();

            String from   = monitor.getUser().getEmail();
            if ((from == null) || (from.trim().length() == 0)) {
                monitor.getRepository().getAdmin().sendEmail(userId, "New Entry",
                        message.toString(), false);
            } else {
                monitor.getRepository().getAdmin().sendEmail(userId, from,
                        "New Entry", message.toString(), false);
            }
        } catch (Exception exc) {
            monitor.handleError("Error posting to Twitter", exc);
        }
    }



}


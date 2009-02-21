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
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class TwitterAction extends PasswordAction {


    /**
     * _more_
     */
    public TwitterAction() {}



    /**
     * _more_
     *
     * @param remoteUserId _more_
     * @param password _more_
     */
    public TwitterAction(String remoteUserId, String password) {
        super(remoteUserId, password);
    }



    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
        try {
            super.entryMatched(monitor, entry);
            System.err.println("twitter got entry ");
            if (true) {
                return;
            }


            twitter4j.Twitter twitter =
                new twitter4j.Twitter(getRemoteUserId(), getPassword());
            String url1 =
                HtmlUtil.url(monitor.getRepository().URL_ENTRY_SHOW.getFullUrl(),
                             ARG_ENTRYID, entry.getId());
            twitter4j.Status status = twitter.update("New entry:"
                                          + entry.getFullName() + "\n"
                                          + url1);
            System.out.println("Successfully updated the status to ["
                               + status.getText() + "].");
        } catch (Exception exc) {
            monitor.handleError("Error posting to Twitter", exc);
        }
    }



}


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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository;

import ucar.unidata.repository.monitor.*;

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;


import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MonitorManager extends RepositoryManager {

    /** _more_ */
    private List<EntryMonitor> entryMonitors =
        new ArrayList<EntryMonitor>();




    /**
     * _more_
     *
     * @param repository _more_
     */
    public MonitorManager(Repository repository) {
        super(repository);
        try {
        entryMonitors.add(new EntryMonitor(repository, getUserManager().findUser("jeffmc",false),
                                           "Test 1", true));
        entryMonitors.add(new EntryMonitor(repository, getUserManager().findUser("jeffmc",false),
                                           "Test 2", true));
        } catch(Exception exc) {
        }
    }


    public List<EntryMonitor> getEntryMonitors() {
        return entryMonitors;
    }


    public Result processEntryListen(Request request) throws Exception {
        SynchronousEntryMonitor entryMonitor = new SynchronousEntryMonitor(getRepository(), request);
        synchronized (entryMonitors) {
            entryMonitors.add(entryMonitor);
        }
        synchronized (entryMonitor) {
            entryMonitor.wait();
            System.err.println("Done waiting");
        }
        Entry entry = entryMonitor.getEntry();
        if (entry == null) {
            System.err.println("No entry");
            return new Result(BLANK, new StringBuffer("No match"),
                              getRepository().getMimeTypeFromSuffix(".html"));
        }
        return getRepository().getOutputHandler(request).outputEntry(request, entry);
    }


    /**
     * _more_
     *
     * @param entries _more_
     */
    public void checkNewEntries(List<Entry> entries) {
        try {
            List<EntryMonitor> monitors;
            synchronized (entryMonitors) {
                monitors = new ArrayList<EntryMonitor>(entryMonitors);
            }
            for (Entry entry : entries) {
                for (EntryMonitor entryMonitor : monitors) {
                    entryMonitor.checkEntry(entry);
                }
            }
        } catch (Exception exc) {
            System.err.println("Error checking monitors:" + exc);
            exc.printStackTrace();
        }
    }



    public void deleteMonitor(Request request, EntryMonitor monitor) throws Exception {
        entryMonitors.remove(monitor);
    }        


    public Result processMonitorEdit(Request request,EntryMonitor monitor) throws Exception {


        if(request.exists(ARG_MONITOR_DELETE_CONFIRM)) {
            deleteMonitor(request, monitor);
            return new Result(request.url(getRepositoryBase().URL_USER_MONITORS));
        }

        if(request.exists(ARG_MONITOR_CHANGE)) {
            monitor.applyEditForm(request);
            return new Result(HtmlUtil.url(
                                           getRepositoryBase().URL_USER_MONITORS.toString(),
                                           ARG_MONITOR_ID,
                                           monitor.getId()));
        }

        StringBuffer sb   = new StringBuffer();
        String listLink = HtmlUtil.href(getRepositoryBase().URL_USER_MONITORS.toString(),msg("List"));
        sb.append(HtmlUtil.center(listLink));

        sb.append(msgLabel("Monitor"));
        sb.append(HtmlUtil.space(1));
        sb.append(monitor.getName());
        sb.append(HtmlUtil.formPost(getRepositoryBase().URL_USER_MONITORS.toString(),HtmlUtil.attr(HtmlUtil.ATTR_NAME,"monitorform")));
        sb.append(HtmlUtil.hidden(ARG_MONITOR_ID,monitor.getId()));

        if(request.exists(ARG_MONITOR_DELETE)) {
            StringBuffer fb = new StringBuffer();
            fb.append(RepositoryUtil.buttons(HtmlUtil.submit(msg("OK"),
                ARG_MONITOR_DELETE_CONFIRM), HtmlUtil.submit(msg("Cancel"),
                                                             ARG_CANCEL)));
            sb.append(getRepository().question(msg("Are you sure you want to delete the monitor?"), fb.toString()));
            sb.append(HtmlUtil.formClose());
            return getUserManager().makeResult(request, msg("Monitor Delete"), sb);
        } 


        StringBuffer buttons = new StringBuffer();
        buttons.append(HtmlUtil.submit(msg("Edit"),ARG_MONITOR_CHANGE));
        buttons.append(HtmlUtil.space(1));
        buttons.append(HtmlUtil.submit(msg("Delete"),ARG_MONITOR_DELETE));
        sb.append(buttons);
        sb.append(HtmlUtil.br());
        monitor.addToEditForm(request,sb);
        sb.append(buttons);

        sb.append(HtmlUtil.formClose());
        return getUserManager().makeResult(request, msg("Edit Entry Monitor"), sb);
    }


    public Result processMonitorCreate(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        EntryMonitor monitor = new EntryMonitor(getRepository(),request.getUser(),"New Monitor",true);
        entryMonitors.add(monitor);
        return new Result(HtmlUtil.url(
                                       getRepositoryBase().URL_USER_MONITORS.toString(),
                                       ARG_MONITOR_ID,
                                       monitor.getId()));
    }

    public Result processMonitors(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        List<EntryMonitor> monitors = EntryMonitor.getEditable(getEntryMonitors());
        if(request.exists(ARG_MONITOR_ID)) {
            EntryMonitor entryMonitor = EntryMonitor.findMonitor(monitors,request.getString(ARG_MONITOR_ID,""));
            if(entryMonitor==null) {
                throw new IllegalArgumentException("Could not find entry monitor");
            }
            if(!entryMonitor.getEditable()) {
                throw new IllegalArgumentException("Entry monitor is not editable");
            }
            if(!request.getUser().getAdmin() && !Misc.equals(entryMonitor.getUser(), request.getUser())) {
                throw new IllegalArgumentException("You are not allowed to edit thr monitor");                
            }
            return processMonitorEdit(request, entryMonitor);
        }

        if(request.exists(ARG_MONITOR_CREATE)) {
            return processMonitorCreate(request);
        }


        sb.append(request.form(getRepositoryBase().URL_USER_MONITORS));
        sb.append(HtmlUtil.submit("Create a monitor",ARG_MONITOR_CREATE));
        sb.append(HtmlUtil.formClose());
        sb.append(HtmlUtil.p());

        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        if(monitors.size()>0) {
            sb.append(HtmlUtil.row(HtmlUtil.cols("",msg("Enabled"),msg("Monitor"),msg("User"))));
        }
        for(EntryMonitor monitor: monitors) {
            sb.append(HtmlUtil.open(HtmlUtil.TAG_TR));
            sb.append(HtmlUtil.open(HtmlUtil.TAG_TD));
            sb.append(HtmlUtil.href(HtmlUtil.url(
                                                 getRepositoryBase().URL_USER_MONITORS.toString(),
                                                 ARG_MONITOR_ID,
                                                 monitor.getId()),HtmlUtil.img(iconUrl(ICON_EDIT))));
            sb.append(HtmlUtil.space(1));
            sb.append(HtmlUtil.href(HtmlUtil.url(
                                                 getRepositoryBase().URL_USER_MONITORS.toString(),
                                                 ARG_MONITOR_DELETE,
                                                 "true",
                                                 ARG_MONITOR_ID,
                                                 monitor.getId()),HtmlUtil.img(iconUrl(ICON_DELETE))));
            sb.append(HtmlUtil.close(HtmlUtil.TAG_TD));
            sb.append(HtmlUtil.open(HtmlUtil.TAG_TD));
            if(monitor.getEnabled()) {
                sb.append(msg("Yes"));
            } else {
                sb.append(msg("No"));
            }
            sb.append(HtmlUtil.close(HtmlUtil.TAG_TD));

            sb.append(HtmlUtil.open(HtmlUtil.TAG_TD));
            sb.append(monitor.getName());
            sb.append(HtmlUtil.close(HtmlUtil.TAG_TD));

            sb.append(HtmlUtil.open(HtmlUtil.TAG_TD));
            sb.append(monitor.getUser().getLabel());
            sb.append(HtmlUtil.close(HtmlUtil.TAG_TD));


            sb.append(HtmlUtil.close(HtmlUtil.TAG_TR));
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));
        
        return getUserManager().makeResult(request, msg("Entry Monitors"), sb);
    }




}


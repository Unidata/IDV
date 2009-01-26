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


import org.w3c.dom.*;



import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;


import javax.swing.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class AccessManager extends RepositoryManager {


    /** _more_ */
    public RequestUrl URL_ACCESS_FORM = new RequestUrl(getRepository(),
                                            "/access/form", "Access");


    /** _more_ */
    public RequestUrl URL_ACCESS_CHANGE = new RequestUrl(getRepository(),
                                              "/access/change");


    /** _more_ */
    private Object MUTEX_PERMISSIONS = new Object();


    /**
     * _more_
     *
     * @param repository _more_
     *
     */
    public AccessManager(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     *
     * @param topGroup _more_
     *
     * @throws Exception _more_
     */
    protected void initTopGroup(Group topGroup) throws Exception {
        topGroup.addPermission(new Permission(Permission.ACTION_VIEW,
                getUserManager().ROLE_ANY));
        topGroup.addPermission(new Permission(Permission.ACTION_VIEWCHILDREN,
                getUserManager().ROLE_ANY));
        topGroup.addPermission(new Permission(Permission.ACTION_EDIT,
                getUserManager().ROLE_NONE));
        topGroup.addPermission(new Permission(Permission.ACTION_NEW,
                getUserManager().ROLE_NONE));
        topGroup.addPermission(new Permission(Permission.ACTION_DELETE,
                getUserManager().ROLE_NONE));
        topGroup.addPermission(new Permission(Permission.ACTION_COMMENT,
                getUserManager().ROLE_ANY));
        insertPermissions(null, topGroup, topGroup.getPermissions());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoAction(Request request, String action)
            throws Exception {

        User user = request.getUser();
        //The admin can do anything

        if (user.getAdmin()) {
            return true;
        }

        if (request.exists(ARG_ENTRYID)) {
            Entry entry = getEntryManager().getEntry(request,
                              request.getString(ARG_ENTRYID, ""), false);
            if (entry == null) {
                throw new IllegalArgumentException("Could not find entry:"
                        + request.getString(ARG_ENTRYID, ""));
            }
            return canDoAction(request, entry, action);
        }

        if (request.exists(ARG_ENTRYIDS)) {
            for (String id : StringUtil.split(request.getString(ARG_ENTRYIDS,
                    ""), ",", true, true)) {
                Entry entry = getEntryManager().getEntry(request, id, false);
                if (entry == null) {
                    throw new IllegalArgumentException(
                        "Could not find entry:" + id);
                }
                if ( !canDoAction(request, entry, action)) {
                    return false;
                }
            }
            return true;
        }

        if (request.exists(ARG_GROUP)) {
            Group group = getEntryManager().findGroup(request);
            if (group == null) {
                throw new IllegalArgumentException("Could not find group:"
                        + request.getString(ARG_GROUP, ""));
            }
            return canDoAction(request, group, action);
        }

        if (request.exists(ARG_ASSOCIATION)) {
            Clause clause = Clause.eq(Tables.ASSOCIATIONS.COL_ID,
                                      request.getString(ARG_ASSOCIATION, ""));
            List<Association> associations =
                getEntryManager().getAssociations(request, clause);
            if (associations.size() == 1) {
                Entry fromEntry = getEntryManager().getEntry(request,
                                      associations.get(0).getFromId());
                Entry toEntry = getEntryManager().getEntry(request,
                                    associations.get(0).getToId());
                if (canDoAction(request, fromEntry, action)) {
                    return true;
                }
                if (canDoAction(request, toEntry, action)) {
                    return true;
                }
                return false;


            }
        }

        throw new IllegalArgumentException("Could not find entry or group");
        //        return false;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDoAction(Request request, Entry entry, String action)
            throws Exception {
        if (entry.getIsLocalFile()) {
            if (action.equals(Permission.ACTION_NEW)) {
                return false;
            }
            if (action.equals(Permission.ACTION_DELETE)) {
                return false;
            }
        }

        String requestIp = null;
        User   user      = null;
        if (request == null) {
            user = getUserManager().getAnonymousUser();
        } else {
            user      = request.getUser();
            requestIp = request.getIp();
        }
        //        System.err.println ("cando:" + user + " " + user.getAdmin());
        //The admin can do anything
        if (user.getAdmin()) {
            return true;
        }

        if ( !user.getAnonymous() && Misc.equals(user, entry.getUser())) {
            return true;
        }


        //        System.err.println ("can do: " + action);
        while (entry != null) {
            List permissions = getPermissions(entry);
            List roles       = getRoles(entry, action);
            if (roles != null) {
                for (int roleIdx = 0; roleIdx < roles.size(); roleIdx++) {
                    String  role  = (String) roles.get(roleIdx);
                    boolean doNot = false;
                    if (role.startsWith("!")) {
                        doNot = true;
                        role  = role.substring(1);
                    }
                    if (role.startsWith("ip:")) {
                        String ip = role.substring(3);
                        if ((requestIp != null) && requestIp.startsWith(ip)) {
                            if (doNot) {
                                return false;
                            }
                        } else {
                            return true;
                        }
                    }
                    if (user.isRole(role)) {
                        return !doNot;
                    }
                }
                break;
            }
            entry = getEntryManager().getEntry(request,
                    entry.getParentGroupId());
        }
        return false;
    }

    /** _more_ */
    Hashtable seen = new Hashtable();

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List getRoles(Entry entry, String action) throws Exception {
        //Make sure we call getPermissions first which forces the instantation of the roles
        getPermissions(entry);
        return entry.getRoles(action);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        if ( !getRepository().getProperty(PROP_DOWNLOAD_OK, false)) {
            return false;
        }
        entry = filterEntry(request, entry);
        if (entry == null) {
            return false;
        }
        if ( !entry.getTypeHandler().canDownload(request, entry)) {
            return false;
        }
        return getStorageManager().canDownload(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry filterEntry(Request request, Entry entry) throws Exception {
        if ((entry.getResource() != null)
                && Misc.equals(entry.getResource().getType(),
                               Resource.TYPE_FILE)) {
            if ( !entry.getResource().getFile().exists()) {
                return null;
            }
        }
        if ( !canDoAction(request, entry, Permission.ACTION_VIEW)) {
            return null;
        }
        

        Group parent = entry.getParentGroup();
        if(parent!=null && !canDoAction(request, parent, Permission.ACTION_VIEWCHILDREN)) {
            return null;
        }

        if(entry.isUploaded()) {
            if(!canDoAction(request,parent,Permission.ACTION_NEW)) {
                return null;
            }
        }


        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> filterEntries(Request request, List entries)
            throws Exception {
        List<Entry> filtered = new ArrayList();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = (Entry) entries.get(i);
            entry = filterEntry(request, entry);
            if (entry != null) {
                filtered.add(entry);
            }
        }
        return filtered;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canEditEntry(Request request, Entry entry)
            throws Exception {
        //        if(entry.getIsLocalFile()) return false;
        return canDoAction(request, entry, Permission.ACTION_EDIT);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    protected void listAccess(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        if (entry == null) {
            return;
        }
        List<Permission> permissions = getPermissions(entry);
        String entryUrl = HtmlUtil.href(request.url(URL_ACCESS_FORM,
                              ARG_ENTRYID, entry.getId()), entry.getName());

        Hashtable map = new Hashtable();
        for (Permission permission : permissions) {
            List roles = (List) map.get(permission.getAction());
            if (roles == null) {
                map.put(permission.getAction(), roles = new ArrayList());
            }
            roles.addAll(permission.getRoles());
        }

        StringBuffer cols = new StringBuffer(HtmlUtil.cols(entryUrl));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            List roles = (List) map.get(Permission.ACTIONS[i]);
            if (roles == null) {
                cols.append(HtmlUtil.cols("&nbsp;"));
            } else {
                cols.append(HtmlUtil.cols(StringUtil.join("<br>", roles)));
            }
        }
        sb.append(HtmlUtil.rowTop(cols.toString()));
        listAccess(request,
                   getEntryManager().getEntry(request,
                       entry.getParentGroupId()), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param permissions _more_
     *
     * @throws Exception _more_
     */
    protected void insertPermissions(Request request, Entry entry,
                                     List<Permission> permissions)
            throws Exception {
        synchronized (MUTEX_PERMISSIONS) {
            getDatabaseManager().delete(
                Tables.PERMISSIONS.NAME,
                Clause.eq(Tables.PERMISSIONS.COL_ENTRY_ID, entry.getId()));

            for (Permission permission : permissions) {
                List roles = permission.getRoles();
                for (int i = 0; i < roles.size(); i++) {
                    getDatabaseManager().executeInsert(
                        Tables.PERMISSIONS.INSERT,
                        new Object[] { entry.getId(),
                                       permission.getAction(),
                                       roles.get(i) });
                }
            }
        }
        entry.setPermissions(permissions);
    }


    public boolean hasPermissionSet(Entry entry, String permission) throws Exception {
        for(Permission p:getPermissions(entry)) {
            if(Misc.equals(p.getAction(),permission)) return true;
        }
        return false;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Permission> getPermissions(Entry entry) throws Exception {
        synchronized (MUTEX_PERMISSIONS) {
            if (entry.isGroup() && ((Group) entry).isDummy()) {
                return new ArrayList<Permission>();
            }
            if (entry.getPermissions() != null) {
                return entry.getPermissions();
            }
            //            if(!entry.isGroup()) 
            //                System.err.println ("getPermissions for entry:" + entry.getId());
            SqlUtil.Iterator iter =
                SqlUtil.getIterator(
                    getDatabaseManager().select(
                        Tables.PERMISSIONS.COLUMNS, Tables.PERMISSIONS.NAME,
                        Clause.eq(
                            Tables.PERMISSIONS.COL_ENTRY_ID, entry.getId())));

            List<Permission> permissions = new ArrayList<Permission>();

            ResultSet        results;
            Hashtable        actions = new Hashtable();
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    String id     = results.getString(1);
                    String action = results.getString(2);
                    String role   = results.getString(3);
                    List   roles  = (List) actions.get(action);
                    if (roles == null) {
                        actions.put(action, roles = new ArrayList());
                        permissions.add(new Permission(action, roles));
                    }
                    roles.add(role);
                }
            }
            entry.setPermissions(permissions);
            return permissions;
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processAccessForm(Request request) throws Exception {
        StringBuffer sb    = new StringBuffer();
        Entry        entry = getEntryManager().getEntry(request);
        //        sb.append(getEntryManager().makeEntryHeader(request, entry));
        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }

        StringBuffer currentAccess = new StringBuffer();
        currentAccess.append(HtmlUtil.open(HtmlUtil.TAG_TABLE," cellspacing=0 ccellpadding=0 border=1 "));
        StringBuffer header =
            new StringBuffer(HtmlUtil.cols(HtmlUtil.bold("Entry")));
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            header.append(
                HtmlUtil.cols(HtmlUtil.bold(Permission.ACTION_NAMES[i])));
        }
        currentAccess.append(HtmlUtil.rowTop(header.toString()));

        listAccess(request, entry, currentAccess);
        currentAccess.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));




        Hashtable        map         = new Hashtable();
        List<Permission> permissions = getPermissions(entry);
        for (Permission permission : permissions) {
            map.put(permission.getAction(),
                    StringUtil.join("\n", permission.getRoles()));
        }
        sb.append(request.form(URL_ACCESS_CHANGE, ""));

        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.submit("Change Access"));
        sb.append("<p>");
        //        sb.append("<table><tr valign=\"top\"><td>");
        sb.append(HtmlUtil.formTable());
        sb.append("<tr valign=top>");
        sb.append(HtmlUtil.cols(HtmlUtil.bold("Action"),
                                HtmlUtil.bold("Role") + " (one per line)"));
        sb.append(HtmlUtil.cols(HtmlUtil.space(5)));
        sb.append("<td rowspan=6><b>All Roles</b><i><br>");
        sb.append(StringUtil.join("<br>", getUserManager().getRoles()));
        sb.append("</i></td>");

        sb.append(HtmlUtil.cols(HtmlUtil.space(5)));

        sb.append("<td rowspan=6><b>Current settings:</b><i><br>");
        sb.append(currentAccess.toString());
        sb.append("</i></td>");

        sb.append("</tr>");
        for (int i = 0; i < Permission.ACTIONS.length; i++) {
            String roles = (String) map.get(Permission.ACTIONS[i]);
            if (roles == null) {
                roles = "";
            }
            sb.append(
                HtmlUtil.rowTop(
                    HtmlUtil.cols(
                        Permission.ACTION_NAMES[i],
                        HtmlUtil.textArea(
                            ARG_ROLES + "." + Permission.ACTIONS[i], roles,
                            5, 20))));
        }
        sb.append(HtmlUtil.formTableClose());
        //        sb.append("</td><td>&nbsp;&nbsp;&nbsp;</td><td>");
        //        sb.append("All Roles:<br>");
        //        sb.append(StringUtil.join("<br>",getUserManager().getRoles()));
        //        sb.append("</td></tr></table>");
        sb.append(HtmlUtil.submit("Change Access"));
        sb.append(HtmlUtil.formClose());

        return getEntryManager().makeEntryEditResult(request, entry,
                "Edit Access", sb);

    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processAccessChange(Request request) throws Exception {
        synchronized (MUTEX_PERMISSIONS) {
            Entry            entry       =
                getEntryManager().getEntry(request);
            String           message     = "Access Changed";

            List<Permission> permissions = new ArrayList<Permission>();

            for (int i = 0; i < Permission.ACTIONS.length; i++) {
                List roles = StringUtil.split(request.getString(ARG_ROLES
                                 + "." + Permission.ACTIONS[i], ""), "\n",
                                     true, true);
                if (roles.size() > 0) {
                    permissions.add(new Permission(Permission.ACTIONS[i],
                            roles));
                }
            }

            insertPermissions(request, entry, permissions);


            return new Result(request.url(URL_ACCESS_FORM, ARG_ENTRYID,
                                          entry.getId(), ARG_MESSAGE,
                                          message));
        }
    }




}


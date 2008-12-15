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
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
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
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WebHarvester extends Harvester {

    /** _more_ */
    public static final String TAG_URLS = "urls";

    /** _more_ */
    public static final String ATTR_URLS = "urls";

    /** _more_ */
    public static final String TAG_URLENTRY = "urlentry";


    /** _more_ */
    private List<String> patternNames = new ArrayList<String>();


    /** _more_ */
    private List<HarvesterEntry> urlEntries = new ArrayList<HarvesterEntry>();

    /** _more_ */
    User user;


    /** _more_ */
    List<String> statusMessages = new ArrayList<String>();

    /** _more_ */
    private int entryCnt = 0;

    /** _more_ */
    private int newEntryCnt = 0;


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public WebHarvester(Repository repository, String id) throws Exception {
        super(repository, id);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public WebHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }




    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);
        rootDir = new File(XmlUtil.getAttribute(element, ATTR_ROOTDIR, ""));

        List children = XmlUtil.findChildren(element, TAG_URLENTRY);
        urlEntries = new ArrayList<HarvesterEntry>();
        for (int i = 0; i < children.size(); i++) {
            Element node = (Element) children.get(i);
            urlEntries.add(new HarvesterEntry(node));
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getUser() throws Exception {
        if (user == null) {
            user = repository.getUserManager().getDefaultUser();
        }
        return user;
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        for (HarvesterEntry urlEntry : urlEntries) {
            Element node = XmlUtil.create(element.getOwnerDocument(),
                                          TAG_URLENTRY, element,
                                          new String[] {
                ATTR_URL, urlEntry.url, ATTR_NAME, urlEntry.name,
                ATTR_DESCRIPTION, urlEntry.description, ATTR_GROUP,
                urlEntry.group
            });
        }

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        StringBuffer sb  = new StringBuffer();
        int          cnt = 1;
        urlEntries = new ArrayList<HarvesterEntry>();
        while (true) {
            if ( !request.exists(ATTR_URL + cnt)) {
                break;
            }
            if ( !request.defined(ATTR_URL + cnt)) {
                cnt++;
                continue;
            }
            String group = request.getUnsafeString(ATTR_GROUP + cnt, "");
            group = group.replace(" > ", "/");
            group = group.replace(">", "/");
            urlEntries.add(
                new HarvesterEntry(
                    request.getUnsafeString(ATTR_URL + cnt, ""),
                    request.getUnsafeString(ATTR_NAME + cnt, ""),
                    request.getUnsafeString(ATTR_DESCRIPTION + cnt, ""),
                    group));
            cnt++;
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
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);
        sb.append(
            RepositoryManager.tableSubHeader(
                "Enter urls and the groups to add them to."));


        int cnt = 1;
        String templateHelp =
            "Use macros: ${filename}, ${fromdate}, ${todate}, etc.";
        for (HarvesterEntry urlEntry : urlEntries) {
            sb.append(RepositoryManager.tableSubHeader("URL #" + cnt));
            sb.append(HtmlUtil.formEntry(msgLabel("Fetch URL"),
                                         HtmlUtil.input(ATTR_URL + cnt,
                                             urlEntry.url,
                                             HtmlUtil.SIZE_90)));
            sb.append(
                RepositoryManager.tableSubHeader(
                    "Then create an entry with"));
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Name"),
                    HtmlUtil.input(
                        ATTR_NAME + cnt, urlEntry.name,
                        HtmlUtil.SIZE_90 + HtmlUtil.title(templateHelp))));
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Description"),
                    HtmlUtil.input(
                        ATTR_DESCRIPTION + cnt, urlEntry.description,
                        HtmlUtil.SIZE_90 + HtmlUtil.title(templateHelp))));
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Group"),
                    HtmlUtil.input(
                        ATTR_GROUP + cnt, urlEntry.group,
                        HtmlUtil.SIZE_90 + HtmlUtil.title(templateHelp))));
            cnt++;
        }
        sb.append(RepositoryManager.tableSubHeader("URL #" + cnt));
        sb.append(HtmlUtil.formEntry(msgLabel("Fetch URL"),
                                     HtmlUtil.input(ATTR_URL + cnt, "",
                                         HtmlUtil.SIZE_90)));
        sb.append(
            RepositoryManager.tableSubHeader("Then create an entry with"));
        sb.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ATTR_NAME + cnt, "",
                                         HtmlUtil.SIZE_90
                                         + HtmlUtil.title(templateHelp))));
        sb.append(HtmlUtil.formEntry(msgLabel("Description"),
                                     HtmlUtil.input(ATTR_DESCRIPTION + cnt,
                                         "",
                                         HtmlUtil.SIZE_90
                                         + HtmlUtil.title(templateHelp))));
        sb.append(HtmlUtil.formEntry(msgLabel("Group"),
                                     HtmlUtil.input(ATTR_GROUP + cnt, "",
                                         HtmlUtil.SIZE_90
                                         + HtmlUtil.title(templateHelp))));
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        String error = getError();
        if (error != null) {
            return super.getExtraInfo();
        }

        String messages = StringUtil.join("", statusMessages);
        return status.toString() + ((messages.length() == 0)
                                    ? ""
                                    : HtmlUtil.makeShowHideBlock(
                                                                 "Entries", messages,
                                                                 false));
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void runInner() throws Exception {
        if ( !getActive()) {
            return;
        }
        entryCnt       = 0;
        newEntryCnt    = 0;
        statusMessages = new ArrayList<String>();
        status         = new StringBuffer("Fetching URLS<br>");
        int cnt = 0;
        while (getActive()) {
            long t1 = System.currentTimeMillis();
            collectEntries((cnt == 0));
            long t2 = System.currentTimeMillis();
            cnt++;
            if ( !getMonitor()) {
                status = new StringBuffer("Done<br>");
                break;
            }

            status.append("Done... sleeping for " + getSleepMinutes()
                          + " minutes<br>");
            Misc.sleep((long) (getSleepMinutes() * 60 * 1000));
            status = new StringBuffer();
        }
    }




    /**
     * _more_
     *
     * @param firstTime _more_
     *
     *
     * @throws Exception _more_
     */
    public void collectEntries(boolean firstTime) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        for (HarvesterEntry urlEntry : urlEntries) {
            if ( !getActive()) {
                return;
            }
            Entry entry = processUrl(urlEntry.url, urlEntry.name,
                                     urlEntry.description, urlEntry.group);
            if (entry != null) {
                entries.add(entry);
                if (statusMessages.size() > 100) {
                    statusMessages = new ArrayList<String>();
                }
                String crumbs = getEntryManager().makeEntryHeader(null,
                                    entry);
                crumbs = crumbs.replace("class=", "xclass=");
                statusMessages.add(crumbs);
                entryCnt++;
            }
        }

        newEntryCnt += entries.size();
        getEntryManager().insertEntries(entries, true, true);
    }



    /**
     * _more_
     *
     * @param f _more_
     * @param name _more_
     * @param desc _more_
     * @param groupName _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry processUrl(String url, String name, String desc,
                             String groupName)
            throws Exception {
        String fileName = url;
        String tail     = IOUtil.getFileTail(url);
        File   tmpFile  = getStorageManager().getTmpFile(null, tail);
        //        System.err.println ("fetching");
        IOUtil.writeTo(new URL(url), tmpFile, null);
        File newFile = getStorageManager().moveToStorage(null, tmpFile,
                           getRepository().getGUID() + "_");
        //        System.err.println ("got it " + newFile);
        String tag        = tagTemplate;


        Date   createDate = new Date();
        Date   fromDate   = createDate;
        Date   toDate     = createDate;
        String ext        = IOUtil.getFileExtension(url);
        tag = tag.replace("${extension}", ext);

        //        groupName = groupName.replace("${dirgroup}", dirGroup);

        groupName = groupName.replace("${fromdate}",
                                      getRepository().formatDate(fromDate));
        groupName = groupName.replace("${todate}",
                                      getRepository().formatDate(toDate));

        name = name.replace("${filename}", tail);
        name = name.replace("${fromdate}",
                            getRepository().formatDate(fromDate));

        name = name.replace("${todate}", getRepository().formatDate(toDate));

        desc = desc.replace("${fromdate}",
                            getRepository().formatDate(fromDate));
        desc = desc.replace("${todate}", getRepository().formatDate(toDate));
        desc = desc.replace("${name}", name);

        Group group = getEntryManager().findGroupFromName(groupName,
                          getUser(), true);
        Entry entry = typeHandler.createEntry(repository.getGUID());
        Resource resource = new Resource(newFile.toString(),
                                         Resource.TYPE_STOREDFILE);

        entry.initEntry(name, desc, group, getUser(), resource, "",
                        createDate.getTime(), fromDate.getTime(),
                        toDate.getTime(), null);
        if (tag.length() > 0) {
            List tags = StringUtil.split(tag, ",", true, true);
            for (int i = 0; i < tags.size(); i++) {
                entry.addMetadata(new Metadata(repository.getGUID(),
                        entry.getId(), EnumeratedMetadataHandler.TYPE_TAG,
                        DFLT_INHERITED, (String) tags.get(i), "", "", ""));
            }

        }
        typeHandler.initializeNewEntry(entry);
        return entry;

    }



}


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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository;


import org.w3c.dom.*;



import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;

import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class EntryManager extends RepositoryManager {

    /** _more_ */
    private Hashtable entryCache = new Hashtable();

    private Object  MUTEX_ENTRY = new Object();


    /** _more_ */
    public Object MUTEX_GROUP = new Object();

    /** _more_ */
    private static final String GROUP_TOP = "Top";

    /** _more_ */
    private Group topGroup;





    /** _more_ */
    public Hashtable<String, Group> groupCache = new Hashtable<String,
                                                      Group>();




    public EntryManager(Repository repository) {
        super(repository);
    }

    public Group getTopGroup() {
        return topGroup;
    }



    protected void clearCache() {
        entryCache    = new Hashtable();
        groupCache    = new Hashtable();
        topGroups     = null;
    }




    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void clearCache(Entry entry) {
        //        System.err.println ("Clear cache " + entry.getId());
        synchronized(MUTEX_ENTRY) {
            entryCache.remove(entry.getId());
        if (entry.isGroup()) {
            Group group = (Group) entry;
            groupCache.remove(group.getId());
            groupCache.remove(group.getFullName());
        }
        }
    }



    public Result processCatalog(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        String title = msg("Catalog View");

        String url = request.getString(ARG_CATALOG,(String) null);
        if(url == null) {
            sb.append(HtmlUtil.p());
            sb.append(HtmlUtil.form("/repository/catalog"));
            sb.append(msgLabel("Catalog URL"));
            sb.append(HtmlUtil.space(1));
            sb.append(HtmlUtil.input(ARG_CATALOG,"http://dataportal.ucar.edu/metadata/browse/human_dimensions.thredds.xml",HtmlUtil.SIZE_60));
            sb.append(HtmlUtil.submit("View"));
            sb.append(HtmlUtil.formClose());
            //            sb.append("No catalog argument given");
            return new Result(title, sb);
        }
        return new Result(request.url(getRepository().URL_ENTRY_SHOW, ARG_ENTRYID, CatalogTypeHandler.getCatalogId(url)));
                          /*

        Element root = XmlUtil.getRoot(url, getClass());
        if (root == null) {
            sb.append("Could not load catalog: " + url);
            return new Result(title, sb);
        } 
        Element child = (Element)XmlUtil.findChild(root,
                                       CatalogOutputHandler.TAG_DATASET);

        if(child!=null) root = child;
        String name = XmlUtil.getAttribute(root, ATTR_NAME,"");
        sb.append(name);
        sb.append("<ul>");
        recurseCatalog(request, root,sb);
        sb.append("</ul>");
        return new Result(title, sb);
                          */
    }

    private void recurseCatalog(Request request, Element node, StringBuffer sb) {
        NodeList elements = XmlUtil.getElements(node);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (child.getTagName().equals(CatalogOutputHandler.TAG_DATASET)) {
                String name = XmlUtil.getAttribute(child, ATTR_NAME,"");
                sb.append("<li>");
                sb.append(name);
                sb.append("<ul>");
                recurseCatalog(request,child,sb);
                sb.append("</ul>");
            }
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
    public Result processEntryShow(Request request) throws Exception {
        Entry entry;
        if (request.defined(ARG_ENTRYID)) {
            entry = getEntry(request);
            if (entry == null) {
                Entry tmp = getEntry(request,
                                     request.getString(ARG_ENTRYID, BLANK), false);
                if (tmp != null) {
                    throw new IllegalArgumentException(
                        "You do not have access to this entry");
                }
            }
        } else if (request.defined(ARG_GROUP)) {
            entry = findGroup(request);
        } else {
            entry = getTopGroup();
        }
        if (entry == null) {
            throw new IllegalArgumentException("No entry specified");
        }

        //System.err.println (request);
        if (request.get(ARG_NEXT, false)
                || request.get(ARG_PREVIOUS, false)) {
            boolean next = request.get(ARG_NEXT, false);
            List<String> ids =
                getChildIds(
                    request, findGroup(request, entry.getParentGroupId()),
                    new ArrayList<Clause>());
            String nextId = null;
            for (int i = 0; (i < ids.size()) && (nextId == null); i++) {
                String id = ids.get(i);
                if (id.equals(entry.getId())) {
                    if (next) {
                        if (i == ids.size() - 1) {
                            nextId = ids.get(0);
                        } else {
                            nextId = ids.get(i + 1);
                        }
                    } else {
                        if (i == 0) {
                            nextId = ids.get(ids.size() - 1);
                        } else {
                            nextId = ids.get(i - 1);
                        }
                    }
                }
            }
            //Do a redirect
            if (nextId != null) {
                return new Result(request.url(getRepository().URL_ENTRY_SHOW, ARG_ENTRYID, nextId,
                        ARG_OUTPUT,
                        request.getString(ARG_OUTPUT,
                                          OutputHandler.OUTPUT_HTML.getId().toString())));
            }
        }

        String output = request.getString(ARG_OUTPUT,(String)"");
        Result result;
        if (entry.isGroup()) {
            result =  processGroupShow(request, (Group) entry);
        } else {
            result =  getRepository().getOutputHandler(request).outputEntry(request, entry);
        }

        
        if(result.getShouldDecorate()) {
            request.put(ARG_OUTPUT,output);
            StringBuffer sb = new StringBuffer();
            if (!entry.isGroup() || !((Group)entry).isDummy()) {
                String[] crumbs = getBreadCrumbs(request, entry, false);
                sb.append(crumbs[1]);
                sb.append(new String(result.getContent()));
                result.setContent(sb.toString().getBytes());
                result.setTitle(result.getTitle() + ": " + crumbs[0]);
            } 
        }
        return result;
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
    public Result processEntryForm(Request request) throws Exception {


        Group        group = null;
        String       type  = null;
        Entry        entry = null;
        StringBuffer sb    = new StringBuffer();
        //        sb.append(makeTabs(Misc.newList("title1","title2","title3"),
        //                           Misc.newList("contents1","contents2","contents3")));
        if (request.defined(ARG_ENTRYID)) {
            entry = getEntry(request);
            /*
            if (entry.isTopGroup()) {
                sb.append(makeEntryHeader(request, entry));
                sb.append(getRepository().note("Cannot edit top-level group"));
                return makeEntryEditResult(request, entry, "Edit Entry", sb);
                }*/
            type = entry.getTypeHandler().getType();
            if ( !entry.isTopGroup()) {
                group = findGroup(request, entry.getParentGroupId());
            }
        }
        boolean isEntryTop = ((entry != null) && entry.isTopGroup());


        if ( !isEntryTop && (group == null)) {
            group = findGroup(request);
        }
        if (type == null) {
            type = request.getString(ARG_TYPE, (String) null);
        }

        if (entry == null) {
            sb.append(makeEntryHeader(request, group));
        } else {
            sb.append(makeEntryHeader(request, entry));
        }

        if (((entry != null) && entry.getIsLocalFile())) {
            sb.append("This is a local file and cannot be edited");
            return makeEntryEditResult(request, entry, "Entry Edit", sb);
        }


        if (type == null) {
            sb.append(request.form(getRepository().URL_ENTRY_FORM, " name=\"entryform\" "));
        } else {
            sb.append(request.uploadForm(getRepository().URL_ENTRY_CHANGE,
                                         " name=\"entryform\" "));
        }

        sb.append(HtmlUtil.formTable());
        String title = BLANK;

        if (type == null) {
            sb.append(HtmlUtil.formEntry("Type:",
                                         getRepository().makeTypeSelect(request, false,"",true)));

            sb.append(
                HtmlUtil.formEntry(
                    BLANK, HtmlUtil.submit("Select Type to Add")));
            sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
        } else {
            TypeHandler typeHandler = ((entry == null)
                                       ? getRepository().getTypeHandler(type)
                                       : entry.getTypeHandler());


            String submitButton = HtmlUtil.submit(title = ((entry == null)
                    ? "Add " + typeHandler.getLabel()
                    : "Edit " + typeHandler.getLabel()));

            List<Metadata> metadataList = ((entry == null)
                                           ? (List<Metadata>) new ArrayList<Metadata>()
                                           : getRepository().getMetadataManager().getMetadata(
                                               entry));
            String metadataButton = HtmlUtil.submit("Edit Metadata",
                                        ARG_EDIT_METADATA);

            String deleteButton = (((entry != null) && entry.isTopGroup())
                                   ? ""
                                   : HtmlUtil.submit(msg("Delete"),
                                       ARG_DELETE));

            String cancelButton = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
            String buttons      = ((entry != null)
                                   ? RepositoryUtil.buttons(submitButton, deleteButton,
                                             cancelButton)
                                   : RepositoryUtil.buttons(submitButton, cancelButton));


            String topLevelCheckbox = "";
            if ((entry == null) && request.getUser().getAdmin()) {
                topLevelCheckbox = HtmlUtil.space(1)
                                   + HtmlUtil.checkbox(ARG_TOPLEVEL, "true",
                                       false) + HtmlUtil.space(1)
                                           + msg("Make top level");

            }
            topLevelCheckbox = "";
            sb.append(HtmlUtil.row(HtmlUtil.colspan(buttons
                    + topLevelCheckbox, 2)));
            if (entry != null) {
                sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
            } else {
                sb.append(HtmlUtil.hidden(ARG_TYPE, type));
                sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
            }
            //            sb.append(HtmlUtil.formEntry("Type:", typeHandler.getLabel()));
            typeHandler.addToEntryForm(request, sb, entry);
            sb.append(HtmlUtil.row(HtmlUtil.colspan(buttons, 2)));
        }
        sb.append(HtmlUtil.formTableClose());
        if (entry == null) {
            return new Result(title, sb);
        }
        return makeEntryEditResult(request, entry, title, sb);
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
    public Result processEntryChange(final Request request) throws Exception {
        boolean download = request.get(ARG_RESOURCE_DOWNLOAD, false);
        if (download) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    Result result = doProcessEntryChange(request, actionId);
                    getActionManager().setContinueHtml(actionId,
                            HtmlUtil.href(result.getRedirectUrl(),
                                          msg("Continue")));
                }
            };
            return getActionManager().doAction(request, action,
                    "Downloading file", "");

        }
        return doProcessEntryChange(request, null);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param actionId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result doProcessEntryChange(Request request, Object actionId)
            throws Exception {

        boolean     download    = request.get(ARG_RESOURCE_DOWNLOAD, false);

        Entry       entry       = null;
        TypeHandler typeHandler = null;
        boolean     newEntry    = true;
        if (request.defined(ARG_ENTRYID)) {
            entry = getEntry(request);
            if (entry.getIsLocalFile()) {
                return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                        ARG_MESSAGE, "Cannot edit local files"));

            }
            typeHandler = entry.getTypeHandler();
            newEntry    = false;


            if (request.exists(ARG_CANCEL)) {
                return new Result(request.entryUrl(getRepository().URL_ENTRY_FORM, entry));
            }


            if (request.exists(ARG_DELETE_CONFIRM)) {
                if (entry.isTopGroup()) {
                    return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                            ARG_MESSAGE, "Cannot delete top-level group"));
                }

                List<Entry> entries = new ArrayList<Entry>();
                entries.add(entry);
                deleteEntries(request, entries, null);
                Group group = findGroup(request, entry.getParentGroupId());
                return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, group,
                        ARG_MESSAGE, "Entry is deleted"));
            }


            if (request.exists(ARG_DELETE)) {
                return new Result(request.entryUrl(getRepository().URL_ENTRY_DELETE, entry));
            }
        } else {
            typeHandler = getRepository().getTypeHandler(request.getString(ARG_TYPE,
                    TypeHandler.TYPE_ANY));

        }


        List<Entry> entries = new ArrayList<Entry>();

        //Synchronize  in case we need to create a group
        //There is a possible case where we can get two groups with the same id
        Object mutex = new Object();
        if (typeHandler.isType(TypeHandler.TYPE_GROUP)) {
            mutex = MUTEX_GROUP;
        }
        String dataType = "";
        if (request.defined(ARG_DATATYPE)) {
            dataType = request.getString(ARG_DATATYPE, "");
        } else {
            dataType = request.getString(ARG_DATATYPE_SELECT, "");
        }
        synchronized (mutex) {
            if (entry == null) {
                List<String> resources    = new ArrayList();
                List<String> origNames    = new ArrayList();
                String       resource = request.getString(ARG_URL,
                                            BLANK);
                String       filename     = request.getUploadedFile(ARG_FILE);
                boolean      unzipArchive = false;
                boolean      isFile       = false;
                String       resourceName = request.getString(ARG_FILE,
                                                BLANK);
                if (resourceName.length() == 0) {
                    resourceName = IOUtil.getFileTail(resource);
                }

                String groupId = request.getString(ARG_GROUP, (String) null);
                if (groupId == null) {
                    throw new IllegalArgumentException(
                        "You must specify a parent group");
                }
                Group parentGroup = findGroup(request);
                if (filename != null) {
                    isFile       = true;
                    unzipArchive = request.get(ARG_FILE_UNZIP, false);
                    resource     = filename;
                } else if (download) {
                    String url = resource;
                    if ( !url.startsWith("http:")
                            && !url.startsWith("https:")
                            && !url.startsWith("ftp:")) {
                        throw new IllegalArgumentException(
                            "Cannot download url:" + url);
                    }
                    isFile = true;
                    String tail = IOUtil.getFileTail(resource);
                    File newFile = getStorageManager().getTmpFile(request,
                                       tail);
                    RepositoryUtil.checkFilePath(newFile.toString());
                    resourceName = tail;
                    resource     = newFile.toString();
                    URL           fromUrl    = new URL(url);
                    URLConnection connection = fromUrl.openConnection();
                    InputStream   fromStream = connection.getInputStream();
                    //                Object startLoad(String name) {
                    if (actionId != null) {
                        JobManager.getManager().startLoad("File copy",
                                actionId);
                    }
                    int length = connection.getContentLength();
                    if (length > 0 & actionId != null) {
                        getActionManager().setActionMessage(actionId,
                                msg("Downloading") + " " + length + " "
                                + msg("bytes"));
                    }
                    FileOutputStream toStream = new FileOutputStream(newFile);
                    try {
                        if (IOUtil.writeTo(fromStream, toStream, actionId,
                                           length) < 0) {
                            return new Result(
                                request.entryUrl(
                                    getRepository().URL_ENTRY_SHOW, parentGroup));
                        }
                    } finally {
                        try {
                            toStream.close();
                            fromStream.close();
                        } catch (Exception exc) {}
                    }
                }

                if ( !unzipArchive) {
                    resources.add(resource);
                    origNames.add(resourceName);
                } else {
                    ZipInputStream zin =
                        new ZipInputStream(new FileInputStream(resource));
                    ZipEntry ze = null;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (ze.isDirectory()) {
                            continue;
                        }
                        String name =
                            IOUtil.getFileTail(ze.getName().toLowerCase());
                        File f = getStorageManager().getTmpFile(request,
                                     name);
                        RepositoryUtil.checkFilePath(f.toString());
                        FileOutputStream fos = new FileOutputStream(f);

                        IOUtil.writeTo(zin, fos);
                        fos.close();
                        resources.add(f.toString());
                        origNames.add(name);
                    }
                }

                if (request.exists(ARG_CANCEL)) {
                    return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                            parentGroup));
                }


                String description = request.getString(ARG_DESCRIPTION,
                                         BLANK);

                Date createDate = new Date();
                Date[] dateRange = request.getDateRange(ARG_FROMDATE,
                                       ARG_TODATE, createDate);
                if (dateRange[0] == null) {
                    dateRange[0] = ((dateRange[1] == null)
                                    ? createDate
                                    : dateRange[1]);
                }
                if (dateRange[1] == null) {
                    dateRange[1] = dateRange[0];
                }



                for (int resourceIdx = 0; resourceIdx < resources.size();
                        resourceIdx++) {
                    String theResource = (String) resources.get(resourceIdx);
                    String origName    = (String) origNames.get(resourceIdx);
                    if (isFile) {
                        theResource =
                            getStorageManager().moveToStorage(request,
                                new File(theResource)).toString();
                    }
                    String name = request.getString(ARG_NAME, BLANK);
                    if (name.indexOf("${") >= 0) {}

                    if (name.trim().length() == 0) {
                        name = IOUtil.getFileTail(origName);
                    }
                    if (name.trim().length() == 0) {
                        //                        throw new IllegalArgumentException(
                        //                            "You must specify a name");
                    }

                    if (typeHandler.isType(TypeHandler.TYPE_GROUP)) {
                        if (name.indexOf("/") >= 0) {
                            throw new IllegalArgumentException(
                                "Cannot have a '/' in group name: '" + name
                                + "'");
                        }
                        Entry existing = findEntryWithName(request,
                                                           parentGroup, name);
                        if ((existing != null) && existing.isGroup()) {
                            throw new IllegalArgumentException(
                                "A group with the given name already exists");

                        }
                    }

                    Date[] theDateRange = { dateRange[0], dateRange[1] };

                    if (request.defined(ARG_DATE_PATTERN)) {
                        String format =
                            request.getUnsafeString(ARG_DATE_PATTERN, BLANK);
                        String pattern = null;
                        for (int i = 0; i < DateUtil.DATE_PATTERNS.length;
                                i++) {
                            if (format.equals(DateUtil.DATE_FORMATS[i])) {
                                pattern = DateUtil.DATE_PATTERNS[i];
                                break;
                            }
                        }
                        //                    System.err.println("format:" + format);
                        //                    System.err.println("orignName:" + origName);
                        //                    System.err.println("pattern:" + pattern);

                        if (pattern != null) {
                            Pattern datePattern = Pattern.compile(pattern);
                            Matcher matcher = datePattern.matcher(origName);
                            if (matcher.find()) {
                                String dateString = matcher.group(0);
                                Date dttm =
                                    RepositoryUtil.makeDateFormat(format).parse(dateString);
                                theDateRange[0] = dttm;
                                theDateRange[1] = dttm;
                                //                            System.err.println("got it");
                            } else {
                                //                            System.err.println("not found");
                            }
                        }
                    }

                    String id = (typeHandler.isType(TypeHandler.TYPE_GROUP)
                                 ? getGroupId(parentGroup)
                                 : getRepository().getGUID());

                    String resourceType = Resource.TYPE_UNKNOWN;
                    if (isFile) {
                        resourceType = Resource.TYPE_STOREDFILE;
                    } else {
                        try {
                            new URL(theResource);
                            resourceType = Resource.TYPE_URL;
                        } catch (Exception exc) {}

                    }

                    if(!typeHandler.canBeCreatedBy(request)) {
                        throw new IllegalArgumentException("Cannot create an entry of type " + typeHandler.getDescription());
                    }

                    entry = typeHandler.createEntry(id);
                    entry.initEntry(name, description, parentGroup, 
                                    request.getUser(),
                                    new Resource(theResource, resourceType),
                                    dataType, createDate.getTime(),
                                    theDateRange[0].getTime(),
                                    theDateRange[1].getTime(), null);
                    setEntryState(request, entry);
                    entries.add(entry);
                }
            } else {
                if (entry.isTopGroup()) {
                    //                    throw new IllegalArgumentException(
                    //                        "Cannot edit top-level group");
                }
                Date[] dateRange = request.getDateRange(ARG_FROMDATE,
                                       ARG_TODATE, new Date());
                String newName = request.getString(ARG_NAME,
                                     entry.getLabel());



                if (entry.isGroup()) {
                    if (newName.indexOf(Group.IDDELIMITER) >= 0) {
                        throw new IllegalArgumentException(
                            "Cannot have a '/' in group name:" + newName);
                    }

                    /**
                     * TODO Do we want to not allow 2 or more groups with the same name?
                     * Entry existing = findEntryWithName(request,
                     *                                  entry.getParentGroup(), newName);
                     * if ((existing != null) && existing.isGroup()
                     *       && !existing.getId().equals(entry.getId())) {
                     *   throw new IllegalArgumentException(
                     *       "A group with the given name already exists");
                     * }
                     */
                }

                entry.setName(newName);
                entry.setDescription(request.getString(ARG_DESCRIPTION,
                        entry.getDescription()));
                entry.setDataType(dataType);
                if (request.defined(ARG_URL)) {
                    entry.setResource(
                        new Resource(request.getString(ARG_URL, BLANK)));
                }

                //                System.err.println("dateRange:" + dateRange[0] + " " + dateRange[1]);

                if (dateRange[0] != null) {
                    entry.setStartDate(dateRange[0].getTime());
                }
                if (dateRange[1] == null) {
                    dateRange[1] = dateRange[0];
                }

                if (dateRange[1] != null) {
                    entry.setEndDate(dateRange[1].getTime());
                }
                setEntryState(request, entry);
                entries.add(entry);
            }

            if (newEntry && request.get(ARG_ADDMETADATA, false)) {
                addInitialMetadata(request, entries);
            }
            insertEntries(entries, newEntry);
        }
        if (entries.size() == 1) {
            entry = (Entry) entries.get(0);
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
        } else if (entries.size() > 1) {
            entry = (Entry) entries.get(0);
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                    entry.getParentGroup(), ARG_MESSAGE,
                    entries.size() + HtmlUtil.pad(msg("files uploaded"))));
        } else {
            return new Result(BLANK,
                              new StringBuffer(msg("No entries created")));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void setEntryState(Request request, Entry entry)
            throws Exception {
        entry.setSouth(request.get(ARG_AREA + "_south", entry.getSouth()));
        entry.setNorth(request.get(ARG_AREA + "_north", entry.getNorth()));
        entry.setWest(request.get(ARG_AREA + "_west", entry.getWest()));
        entry.setEast(request.get(ARG_AREA + "_east", entry.getEast()));


        entry.getTypeHandler().initializeEntry(request, entry);
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
    public Result processEntryDelete(Request request) throws Exception {
        Entry        entry = getEntry(request);
        StringBuffer sb    = new StringBuffer();
        sb.append(makeEntryHeader(request, entry));
        if (entry.isTopGroup()) {
            sb.append(getRepository().note("Cannot delete top-level group"));
            return makeEntryEditResult(request, entry, "Delete Entry", sb);
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(getRepository().URL_ENTRY_FORM, entry));
        }


        if (request.exists(ARG_DELETE_CONFIRM)) {
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            Group group = findGroup(request, entry.getParentGroupId());
            if (entry.isGroup()) {
                return asynchDeleteEntries(request, entries);
            } else {
                deleteEntries(request, entries, null);
                return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, group));
            }
        }



        StringBuffer inner = new StringBuffer();
        if (entry.isGroup()) {
            inner.append(
                msgLabel("Are you sure you want to delete the group"));
            inner.append(entry.getLabel());
            inner.append(HtmlUtil.p());
            inner.append(
                msg(
                "Note: This will also delete all of the descendents of the group"));
        } else {
            inner.append(
                msgLabel("Are you sure you want to delete the entry"));
            inner.append(entry.getLabel());
        }

        StringBuffer fb = new StringBuffer();
        fb.append(request.form(getRepository().URL_ENTRY_DELETE, BLANK));
        fb.append(RepositoryUtil.buttons(HtmlUtil.submit(msg("OK"), ARG_DELETE_CONFIRM),
                          HtmlUtil.submit(msg("Cancel"), ARG_CANCEL)));
        fb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        fb.append(HtmlUtil.formClose());
        sb.append(getRepository().question(inner.toString(), fb.toString()));
        return makeEntryEditResult(request, entry,
                                   msg("Entry delete confirm"), sb);
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
    public Result processEntryListDelete(Request request) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : StringUtil.split(request.getString(ARG_ENTRYIDS, ""),
                                          ",", true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException("Could not find entry:"  + id);
            }
            if (entry.isTopGroup()) {
                StringBuffer sb = new StringBuffer();
                sb.append(getRepository().note(msg("Cannot delete top-level group")));
                return new Result(msg("Entry Delete"), sb);
            }
            entries.add(entry);
        }
        return processEntryListDelete(request, entries);

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
    public Result processEntryListDelete(Request request, List<Entry> entries)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        if (request.exists(ARG_CANCEL)) {
            if (entries.size() == 0) {
                return new Result(request.url(getRepository().URL_ENTRY_SHOW));
            }
            String id = entries.get(0).getParentGroupId();
            return new Result(request.url(getRepository().URL_ENTRY_SHOW, ARG_ENTRYID, id));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            return asynchDeleteEntries(request, entries);
        }


        if (entries.size() == 0) {
            return new Result(
                "", new StringBuffer(getRepository().warning(msg("No entries selected"))));
        }

        StringBuffer msgSB    = new StringBuffer();
        StringBuffer idBuffer = new StringBuffer();
        for (Entry entry : entries) {
            idBuffer.append(",");
            idBuffer.append(entry.getId());
        }
        msgSB.append(
            msg("Are you sure you want to delete all of the entries?"));
        sb.append(request.form(getRepository().URL_ENTRY_DELETELIST));
        String hidden = HtmlUtil.hidden(ARG_ENTRYIDS, idBuffer.toString());
        String form = RepositoryUtil.makeOkCancelForm(request, getRepository().URL_ENTRY_DELETELIST,
                                       ARG_DELETE_CONFIRM, hidden);
        sb.append(getRepository().question(msgSB.toString(), form));
        sb.append("<ul>");
        new OutputHandler(getRepository(),"tmp").getEntryHtml(sb, entries, request, false,
                          false, true);
        sb.append("</ul>");
        return new Result(msg("Delete Confirm"), sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     */
    protected Result asynchDeleteEntries(Request request,
                                       final List<Entry> entries) {
        final Request theRequest = request;
        Entry         entry      = entries.get(0);
        /*
//        if (request.getCollectionEntry() != null) {
//            if (Misc.equals(entry.getId(),
//                            request.getCollectionEntry().getId())) {
//                request.setCollectionEntry(null);
//            }
            }*/
        Entry                group   = entries.get(0).getParentGroup();
        final String         groupId = entries.get(0).getParentGroupId();

        ActionManager.Action action  = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                deleteEntries(theRequest, entries, actionId);
            }
        };
        String href = HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW, group),
                                    "Continue");
        return getActionManager().doAction(request, action, "Deleting entry",
                                           "Continue: " + href);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     */
    protected Result makeEntryEditResult(Request request, Entry entry,
                                         String title, StringBuffer sb) {
        Result result = new Result(title, sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, (entry.isGroup()?getRepository().groupEditUrls:getRepository().entryEditUrls),
                                          "?" + ARG_ENTRYID + "="
                                          + entry.getId()));
        return result;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param asynchId _more_
     *
     * @throws Exception _more_
     */
    protected void deleteEntries(Request request, List<Entry> entries,
                                 Object asynchId)
            throws Exception {

        if (entries.size() == 0) {
            return;
        }
        delCnt = 0;
        Connection connection = getDatabaseManager().getNewConnection();
        try {
            deleteEntriesInner(request, entries, connection, asynchId);
        } finally {
            try {
                connection.close();
            } catch (Exception exc) {}
        }
        getRepository().clearCache();
    }




    /** _more_ */
    int delCnt = 0;



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param connection _more_
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    private void deleteEntriesInner(Request request, List<Entry> entries,
                                    Connection connection, Object actionId)
            throws Exception {

        List<String[]> found = getDescendents(request, entries, connection,
                                              true);
        String query;
        query = SqlUtil.makeDelete(Tables.PERMISSIONS.NAME,
                                   SqlUtil.eq(Tables.PERMISSIONS.COL_ENTRY_ID, "?"));

        PreparedStatement permissionsStmt =
            connection.prepareStatement(query);

        query = SqlUtil.makeDelete(
            Tables.ASSOCIATIONS.NAME,
            SqlUtil.makeOr(
                Misc.newList(
                    SqlUtil.eq(Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID, "?"),
                    SqlUtil.eq(Tables.ASSOCIATIONS.COL_TO_ENTRY_ID, "?"))));
        PreparedStatement assocStmt = connection.prepareStatement(query);

        query = SqlUtil.makeDelete(Tables.COMMENTS.NAME,
                                   SqlUtil.eq(Tables.COMMENTS.COL_ENTRY_ID, "?"));
        PreparedStatement commentsStmt = connection.prepareStatement(query);

        query = SqlUtil.makeDelete(Tables.METADATA.NAME,
                                   SqlUtil.eq(Tables.METADATA.COL_ENTRY_ID, "?"));
        PreparedStatement metadataStmt = connection.prepareStatement(query);


        PreparedStatement entriesStmt =
            connection.prepareStatement(SqlUtil.makeDelete(Tables.ENTRIES.NAME,
                Tables.ENTRIES.COL_ID, "?"));

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        int       deleteCnt = 0;
        int       totalDeleteCnt = 0;
        //Go backwards so we go up the tree and hit the children first
        List allIds = new ArrayList();
        for (int i = found.size() - 1; i >= 0; i--) {
            String[] tuple = found.get(i);
            String   id    = tuple[0];
            allIds.add(id);
            //            System.err.println ("id:" + id + " type:" + tuple[1] +" resource:" +tuple[2]);
            deleteCnt++;
            totalDeleteCnt++;
            if ((actionId != null)
                    && !getActionManager().getActionOk(actionId)) {
                getActionManager().setActionMessage(actionId,
                        "Delete canceled");
                connection.rollback();
                permissionsStmt.close();
                metadataStmt.close();
                commentsStmt.close();
                assocStmt.close();
                entriesStmt.close();
                return;
            }
            getActionManager().setActionMessage(actionId,
                    "Deleted:" + totalDeleteCnt + "/" + found.size() + " entries");
            if (totalDeleteCnt % 100 == 0) {
                System.err.println("Deleted:" + deleteCnt);
            }
            getStorageManager().removeFile(new Resource(new File(tuple[2]),
                    tuple[3]));

            permissionsStmt.setString(1, id);
            permissionsStmt.addBatch();

            metadataStmt.setString(1, id);
            metadataStmt.addBatch();

            commentsStmt.setString(1, id);
            commentsStmt.addBatch();

            assocStmt.setString(1, id);
            assocStmt.setString(2, id);
            assocStmt.addBatch();

            entriesStmt.setString(1, id);
            entriesStmt.addBatch();

            //TODO: Batch up the specific type deletes
            TypeHandler typeHandler = getRepository().getTypeHandler(tuple[1]);
            typeHandler.deleteEntry(request, statement, id);
            if (deleteCnt > 1000) {
                permissionsStmt.executeBatch();
                metadataStmt.executeBatch();
                commentsStmt.executeBatch();
                assocStmt.executeBatch();
                entriesStmt.executeBatch();
                deleteCnt = 0;
            }
        }

        permissionsStmt.executeBatch();
        metadataStmt.executeBatch();
        commentsStmt.executeBatch();
        assocStmt.executeBatch();
        entriesStmt.executeBatch();

        connection.commit();
        connection.setAutoCommit(true);

        for(int i=0;i<allIds.size();i++) {
            getStorageManager().deleteEntryDir((String) allIds.get(i));
        }

        permissionsStmt.close();
        metadataStmt.close();
        commentsStmt.close();
        assocStmt.close();
        entriesStmt.close();
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
    public Result processEntryNew(Request request) throws Exception {
        Group        group = findGroup(request);
        StringBuffer sb    = new StringBuffer();
        sb.append(makeEntryHeader(request, group));
        sb.append(HtmlUtil.p());
        sb.append(request.form(getRepository().URL_ENTRY_FORM));
        sb.append(msgLabel("Create a"));
        sb.append(HtmlUtil.space(1));
        sb.append(getRepository().makeTypeSelect(request, false,"",true));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.submit("Go"));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
        sb.append(HtmlUtil.formClose());
        sb.append(makeNewGroupForm(request, group, BLANK));

        /*
        sb.append(request.uploadForm(getRepository().URL_ENTRY_XMLCREATE));
        sb.append("File:" + HtmlUtil.fileInput(ARG_FILE, ""));
        sb.append("<br>" + HtmlUtil.submit("Submit"));
        sb.append(HtmlUtil.formClose());
        */


        return makeEntryEditResult(request, group, "Create Entry", sb);
        //        return new Result("New Form", sb, Result.TYPE_HTML);
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
    public Result processEntryGet(Request request) throws Exception {
        String entryId = (String) request.getId((String) null);
        
        if (entryId == null) {
            throw new IllegalArgumentException("No " + ARG_ENTRYID + " given");
        }
        Entry entry = getEntry(request, entryId);
        if (entry == null) {
            throw new RepositoryUtil.MissingEntryException(
                "Could not find entry with id:" + entryId);
        }

        if ( !entry.getResource().isUrl()) {
            if ( !getAccessManager().canDownload(request, entry)) {
                throw new IllegalArgumentException(
                    "Cannot download file with id:" + entryId);
            }
        }
        //        System.err.println("request:" + request);

        if (request.defined(ARG_IMAGEWIDTH)
                && ImageUtils.isImage(entry.getResource().getPath())) {
            int    width    = request.get(ARG_IMAGEWIDTH, 75);
            String thumbDir = getStorageManager().getThumbDir();
            String thumb = IOUtil.joinDir(thumbDir,
                                          "entry" + entry.getId() + "_"
                                          + width + ".jpg");
            if ( !new File(thumb).exists()) {
                Image image =
                    ImageUtils.readImage(entry.getResource().getPath());
                Image resizedImage = image.getScaledInstance(width, -1,
                                         Image.SCALE_AREA_AVERAGING);
                ImageUtils.waitOnImage(resizedImage);
                ImageUtils.writeImageToFile(resizedImage, thumb);
            }
            byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(thumb,
                               getClass()));
            return new Result(
                BLANK, bytes,
                IOUtil.getFileExtension(entry.getResource().getPath()));
        } else {
            return new Result(BLANK,
                              IOUtil.getInputStream(entry.getResource()
                                  .getPath(), getClass()), IOUtil
                                      .getFileExtension(entry.getResource()
                                          .getPath()));
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
    public Result processGetEntries(Request request) throws Exception {
        List<Entry> entries    = new ArrayList();
        boolean     doAll      = request.defined("getall");
        boolean     doSelected = request.defined("getselected");
        String      prefix     = (doAll
                                  ? "all_"
                                  : "entry_");

        for (Enumeration keys = request.keys(); keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if (doSelected) {
                if ( !request.get(id, false)) {
                    continue;
                }
            }
            if ( !id.startsWith(prefix)) {
                continue;
            }
            id = id.substring(prefix.length());
            Entry entry = getEntry(request, id);
            if (entry != null) {
                entries.add(entry);
            }
        }
        String ids = request.getIds((String) null);
        if (ids != null) {
            List<String> idList = StringUtil.split(ids, ",", true, true);
            for (String id : idList) {
                Entry entry = getEntry(request, id);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        entries = getAccessManager().filterEntries(request, entries);

        return getRepository().getOutputHandler(request).outputGroup(request,
                                getDummyGroup(), new ArrayList<Group>(),
                                entries);

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
    public Result processEntryCopy(Request request) throws Exception {

        String fromId = request.getString(ARG_FROM, "");
        if (fromId == null) {
            throw new IllegalArgumentException("No " + ARG_FROM + " given");
        }
        Entry fromEntry = getEntry(request, fromId);
        if (fromEntry == null) {
            throw new RepositoryUtil.MissingEntryException("Could not find entry "
                    + fromId);
        }


        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
        }


        if ( !request.exists(ARG_TO)) {
            StringBuffer sb     = new StringBuffer();
            List<Entry>  cart   = getUserManager().getCart(request);
            boolean      didOne = false;
            sb.append(makeEntryHeader(request, fromEntry));
            for (Entry entry : cart) {
                if ( !getAccessManager().canDoAction(request, entry,
                        Permission.ACTION_NEW)) {
                    continue;
                }
                if ( !entry.isGroup()) {
                    continue;
                }
                if ( !okToMove(fromEntry, entry)) {
                    continue;
                }
                if ( !didOne) {
                    sb.append(header("Move to:"));
                    sb.append("<ul>");
                }
                sb.append("<li> ");
                sb.append(HtmlUtil.href(request.url(getRepository().URL_ENTRY_COPY, ARG_FROM,
                        fromEntry.getId(), ARG_TO, entry.getId(), ARG_ACTION,
                        ACTION_MOVE), entry.getLabel()));
                sb.append(HtmlUtil.br());
                didOne = true;

            }
            if ( !didOne) {
                sb.append(
                    getRepository().note(msg(
                        "You need to add a destination group to your cart")));
            } else {
                sb.append("</ul>");
            }

            return new Result(msg("Entry Move/Copy"), sb);
        }


        String toId = request.getString(ARG_TO, "");
        if (toId == null) {
            throw new IllegalArgumentException("No " + ARG_TO + " given");
        }

        Entry toEntry = getEntry(request, toId);
        if (toEntry == null) {
            throw new RepositoryUtil.MissingEntryException("Could not find entry "
                    + toId);
        }
        if ( !toEntry.isGroup()) {
            throw new IllegalArgumentException(
                "Can only copy/move to a group");
        }
        Group toGroup = (Group) toEntry;


        if ( !getAccessManager().canDoAction(request, fromEntry,
                                             Permission.ACTION_EDIT)) {
            throw new RepositoryUtil.AccessException("Cannot move:" + fromEntry.getLabel());
        }


        if ( !getAccessManager().canDoAction(request, toEntry,
                                             Permission.ACTION_NEW)) {
            throw new RepositoryUtil.AccessException("Cannot copy to:" + toEntry.getLabel());
        }


        if ( !okToMove(fromEntry, toEntry)) {
            StringBuffer sb = new StringBuffer();
            sb.append(makeEntryHeader(request, fromEntry));
            sb.append(getRepository().error(msg("Cannot move a group to its descendent")));
            return new Result("", sb);
        }



        String action = request.getString(ARG_ACTION, ACTION_COPY);



        if ( !request.exists(ARG_MOVE_CONFIRM)) {
            StringBuffer sb = new StringBuffer();
            sb.append(msgLabel("Are you sure you want to move"));
            sb.append(HtmlUtil.br());
            sb.append(HtmlUtil.space(3));
            sb.append(fromEntry.getLabel());
            sb.append(HtmlUtil.br());
            sb.append(msgLabel("To"));
            sb.append(HtmlUtil.br());
            sb.append(HtmlUtil.space(3));
            sb.append(toEntry.getLabel());
            sb.append(HtmlUtil.br());

            String hidden = HtmlUtil.hidden(ARG_FROM, fromEntry.getId())
                            + HtmlUtil.hidden(ARG_TO, toEntry.getId())
                            + HtmlUtil.hidden(ARG_ACTION, action);
            String form = RepositoryUtil.makeOkCancelForm(request, getRepository().URL_ENTRY_COPY,
                                           ARG_MOVE_CONFIRM, hidden);
            return new Result(msg("Move confirm"),
                              new StringBuffer(getRepository().question(sb.toString(),
                                  form)));
        }


        Connection connection = getDatabaseManager().getNewConnection();
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        try {
            if (action.equals(ACTION_MOVE)) {
                fromEntry.setParentGroup(toGroup);
                String oldId = fromEntry.getId();
                String newId = oldId;
                //TODO: critical section around new group id
                //Don't do this for now
                if (false && fromEntry.isGroup()) {
                    newId = getGroupId(toGroup);
                    fromEntry.setId(newId);
                    String[] info = {
                        Tables.ENTRIES.NAME, Tables.ENTRIES.COL_ID, 
                        Tables.ENTRIES.NAME, Tables.ENTRIES.COL_PARENT_GROUP_ID, 
                        Tables.METADATA.NAME, Tables.METADATA.COL_ENTRY_ID, 
                        Tables.COMMENTS.NAME, Tables.COMMENTS.COL_ENTRY_ID, 
                        Tables.ASSOCIATIONS.NAME, Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID, 
                        Tables.ASSOCIATIONS.NAME, Tables.ASSOCIATIONS.COL_TO_ENTRY_ID, 
                        Tables.PERMISSIONS.NAME,  Tables.PERMISSIONS.COL_ENTRY_ID
                    };


                    for (int i = 0; i < info.length; i += 2) {
                        String sql = "UPDATE  " + info[i] + " SET "
                                     + SqlUtil.unDot(info[i + 1]) + " = "
                                     + SqlUtil.quote(newId) + " WHERE "
                                     + SqlUtil.eq(info[i + 1],
                                         SqlUtil.quote(oldId));
                        //                        System.err.println (sql);
                        statement.execute(sql);
                    }

                    //TODO: we also cache the group full names
                    /*                    synchronized(MUTEX_ENTRY) {
                        entryCache.remove(oldId);
                        entryCache.put(fromEntry.getId(), fromEntry);
                        groupCache.remove(fromEntry.getId());
                        groupCache.put(fromEntry.getId(), (Group) fromEntry);
                        }*/
                }

                //Change the parent
                String sql = "UPDATE  " + Tables.ENTRIES.NAME + " SET "
                             + SqlUtil.unDot(Tables.ENTRIES.COL_PARENT_GROUP_ID)
                             + " = "
                             + SqlUtil.quote(fromEntry.getParentGroupId())
                             + " WHERE "
                             + SqlUtil.eq(Tables.ENTRIES.COL_ID,
                                          SqlUtil.quote(fromEntry.getId()));
                statement.execute(sql);
                connection.commit();
                connection.setAutoCommit(true);
                getRepository().clearCache();
                return new Result(request.url(getRepository().URL_ENTRY_SHOW, ARG_ENTRYID,
                        fromEntry.getId()));
            }
        } finally {
            try {
                connection.close();
            } catch (Exception exc) {}
        }


        String       title = (action.equals(ACTION_COPY)
                              ? "Entry Copy"
                              : "Entry Move");
        StringBuffer sb    = new StringBuffer();
        return new Result(msg(title), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentGroup _more_
     * @param name _more_
     *
     * @return _more_
     */
    protected String makeNewGroupForm(Request request, Group parentGroup,
                                      String name) {
        StringBuffer sb = new StringBuffer();
        if ((parentGroup != null) && request.getUser().getAdmin()) {
            sb.append("<p>&nbsp;");
            sb.append(
                request.form(
                    getHarvesterManager().URL_HARVESTERS_IMPORTCATALOG));
            sb.append(HtmlUtil.hidden(ARG_GROUP, parentGroup.getId()));
            sb.append(msgLabel("Import a catalog"));
            sb.append(HtmlUtil.space(1));
            sb.append(HtmlUtil.input(ARG_CATALOG, BLANK, HtmlUtil.SIZE_70)
                      + HtmlUtil.space(1)
                      + HtmlUtil.checkbox(ARG_RECURSE, "true", false)
                      + " Recurse");
            sb.append(HtmlUtil.submit(msg("Go")));


            sb.append(HtmlUtil.formClose());
        }
        return sb.toString();
    }



    /**
     * _more_
     *
     * @param fromEntry _more_
     * @param toEntry _more_
     *
     * @return _more_
     */
    protected boolean okToMove(Entry fromEntry, Entry toEntry) {
        if ( !toEntry.isGroup()) {
            return false;
        }

        if (toEntry.getId().equals(fromEntry.getId())) {
            return false;
        }
        if (toEntry.getParentGroup() == null) {
            return true;
        }
        return okToMove(fromEntry, toEntry.getParentGroup());
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
    public Result processEntryXmlCreate(Request request) throws Exception {
        try {
            return processEntryXmlCreateInner(request);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (request.getString(ARG_OUTPUT,"").equals("xml")) {
                return new Result(XmlUtil.tag(TAG_RESPONSE,
                        XmlUtil.attr(ATTR_CODE, "error"),
                        "" + exc.getMessage()), MIME_XML);
            }
            return new Result("Error:" + exc, Result.TYPE_XML);
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
    private Result processEntryXmlCreateInner(Request request)
            throws Exception {
        String file = request.getUploadedFile(ARG_FILE);
        if (file == null) {
            throw new IllegalArgumentException("No file argument given");
        }
        String    entriesXml        = null;
        Hashtable origFileToStorage = new Hashtable();
        //        System.err.println ("\nprocessing");
        if (file.endsWith(".zip")) {
            ZipInputStream zin =
                new ZipInputStream(IOUtil.getInputStream(file));
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                String entryName = ze.getName();
                //                System.err.println ("ZIP: " + ze.getName());
                if (entryName.equals("entries.xml")) {
                    entriesXml = new String(IOUtil.readBytes(zin, null,
                            false));
                } else {
                    String name =
                        IOUtil.getFileTail(ze.getName().toLowerCase());
                    File f = getStorageManager().getTmpFile(request, name);
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtil.writeTo(zin, fos);
                    fos.close();
                    //                    System.err.println ("orig file:" + ze.getName() + " tmp file:" + f);
                    origFileToStorage.put(ze.getName(), f.toString());
                }
            }
            if (entriesXml == null) {
                throw new IllegalArgumentException(
                    "No entries.xml file provided");
            }
        }

        if (entriesXml == null) {
            entriesXml = IOUtil.readContents(file, getClass());
        }

        //        System.err.println ("xml:" + entriesXml);

        List      newEntries = new ArrayList();
        Hashtable entries    = new Hashtable();
        Element   root       = XmlUtil.getRoot(entriesXml);
        NodeList  children   = XmlUtil.getElements(root);

        Document  resultDoc  = XmlUtil.makeDocument();
        Element resultRoot = XmlUtil.create(resultDoc, TAG_RESPONSE, null,
                                            new String[] { ATTR_CODE,
                "ok" });
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            if (node.getTagName().equals(TAG_ENTRY)) {
                Entry entry = processEntryXml(request, node, entries,
                                  origFileToStorage,true);
                XmlUtil.create(resultDoc, TAG_ENTRY, resultRoot,
                               new String[] { ATTR_ID,
                        entry.getId() });
                newEntries.add(entry);
                if (XmlUtil.getAttribute(node, ATTR_ADDMETADATA, false)) {
                    List<Entry> tmpEntries =
                        (List<Entry>) Misc.newList(entry);
                    addInitialMetadata(request, tmpEntries);
                }

            } else if (node.getTagName().equals(TAG_ASSOCIATION)) {
                String id = processAssociationXml(request, node, entries,
                                origFileToStorage);
                XmlUtil.create(resultDoc, TAG_ASSOCIATION, resultRoot,
                               new String[] { ATTR_ID,
                        id });
            } else {
                throw new IllegalArgumentException("Unknown tag:"
                        + node.getTagName());
            }
        }


        insertEntries(newEntries, true);

        if (request.getString(ARG_OUTPUT,"").equals("xml")) {
            //TODO: Return a list of the newly created entries
            String xml = XmlUtil.toString(resultRoot);
            return new Result(xml, MIME_XML);
        }

        StringBuffer sb = new StringBuffer("OK");
        return new Result("", sb);

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param node _more_
     * @param entries _more_
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry processEntryXml(Request request, Element node,
                                  Hashtable entries, Hashtable files, boolean checkAccess)
            throws Exception {
        String name = XmlUtil.getAttribute(node, ATTR_NAME);
        String type = XmlUtil.getAttribute(node, ATTR_TYPE,
                                           TypeHandler.TYPE_FILE);
        String dataType    = XmlUtil.getAttribute(node, ATTR_DATATYPE, "");
        String description = XmlUtil.getAttribute(node, ATTR_DESCRIPTION, (String) null);
        if(description==null) {
            description = XmlUtil.getGrandChildText(node, TAG_DESCRIPTION);
        }
        if(description==null) {
            description="";
        }
        String file = XmlUtil.getAttribute(node, ATTR_FILE, (String) null);
        if (file != null) {
            String tmp = (String) files.get(file);
            String newFile = getStorageManager().moveToStorage(request,
                                 new File(tmp)).toString();
            file = newFile;
        }
        String url   = XmlUtil.getAttribute(node, ATTR_URL, (String) null);
        String tmpid = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
        String parentId = XmlUtil.getAttribute(node, ATTR_PARENT,
                                               getTopGroup().getId());
        Group parentGroup = (Group) entries.get(parentId);
        if (parentGroup == null) {
            parentGroup = (Group) getEntry(request, parentId);
            if (parentGroup == null) {
                throw new RepositoryUtil.MissingEntryException("Could not find parent:"
                        + parentId);
            }
        }
        if(checkAccess) {
            if ( !getAccessManager().canDoAction(request, parentGroup,
                                                 Permission.ACTION_NEW)) {
                throw new IllegalArgumentException("Cannot add to parent group:"
                                                   + parentId);
            }
        }

        TypeHandler typeHandler = getRepository().getTypeHandler(type);
        if (typeHandler == null) {
            throw new RepositoryUtil.MissingEntryException("Could not find type:" + type);
        }
        String   id = (typeHandler.isType(TypeHandler.TYPE_GROUP)
                       ? getGroupId(parentGroup)
                       : getRepository().getGUID());

        Resource resource;

        if (file != null) {
            resource = new Resource(file, Resource.TYPE_STOREDFILE);
        } else if (url != null) {
            resource = new Resource(url, Resource.TYPE_URL);
        } else {
            resource = new Resource("", Resource.TYPE_UNKNOWN);
        }
        Date createDate = new Date();
        Date fromDate   = createDate;
        //        System.err.println("node:" + XmlUtil.toString(node));
        if (XmlUtil.hasAttribute(node, ATTR_FROMDATE)) {
            fromDate = getRepository().parseDate(XmlUtil.getAttribute(node, ATTR_FROMDATE));
        }
        Date toDate = fromDate;
        if (XmlUtil.hasAttribute(node, ATTR_TODATE)) {
            toDate = getRepository().parseDate(XmlUtil.getAttribute(node, ATTR_TODATE));
        }

        if(!typeHandler.canBeCreatedBy(request)) {
            throw new IllegalArgumentException("Cannot create an entry of type " + typeHandler.getDescription());
        }
        Entry entry = typeHandler.createEntry(id);
        entry.initEntry(name, description, parentGroup, 
                        request.getUser(), resource, dataType,
                        createDate.getTime(), fromDate.getTime(),
                        toDate.getTime(), null);

        entry.setNorth(Misc.decodeLatLon(XmlUtil.getAttribute(node, ATTR_NORTH,entry.getNorth()+"")));
        entry.setSouth(Misc.decodeLatLon(XmlUtil.getAttribute(node, ATTR_SOUTH, entry.getSouth()+"")));
        entry.setEast(Misc.decodeLatLon(XmlUtil.getAttribute(node, ATTR_EAST, entry.getEast()+"")));
        entry.setWest(Misc.decodeLatLon(XmlUtil.getAttribute(node, ATTR_WEST, entry.getWest()+"")));
        NodeList entryChildren = XmlUtil.getElements(node);
        for (Element entryChild : (List<Element>) entryChildren) {
            String tag  = entryChild.getTagName();
            if (tag.equals(TAG_METADATA)) {
                getMetadataManager().processMetadataXml(entry, entryChild);
            } else if(tag.equals(TAG_DESCRIPTION)) {
            } else {
                throw new IllegalArgumentException("Unknown tag:"
                                                   + node.getTagName());
            }
        }
        entry.getTypeHandler().initializeEntry(request, entry, node);


        if (tmpid != null) {
            entries.put(tmpid, entry);
        }
        return entry;
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
    public String makeEntryHeader(Request request, Entry entry)
            throws Exception {
        String crumbs = getBreadCrumbs(request, entry, false)[1];
        return crumbs;
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
    public Result processCommentsShow(Request request) throws Exception {
        Entry        entry = getEntry(request);
        StringBuffer sb    = new StringBuffer();
        if (request.exists(ARG_MESSAGE)) {
            sb.append(getRepository().note(request.getUnsafeString(ARG_MESSAGE, BLANK)));
        }
        sb.append(makeEntryHeader(request, entry));
        sb.append("<p>");
        sb.append(getCommentHtml(request, entry));
        return  new OutputHandler(getRepository(),"tmp").makeLinksResult(request, msg("Entry Comments"), sb, new OutputHandler.State(entry));
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
    protected List<Comment> getComments(Request request, Entry entry)
            throws Exception {
        if (entry.getComments() != null) {
            return entry.getComments();
        }
        if (entry.isDummy()) {
            return new ArrayList<Comment>();
        }
        Statement stmt = getDatabaseManager().select(Tables.COMMENTS.COLUMNS,
                             Tables.COMMENTS.NAME,
                             Clause.eq(Tables.COMMENTS.COL_ENTRY_ID, entry.getId()),
                             " order by " + Tables.COMMENTS.COL_DATE + " asc ");
        SqlUtil.Iterator iter     = SqlUtil.getIterator(stmt);
        List<Comment>    comments = new ArrayList();
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                comments
                    .add(new Comment(results
                        .getString(1), entry, getUserManager()
                        .findUser(results
                                  .getString(3), true), 
                                     getDatabaseManager().getDate(results,4),
                                     results.getString(5), results.getString(6)));
            }
        }
        entry.setComments(comments);
        return comments;
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
    public Result processCommentsEdit(Request request) throws Exception {
        Entry entry = getEntry(request);
        getDatabaseManager().delete(Tables.COMMENTS.NAME,
                       Clause.eq(Tables.COMMENTS.COL_ID,
                                 request.getUnsafeString(ARG_COMMENT_ID,
                                     BLANK)));
        entry.setComments(null);
        return new Result(request.url(getRepository().URL_COMMENTS_SHOW, ARG_ENTRYID,
                                      entry.getId(), ARG_MESSAGE,
                                      "Comment deleted"));
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
    public Result processCommentsAdd(Request request) throws Exception {
        Entry        entry = getEntry(request);
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.url(getRepository().URL_COMMENTS_SHOW, ARG_ENTRYID,
                                          entry.getId()));
        }

        StringBuffer sb    = new StringBuffer();
        sb.append(makeEntryHeader(request, entry));
        if (request.exists(ARG_MESSAGE)) {
            sb.append(getRepository().note(request.getUnsafeString(ARG_MESSAGE, BLANK)));
        }


        String subject = BLANK;
        String comment = BLANK;
        subject = request.getEncodedString(ARG_SUBJECT, BLANK).trim();
        comment = request.getEncodedString(ARG_COMMENT, BLANK).trim();
        if (comment.length() == 0) {
            sb.append(getRepository().warning(msg("Please enter a comment")));
        } else {
            getDatabaseManager().executeInsert(Tables.COMMENTS.INSERT,
                                               new Object[]{
                                                   getRepository().getGUID(),
                                                   entry.getId(),
                                                   request.getUser().getId(),
                                                   new Date(),
                                                   subject,
                                                   comment});
            //Now clear out the comments in the cached entry
            entry.setComments(null);
            return new Result(request.url(getRepository().URL_COMMENTS_SHOW, ARG_ENTRYID,
                                          entry.getId(), ARG_MESSAGE,
                                          "Comment added"));
        }

        sb.append(msgLabel("Add comment for") + getEntryLink(request, entry));
        sb.append(request.form(getRepository().URL_COMMENTS_ADD, BLANK));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Subject"),
                                     HtmlUtil.input(ARG_SUBJECT, subject,
                                         HtmlUtil.SIZE_40)));
        sb.append(HtmlUtil.formEntryTop(msgLabel("Comment"),
                                        HtmlUtil.textArea(ARG_COMMENT,
                                            comment, 5, 40)));
        sb.append(
            HtmlUtil.formEntry(
                BLANK,
                RepositoryUtil.buttons(
                    HtmlUtil.submit(msg("Add Comment")),
                    HtmlUtil.submit(msg("Cancel"), ARG_CANCEL))));
        sb.append(HtmlUtil.formTableClose());
        sb.append(HtmlUtil.formClose());
        return new OutputHandler(getRepository(),"tmp").makeLinksResult(request, msg("Entry Comments"), sb, new OutputHandler.State(entry));
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
    public String getCommentHtml(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);
        boolean canComment = getAccessManager().canDoAction(request, entry,
                                 Permission.ACTION_COMMENT);

        StringBuffer  sb       = new StringBuffer();
        List<Comment> comments = getComments(request, entry);


        if (canComment) {
            sb.append(request.form(getRepository().URL_COMMENTS_ADD, BLANK));
            sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtil.submit("Add Comment", ARG_ADD));
            sb.append(HtmlUtil.formClose());
        }


        if (comments.size() == 0) {
            sb.append("<br>");
            sb.append(msg("No comments"));
        }
        //        sb.append("<table>");
        int rowNum = 1;
        for (Comment comment : comments) {
            //            sb.append(HtmlUtil.formEntry(BLANK, HtmlUtil.hr()));
            //TODO: Check for access
            String deleteLink = ( !canEdit
                                  ? ""
                                  : HtmlUtil
                                      .href(request
                                          .url(getRepository().URL_COMMENTS_EDIT, ARG_DELETE,
                                              "true", ARG_ENTRYID, entry.getId(),
                                              ARG_COMMENT_ID,
                                              comment.getId()), HtmlUtil
                                                  .img(getRepository().fileUrl(ICON_DELETE),
                                                      msg(
                                                      "Delete comment"))));
            if (canEdit) {
                //                sb.append(HtmlUtil.formEntry(BLANK, deleteLink));
            }
            //            sb.append(HtmlUtil.formEntry("Subject:", comment.getSubject()));


            String theClass = HtmlUtil.cssClass("listrow" + rowNum);
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
            StringBuffer content = new StringBuffer();
            content.append("<table>");
            String byLine = "By: " + comment.getUser().getLabel() + " @ "
                            + formatDate(request, comment.getDate())
                            + HtmlUtil.space(1) + deleteLink;
            //            content.append(HtmlUtil.formEntry("By:",
            //                                         ));
            //            System.err.println("Comment: " + comment.getComment());
            content.append(HtmlUtil.formEntryTop("", comment.getComment()));
            content.append("</table>");
            sb.append(HtmlUtil.div(getRepository().makeShowHideBlock(request,
                    "<b>Subject</b>:" + comment.getSubject()
                    + HtmlUtil.space(2) + byLine, content, true,
                        ""), theClass));
        }
        //        sb.append("</table>");
        return sb.toString();
    }






    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryLink(Request request, Entry entry) {
        return getEntryLink(request, entry, new ArrayList());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     */
    protected String getEntryLink(Request request, Entry entry, List args) {
        return HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry, args),
                             entry.getLabel());
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getAjaxLink(Request request, Entry entry) throws Exception {
        return getAjaxLink(request, entry, entry.getLabel(), true);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param includeIcon _more_
     *
     * @return _more_
     */
    protected String getAjaxLink(Request request, Entry entry,
                                 String linkText, boolean includeIcon) throws Exception {
        StringBuffer sb = new StringBuffer();
        String entryId = entry.getId();
        if (includeIcon) {
            boolean okToMove = !request.getUser().getAnonymous();
            String  icon     = getIconUrl(entry);
            String dropEvent = HtmlUtil.onMouseUp("mouseUpOnEntry(event,'"
                                   + entry.getId() + "')");
            String event = (entry.isGroup()
                            ? HtmlUtil.onMouseClick("folderClick('"
                                + entryId + "')")
                            : "");

            if (okToMove) {
                event += (entry.isGroup()
                          ? HtmlUtil.onMouseOver("mouseOverOnEntry(event," + HtmlUtil.squote(entryId)+")")
                          : "") + HtmlUtil.onMouseOut(
                                                      "mouseOutOnEntry(event," + 
                                                      HtmlUtil.squote(entryId)+ ")") + 
                    HtmlUtil.onMouseDown("mouseDownOnEntry(event," + HtmlUtil.squote(entryId)    + "," + 
                                         HtmlUtil.squote(entry.getLabel().replace("'", ""))  + ");") + (entry.isGroup()
                        ? dropEvent
                        : "");
            }


            String img = HtmlUtil.img(icon, (entry.isGroup()
                                             ? "Click to open group; "
                                             : "") + (okToMove
                    ? "Drag to move"
                    : ""), " id=" + HtmlUtil.quote("img_" + entryId)
                           + event);
            if (entry.isGroup()) {
                //                sb.append("<a href=\"JavaScript: noop()\" " + event +"/>" +      img +"</a>");
                sb.append(img);
            } else {
                sb.append(img);
            }
            sb.append(HtmlUtil.space(1));
            getMetadataManager().decorateEntry(request, entry, sb,true);
        }

        String elementId = entry.getId();
        String qid = HtmlUtil.squote(elementId);
        String tooltipEvents =  HtmlUtil.onMouseOver("tooltip.onMouseOver(event," + qid+ ");") + 
            HtmlUtil.onMouseOut("tooltip.onMouseOut(event," + qid+ ");") +
            HtmlUtil.onMouseMove("tooltip.onMouseMove(event," + qid+ ");");
        sb.append(
            HtmlUtil.href(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                linkText,
                " id=" + HtmlUtil.quote(elementId) + " " +tooltipEvents));

        if (includeIcon) {
            //            getMetadataManager().decorateEntry(request, entry, sb,true);
        }


        return HtmlUtil.span(sb.toString(),
                             " id="
                             + HtmlUtil.quote("span_" + entry.getId()));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param forMenu _more_
     * @param forHeader _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Link> getEntryLinks(Request request, Entry entry,
                                       boolean forHeader)
            throws Exception {
        List<Link> links = new ArrayList<Link>();
        if ( !forHeader) {
            entry.getTypeHandler().getEntryLinks(request, entry, links,
                    forHeader);
            //            if(!forHeader)
            //                links.add(new Link(true));
            for (OutputHandler outputHandler : getRepository().getOutputHandlers()) {
                outputHandler.getEntryLinks(request, entry, links, forHeader);
            }
            //            if(!forHeader)
            //                links.add(new Link(true));
        }
        OutputHandler outputHandler = getRepository().getOutputHandler(request);
        if ( !entry.isTopGroup()) {
            links.addAll(outputHandler.getNextPrevLinks(request, entry,
                    request.getOutput()));
        }
        return links;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param forHeader _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryLinksHtml(Request request, Entry entry,
                                       boolean forHeader)
            throws Exception {
        return StringUtil.join(HtmlUtil.space(1),
                               getEntryLinks(request, entry, forHeader));
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
    protected String getEntryLinksList(Request request, Entry entry)
            throws Exception {
        List<Link>   links = getEntryLinks(request, entry, false);
        StringBuffer menu  = new StringBuffer();
        menu.append("<table cellspacing=\"0\" cellpadding=\"0\">");
        for (Link link : links) {
            if (link.hr) {
                menu.append("<tr><td colspan=2><hr class=menuseparator>");
            } else {
                menu.append("<tr><td>");
                menu.append(HtmlUtil.img(link.getIcon()));
                menu.append(HtmlUtil.space(1));
                menu.append("</td><td>");
                menu.append(HtmlUtil.href(link.getUrl(), link.getLabel(),
                                          HtmlUtil.cssClass("menulink")));
            }
            menu.append("</td></tr>");
        }
        menu.append("</table>");
        return menu.toString();

    }


    protected String getEntryLinksToolbar(Request request, Entry entry)
        throws Exception {
        List<Link>   links = getEntryLinks(request, entry, false);
        StringBuffer sb  = new StringBuffer();
        for (Link link : links) {
            String href = HtmlUtil.href(link.getUrl(), HtmlUtil.img(link.getIcon(), link.getLabel(),link.getLabel()));
            sb.append(HtmlUtil.inset(href,0,3,0,0));
        }
        return sb.toString();
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return A 2 element array.  First element is the title to use. Second is the links
     *
     * @throws Exception _more_
     */
    public String getBreadCrumbs(Request request, Entry entry)
            throws Exception {
        return getBreadCrumbs(request, entry,null);
    }

    public String getBreadCrumbs(Request request, Entry entry, RequestUrl requestUrl)
            throws Exception {
        if (entry == null) {
            return BLANK;
        }
        List breadcrumbs = new ArrayList();
        Group parent = findGroup(request, entry.getParentGroupId());
        int   length = 0;
        while (parent != null) {
            if (length > 100) {
                breadcrumbs.add(0, "...");
                break;
            }
            String name = parent.getName();
            if (name.length() > 20) {
                name = name.substring(0, 19) + "...";
            }
            length += name.length();
            String link =  (requestUrl==null?
                            getAjaxLink(request, parent, name, false):
                            HtmlUtil.href(request.entryUrl(requestUrl,parent),name));
            breadcrumbs.add(0, link);
            //            breadcrumbs.add(0, HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
            //                    parent), name));
            parent = findGroup(request, parent.getParentGroupId());
        }
        if(requestUrl==null) {
            breadcrumbs.add(getAjaxLink(request, entry, entry.getLabel(), false));
        } else {
            breadcrumbs.add(HtmlUtil.href(request.entryUrl(requestUrl,entry),entry.getLabel()));
        }
        //        breadcrumbs.add(HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
        //                entry), entry.getLabel()));
        //        breadcrumbs.add(HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
        //                entry), entry.getLabel()));
        String separator = getProperty("ramadda.breadcrumbs.separator","");
        return StringUtil.join(HtmlUtil.pad("&gt;"), breadcrumbs);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param makeLinkForLastGroup _more_
     * @param extraArgs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getBreadCrumbs(Request request, Entry entry,
                                   boolean makeLinkForLastGroup)
            throws Exception {
        return getBreadCrumbs(request, entry, makeLinkForLastGroup,null);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param makeLinkForLastGroup _more_
     * @param extraArgs _more_
     * @param stopAt _more_
     *
     * @return A 2 element array.  First element is the title to use. Second is the links
     *
     * @throws Exception _more_
     */
    public String[] getBreadCrumbs(Request request, Entry entry,
                                   boolean makeLinkForLastGroup,
                                   Group stopAt)
            throws Exception {
        if (request == null) {
            request = new Request(getRepository(), "", new Hashtable());
        }

        List breadcrumbs = new ArrayList();
        List titleList   = new ArrayList();
        if (entry == null) {
            return new String[] { BLANK, BLANK };
        }
        Group  parent = findGroup(request, entry.getParentGroupId());
        OutputType output =  OutputHandler.OUTPUT_HTML;
        int length = 0;
        while (parent != null) {
            if ((stopAt != null)
                    && parent.getFullName().equals(stopAt.getFullName())) {
                break;
            }
            if (length > 100) {
                titleList.add(0, "...");
                breadcrumbs.add(0, "...");
                break;
            }
            String name = parent.getName();
            if (name.length() > 20) {
                name = name.substring(0, 19) + "...";
            }
            length += name.length();
            titleList.add(0, name);
            String link =  getAjaxLink(request, parent, name, false);
            breadcrumbs.add(0, link);
            parent = findGroup(request, parent.getParentGroupId());
        }
        titleList.add(entry.getLabel());
        String nav;
        String separator = getProperty("ramadda.breadcrumbs.separator","");
        String entryLink =  getAjaxLink(request, entry, entry.getLabel(), false);
        if (makeLinkForLastGroup) {
            breadcrumbs.add(entryLink);
            nav = StringUtil.join(separator, breadcrumbs);
            nav = HtmlUtil.div(nav, HtmlUtil.cssClass("breadcrumbs"));
        } else {
            nav = StringUtil.join(separator, breadcrumbs);
            String toolbar = getEntryLinksToolbar(request, entry);
            /***
            StringBuffer menu = new StringBuffer();
            menu.append(
                HtmlUtil.div(
                    getEntryLinksList(request, entry),
                    HtmlUtil.id("entrylinksmenu" + entry.getId())
                    + HtmlUtil.cssClass("menu")));
            String compId = "menubutton" + entry.getId();
            String events = HtmlUtil.onMouseOver(
                                "setImage(" + HtmlUtil.squote(compId) + ",'"
                                + getRepository().fileUrl(ICON_GRAYRECTARROW)
                                + "')") + HtmlUtil.onMouseOut(
                                    "setImage(" + HtmlUtil.squote(compId)
                                    + ",'" + getRepository().fileUrl(ICON_GRAYRECT)
                                    + "')") + HtmlUtil.onMouseClick(
                                        "showMenu(event, "
                                        + HtmlUtil.squote(compId) + ", "
                                        + HtmlUtil.squote(
                                            "entrylinksmenu"
                                            + entry.getId()) + ")");
            String menuLink = HtmlUtil.space(1)
                              + HtmlUtil.jsLink(events,
                                  HtmlUtil.img(getRepository().fileUrl(ICON_GRAYRECT),
                                      msg("Show menu"), HtmlUtil.id(compId)));

            ***/
            String linkHtml = getEntryLinksHtml(request, entry, true);
            linkHtml = toolbar;
            String header =
                "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
                + HtmlUtil.rowBottom("<td class=\"entryname\" >" + entryLink
                                     + "</td><td align=\"right\">"
                                     + linkHtml + "</td>") + "</table>";
            nav = HtmlUtil.div(
                HtmlUtil.div(nav, HtmlUtil.cssClass("breadcrumbs")) + header,
                HtmlUtil.cssClass("entryheader"));

        }
        String title = StringUtil.join(HtmlUtil.pad("&gt;"), titleList);
        return new String[] { title, nav };
    }







    /**
     * _more_
     *
     *
     * @param entryId _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(Request request, String entryId)
            throws Exception {
        return getEntry(request, entryId, true);
    }

    /**
     * _more_
     *
     * @param entryId _more_
     * @param request _more_
     * @param andFilter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(Request request, String entryId,
                             boolean andFilter)
            throws Exception {
        return getEntry(request, entryId, andFilter, false);
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
    protected Entry getEntry(Request request) throws Exception {
        String entryId = request.getString(ARG_ENTRYID, BLANK);
        Entry  entry   = getEntry(request, entryId);
        if (entry == null) {
            Entry tmp = getEntry(request, request.getString(ARG_ENTRYID, BLANK),
                                 false);
            if (tmp != null) {
                throw new RepositoryUtil.AccessException(
                    "You do not have access to this entry");
            }
            throw new RepositoryUtil.MissingEntryException("Could not find entry:"
                    + request.getString(ARG_ENTRYID, BLANK));
        }
        return entry;
    }





    /**
     * _more_
     *
     * @param entryId _more_
     * @param request _more_
     * @param andFilter _more_
     * @param abbreviated _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(Request request, String entryId,
                             boolean andFilter, boolean abbreviated)
            throws Exception {


        if (entryId == null) {
            return null;
        }
        if (entryId.equals(getTopGroup().getId())) {
            return getTopGroup();
        }

        synchronized(MUTEX_ENTRY) {
        Entry entry = (Entry) entryCache.get(entryId);
        if (entry != null) {
            if ( !andFilter) {
                return entry;
            }
            return getAccessManager().filterEntry(request, entry);
        }

        //catalog:url:dataset:datasetid
        if(entryId.startsWith("catalog:")) {
            CatalogTypeHandler typeHandler = (CatalogTypeHandler) getRepository().getTypeHandler(TypeHandler.TYPE_CATALOG);
            entry = typeHandler.makeSynthEntry(request, null, entryId);
        } else  if (isSynthEntry(entryId)) {
            String[] pair = getSynthId(entryId);
            String parentEntryId = pair[0];
            Entry parentEntry = getEntry(request, parentEntryId, andFilter, abbreviated);
            if(parentEntry == null) return null;
            TypeHandler typeHandler = parentEntry.getTypeHandler();
            entry =typeHandler.makeSynthEntry(request, parentEntry, pair[1]);
            if(entry == null) return null;
        } else {
            Statement entryStmt =
                getDatabaseManager().select(Tables.ENTRIES.COLUMNS, Tables.ENTRIES.NAME,
                                            Clause.eq(Tables.ENTRIES.COL_ID,
                                                      entryId));

            ResultSet results = entryStmt.getResultSet();
            if ( !results.next()) {
                entryStmt.close();
                return null;
            }

            TypeHandler typeHandler = getRepository().getTypeHandler(results.getString(2));
            entry = typeHandler.getEntry(results, abbreviated);
            entryStmt.close();

        }
        if ( !abbreviated && (entry != null)) {
            if (entryCache.size() > ENTRY_CACHE_LIMIT) {
                entryCache = new Hashtable();
            }
            entryCache.put(entryId, entry);
        }

        if (andFilter) {
            entry = getAccessManager().filterEntry(request, entry);
        }

        return entry;
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
    protected List[] getEntries(Request request) throws Exception {
        return getEntries(request, new StringBuffer());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteriaSB _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List[] getEntries(Request request,
                                StringBuffer searchCriteriaSB)
            throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        List<Clause> where = typeHandler.assembleWhereClause(request,
                                 searchCriteriaSB);
        int skipCnt = request.get(ARG_SKIP, 0);

        Statement statement = typeHandler.select(request, Tables.ENTRIES.COLUMNS,
                                  where,
                                  getRepository().getQueryOrderAndLimit(request, false));


        SqlUtil.debug = false;

        List<Entry>      entries = new ArrayList<Entry>();
        List<Entry>      groups  = new ArrayList<Entry>();
        ResultSet        results;
        SqlUtil.Iterator iter       = SqlUtil.getIterator(statement);
        boolean canDoSelectOffset   =
            getDatabaseManager().canDoSelectOffset();
        Hashtable        seen       = new Hashtable();
        List<Entry>      allEntries = new ArrayList<Entry>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if ( !canDoSelectOffset && (skipCnt-- > 0)) {
                    continue;
                }
                String id    = results.getString(1);
                Entry  entry = (Entry) entryCache.get(id);
                if (entry == null) {
                    //id,type,name,desc,group,user,file,createdata,fromdate,todate
                    TypeHandler localTypeHandler =
                        getRepository().getTypeHandler(results.getString(2));
                    entry = localTypeHandler.getEntry(results);
                    entryCache.put(entry.getId(), entry);
                }
                if (seen.get(entry.getId()) != null) {
                    continue;
                }
                seen.put(entry.getId(), BLANK);
                allEntries.add(entry);
            }
        }



        for (Entry entry : allEntries) {
            if (entry.isGroup()) {
                groups.add(entry);
            } else {
                entries.add(entry);
            }
        }

        entries = getAccessManager().filterEntries(request, entries);
        groups  = getAccessManager().filterEntries(request, groups);


        return new List[] { groups, entries };
    }



    public boolean isSynthEntry(String id) {
        return id.startsWith(ID_PREFIX_SYNTH);
    }

    public String[] getSynthId(String id) {
        id = id.substring(ID_PREFIX_SYNTH.length());
        String[]pair = StringUtil.split(id, ":", 2);
        if(pair == null) return new String[]{id,null};
        return pair;
    }



    public void clearSeenResources() {
        seenResources = new Hashtable();
    }




    /** _more_ */
    private Hashtable seenResources = new Hashtable();



    /**
     * _more_
     *
     * @param harvester _more_
     * @param typeHandler _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processEntries(Harvester harvester,
                                  TypeHandler typeHandler,
                                  List<Entry> entries, boolean makeThemUnique)
            throws Exception {
        if(makeThemUnique) {
            entries = getUniqueEntries(entries);
        }
        insertEntries(entries, true, true);
        return true;
    }




    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_b
     */
    public void addNewEntry(Entry entry) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        insertEntries(entries, true);
    }





    /**
     * _more_
     *
     * @param entry _more_
     * @param statement _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    protected void setStatement(Entry entry, PreparedStatement statement,
                                boolean isNew)
            throws Exception {
        int col = 1;
        //id,type,name,desc,group,user,file,createdata,fromdate,todate
        statement.setString(col++, entry.getId());
        statement.setString(col++, entry.getType());
        statement.setString(col++, entry.getName());
        statement.setString(col++, entry.getDescription());
        statement.setString(col++, entry.getParentGroupId());
        //        statement.setString(col++, entry.getCollectionGroupId());
        statement.setString(col++, entry.getUser().getId());
        if (entry.getResource() == null) {
            entry.setResource(new Resource());
        }
        statement.setString(col++, getStorageManager().resourceToDB(entry.getResource().getPath()));
        statement.setString(col++, entry.getResource().getType());
        statement.setString(col++, entry.getDataType());
        getDatabaseManager().setDate(statement,col++, getRepository().currentTime());
        try {
            getDatabaseManager().setDate(statement, col,entry.getStartDate());
            getDatabaseManager().setDate(statement, col+1,entry.getEndDate());
        } catch (Exception exc) {
            System.err.println("Error: Bad date " + entry.getResource() + " "
                               + new Date(entry.getStartDate()));
            getDatabaseManager().setDate(statement,col, new Date());
            getDatabaseManager().setDate(statement, col+1, new Date());
        }
        col += 2;
        statement.setDouble(col++, entry.getSouth());
        statement.setDouble(col++, entry.getNorth());
        statement.setDouble(col++, entry.getEast());
        statement.setDouble(col++, entry.getWest());
        if ( !isNew) {
            statement.setString(col++, entry.getId());
        }
    }




    /**
     * _more_
     *
     * @param entries _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void insertEntries(List<Entry> entries, boolean isNew)
            throws Exception {
        insertEntries(entries, isNew, false);
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param isNew _more_
     * @param canBeBatched _more_
     *
     * @throws Exception _more_
     */
    public void insertEntries(List<Entry> entries, boolean isNew,
                              boolean canBeBatched)
            throws Exception {

        if (entries.size() == 0) {
            return;
        }
        if ( !isNew) {
            clearCache();
        }


        //We have our own connection
        Connection connection = getDatabaseManager().getNewConnection();
        try {
            insertEntriesInner(entries, connection, isNew, canBeBatched);
        } finally {
            try {
                connection.close();
            } catch (Exception exc) {}
        }
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param connection _more_
     * @param isNew _more_
     * @param canBeBatched _more_
     *
     * @throws Exception _more_
     */
    private void insertEntriesInner(List<Entry> entries,
                                    Connection connection, boolean isNew,
                                    boolean canBeBatched)
            throws Exception {

        if (entries.size() == 0) {
            return;
        }
        if ( !isNew) {
            clearCache();
        }

        long              t1          = System.currentTimeMillis();
        int               cnt         = 0;
        int               metadataCnt = 0;

        PreparedStatement entryStmt   = connection.prepareStatement(isNew
                ? Tables.ENTRIES.INSERT
                : Tables.ENTRIES.UPDATE);

        PreparedStatement metadataStmt =
            connection.prepareStatement(Tables.METADATA.INSERT);


        Hashtable typeStatements = new Hashtable();

        int       batchCnt       = 0;
        connection.setAutoCommit(false);
        for (Entry entry : entries) {
          
// if (entry.isCollectionGroup()) {
//                getTopGroup()s = null;
//                }
            TypeHandler typeHandler = entry.getTypeHandler();
            String      sql         = typeHandler.getInsertSql(isNew);
            //            System.err.println("sql:" + sql);
            PreparedStatement typeStatement = null;
            if (sql != null) {
                typeStatement = (PreparedStatement) typeStatements.get(sql);
                if (typeStatement == null) {
                    typeStatement = connection.prepareStatement(sql);
                    typeStatements.put(sql, typeStatement);
                }
            }
            //           System.err.println ("entry: " + entry.getId());
            setStatement(entry, entryStmt, isNew);
            batchCnt++;
            entryStmt.addBatch();

            if (typeStatement != null) {
                batchCnt++;
                typeHandler.setStatement(entry, typeStatement, isNew);
                typeStatement.addBatch();
            }


            List<Metadata> metadataList = entry.getMetadata();
            if (metadataList != null) {
                if ( !isNew) {
                    getDatabaseManager().delete(Tables.METADATA.NAME,
                                   Clause.eq(Tables.METADATA.COL_ENTRY_ID,
                                             entry.getId()));
                }
                for (Metadata metadata : metadataList) {
                    //                    System.err.println ("\tmetadata:" + metadata.getEntryId() +" " + metadata.getType() + " " + metadata.getAttr1());
                    int col = 1;
                    metadataCnt++;
                    metadataStmt.setString(col++, metadata.getId());
                    metadataStmt.setString(col++, entry.getId());
                    metadataStmt.setString(col++, metadata.getType());
                    metadataStmt.setInt(col++, metadata.getInherited()
                            ? 1
                            : 0);
                    metadataStmt.setString(col++, metadata.getAttr1());
                    metadataStmt.setString(col++, metadata.getAttr2());
                    metadataStmt.setString(col++, metadata.getAttr3());
                    metadataStmt.setString(col++, metadata.getAttr4());
                    metadataStmt.addBatch();
                    batchCnt++;

                }
            }

            if (batchCnt > 1000) {
                //                    if(isNew)
                entryStmt.executeBatch();
                //                    else                        entryStmt.executeUpdate();
                if (metadataCnt > 0) {
                    metadataStmt.executeBatch();
                }
                for (Enumeration keys = typeStatements.keys();
                        keys.hasMoreElements(); ) {
                    typeStatement = (PreparedStatement) typeStatements.get(
                        keys.nextElement());
                    //                        if(isNew)
                    typeStatement.executeBatch();
                    //                        else                            typeStatement.executeUpdate();
                }
                batchCnt    = 0;
                metadataCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryStmt.executeBatch();
            metadataStmt.executeBatch();
            for (Enumeration keys = typeStatements.keys();
                    keys.hasMoreElements(); ) {
                PreparedStatement typeStatement =
                    (PreparedStatement) typeStatements.get(
                        keys.nextElement());
                typeStatement.executeBatch();
            }
        }
        connection.commit();
        connection.setAutoCommit(true);


        long t2 = System.currentTimeMillis();
        totalTime    += (t2 - t1);
        totalEntries += entries.size();
        if (t2 > t1) {
            //System.err.println("added:" + entries.size() + " entries in " + (t2-t1) + " ms  Rate:" + (entries.size()/(t2-t1)));
            double seconds = totalTime / 1000.0;
            //            if ((totalEntries % 100 == 0) && (seconds > 0)) {
            if (seconds > 0) {
                //                System.err.println(totalEntries + " average rate:"
                //                 + (int) (totalEntries / seconds)
                //                 + "/second");
            }
        }


        entryStmt.close();
        metadataStmt.close();
        for (Enumeration keys =
                typeStatements.keys(); keys.hasMoreElements(); ) {
            PreparedStatement typeStatement =
                (PreparedStatement) typeStatements.get(keys.nextElement());
            typeStatement.close();
        }

        connection.close();
        Misc.run(getRepository(), "checkNewEntries", entries);
    }





    /** _more_ */
    long totalTime = 0;

    /** _more_ */
    int totalEntries = 0;



    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getUniqueEntries(List<Entry> entries)
            throws Exception {
        List<Entry> needToAdd = new ArrayList();
        String      query     = BLANK;
        try {
            if (entries.size() == 0) {
                return needToAdd;
            }
            if (seenResources.size() > 10000) {
                seenResources = new Hashtable();
            }
            Connection connection = getDatabaseManager().getConnection();
            PreparedStatement select = 
                SqlUtil.getSelectStatement(
                                           connection, "count(" + Tables.ENTRIES.COL_ID + ")",
                    Misc.newList(Tables.ENTRIES.NAME),
                    Clause.and(
                        Clause.eq(Tables.ENTRIES.COL_RESOURCE, ""),
                        Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID, "?")), "");
            long t1 = System.currentTimeMillis();
            for (Entry entry : entries) {
                String path = getStorageManager().resourceToDB(entry.getResource().getPath());
                String key  = entry.getParentGroup().getId() + "_" + path;
                if (seenResources.get(key) != null) {
                    //                    System.out.println("seen resource:" + path);
                    continue;
                }
                seenResources.put(key, key);
                select.setString(1, path);
                select.setString(2, entry.getParentGroup().getId());
                //                select.addBatch();
                ResultSet results = select.executeQuery();
                if (results.next()) {
                    int found = results.getInt(1);
                    if (found == 0) {
                        needToAdd.add(entry);
                    } else {
                        //                        System.out.println("in db:" + path + " " + entry.getParentGroup().getId());
                    }
                }
            }
            select.close();
            getDatabaseManager().releaseConnection(connection);
            long t2 = System.currentTimeMillis();
            //            System.err.println("Took:" + (t2 - t1) + "ms to check: "
            //                               + entries.size() + " entries");
        } catch (Exception exc) {
            log("Processing:" + query, exc);
            throw exc;
        }
        return needToAdd;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryResourceUrl(Request request, Entry entry) {
        String fileTail = getStorageManager().getFileTail(entry);
        return HtmlUtil.url(request.url(getRepository().URL_ENTRY_GET) + "/"
                            + fileTail, ARG_ENTRYID, entry.getId());
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Group getDummyGroup() throws Exception {
        Group dummyGroup = new Group(getRepository().getGroupTypeHandler(), true);
        dummyGroup.setId(getRepository().getGUID());
        dummyGroup.setUser(getUserManager().getAnonymousUser());
        return dummyGroup;
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<String> getChildIds(Request request, Group group,
            List<Clause> where)
            throws Exception {
        List<String> ids = new ArrayList<String>();


        boolean isSynthEntry = isSynthEntry(group.getId());
        if (group.getTypeHandler().isSynthType() || isSynthEntry) {
            String synthId =null;
            if(isSynthEntry) {
                String[] pair = getSynthId(group.getId());
                String entryId = pair[0];
                synthId = pair[1];
                group = (Group)getEntry(request, entryId, false, false);
                if(group == null) {
                    return ids; 
                }
            } 
            return group.getTypeHandler().getSynthIds(request, group, synthId);
        }


        where = new ArrayList<Clause>(where);
        where.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID, group.getId()));
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        int         skipCnt     = request.get(ARG_SKIP, 0);
        Statement statement = typeHandler.select(request, Tables.ENTRIES.COL_ID,
                                  where,
                                  getRepository().getQueryOrderAndLimit(request, true));
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        ResultSet        results;
        boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if ( !canDoSelectOffset && (skipCnt-- > 0)) {
                    continue;
                }
                ids.add(results.getString(1));
            }
        }

        group.addChildrenIds(ids);
        return ids;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGroupShow(Request request, Group group)
            throws Exception {
        boolean       doLatest      = request.get(ARG_LATEST, false);

        OutputHandler outputHandler = getRepository().getOutputHandler(request);
        TypeHandler   typeHandler   = getRepository().getTypeHandler(request);
        List<Clause>  where         =
            typeHandler.assembleWhereClause(request);

        List<Entry>   entries       = new ArrayList<Entry>();
        List<Group>   subGroups     = new ArrayList<Group>();
        try {
            List<String> ids = getChildIds(request, group, where);
            for (String id : ids) {
                Entry entry = getEntry(request, id);

                if (entry == null) {
                    continue;
                }
                if (entry.isGroup()) {
                    subGroups.add((Group) entry);
                } else {
                    entries.add(entry);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            request.put(ARG_MESSAGE,
                        "Error finding children:" + exc.getMessage());
        }

        if (doLatest) {
            if (entries.size() > 0) {
                entries = sortEntriesOnDate(entries, true);
                return outputHandler.outputEntry(request, entries.get(0));
            }
        }


        return outputHandler.outputGroup(request, group, subGroups, entries);
    }





    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    protected List<Entry> sortEntriesOnDate(List<Entry> entries,
                                            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                if (e1.getStartDate() < e2.getStartDate()) {
                    return (descending
                            ? 1
                            : -1);
                }
                if (e1.getStartDate() > e2.getStartDate()) {
                    return (descending
                            ? -1
                            : 1);
                }
                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);
        return (List<Entry>) Misc.toList(array);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void addInitialMetadata(Request request, List<Entry> entries)
            throws Exception {
        for (Entry theEntry : entries) {
            Hashtable extra = new Hashtable();
            getMetadataManager().getMetadata(theEntry);
            getMetadataManager().addInitialMetadata(request, theEntry, extra);
            if ( !theEntry.hasAreaDefined()
                    && (extra.get(ARG_MINLAT) != null)) {
                theEntry.setSouth(Misc.getProperty(extra, ARG_MINLAT, 0.0));
                theEntry.setNorth(Misc.getProperty(extra, ARG_MAXLAT, 0.0));
                theEntry.setWest(Misc.getProperty(extra, ARG_MINLON, 0.0));
                theEntry.setEast(Misc.getProperty(extra, ARG_MAXLON, 0.0));
                theEntry.trimAreaResolution();
            }
            if ((extra.get(ARG_FROMDATE) != null)
                    && (theEntry.getStartDate()
                        == theEntry.getCreateDate())) {
                //                System.err.println ("got dttm:" + extra.get(ARG_FROMDATE));
                theEntry.setStartDate(
                    ((Date) extra.get(ARG_FROMDATE)).getTime());
                theEntry.setEndDate(((Date) extra.get(ARG_TODATE)).getTime());
            }
        }
    }





    public Entry parseEntryXml(File xmlFile) throws Exception {
        Element   root       = XmlUtil.getRoot(IOUtil.readContents(xmlFile));
        return processEntryXml(new Request(getRepository(), getUserManager().getDefaultUser()), root,new Hashtable(), new Hashtable(),false);
    }

    public  Entry getTemplateEntry(File file) throws Exception {
        File xmlFile = new File(IOUtil.joinDir(file.getParentFile(),"." + file.getName() +".ramadda"));
        Entry fileInfoEntry = null;
        if(xmlFile.exists()) {
            fileInfoEntry = parseEntryXml(xmlFile);
            if(fileInfoEntry.getName().length()==0) {
                fileInfoEntry.setName(file.getName());
            }
        }
        return fileInfoEntry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryText(Request request, Entry entry, String s)
            throws Exception {
        //<attachment name>
        if (s.indexOf("<attachment") >= 0) {
            List<Association> associations = getEntryManager().getAssociations(request, entry);
            for (Association association : associations) {
                if ( !association.getFromId().equals(entry.getId())) {
                    continue;
                }
            }
        }
        return s;
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroup(Request request, String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Group group = groupCache.get(id);
        if (group != null) {
            return group;
        }

        if (isSynthEntry(id) || id.startsWith("catalog:")) {
            return (Group) getEntry(request, id);
        }


        Statement statement = getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
                                                          Tables.ENTRIES.NAME,
                                                          Clause.eq(Tables.ENTRIES.COL_ID, id));

        List<Group> groups = readGroups(statement);
        if (groups.size() > 0) {
            return groups.get(0);
        }
        return null;
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroupFromName(String name, User user,
                                      boolean createIfNeeded)
            throws Exception {
        return findGroupFromName(name, user, createIfNeeded, false);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryWithName(Request request, Group parent, String name)
            throws Exception {
        String groupName = ((parent == null)
                            ? ""
                            : parent.getFullName()) + Group.IDDELIMITER
                                + name;
        Group group = groupCache.get(groupName);
        if (group != null) {
            return group;
        }
        String[] ids = SqlUtil.readString(
                           getDatabaseManager().select(
                               Tables.ENTRIES.COL_ID, Tables.ENTRIES.NAME,
                               Clause.and(
                                   Clause.eq(
                                       Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                       parent.getId()), Clause.eq(
                                           Tables.ENTRIES.COL_NAME, name))));
        if (ids.length == 0) {
            return null;
        }
        return getEntry(request, ids[0], false);
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     * @param isTop _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Group findGroupFromName(String name, User user,
                                    boolean createIfNeeded, boolean isTop)
            throws Exception {
        synchronized (MUTEX_GROUP) {
            String topGroupName = (topGroup!=null?topGroup.getName():GROUP_TOP);
            if ( !name.equals(topGroupName)
                    && !name.startsWith(topGroupName + Group.PATHDELIMITER)) {
                name = topGroupName + Group.PATHDELIMITER + name;
            }
            Group group = groupCache.get(name);
            if (group != null) {
                return group;
            }
            //            System.err.println("Looking for:" + name);

            List<String> toks = (List<String>) StringUtil.split(name,
                                    Group.PATHDELIMITER, true, true);
            Group  parent = null;
            String lastName;
            if ((toks.size() == 0) || (toks.size() == 1)) {
                lastName = name;
            } else {
                lastName = toks.get(toks.size() - 1);
                toks.remove(toks.size() - 1);
                parent = findGroupFromName(StringUtil.join(Group.PATHDELIMITER,
                        toks), user, createIfNeeded);
                if (parent == null) {
                    if ( !isTop) {
                        return null;
                    }
                    return getTopGroup();
                }
            }
            List<Clause> clauses = new ArrayList<Clause>();
            clauses.add(Clause.eq(Tables.ENTRIES.COL_TYPE, TypeHandler.TYPE_GROUP));
            if (parent != null) {
                clauses.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                      parent.getId()));
            } else {
                clauses.add(Clause.isNull(Tables.ENTRIES.COL_PARENT_GROUP_ID));
            }
            clauses.add(Clause.eq(Tables.ENTRIES.COL_NAME, lastName));
            Statement statement =
                getDatabaseManager().select(Tables.ENTRIES.COLUMNS, Tables.ENTRIES.NAME,
                                            clauses);
            List<Group> groups = readGroups(statement);
            statement.close();
            if (groups.size() > 0) {
                group = groups.get(0);
            } else {
                if ( !createIfNeeded) {
                    return null;
                }
                return makeNewGroup(parent, lastName, user);
            }
            groupCache.put(group.getId(), group);
            groupCache.put(group.getFullName(), group);
            return group;
        }
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Group makeNewGroup(Group parent, String name, User user)
            throws Exception {
        return makeNewGroup(parent, name, user,null);
    }



    public Group makeNewGroup(Group parent, String name, User user, Entry template)
            throws Exception {
        synchronized (MUTEX_GROUP) {
            TypeHandler typeHandler = getRepository().getTypeHandler(TypeHandler.TYPE_GROUP);
            Group       group = new Group(getGroupId(parent), typeHandler);
            if(template!=null) {
                group.initWith(template);
                getRepository().getMetadataManager().newEntry(group);
            } else {
                group.setName(name);
                group.setDate(new Date().getTime());
            }
            group.setParentGroup(parent);
            group.setUser(user);
            addNewEntry(group);
            groupCache.put(group.getId(), group);
            groupCache.put(group.getFullName(), group);
            return group;
        }
    }


    /**
     * _more_
     *
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getGroupId(Group parent) throws Exception {
        //FOr now just use regular ids for groups
        if(true) return getRepository().getGUID();

        int    baseId = 0;
        Clause idClause;
        String idWhere;
        if (parent == null) {
            idClause = Clause.isNull(Tables.ENTRIES.COL_PARENT_GROUP_ID);
        } else {
            idClause = Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID, parent.getId());
        }
        String newId = null;
        while (true) {
            if (parent == null) {
                newId = BLANK + baseId;
            } else {
                newId = parent.getId() + Group.IDDELIMITER + baseId;
            }

            Statement stmt = getDatabaseManager().select(Tables.ENTRIES.COL_ID,
                                 Tables.ENTRIES.NAME, new Clause[] { idClause,
                    Clause.eq(Tables.ENTRIES.COL_ID, newId) });
            ResultSet idResults = stmt.getResultSet();

            if ( !idResults.next()) {
                break;
            }
            baseId++;
        }
        return newId;

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     *
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(Request request, Clause clause)
            throws Exception {

        List<Clause> clauses = new ArrayList<Clause>();
        if (clause != null) {
            clauses.add(clause);
        }
        clauses.add(Clause.eq(Tables.ENTRIES.COL_TYPE, TypeHandler.TYPE_GROUP));
        Statement statement = getDatabaseManager().select(Tables.ENTRIES.COL_ID,
                                  Tables.ENTRIES.NAME, clauses);
        return getGroups(request, SqlUtil.readString(statement, 1));
    }

    /**
     * _more_
     *
     *
     *
     * @param request _more_
     * @param groupIds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(Request request, String[] groupIds)
            throws Exception {
        List<Group> groupList = new ArrayList<Group>();
        for (int i = 0; i < groupIds.length; i++) {
            Group group = findGroup(request, groupIds[i]);
            if (group != null) {
                groupList.add(group);
            }
        }
        return groupList;
    }





    /** _more_ */
    List<Group> topGroups;

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getTopGroups(Request request) throws Exception {
        if (topGroups != null) {
            return topGroups;
        }
        //        System.err.println("ramadda: getTopGroups " + topGroup);

        Statement statement = getDatabaseManager().select(Tables.ENTRIES.COL_ID,
                                  Tables.ENTRIES.NAME,
                                  Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                            getTopGroup().getId()));
        String[]    ids    = SqlUtil.readString(statement, 1);
        List<Group> groups = new ArrayList<Group>();
        for (int i = 0; i < ids.length; i++) {
            //Get the entry but don't check for access control
            Entry e = getEntry(request, ids[i], false);
            if (e == null) {
                continue;
            }
            if ( !e.isGroup()) {
                continue;
            }
            Group g = (Group) e;
            groups.add(g);
        }
        //For now don't check for access control
        //        return topGroups = new ArrayList<Group>(
        //            toGroupList(getAccessManager().filterEntries(request, groups)));
        return topGroups = new ArrayList<Group>(groups);
    }

    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     */
    private List<Group> toGroupList(List<Entry> entries) {
        List<Group> groups = new ArrayList<Group>();
        for (Entry entry : entries) {
            groups.add((Group) entry);
        }
        return groups;
    }



    /**
     * _more_
     *
     * @param statement _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Group> readGroups(Statement statement) throws Exception {
        ResultSet        results;
        SqlUtil.Iterator iter        = SqlUtil.getIterator(statement);
        List<Group>      groups      = new ArrayList<Group>();
        TypeHandler      typeHandler = getRepository().getTypeHandler(TypeHandler.TYPE_GROUP);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                Group group = (Group) typeHandler.getEntry(results);
                groups.add(group);
                groupCache.put(group.getId(), group);
            }
        }
        for (Group group : groups) {
            if (group.getParentGroupId() != null) {
                Group parentGroup =
                    (Group) groupCache.get(group.getParentGroupId());
                group.setParentGroup(parentGroup);
            }
            groupCache.put(group.getFullName(), group);
        }

        if (groupCache.size() > ENTRY_CACHE_LIMIT) {
            groupCache = new Hashtable();
        }
        return groups;
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
    public Group findGroup(Request request) throws Exception {
        String groupNameOrId = (String) request.getString(ARG_GROUP,
                                   (String) null);
        if (groupNameOrId == null) {
            groupNameOrId = (String) request.getString(ARG_ENTRYID,
                                                       (String) null);
        }
        if (groupNameOrId == null) {
            throw new IllegalArgumentException("No group specified");
        }
        Entry entry = getEntry(request, groupNameOrId, false);
        if (entry != null) {
            if ( !entry.isGroup()) {
                throw new IllegalArgumentException("Not a group:"
                        + groupNameOrId);
            }
            return (Group) entry;
        }
        throw new RepositoryUtil.MissingEntryException("Could not find group:"
                                           + groupNameOrId);
    }




    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getIconUrl(Entry entry) {
        return entry.getTypeHandler().getIconUrl(entry);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getPathFromEntry(Entry entry) {
        String name = entry.getName();
        name = name.toLowerCase();
        name = name.replace(" ", "_");
        name = name.replace(">", "_");
        return name;
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initGroups() throws Exception {
        Statement statement = getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
                                                          Tables.ENTRIES.NAME,
                                                          Clause.isNull(Tables.ENTRIES.COL_PARENT_GROUP_ID));


        

        List<Group> groups = readGroups(statement);
        if(groups.size()>0) {
            topGroup = groups.get(0);
        }

        //Make the top group if needed
        if (topGroup == null) {
            topGroup = findGroupFromName(GROUP_TOP,
                                         getUserManager().getDefaultUser(),
                                         true, true);

            getAccessManager().initTopGroup(topGroup);
        }
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     * @param connection _more_
     * @param firstCall _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected List<String[]> getDescendents(Request request,
                                          List<Entry> entries,
                                          Connection connection,
                                          boolean firstCall)
            throws Exception {
        List<String[]> children = new ArrayList();
        for (Entry entry : entries) {
            if (firstCall) {
                children.add(new String[] { entry.getId(),
                                            entry.getTypeHandler().getType(),
                                            entry.getResource().getPath(),
                                            entry.getResource().getType() });
            }
            if ( !entry.isGroup()) {
                continue;
            }
            Statement stmt = SqlUtil.select(connection,
                                            SqlUtil.comma(new String[] {
                                                Tables.ENTRIES.COL_ID,
                    Tables.ENTRIES.COL_TYPE, Tables.ENTRIES.COL_RESOURCE,
                    Tables.ENTRIES.COL_RESOURCE_TYPE }), Misc.newList(
                        Tables.ENTRIES.NAME), Clause.eq(
                        Tables.ENTRIES.COL_PARENT_GROUP_ID, entry.getId()));

            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    int    col          = 1;
                    String childId      = results.getString(col++);
                    String childType    = results.getString(col++);
                    String resource     = getStorageManager().resourceFromDB(results.getString(col++));
                    String resourceType = results.getString(col++);
                    children.add(new String[] { childId, childType, resource,
                            resourceType });
                    if (childType.equals(TypeHandler.TYPE_GROUP)) {
                        children.addAll(getDescendents(request,
                                (List<Entry>) Misc.newList(findGroup(request,
                                    childId)), connection, false));
                    }
                }
            }
        }
        return children;
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
    public Result processAssociationAdd(Request request) throws Exception {
        Entry fromEntry = getEntryManager().getEntry(request,
                                   request.getString(ARG_FROM, BLANK));
        Entry toEntry = getEntryManager().getEntry(request, request.getString(ARG_TO, BLANK));
        if (fromEntry == null) {
            throw new RepositoryUtil.MissingEntryException("Could not find entry:"
                    + request.getString(ARG_FROM, BLANK));
        }
        if (toEntry == null) {
            throw new RepositoryUtil.MissingEntryException("Could not find entry:"
                    + request.getString(ARG_TO, BLANK));
        }
        String name = request.getString(ARG_NAME, (String) null);
        if (name != null) {
            addAssociation(request, fromEntry, toEntry, name);
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
        }

        StringBuffer sb = new StringBuffer();
        sb.append(header("Add assocation"));
        sb.append("Add association between " + fromEntry.getLabel());
        sb.append(" and  " + toEntry.getLabel());
        sb.append(request.form(getRepository().URL_ASSOCIATION_ADD, BLANK));
        sb.append(HtmlUtil.br());
        sb.append("Association Name: ");
        sb.append(HtmlUtil.input(ARG_NAME));
        sb.append(HtmlUtil.hidden(ARG_FROM, fromEntry.getId()));
        sb.append(HtmlUtil.hidden(ARG_TO, toEntry.getId()));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.submit("Add Association"));
        sb.append("</form>");

        return new Result("Add Association", sb);

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
    public Result processAssociationDelete(Request request) throws Exception {
        String associationId = request.getString(ARG_ASSOCIATION, "");
        Clause clause = Clause.eq(Tables.ASSOCIATIONS.COL_ID, associationId);
        List<Association> associations = getAssociations(request, clause);
        if (associations.size() == 0) {
            return new Result(
                msg("Delete Associations"),
                new StringBuffer(getRepository().error("Could not find assocation")));
        }

        Entry fromEntry = getEntryManager().getEntry(request, associations.get(0).getFromId());
        Entry toEntry   = getEntryManager().getEntry(request, associations.get(0).getToId());

        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
        }


        if (request.exists(ARG_DELETE_CONFIRM)) {
            getDatabaseManager().delete(Tables.ASSOCIATIONS.NAME, clause);
            fromEntry.setAssociations(null);
            toEntry.setAssociations(null);
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
        }
        StringBuffer sb = new StringBuffer();
        String form = RepositoryUtil.makeOkCancelForm(request, getRepository().URL_ASSOCIATION_DELETE,
                                       ARG_DELETE_CONFIRM,
                                       HtmlUtil.hidden(ARG_ASSOCIATION,
                                           associationId));
        sb.append(
            getRepository().question(
                msg("Are you sure you want to delete the assocation?"),
                form));

        sb.append(associations.get(0).getName());
        sb.append(HtmlUtil.br());
        sb.append(fromEntry.getLabel());
        sb.append(HtmlUtil.pad(HtmlUtil.img(fileUrl(ICON_ARROW))));
        sb.append(toEntry.getLabel());
        return new Result(msg("Delete Associations"),     sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param node _more_
     * @param entries _more_
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String processAssociationXml(Request request, Element node,
                                         Hashtable entries, Hashtable files)
            throws Exception {

        String fromId    = XmlUtil.getAttribute(node, ATTR_FROM);
        String toId      = XmlUtil.getAttribute(node, ATTR_TO);
        Entry  fromEntry = (Entry) entries.get(fromId);
        Entry  toEntry   = (Entry) entries.get(toId);
        if (fromEntry == null) {
            fromEntry = getEntryManager().getEntry(request, fromId);
            if (fromEntry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find from entry:" + fromId);
            }
        }
        if (toEntry == null) {
            toEntry = getEntryManager().getEntry(request, toId);
            if (toEntry == null) {
                throw new RepositoryUtil.MissingEntryException("Could not find to entry:"
                        + toId);
            }
        }
        return addAssociation(request, fromEntry, toEntry,
                              XmlUtil.getAttribute(node, ATTR_NAME));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param fromEntry _more_
     * @param toEntry _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String addAssociation(Request request, Entry fromEntry,
                                  Entry toEntry, String name)
            throws Exception {
        if ( !getAccessManager().canDoAction(request, fromEntry,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add association to "
                    + fromEntry);
        }
        if ( !getAccessManager().canDoAction(request, toEntry,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add association to "
                    + toEntry);
        }


        PreparedStatement assocInsert = getDatabaseManager().getPreparedStatement(Tables.ASSOCIATIONS.INSERT);
        int    col = 1;
        String id  = getRepository().getGUID();
        assocInsert.setString(col++, id);
        assocInsert.setString(col++, name);
        assocInsert.setString(col++, "");
        assocInsert.setString(col++, fromEntry.getId());
        assocInsert.setString(col++, toEntry.getId());
        assocInsert.execute();
        assocInsert.close();
        fromEntry.setAssociations(null);
        toEntry.setAssociations(null);
        return id;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param association _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getAssociationLinks(Request request, String association)
            throws Exception {
        if (true) {
            return BLANK;
        }
        String search = HtmlUtil.href(
                            request.url(
                                getRepository().URL_SEARCH_FORM, ARG_ASSOCIATION,
                                getRepository().encode(association)), HtmlUtil.img(
                                    fileUrl(ICON_SEARCH),
                                    msg("Search in association")));

        return search;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Association> getAssociations(Request request,
            String entryId)
            throws Exception {
        Entry entry = getEntryManager().getEntry(request, entryId);
        if (entry == null) {
            System.err.println("Entry is null:" + entryId);
        }
        return getAssociations(request, entry);
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
    protected List<Association> getAssociations(Request request, Entry entry)
            throws Exception {
        if (entry.getAssociations() != null) {
            return entry.getAssociations();
        }
        if (entry.isDummy()) {
            return new ArrayList<Association>();
        }

        entry.setAssociations(
            getAssociations(
                request,
                Clause.or(
                    Clause.eq(Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID, entry.getId()),
                    Clause.eq(Tables.ASSOCIATIONS.COL_TO_ENTRY_ID, entry.getId()))));
        return entry.getAssociations();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Association> getAssociations(Request request,
            Clause clause)
            throws Exception {
        Statement stmt = getDatabaseManager().select(Tables.ASSOCIATIONS.COLUMNS,
                             Tables.ASSOCIATIONS.NAME, clause);
        List<Association> associations = new ArrayList();
        SqlUtil.Iterator  iter         = SqlUtil.getIterator(stmt);
        ResultSet         results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                associations.add(new Association(results.getString(1),
                        results.getString(2), results.getString(3),
                        results.getString(4), results.getString(5)));
            }
        }
        return associations;
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
    public String[] getAssociations(Request request) throws Exception {
        TypeHandler  typeHandler = getRepository().getTypeHandler(request);
        List<Clause> where       = typeHandler.assembleWhereClause(request);
        if (where.size() > 0) {
            where.add(0, Clause.eq(Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID,
                                   Tables.ENTRIES.COL_ID));
            where.add(0, Clause.eq(Tables.ASSOCIATIONS.COL_TO_ENTRY_ID,
                                   Tables.ENTRIES.COL_ID));
        }

        return SqlUtil.readString(typeHandler.select(request,
                SqlUtil.distinct(Tables.ASSOCIATIONS.COL_NAME), where, ""), 1);
    }



    public String processText(Request request, Entry entry, String text) {
        int idx = text.indexOf("<more>");
        if(idx>=0) {
            String first = text.substring(0,idx);
            String base = ""+(Repository.blockCnt++);
            String divId = "morediv_" + base;
            String linkId = "morelink_" + base;
            String second = text.substring(idx+"<more>".length());
            String moreLink  = "javascript:showMore(" + HtmlUtil.squote(base) +")";
            String lessLink  = "javascript:hideMore(" + HtmlUtil.squote(base) +")";
            text = first+"<br><a " + HtmlUtil.id(linkId) +" href=" + HtmlUtil.quote(moreLink) +">More...</a><div style=\"\" class=\"moreblock\" " + HtmlUtil.id(divId)+">" + second +
                "<br>" +
                "<a href=" + HtmlUtil.quote(lessLink) +">...Less</a>" +
                "</div>";
        }
        return text;
    }




}


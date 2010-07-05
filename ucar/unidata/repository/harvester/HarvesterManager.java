/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository.harvester;


import org.w3c.dom.*;


import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.database.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;



import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.lang.reflect.*;

import java.sql.ResultSet;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class HarvesterManager extends RepositoryManager {

    /** _more_ */
    public RequestUrl URL_HARVESTERS_IMPORTCATALOG =
        new RequestUrl(this, "/harvester/importcatalog", "Import Catalog");

    /** _more_ */
    public RequestUrl URL_HARVESTERS_LIST = new RequestUrl(this,
                                                "/harvester/list",
                                                "Harvesters");

    /** _more_ */
    public RequestUrl URL_HARVESTERS_NEW = new RequestUrl(this,
                                               "/harvester/new");


    /** _more_ */
    public RequestUrl URL_HARVESTERS_EDIT = new RequestUrl(this,
                                                "/harvester/edit");


    /** _more_ */
    private List<Harvester> harvesters = new ArrayList();

    /** _more_ */
    private Hashtable harvesterMap = new Hashtable();

    List<TwoFacedObject> harvesterTypes = new ArrayList<TwoFacedObject>();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public HarvesterManager(Repository repository) {
        super(repository);
        addHarvesterType(PatternHarvester.class);
        addHarvesterType(WebHarvester.class);
        addHarvesterType(DirectoryHarvester.class);
    }

    
    public void addHarvesterType(Class c)  {
        try {
            Constructor ctor = Misc.findConstructor(c,
                                                    new Class[] { Repository.class,
                                                                  String.class });
            if(ctor == null) {
                throw new IllegalArgumentException("Could not find constructor for harvester:" + c.getName());
            }
            Harvester dummy =
                (Harvester) ctor.newInstance(new Object[] {
                        getRepository(),
                        ""});
            harvesterTypes.add(new TwoFacedObject(dummy.getDescription(), c.getName()));
        } catch(Exception exc) {
            logError("Error creating harvester: " + c.getName(), exc);
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected Harvester findHarvester(String id) {
        if (id == null) {
            return null;
        }
        for (Harvester harvester : harvesters) {
            if (harvester.getId().equals(id)) {
                return harvester;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected List<Harvester> getHarvesters() {
        return harvesters;
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void initHarvesters() throws Exception {
        List<String> harvesterFiles =
            getRepository().getResourcePaths(PROP_HARVESTERS);
        boolean okToStart =
            getRepository().getProperty(PROP_HARVESTERS_ACTIVE, true);

        harvesters = new ArrayList<Harvester>();


        SqlUtil.Iterator iter = getDatabaseManager().getIterator(
                                    getDatabaseManager().select(
                                        Tables.HARVESTERS.COLUMNS,
                                        Tables.HARVESTERS.NAME,
                                        new Clause()));;
        ResultSet results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                String id        = results.getString(1);
                String className = results.getString(2);
                String content   = results.getString(3);

                Class  c         = null;

                try {
                    c = Misc.findClass(className);
                } catch (ClassNotFoundException cnfe1) {
                    className = className.replace("repository.",
                            "repository.harvester.");
                    try {
                        c = Misc.findClass(className);
                    }  catch (ClassNotFoundException cnfe2) {
                        getRepository().getLogManager().logError("HarvesterManager: Could not load harvester class: " + className);
                        continue;
                    }
                }
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class,
                        String.class });
                Harvester harvester =
                    (Harvester) ctor.newInstance(new Object[] {
                        getRepository(),
                        id });

                harvester.initFromContent(content);
                harvesters.add(harvester);
                harvesterMap.put(harvester.getId(), harvester);

            }
        }



        try {
            for (String file : harvesterFiles) {
                Element root = XmlUtil.getRoot(file, getClass());
                if (root == null) {
                    continue;
                }
                try {
                    List<Harvester> newHarvesters =
                        Harvester.createHarvesters(getRepository(), root);
                    harvesters.addAll(newHarvesters);
                    for (Harvester harvester : newHarvesters) {
                        harvesterMap.put(harvester.getId(), harvester);
                    }
                } catch (Exception exc) {
                    logError("Error loading harvester file:" + file, exc);
                }
            }
        } catch (Exception exc) {
            logError("Error loading harvester file", exc);
        }


        for (Harvester harvester : harvesters) {
            File rootDir = harvester.getRootDir();
            if (rootDir != null) {
                getStorageManager().addDownloadDirectory(rootDir);
            }
            if ( !okToStart) {
                harvester.setActive(false);
            } else if (harvester.getActiveOnStart()) {
                Misc.run(harvester, "run");
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
    public Result processNew(Request request) throws Exception {

        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.url(URL_HARVESTERS_LIST));
        }

        Harvester harvester = null;
        if (request.exists(ARG_HARVESTER_XMLFILE)) {
            String file = request.getUploadedFile(ARG_HARVESTER_XMLFILE);
            if ((file == null) || !new File(file).exists()) {
                return getAdmin().makeResult(
                    request, msg("New Harvester"),
                    new StringBuffer("You must specify a file"));
            }
            String xml = getStorageManager().readSystemResource(file);
            List<Harvester> harvesters =
                Harvester.createHarvesters(getRepository(),
                                           XmlUtil.getRoot(xml));
            if (harvesters.size() == 0) {
                return getAdmin().makeResult(
                    request, msg("New Harvester"),
                    new StringBuffer("No harvesters defined"));
            }
            harvester = harvesters.get(0);
            harvester.setId(getRepository().getGUID());

        } else if (request.exists(ARG_NAME)) {
            String id = getRepository().getGUID();
            Class c = Misc.findClass(request.getString(ARG_HARVESTER_CLASS));
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    String.class });
            harvester = (Harvester) ctor.newInstance(new Object[] {
                getRepository(),
                id });
            harvester.setName(request.getString(ARG_NAME, ""));
            harvester.setUser(request.getUser());
        }

        if (harvester != null) {
            harvester.setIsEditable(true);
            harvesters.add(harvester);
            harvesterMap.put(harvester.getId(), harvester);

            getDatabaseManager().executeInsert(Tables.HARVESTERS.INSERT,
                    new Object[] { harvester.getId(),
                                   harvester.getClass().getName(),
                                   harvester.getContent() });
            return new Result(request.url(URL_HARVESTERS_EDIT,
                                          ARG_HARVESTER_ID,
                                          harvester.getId()));
        }


        sb.append(RepositoryUtil.header("Create new harvester"));

        sb.append(request.formPost(URL_HARVESTERS_NEW));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ARG_NAME, "",
                                         HtmlUtil.SIZE_40)));
        String typeInput =
            HtmlUtil.select(
                ARG_HARVESTER_CLASS,
                harvesterTypes);
        sb.append(HtmlUtil.formEntry(msgLabel("Type"), typeInput));
        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit(msg("Create"))
                                     + HtmlUtil.space(1)
                                     + HtmlUtil.submit(msg("Cancel"),
                                         ARG_CANCEL)));

        sb.append(HtmlUtil.formClose());

        sb.append(HtmlUtil.formEntry(HtmlUtil.p(), ""));
        sb.append(HtmlUtil.uploadForm(URL_HARVESTERS_NEW.toString(), ""));

        sb.append(
            HtmlUtil.row(
                HtmlUtil.colspan(HtmlUtil.b("Or upload xml file"), 2)));
        sb.append(
            HtmlUtil.formEntry(
                msgLabel("File"),
                HtmlUtil.fileInput(ARG_HARVESTER_XMLFILE, HtmlUtil.SIZE_70)));

        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit(msg("Upload"))
                                     + HtmlUtil.space(1)
                                     + HtmlUtil.submit(msg("Cancel"),
                                         ARG_CANCEL)));
        sb.append(HtmlUtil.formClose());
        sb.append(HtmlUtil.formTableClose());

        return getAdmin().makeResult(request, msg("New Harvester"), sb);

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
    public Result processEdit(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        Harvester harvester =
            findHarvester(request.getString(ARG_HARVESTER_ID));
        if (harvester == null) {
            throw new IllegalArgumentException("Could not find harvester");
        }
        if (request.get(ARG_HARVESTER_GETXML, false)) {
            String xml = harvester.getContent();
            xml = XmlUtil.tag(Harvester.TAG_HARVESTERS, "", xml);
            return new Result("",
                              new StringBuffer(XmlUtil.getHeader() + "\n"
                                  + xml), "text/xml");
        }

        if ( !harvester.getIsEditable()) {
            throw new IllegalArgumentException("Cannot edit harvester");
        }
        sb.append(header(msgLabel("Harvester") + harvester.getName()));
        sb.append(request.formPost(URL_HARVESTERS_EDIT));
        sb.append(HtmlUtil.hidden(ARG_HARVESTER_ID, harvester.getId()));
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.url(URL_HARVESTERS_LIST));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            harvesterMap.remove(harvester.getId());
            harvesters.remove(harvester);
            getDatabaseManager().delete(Tables.HARVESTERS.NAME,
                                        Clause.eq(Tables.HARVESTERS.COL_ID,
                                            harvester.getId()));
            return new Result(request.url(URL_HARVESTERS_LIST));
        } else if (request.exists(ARG_DELETE)) {
            sb.append(
                getRepository().showDialogQuestion(
                    msg("Are you sure you want to delete the harvester"),
                    RepositoryUtil.buttons(
                        HtmlUtil.submit(msg("Yes"), ARG_DELETE_CONFIRM),
                        HtmlUtil.submit(msg("Cancel"), ARG_CANCEL_DELETE))));
        } else {
            if (request.exists(ARG_CHANGE)) {
                harvester.applyEditForm(request);
                getDatabaseManager().delete(
                    Tables.HARVESTERS.NAME,
                    Clause.eq(Tables.HARVESTERS.COL_ID, harvester.getId()));
                getDatabaseManager().executeInsert(Tables.HARVESTERS.INSERT,
                        new Object[] { harvester.getId(),
                                       harvester.getClass().getName(),
                                       harvester.getContent() });

                return new Result(request.url(URL_HARVESTERS_EDIT,
                        ARG_HARVESTER_ID, harvester.getId()));
            }
            String xmlLink =
                HtmlUtil.href(
                    HtmlUtil.url(
                        URL_HARVESTERS_EDIT.toString() + "/harvester.xml",
                        ARG_HARVESTER_GETXML, "true", ARG_HARVESTER_ID,
                        harvester.getId()), msg("Download"));

            String buttons = HtmlUtil.submit(msg("Change"), ARG_CHANGE)
                             + HtmlUtil.space(1)
                             + HtmlUtil.submit(msg("Delete"), ARG_DELETE)
                             + HtmlUtil.space(1)
                             + HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);



            sb.append(buttons);
            sb.append(HtmlUtil.space(2));
            sb.append(xmlLink);
            StringBuffer formSB = new StringBuffer();
            formSB.append(HtmlUtil.formTable());
            harvester.createEditForm(request, formSB);
            formSB.append(HtmlUtil.formTableClose());


            String extra = harvester.getExtraInfo();
            if ((extra != null) && (extra.length() > 0)) {
                extra = HtmlUtil.makeShowHideBlock(msg("Information"), extra,
                        false);
            }
            sb.append(formSB);
            /*
            sb.append(HtmlUtil.table(new Object[] { formSB,
                    HtmlUtil.br() + harvester.getRunLink(request, true)
                    + extra }));*/
            sb.append(buttons);
        }
        sb.append(HtmlUtil.formClose());
        return getAdmin().makeResult(request, msg("Edit Harvester"), sb);
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
    public Result processList(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_ACTION)) {
            String action = request.getString(ARG_ACTION);
            Harvester harvester =
                findHarvester(request.getString(ARG_HARVESTER_ID));
            if (action.equals(ACTION_STOP)) {
                harvester.setActive(false);
            } else if (action.equals(ACTION_REMOVE)) {
                harvester.setActive(false);
                harvesters.remove(harvester);
            } else if (action.equals(ACTION_START)) {
                if ( !harvester.getActive()) {
                    getEntryManager().clearSeenResources();
                    harvester.clearCache();
                    harvester.setActive(true);
                    Misc.run(harvester, "run");
                }
            }
            if (request.get(ARG_HARVESTER_REDIRECTTOEDIT, false)) {
                return new Result(request.url(URL_HARVESTERS_EDIT,
                        ARG_HARVESTER_ID, harvester.getId()));
            }
            return new Result(request.url(URL_HARVESTERS_LIST));
        }


        sb.append(msgHeader("Harvesters"));
        sb.append(request.formPost(URL_HARVESTERS_NEW));
        sb.append(HtmlUtil.submit(msg("New Harvester")));
        sb.append(HtmlUtil.formClose());

        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().showDialogNote(
                    request.getString(ARG_MESSAGE, "")));
        }
        makeHarvestersList(request, harvesters, sb);
        return getAdmin().makeResult(request, msg("Harvesters"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param harvesters _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void makeHarvestersList(Request request,
                                    List<Harvester> harvesters,
                                    StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.row(HtmlUtil.cols("", HtmlUtil.bold(msg("Name")),
                                             HtmlUtil.bold(msg("State")),
                                             HtmlUtil.bold(msg("Action")),
                                             "", "")));

        int cnt = 0;
        for (Harvester harvester : harvesters) {
            String removeLink =
                HtmlUtil.href(request.url(URL_HARVESTERS_LIST, ARG_ACTION,
                                          ACTION_REMOVE, ARG_HARVESTER_ID,
                                          harvester.getId()), msg("Remove"));
            if (harvester.getIsEditable()) {
                removeLink = "";
            }

            String edit = "&nbsp;";
            if (harvester.getIsEditable()) {
                edit = HtmlUtil
                    .href(request
                        .url(URL_HARVESTERS_EDIT, ARG_HARVESTER_ID,
                             harvester.getId()), HtmlUtil
                                 .img(getRepository().iconUrl(ICON_EDIT),
                                      msg("Edit")));
            }
            cnt++;
            sb.append(HtmlUtil.rowTop(HtmlUtil.cols(edit,
                    harvester.getName(), (harvester.getActive()
                                          ? msg("Active")
                                          : msg("Stopped")) + HtmlUtil.space(
                                          2), harvester.getRunLink(
                                          request, false), removeLink,
                                              harvester.getExtraInfo())));
        }
        sb.append(HtmlUtil.formTableClose());


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
    public Result processImportCatalog(Request request) throws Exception {

        Group group = getEntryManager().findGroup(request);
        if ( !request.exists(ARG_CATALOG)) {
            StringBuffer sb = new StringBuffer();
            sb.append(
                request.form(
                    getHarvesterManager().URL_HARVESTERS_IMPORTCATALOG));
            sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
            sb.append(msgHeader("Import a THREDDS catalog"));
            sb.append(HtmlUtil.formTable());
            sb.append(HtmlUtil.formEntry(msgLabel("URL"),
                                         HtmlUtil.input(ARG_CATALOG, BLANK,
                                             HtmlUtil.SIZE_70)));

            sb.append(
                HtmlUtil.formEntry(
                    "",
                    HtmlUtil.checkbox(ARG_RECURSE, "true", false)
                    + HtmlUtil.space(1) + msg("Recurse") + HtmlUtil.space(1)
                    + HtmlUtil.checkbox(ATTR_ADDMETADATA, "true", false)
                    + HtmlUtil.space(1) + msg("Add full metadata")
                    + HtmlUtil.space(1)
                    + HtmlUtil.checkbox(ATTR_ADDSHORTMETADATA, "true", false)
                    + HtmlUtil.space(1)
                    + msg("Just add spatial/temporal metadata")
                    + HtmlUtil.space(1)
                    + HtmlUtil.checkbox(ARG_RESOURCE_DOWNLOAD, "true", false)
                    + HtmlUtil.space(1) + msg("Download URLS")));
            sb.append(HtmlUtil.formEntry("", HtmlUtil.submit(msg("Go"))));
            sb.append(HtmlUtil.formTableClose());
            sb.append(HtmlUtil.formClose());



            return getEntryManager().makeEntryEditResult(request, group,
                    "Catalog Import", sb);
        }



        boolean      recurse     = request.get(ARG_RECURSE, false);
        boolean      addMetadata = request.get(ATTR_ADDMETADATA, false);
        boolean addShortMetadata = request.get(ATTR_ADDSHORTMETADATA, false);
        boolean      download    = request.get(ARG_RESOURCE_DOWNLOAD, false);
        StringBuffer sb          = new StringBuffer();
        //        sb.append(getEntryManager().makeEntryHeader(request, group));
        sb.append("<p>");
        String catalog = request.getString(ARG_CATALOG, "").trim();
        sb.append(request.form(URL_HARVESTERS_IMPORTCATALOG));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
        sb.append(HtmlUtil.submit(msgLabel("Import catalog")));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.input(ARG_CATALOG, catalog, " size=\"75\""));

        sb.append(HtmlUtil.checkbox(ARG_RECURSE, "true", recurse));
        sb.append(HtmlUtil.space(1));
        sb.append(msg("Recurse"));



        sb.append(HtmlUtil.checkbox(ATTR_ADDMETADATA, "true", addMetadata));
        sb.append(HtmlUtil.space(1));
        sb.append(msg("Add Metadata"));

        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.checkbox(ATTR_ADDSHORTMETADATA, "true",
                                    addShortMetadata));
        sb.append(HtmlUtil.space(1));
        sb.append(msg("Just add spatial/temporal metadata"));


        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.checkbox(ARG_RESOURCE_DOWNLOAD, "true", download));
        sb.append(HtmlUtil.space(1));
        sb.append(msg("Download URLs"));
        sb.append("</form>");

        if (catalog.length() > 0) {
            sb = new StringBuffer();
            sb.append(msg("Catalog is being harvested"));
            sb.append(HtmlUtil.p());
            ucar.unidata.repository.data.CatalogHarvester harvester =
                new ucar.unidata.repository.data.CatalogHarvester(
                    getRepository(), group, catalog, request.getUser(),
                    recurse, download);
            harvester.setAddMetadata(addMetadata);
            harvester.setAddShortMetadata(addShortMetadata);
            harvesters.add(harvester);
            Misc.run(harvester, "run");
            //            makeHarvestersList(request, (List<Harvester>)Misc.newList(harvester),  sb);
            //            return  getEntryManager().addEntryHeader(request, group,
            //                                                     new Result("",sb));
            return getEntryManager().addEntryHeader(request, group,
                    new Result(request.url(URL_HARVESTERS_LIST, ARG_MESSAGE,
                                           "Catalog is being harvested")));
        }


        Result result = getEntryManager().addEntryHeader(request, group,
                            new Result(request.url(URL_HARVESTERS_LIST)));
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
    public Result processFile(Request request) throws Exception {
        List<Harvester> harvesters  = getHarvesters();
        TypeHandler     typeHandler = getRepository().getTypeHandler(request);
        String          filepath    = request.getUnsafeString(ARG_FILE,
                                          BLANK);
        //Check to  make sure we can access this file
        if ( !getStorageManager().isInDownloadArea(new File(filepath))) {
            return new Result(BLANK,
                              new StringBuffer("Cannot load file:"
                                  + filepath), "text/plain");
        }
        for (Harvester harvester : harvesters) {
            Entry entry = harvester.processFile(typeHandler, filepath);
            if (entry != null) {
                getEntryManager().addNewEntry(entry);
                return new Result(BLANK, new StringBuffer("OK"),
                                  "text/plain");
            }
        }
        return new Result(BLANK, new StringBuffer("Could not create entry"),
                          "text/plain");
    }






}

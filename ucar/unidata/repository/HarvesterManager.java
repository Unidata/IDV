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

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.lang.reflect.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


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
    public static final String ARG_HARVESTER_ID = "harvester.id";

    /** _more_ */
    public RequestUrl URL_HARVESTERS_IMPORTCATALOG =
        new RequestUrl(this, "/harvesters/importcatalog", "Import Catalog");

    /** _more_ */
    public RequestUrl URL_HARVESTERS_LIST = new RequestUrl(this,
                                                "/harvesters/list",
                                                "Harvesters");

    /** _more_ */
    public RequestUrl URL_HARVESTERS_NEW = new RequestUrl(this,
                                               "/harvesters/new");


    /** _more_ */
    public RequestUrl URL_HARVESTERS_EDIT = new RequestUrl(this,
                                                "/harvesters/edit");



    /** _more_ */
    private List<Harvester> harvesters = new ArrayList();

    /** _more_ */
    private Hashtable harvesterMap = new Hashtable();

    /**
     * _more_
     *
     * @param repository _more_
     */
    public HarvesterManager(Repository repository) {
        super(repository);
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
    protected void initHarvesters() throws Exception {
        List<String> harvesterFiles =
            getRepository().getResourcePaths(PROP_HARVESTERS);
        boolean okToStart =
            getRepository().getProperty(PROP_HARVESTERS_ACTIVE, true);

        harvesters = new ArrayList<Harvester>();


        SqlUtil.Iterator iter = SqlUtil.getIterator(
                                    getDatabaseManager().select(
                                        COLUMNS_HARVESTERS, TABLE_HARVESTERS,
                                        new Clause()));;
        ResultSet results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                String id        = results.getString(1);
                String className = results.getString(2);
                String content   = results.getString(3);
                Class  c         = Misc.findClass(className);
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
                List<Harvester> newHarvesters =
                    Harvester.createHarvesters(getRepository(), root);
                harvesters.addAll(newHarvesters);
                for (Harvester harvester : newHarvesters) {
                    harvesterMap.put(harvester.getId(), harvester);
                }
            }
        } catch (Exception exc) {
            System.err.println("Error loading harvester file");
            throw exc;
        }



        for (Harvester harvester : harvesters) {
            File rootDir = harvester.getRootDir();
            if (rootDir != null) {
                getStorageManager().addDownloadPrefix(
                    rootDir.toString().replace("\\", "/") + "/");
            }
            if ( !okToStart) {
                harvester.setActive(false);
            } else if (harvester.getActive()) {
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

        if (request.exists(ARG_NAME)) {
            String id = getRepository().getGUID();
            PatternHarvester harvester =
                new PatternHarvester(getRepository(), id);
            harvester.setName(request.getString(ARG_NAME, ""));

            harvesters.add(harvester);
            harvesterMap.put(id, harvester);

            getDatabaseManager().executeInsert(INSERT_HARVESTERS,
                    new Object[] { id,
                                   harvester.getClass().getName(),
                                   harvester.getContent() });
            return new Result(request.url(URL_HARVESTERS_EDIT,
                                          ARG_HARVESTER_ID, id));
        }
        sb.append(request.form(URL_HARVESTERS_NEW));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ARG_NAME, "",
                                         HtmlUtil.SIZE_40)));
        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit(msg("Create"))
                                     + HtmlUtil.space(1)
                                     + HtmlUtil.submit(msg("Cancel"),
                                         ARG_CANCEL)));

        sb.append(HtmlUtil.formTableClose());
        sb.append(HtmlUtil.formClose());
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
        if ( !harvester.getIsEditable()) {
            throw new IllegalArgumentException("Cannot edit harvester");
        }
        sb.append(header(msgLabel("Harvester") + harvester.getName()));
        sb.append(request.form(URL_HARVESTERS_EDIT));
        sb.append(HtmlUtil.hidden(ARG_HARVESTER_ID, harvester.getId()));
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.url(URL_HARVESTERS_LIST));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            harvesterMap.remove(harvester.getId());
            harvesters.remove(harvester);
            SqlUtil.delete(getConnection(), TABLE_HARVESTERS,
                           Clause.eq(COL_HARVESTERS_ID, harvester.getId()));
            return new Result(request.url(URL_HARVESTERS_LIST));
        } else if (request.exists(ARG_DELETE)) {
            sb.append(
                getRepository().question(
                    msg("Are you sure you want to delete the harvester"),
                    getRepository().buttons(
                        HtmlUtil.submit(msg("Yes"), ARG_DELETE_CONFIRM),
                        HtmlUtil.submit(msg("Cancel"), ARG_CANCEL_DELETE))));
        } else {
            if (request.exists(ARG_CHANGE)) {
                harvester.applyEditForm(request);
                SqlUtil.delete(getConnection(), TABLE_HARVESTERS,
                               Clause.eq(COL_HARVESTERS_ID,
                                         harvester.getId()));
                getDatabaseManager().executeInsert(INSERT_HARVESTERS,
                        new Object[] { harvester.getId(),
                                       harvester.getClass().getName(),
                                       harvester.getContent() });

            }
            sb.append(HtmlUtil.formTable());
            String buttons = HtmlUtil.submit(msg("Change"), ARG_CHANGE)
                             + HtmlUtil.space(1)
                             + HtmlUtil.submit(msg("Delete"), ARG_DELETE)
                             + HtmlUtil.space(1)
                             + HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);

            sb.append(HtmlUtil.formEntry("", buttons));
            harvester.createEditForm(request, sb);
            sb.append(HtmlUtil.formEntry("", buttons));
            sb.append(HtmlUtil.formTableClose());
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
                    harvester.setActive(true);
                    Misc.run(harvester, "run");
                }
            }
            return new Result(request.url(URL_HARVESTERS_LIST));
        }


        sb.append(msgHeader("Harvesters"));
        sb.append(request.form(URL_HARVESTERS_NEW));
        sb.append(HtmlUtil.submit(msg("New Harvester")));
        sb.append(HtmlUtil.formClose());
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.row(HtmlUtil.cols("", HtmlUtil.bold(msg("Name")),
                                             HtmlUtil.bold(msg("State")),
                                             HtmlUtil.bold(msg("Action")),
                                             "", "")));

        int cnt = 0;
        for (Harvester harvester : harvesters) {
            String remove = HtmlUtil.href(request.url(URL_HARVESTERS_LIST,
                                ARG_ACTION, ACTION_REMOVE, ARG_HARVESTER_ID,
                                harvester.getId()), msg("Remove"));
            String run;
            if (harvester.getActive()) {
                run = HtmlUtil.href(request.url(URL_HARVESTERS_LIST,
                        ARG_ACTION, ACTION_STOP, ARG_HARVESTER_ID,
                        harvester.getId()), msg("Stop"));
            } else {
                run = HtmlUtil.href(request.url(URL_HARVESTERS_LIST,
                        ARG_ACTION, ACTION_START, ARG_HARVESTER_ID,
                        harvester.getId()), msg("Start"));
            }

            String edit = "&nbsp;";
            if (harvester.getIsEditable()) {
                edit = HtmlUtil
                    .href(request
                        .url(URL_HARVESTERS_EDIT, ARG_HARVESTER_ID,
                             harvester.getId()), HtmlUtil
                                 .img(getRepository().fileUrl(ICON_EDIT),
                                      msg("Edit")));
            }
            cnt++;
            sb.append(HtmlUtil.rowTop(HtmlUtil.cols(edit,
                    harvester.getName(), (harvester.getActive()
                                          ? msg("Active")
                                          : msg("Stopped")) + HtmlUtil.space(
                                          2), run, remove,
                                              harvester.getExtraInfo())));
        }
        sb.append(HtmlUtil.formTableClose());
        return getAdmin().makeResult(request, msg("Harvesters"), sb);
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
        Group        group   = getRepository().findGroup(request);
        boolean      recurse = request.get(ARG_RECURSE, false);
        StringBuffer sb      = new StringBuffer();
        sb.append(getRepository().makeEntryHeader(request, group));
        sb.append("<p>");
        String catalog = request.getString(ARG_CATALOG, "").trim();
        sb.append(request.form(URL_HARVESTERS_IMPORTCATALOG));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getFullName()));
        sb.append(HtmlUtil.submit(msgLabel("Import catalog")));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.input(ARG_CATALOG, catalog, " size=\"75\""));
        sb.append(HtmlUtil.checkbox(ARG_RECURSE, "true", recurse));
        sb.append(HtmlUtil.space(1));
        sb.append(msg("Recurse"));
        sb.append("</form>");
        if (catalog.length() > 0) {
            CatalogHarvester harvester =
                new CatalogHarvester(getRepository(), group, catalog,
                                     request.getUser(), recurse);
            harvesters.add(harvester);
            Misc.run(harvester, "run");
        }

        Result result = new Result(request.url(URL_HARVESTERS_LIST));
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
        System.err.println("mem:" + Misc.usedMemory());
        List<Harvester> harvesters  = getHarvesters();
        TypeHandler     typeHandler = getRepository().getTypeHandler(request);
        String          filepath    = request.getUnsafeString(ARG_FILE,
                                          BLANK);
        //Check to  make sure we can access this file
        if ( !getStorageManager().isInDownloadArea(filepath)) {
            return new Result(BLANK,
                              new StringBuffer("Cannot load file:"
                                  + filepath), "text/plain");
        }
        for (Harvester harvester : harvesters) {
            Entry entry = harvester.processFile(typeHandler, filepath);
            if (entry != null) {
                getRepository().addNewEntry(entry);
                return new Result(BLANK, new StringBuffer("OK"),
                                  "text/plain");
            }
        }
        return new Result(BLANK, new StringBuffer("Could not create entry"),
                          "text/plain");
    }




}


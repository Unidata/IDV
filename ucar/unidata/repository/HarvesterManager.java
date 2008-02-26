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

import ucar.unidata.data.SqlUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.io.File;

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
    public RequestUrl URL_HARVESTERS_IMPORTCATALOG = new RequestUrl(this,
                                                     "/harvesters/importcatalog",
                                                     "Import Catalog");

    /** _more_ */
    public RequestUrl URL_HARVESTERS_LIST = new RequestUrl(this,
                                                 "/harvesters/list",
                                                 "Harvesters");


    /** _more_ */
    private List<Harvester> harvesters = new ArrayList();

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
        try {
            harvesters = new ArrayList<Harvester>();
            for (String file : harvesterFiles) {
                Element root = XmlUtil.getRoot(file, getClass());
                if (root == null) {
                    continue;
                }
                harvesters.addAll(Harvester.createHarvesters(getRepository(),
                        root));
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
    public Result processList(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_ACTION)) {
            String    action    = request.getString(ARG_ACTION);
            Harvester harvester = findHarvester(request.getString(ARG_ID));
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
            return new Result(URL_HARVESTERS_LIST.toString());
        }


        sb.append(msgHeader("Harvesters"));
        sb.append("<table cellspacing=\"5\">");
        sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.bold(msg("Name")),
                                             HtmlUtil.bold(msg("State")),
                                             HtmlUtil.bold(msg("Action")),
                                             "", "")));

        int cnt = 0;
        for (Harvester harvester : harvesters) {
            String remove = HtmlUtil.href(HtmlUtil.url(URL_HARVESTERS_LIST,
                                ARG_ACTION, ACTION_REMOVE, ARG_ID,
                                harvester.getId()), msg("Remove"));
            String run;
            if (harvester.getActive()) {
                run = HtmlUtil.href(HtmlUtil.url(URL_HARVESTERS_LIST,
                        ARG_ACTION, ACTION_STOP, ARG_ID,
                        harvester.getId()), msg("Stop"));
            } else {
                run = HtmlUtil.href(HtmlUtil.url(URL_HARVESTERS_LIST,
                        ARG_ACTION, ACTION_START, ARG_ID,
                        harvester.getId()), msg("Start"));
            }
            cnt++;
            sb.append("<tr valign=\"top\">");
            sb.append(HtmlUtil.cols(harvester.getName(),
                                    (harvester.getActive()
                                     ? msg("Active")
                                     : msg("Stopped")) + HtmlUtil.space(
                                         2), run, remove,
                                             harvester.getExtraInfo()));
            sb.append("</tr>\n");
        }
        sb.append("</table>");

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
        sb.append(getRepository().makeGroupHeader(request, group));
        sb.append("<p>");
        String catalog = request.getString(ARG_CATALOG, "").trim();
        sb.append(HtmlUtil.form(URL_HARVESTERS_IMPORTCATALOG.toString()));
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

        Result result = new Result(URL_HARVESTERS_LIST.toString());
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


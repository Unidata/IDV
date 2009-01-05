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

import ucar.unidata.util.StringUtil;



import ucar.unidata.xml.XmlUtil;

import java.io.*;


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

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;


import javax.mail.internet.MimeMessage;


/**
 * Class Admin
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Admin extends RepositoryManager {

    /** _more_ */
    public RequestUrl URL_ADMIN_SQL = new RequestUrl(this, "/admin/sql",
                                          "SQL");

    public RequestUrl URL_ADMIN_USERMESSAGE = new RequestUrl(this, "/admin/usermessage",
                                                            "User Message");

    /** _more_ */
    public RequestUrl URL_ADMIN_IMPORT_CATALOG = new RequestUrl(this,
                                                     "/admin/import/catalog",
                                                     "Import Catalog");

    /** _more_ */
    public RequestUrl URL_ADMIN_CLEANUP = new RequestUrl(this,
                                              "/admin/cleanup", "Cleanup");


    /** _more_ */
    public RequestUrl URL_ADMIN_STARTSTOP = new RequestUrl(this,
                                                "/admin/startstop",
                                                "Database");


    /** _more_ */
    public RequestUrl URL_ADMIN_SETTINGS = new RequestUrl(this,
                                               "/admin/settings", "Settings");

    /** _more_ */
    public RequestUrl URL_ADMIN_SETTINGS_DO = new RequestUrl(this,
                                                  "/admin/settings/do",
                                                  "Settings");

    /** _more_ */
    public RequestUrl URL_ADMIN_TABLES = new RequestUrl(this,
                                             "/admin/tables", "Database");


    /** _more_ */
    public RequestUrl URL_ADMIN_DUMPDB = new RequestUrl(this,
                                             "/admin/dumpdb",
                                             "Dump Database");

    /** _more_ */
    public RequestUrl URL_ADMIN_STATS = new RequestUrl(this, "/admin/stats",
                                            "Statistics");

    /** _more_ */
    public RequestUrl URL_ADMIN_ACCESS = new RequestUrl(this,
                                             "/admin/access", "Access");


    /** _more_ */
    public RequestUrl URL_ADMIN_LOG = new RequestUrl(this,
                                             "/admin/log", "Log");


    /** _more_ */
    protected RequestUrl[] adminUrls = {
        URL_ADMIN_SETTINGS, getRepositoryBase().URL_USER_LIST,
        URL_ADMIN_STATS, URL_ADMIN_ACCESS,
        getHarvesterManager().URL_HARVESTERS_LIST,
        /*URL_ADMIN_STARTSTOP,*/
        /*URL_ADMIN_TABLES, */
        URL_ADMIN_LOG,
        URL_ADMIN_SQL, URL_ADMIN_CLEANUP,
        URL_ADMIN_USERMESSAGE
    };


    /** _more_ */
    int cleanupTS = 0;

    /** _more_ */
    boolean runningCleanup = false;

    /** _more_ */
    StringBuffer cleanupStatus = new StringBuffer();




    /**
     * _more_
     *
     * @param repository _more_
     */
    public Admin(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     *
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean haveDone(String what) throws Exception {
        return getRepository().getDbProperty(what, false);
    }

    /**
     * _more_
     *
     * @param what _more_
     *
     * @throws Exception _more_
     */
    private void didIt(String what) throws Exception {
        getRepository().writeGlobal(what, "true");
    }


    public Result adminUserMessage(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if(request.exists(ARG_MESSAGE)) {
            getUserManager().setSessionMessage(request.getString(ARG_MESSAGE,""));
            sb.append(msg("Message set"));
        } else {
            sb.append(msgHeader("Enter a message to show one time to all users"));
            sb.append(request.form(URL_ADMIN_USERMESSAGE, ""));
            sb.append(HtmlUtil.formTable());
            sb.append(HtmlUtil.formEntry(msgLabel("Message"),
                                         HtmlUtil.textArea(ARG_MESSAGE,"",5,60)));
            sb.append(HtmlUtil.formTableClose());
            sb.append(HtmlUtil.submit(msg("Set user message")));
            sb.append("</form>");

        }
        Result result = new Result(msg("Alert Message"), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                                                          getAdmin().adminUrls));
        return result;
    }

    public Result adminLog(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        sb.append(HtmlUtil.row(HtmlUtil.cols(
                                                HtmlUtil.b(msg("User")),
                                                HtmlUtil.b(msg("Date")),
                                                HtmlUtil.b(msg("Path")),
                                                HtmlUtil.b(msg("IP")),
                                                HtmlUtil.b(msg("User agent"))
                                                )));
        List<Repository.LogEntry> log = getRepository().getLog();
        for(int i=log.size()-1;i>=0;i--) {
            Repository.LogEntry logEntry = log.get(i);
            //Encode the path just in case the user does a XSS attack
            sb.append(HtmlUtil.row(HtmlUtil.cols(
                                                    logEntry.getUser().getLabel(),
                                                    getRepository().formatDate(logEntry.getDate()),
                                                    HtmlUtil.entityEncode(logEntry.getPath()),
                                                    logEntry.getIp(),
                                                    logEntry.getUserAgent())));
            
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

        Result result = new Result(msg("Log"), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                                                          getAdmin().adminUrls));
        return result;
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private StringBuffer getLicenseForm() throws Exception {
        StringBuffer sb = new StringBuffer();
        String license =
            IOUtil.readContents(
                "/ucar/unidata/repository/resources/license.txt", getClass());
        sb.append(HtmlUtil.textArea("", license, 20, 50));
        sb.append("<p>");
        sb.append(HtmlUtil.checkbox("agree", "1"));
        sb.append(
            "I agree to the above terms and conditions of use of the RAMADDA software");
        sb.append("<p>");
        return sb;
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
    protected Result doInitialization(Request request) throws Exception {

        StringBuffer sb    = new StringBuffer();
        String       title = "";

        if (Misc.equals("1", request.getString("agree", ""))) {
            didIt(ARG_ADMIN_LICENSEREAD);
        }

        if ( !haveDone(ARG_ADMIN_INSTALLNOTICESHOWN)) {
            title = "Installation";
            sb.append(HtmlUtil.formTable());
            sb.append(
                "Here is the local file system directory where data is stored and the database information.<br>Now would be a good time to change these settings and restart RAMADDA if this is not what you want.<br>See <a target=\"other\" href=\"http://www.unidata.ucar.edu/software/ramadda/docs/userguide/installing.html\">here</a> for installation instructions.");
            getStorageManager().addInfo(sb);
            getDatabaseManager().addInfo(sb);
            sb.append(HtmlUtil.formEntry("", HtmlUtil.submit(msg("Next"))));
            sb.append(HtmlUtil.formTableClose());
            didIt(ARG_ADMIN_INSTALLNOTICESHOWN);
        } else if ( !haveDone(ARG_ADMIN_LICENSEREAD)) {

            title = "License";
            sb.append(getLicenseForm());
            sb.append(HtmlUtil.submit(msg("Next")));
        } else if ( !haveDone(ARG_ADMIN_ADMINCREATED)) {
            title = "Administrator";
            String  id        = "";
            String  name      = "";
            boolean triedOnce = false;
            if (request.exists(UserManager.ARG_USER_ID)) {
                triedOnce = true;
                id = request.getString(UserManager.ARG_USER_ID, "").trim();
                name = request.getString(UserManager.ARG_USER_NAME,
                                         name).trim();
                String password1 =
                    request.getString(UserManager.ARG_USER_PASSWORD1,
                                      "").trim();
                String password2 =
                    request.getString(UserManager.ARG_USER_PASSWORD2,
                                      "").trim();
                boolean      okToAdd     = true;
                StringBuffer errorBuffer = new StringBuffer();
                if (id.length() == 0) {
                    okToAdd = false;
                    errorBuffer.append(HtmlUtil.space(2));
                    errorBuffer.append(msg("Please enter an ID"));
                    errorBuffer.append(HtmlUtil.br());
                }

                if ((password1.length() == 0)
                        || !password1.equals(password2)) {
                    okToAdd = false;
                    errorBuffer.append(HtmlUtil.space(2));
                    errorBuffer.append(msg("Invalid password"));
                    errorBuffer.append(HtmlUtil.br());
                }


                if (okToAdd) {
                    getUserManager().makeOrUpdateUser(new User(id, name, "",
                            "", "", getUserManager().hashPassword(password1),
                            true, "",""), false);
                    didIt(ARG_ADMIN_ADMINCREATED);
                    didIt(ARG_ADMIN_INSTALLCOMPLETE);
                    sb.append(msg("Site administrator created"));
                    sb.append(HtmlUtil.p());
                    sb.append(getUserManager().makeLoginForm(request));
                    return new Result("", sb);
                }
                sb.append(msg("Error"));
                sb.append(HtmlUtil.br());
                sb.append(errorBuffer);
                sb.append(HtmlUtil.p());
            }


            sb.append(
                msg("Please enter a site administrator name and password"));
            sb.append(HtmlUtil.p());
            sb.append(request.form(getRepository().URL_DUMMY));
            sb.append(HtmlUtil.formTable());
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("ID"),
                    HtmlUtil.input(UserManager.ARG_USER_ID, id)));
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Name"),
                    HtmlUtil.input(UserManager.ARG_USER_NAME, name)));

            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Password"),
                    HtmlUtil.password(UserManager.ARG_USER_PASSWORD1)));

            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Password Again"),
                    HtmlUtil.password(UserManager.ARG_USER_PASSWORD2)));

            sb.append(
                HtmlUtil.formEntry(
                    "", HtmlUtil.submit(msg("Make Administrator"))));
        }

        StringBuffer finalSB = new StringBuffer();
        finalSB.append(request.form(getRepository().URL_DUMMY));
        finalSB.append(msgHeader(title));
        finalSB.append(sb);
        finalSB.append(HtmlUtil.formClose());
        return new Result(msg(title), finalSB);

    }





    /**
     * _more_
     *
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected StringBuffer getDbMetaData() throws Exception {

        Connection connection = getDatabaseManager().getNewConnection();
        try {
            StringBuffer     sb       = new StringBuffer();
            DatabaseMetaData dbmd     = connection.getMetaData();
            ResultSet        catalogs = dbmd.getCatalogs();
            ResultSet tables = dbmd.getTables(null, null, null,
                                   new String[] { "TABLE" });

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                //            System.err.println("table name:" + tableName);
                String tableType = tables.getString("TABLE_TYPE");
                //            System.err.println("table type" + tableType);
                if (Misc.equals(tableType, "INDEX")) {
                    continue;
                }
                if (tableType == null) {
                    continue;
                }

                if ((tableType != null) && tableType.startsWith("SYSTEM")) {
                    continue;
                }


                ResultSet columns = dbmd.getColumns(null, null, tableName,
                                        null);
                String encoded = new String(XmlUtil.encodeBase64(("text:?"
                                     + tableName).getBytes()));

                int cnt = 0;
                if (tableName.toLowerCase().indexOf("_index_") < 0) {
                    cnt = getDatabaseManager().getCount(tableName,
                            new Clause());
                }
                String tableVar  = null;
                String TABLENAME = tableName.toUpperCase();
                sb.append("Table:" + tableName + " (#" + cnt + ")");
                sb.append("<ul>");
                List colVars = new ArrayList();

                while (columns.next()) {
                    String colName = columns.getString("COLUMN_NAME");
                    String colSize = columns.getString("COLUMN_SIZE");
                    sb.append("<li>");
                    sb.append(colName + " (" + columns.getString("TYPE_NAME")
                              + " " + colSize + ")");
                }

                ResultSet indices = dbmd.getIndexInfo(null, null, tableName,
                                        false, true);
                boolean didone = false;
                while (indices.next()) {
                    if ( !didone) {
                        //                            sb.append(
                        //                                "<br><b>Indices</b> (name,order,type,pages)<br>");
                        sb.append("<br><b>Indices</b><br>");
                    }
                    didone = true;
                    String indexName  = indices.getString("INDEX_NAME");
                    String asc        = indices.getString("ASC_OR_DESC");
                    int    type       = indices.getInt("TYPE");
                    String typeString = "" + type;
                    int    pages      = indices.getInt("PAGES");
                    if (type == DatabaseMetaData.tableIndexClustered) {
                        typeString = "clustered";
                    } else if (type == DatabaseMetaData.tableIndexHashed) {
                        typeString = "hashed";
                    } else if (type == DatabaseMetaData.tableIndexOther) {
                        typeString = "other";
                    }
                    //                        sb.append("Index:" + indexName + "  " + asc + " "
                    //                                  + typeString + " " + pages + "<br>");
                    sb.append("Index:" + indexName + "<br>");


                }

                sb.append("</ul>");
            }
            return sb;
        } finally {
            getDatabaseManager().closeConnection(connection);
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
    public Result adminDbStartStop(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Database Administration"));
        String what = request.getString(ARG_ADMIN_WHAT, "nothing");
        if (what.equals("shutdown")) {
            if ( !getDatabaseManager().hasConnection()) {
                sb.append("Not connected to database");
            } else {
                getRepository().getDatabaseManager().closeConnection();
                sb.append("Database is shut down");
            }
        } else if (what.equals("restart")) {
            if (getDatabaseManager().hasConnection()) {
                sb.append("Already connected to database");
            } else {
                getRepository().getDatabaseManager().makeConnection();
                sb.append("Database is restarted");
            }
        }
        sb.append("<p>");
        sb.append(request.form(URL_ADMIN_STARTSTOP, " name=\"admin\""));
        if ( !getDatabaseManager().hasConnection()) {
            sb.append(HtmlUtil.hidden(ARG_ADMIN_WHAT, "restart"));
            sb.append(HtmlUtil.submit("Restart Database"));
        } else {
            sb.append(HtmlUtil.hidden(ARG_ADMIN_WHAT, "shutdown"));
            sb.append(HtmlUtil.submit("Shut Down Database"));
        }
        sb.append("</form>");
        return makeResult(request, "Administration", sb);

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
    public Result adminActions(Request request) throws Exception {
        StringBuffer    sb         = new StringBuffer();
        List<ApiMethod> apiMethods = getRepository().getApiMethods();
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.row(HtmlUtil.cols("Name", "Admin?", "Actions")));
        for (ApiMethod apiMethod : apiMethods) {
            sb.append(HtmlUtil.row(HtmlUtil.cols(apiMethod.getName(),
                    "" + apiMethod.getMustBeAdmin(),
                    StringUtil.join(",", apiMethod.getActions()))));
        }
        sb.append(HtmlUtil.formTableClose());

        return makeResult(request, "Administration", sb);
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
    public Result adminDbTables(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Database Tables"));
        sb.append(getDbMetaData());
        return makeResult(request, "Administration", sb);
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
    public Result adminDbDump(Request request) throws Exception {
        File tmp = getStorageManager().getTmpFile(request, "dbdump");
        FileOutputStream     fos = new FileOutputStream(tmp);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        getDatabaseManager().makeDatabaseCopy(bos, true);
        bos.close();
        fos.close();
        FileInputStream is = new FileInputStream(tmp);
        return new Result("", is, "text/sql");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result makeResult(Request request, String title,
                                StringBuffer sb)
            throws Exception {
        return getRepository().makeResult(request, title, sb, adminUrls);
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
    public Result adminHome(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Repository Administration"));
        sb.append("<ul>\n");
        sb.append("<li> ");
        sb.append(HtmlUtil.href(request.url(URL_ADMIN_STARTSTOP),
                                "Administer Database"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(request.url(URL_ADMIN_TABLES),
                                "Show Tables"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(request.url(URL_ADMIN_STATS), "Statistics"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(request.url(URL_ADMIN_SQL), "Execute SQL"));
        sb.append("</ul>");
        return makeResult(request, "Administration", sb);

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
    public Result adminSettings(Request request) throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append(request.form(URL_ADMIN_SETTINGS_DO));
        String size = HtmlUtil.SIZE_60;
        sb.append(HtmlUtil.submit(msg("Change Settings")));
        sb.append(HtmlUtil.br());
        StringBuffer csb = new StringBuffer();
        csb.append(HtmlUtil.formTable());
        csb.append(HtmlUtil.formEntry(msgLabel("Administrator Email"),
                                      HtmlUtil.input(PROP_ADMIN_EMAIL,
                                          getProperty(PROP_ADMIN_EMAIL, ""),
                                          HtmlUtil.SIZE_40)));

        csb.append(
            HtmlUtil.formEntry(
                msgLabel("Mail Server"),
                HtmlUtil.input(
                    PROP_ADMIN_SMTP, getProperty(PROP_ADMIN_SMTP, ""),
                    HtmlUtil.SIZE_40) + " "
                          + msg("For sending password reset messages")));


        csb.append(HtmlUtil.formTableClose());
        sb.append(makeConfigBlock("Contact", csb.toString()));


        StringBuffer dsb = new StringBuffer();

        dsb.append(HtmlUtil.formTable());
        dsb.append(HtmlUtil.formEntry(msgLabel("Title"),
                                      HtmlUtil.input(PROP_REPOSITORY_NAME,
                                          getProperty(PROP_REPOSITORY_NAME,
                                              "Repository"), size)));
        dsb.append(HtmlUtil.formEntryTop(msgLabel("Footer"),
                                         HtmlUtil.textArea(PROP_HTML_FOOTER,
                                             getProperty(PROP_HTML_FOOTER,
                                                 ""), 5, 60)));



        String phrases = getProperty(PROP_ADMIN_PHRASES, (String) null);
        if (phrases == null) {
            phrases = "#label=new label to use\n#e.g.: Groups=Projects";
        }
        dsb.append(
            HtmlUtil.formEntryTop(
                msgLabel("Translations"),
                HtmlUtil.textArea(PROP_ADMIN_PHRASES, phrases, 5, 60)));


        dsb.append(HtmlUtil.formEntryTop(msgLabel("Google Maps Keys"), "<table><tr valign=top><td>"
                + HtmlUtil.textArea(PROP_GOOGLEAPIKEYS, getProperty(PROP_GOOGLEAPIKEYS, ""), 5, 80)
                + "</td><td>One per line:<br><i>host domain:apikey</i><br>e.g.:<i>www.yoursite.edu:google api key</i></table>"));


        dsb.append(HtmlUtil.formTableClose());
        sb.append(makeConfigBlock("Display", dsb.toString()));


        StringBuffer asb = new StringBuffer();
        asb.append(HtmlUtil.formTable());
        asb.append(
            HtmlUtil.formEntry(
                "",
                HtmlUtil.checkbox(
                    PROP_ACCESS_ADMINONLY, "true",
                    getProperty(
                        PROP_ACCESS_ADMINONLY, false)) + HtmlUtil.space(2)
                            + msg("Admin only")));
        asb.append(
            HtmlUtil.formEntry(
                "",
                HtmlUtil.checkbox(
                    PROP_ACCESS_REQUIRELOGIN, "true",
                    getProperty(
                        PROP_ACCESS_REQUIRELOGIN, false)) + HtmlUtil.space(2)
                            + msg("Require login")));





        asb.append(HtmlUtil.formEntry(
                msgLabel("SSL Port"),
                HtmlUtil.input(
                               PROP_SSL_PORT, getProperty(PROP_SSL_PORT, ""),
                               HtmlUtil.SIZE_5) + HtmlUtil.space(1) +
                msg("Port number for SSL access.") +HtmlUtil.space(1) +msg("Enter 'default' for default SSL port.")));




        String fileWidget = HtmlUtil.textArea(PROP_LOCALFILEPATHS,
                                getProperty(PROP_LOCALFILEPATHS, ""), 5, 40);
        String fileLabel =
            msg("Enter one server file system directory per line.");
        asb.append(HtmlUtil.formEntryTop(msgLabel("File system access"),
                                         "<table><tr valign=top><td>"
                                         + fileWidget + "</td><td>"
                                         + fileLabel + "</td></tr></table>"));



        asb.append(HtmlUtil.formTableClose());
        sb.append(makeConfigBlock("Access", asb.toString()));

        StringBuffer osb = new StringBuffer();
        osb.append(HtmlUtil.formTable());


        StringBuffer     outputSB      = new StringBuffer();
        List<OutputType> types         = getRepository().getOutputTypes();
        String           lastGroupName = null;
        for (OutputType type : types) {
            if ( !type.getForUser()) {
                continue;
            }
            boolean ok = getRepository().isOutputTypeOK(type);
            if ( !Misc.equals(lastGroupName, type.getGroupName())) {
                if (lastGroupName != null) {
                    outputSB.append("</div>\n");
                    outputSB.append(HtmlUtil.p());
                }
                lastGroupName = type.getGroupName();
                outputSB.append(HtmlUtil.div(lastGroupName,HtmlUtil.cssClass("pagesubheading"))
                                              +"\n<div style=\"margin-left:20px\">");
            }
            outputSB.append(HtmlUtil.checkbox("outputtype." + type.getId(),
                    "true", ok));
            outputSB.append(type.getLabel());
            outputSB.append(HtmlUtil.space(3));
        }
        outputSB.append("</div>\n");
        String outputDiv = HtmlUtil.div(outputSB.toString(),
                                        HtmlUtil.cssClass("scrollablediv"));
        osb.append("\n");
        String doAllOutput = HtmlUtil.checkbox("outputtype.all", "true",
                                 false) + HtmlUtil.space(1) + msg("Use all");
        osb.append(HtmlUtil.formEntryTop("", doAllOutput + outputDiv));
        osb.append("\n");
        StringBuffer handlerSB = new StringBuffer();
        List<OutputHandler> outputHandlers =
            getRepository().getOutputHandlers();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.addToSettingsForm(handlerSB);
        }

        String extra = handlerSB.toString();
        if (extra.length() > 0) {
            osb.append(tableSubHeader(msg("Output")));
            osb.append(extra);
        }

        osb.append(HtmlUtil.formEntry("&nbsp;<p>", ""));
        osb.append(HtmlUtil.formTableClose());
        sb.append(makeConfigBlock("Available Output Types", osb.toString()));


        sb.append(HtmlUtil.submit(msg("Change Settings")));
        sb.append(HtmlUtil.formClose());
        return makeResult(request, msg("Settings"), sb);

    }

    /**
     * _more_
     *
     * @param title _more_
     * @param contents _more_
     *
     * @return _more_
     */
    private String makeConfigBlock(String title, String contents) {
        return HtmlUtil.makeShowHideBlock(
            msg(title),
            HtmlUtil.div(contents, HtmlUtil.cssClass("admin-block-inner")),
            false, HtmlUtil.cssClass("pagesubheading"), HtmlUtil.cssClass("admin-block"));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEmailCapable() {
        String smtpServer = getRepository().getProperty(PROP_ADMIN_SMTP,
                                "").trim();
        String serverAdmin = getRepository().getProperty(PROP_ADMIN_EMAIL,
                                 "").trim();
        if ((serverAdmin.length() == 0) || (smtpServer.length() == 0)) {
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @param to _more_
     * @param subject _more_
     * @param contents _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(String to, String subject, String contents,
                          boolean asHtml)
            throws Exception {
        if ( !isEmailCapable()) {
            throw new IllegalStateException(
                "This RAMADDA server has not been configured to send email");
        }

        //        System.err.println("subject:" + subject);
        //        System.err.println("contents:" + contents);
        String smtpServer = getRepository().getProperty(PROP_ADMIN_SMTP,
                                "").trim();
        String serverAdmin = getRepository().getProperty(PROP_ADMIN_EMAIL,
                                 "").trim();

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.from", serverAdmin);
        javax.mail.Session session = javax.mail.Session.getInstance(props,
                                         null);
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(serverAdmin));
        msg.setRecipients(Message.RecipientType.TO, to);
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setContent(contents, (asHtml
                                  ? "text/html"
                                  : "text/plain"));
        Transport.send(msg);
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
    public Result adminSettingsDo(Request request) throws Exception {

        getRepository().writeGlobal(PROP_ADMIN_EMAIL,
                                    request.getString(PROP_ADMIN_EMAIL, ""));
        getRepository().writeGlobal(PROP_ADMIN_SMTP,
                                    request.getString(PROP_ADMIN_SMTP, ""));

        if (request.exists(PROP_REPOSITORY_NAME)) {
            getRepository().writeGlobal(
                PROP_REPOSITORY_NAME,
                request.getString(PROP_REPOSITORY_NAME, ""));
        }

        if (request.exists(PROP_ADMIN_PHRASES)) {
            getRepository().writeGlobal(PROP_ADMIN_PHRASES,
                                        request.getString(PROP_ADMIN_PHRASES,
                                            ""));
        }

        if (request.exists(PROP_HTML_FOOTER)) {
            getRepository().writeGlobal(PROP_HTML_FOOTER,
                                        request.getString(PROP_HTML_FOOTER,
                                            ""));
        }

        if (request.exists(PROP_GOOGLEAPIKEYS)) {
            getRepository().writeGlobal(PROP_GOOGLEAPIKEYS,
                                        request.getString(PROP_GOOGLEAPIKEYS,
                                            ""));
        }

        getRepository().writeGlobal(
                                    PROP_SSL_PORT,
                                    request.getString(PROP_SSL_PORT, getProperty(PROP_SSL_PORT,"")));


        if (request.exists(PROP_LOCALFILEPATHS)) {
            getRepository().writeGlobal(
                PROP_LOCALFILEPATHS,
                request.getString(PROP_LOCALFILEPATHS, ""));
            getRepository().setLocalFilePaths();
            getRepository().clearCache();
        }

        List<OutputHandler> outputHandlers =
            getRepository().getOutputHandlers();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.applySettings(request);
        }

        List<OutputType> types = getRepository().getOutputTypes();
        boolean          doAll = request.get("outputtype.all", false);
        for (OutputType type : types) {
            if ( !type.getForUser()) {
                continue;
            }
            boolean ok = doAll
                         || request.get("outputtype." + type.getId(), false);
            //            if(!ok)System.err.println("TYPE:" + type + " " + ok);
            getRepository().setOutputTypeOK(type, ok);
        }

        getRepository().writeGlobal(PROP_ACCESS_ADMINONLY,
                                    request.get(PROP_ACCESS_ADMINONLY,
                                        false));
        getRepository().writeGlobal(PROP_ACCESS_REQUIRELOGIN,
                                    request.get(PROP_ACCESS_REQUIRELOGIN,
                                        false));
        return new Result(request.url(URL_ADMIN_SETTINGS));
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
    public Result adminAccess(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(msgHeader("Access Overview"));

        Statement stmt = getDatabaseManager().execute(
                             "select "
                             + SqlUtil.comma(
                                 Tables.PERMISSIONS.COL_ENTRY_ID,
                                 Tables.PERMISSIONS.COL_ACTION,
                                 Tables.PERMISSIONS.COL_ROLE) + " from "
                                     + Tables.PERMISSIONS.NAME, 10000000, 0);

        Hashtable<String, List> idToPermissions = new Hashtable<String,
                                                      List>();

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        List<String>     ids = new ArrayList<String>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                String id          = results.getString(1);
                String action      = results.getString(2);
                String role        = results.getString(3);
                List   permissions = idToPermissions.get(id);
                if (permissions == null) {
                    idToPermissions.put(id, permissions = new ArrayList());
                    ids.add(id);
                }
                permissions.add(new Permission(action, role));
            }
        }

        sb.append("<table cellspacing=\"0\" cellpadding=\"0\">");
        sb.append(
            HtmlUtil.row(
                HtmlUtil.cols(
                    HtmlUtil.space(10),
                    HtmlUtil.b("Action") + HtmlUtil.space(3),
                    HtmlUtil.b("Role"))));
        for (String id : ids) {
            Entry entry = getEntryManager().getEntry(request, id);
            sb.append(
                HtmlUtil.row(
                    HtmlUtil.colspan(
                        getEntryManager().getBreadCrumbs(
                            request, entry,
                            getRepository().URL_ACCESS_FORM), 3)));
            List<Permission> permissions =
                (List<Permission>) idToPermissions.get(id);
            for (Permission permission : permissions) {
                sb.append(HtmlUtil.row(HtmlUtil.cols("",
                        permission.getAction(),
                        permission.getRoles().get(0))));

            }
            sb.append(HtmlUtil.row(HtmlUtil.colspan("<hr>", 3)));
        }
        sb.append("</table>");

        return makeResult(request, msg("Access Overview"), sb);
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
    public Result adminStats(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();

        sb.append(msgHeader("Repository State"));
        sb.append(HtmlUtil.formTable());
        getStorageManager().addInfo(sb);
        getDatabaseManager().addInfo(sb);
        sb.append(HtmlUtil.formTableClose());
        sb.append("<p>");

        sb.append(msgHeader("Repository Database Statistics"));
        sb.append("<table>\n");
        String[] names = { msg("Users"), msg("Associations"),
                           msg("Metadata Items") };
        String[] tables = { Tables.USERS.NAME, Tables.ASSOCIATIONS.NAME,
                            Tables.METADATA.NAME };
        for (int i = 0; i < tables.length; i++) {
            sb.append(HtmlUtil.row(HtmlUtil.cols(""
                    + getDatabaseManager().getCount(tables[i].toLowerCase(),
                        new Clause()), names[i])));
        }


        sb.append(
            HtmlUtil.row(
                HtmlUtil.colspan(HtmlUtil.bold(msgLabel("Types")), 2)));
        int total = 0;
        sb.append(
            HtmlUtil.row(
                HtmlUtil.cols(
                    "" + getDatabaseManager().getCount(
                        Tables.ENTRIES.NAME, new Clause()), msg(
                        "Total entries"))));
        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            if (typeHandler.isType(TypeHandler.TYPE_ANY)) {
                continue;
            }
            int cnt = getDatabaseManager().getCount(Tables.ENTRIES.NAME,
                          Clause.eq("type", typeHandler.getType()));

            String url =
                HtmlUtil.href(
                    request.url(
                        getRepository().URL_SEARCH_FORM, ARG_TYPE,
                        typeHandler.getType()), typeHandler.getLabel());
            sb.append(HtmlUtil.row(HtmlUtil.cols("" + cnt, url)));
        }



        sb.append("</table>\n");

        return makeResult(request, msg("Statistics"), sb);
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
    public Result adminSql(Request request) throws Exception {

        boolean bulkLoad = false;
        String  query    = null;
        String  sqlFile  = request.getUploadedFile(ARG_SQLFILE);
        if ((sqlFile != null) && (sqlFile.length() > 0)
                && new File(sqlFile).exists()) {
            query = IOUtil.readContents(sqlFile, getClass());
            if ((query != null) && (query.trim().length() > 0)) {
                bulkLoad = true;
            }
        }
        if ( !bulkLoad) {
            query = (String) request.getUnsafeString(ARG_QUERY,
                    (String) null);
            if ((query != null) && query.trim().startsWith("file:")) {
                query = IOUtil.readContents(query.trim().substring(5),
                                            getClass());
                bulkLoad = true;
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(msgHeader("SQL"));
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.href(request.url(URL_ADMIN_TABLES),
                                msg("View Schema")));
        sb.append(HtmlUtil.bold("&nbsp;|&nbsp;"));
        sb.append(HtmlUtil.href(request.url(URL_ADMIN_DUMPDB),
                                msg("Dump Database")));
        sb.append(HtmlUtil.p());
        sb.append(request.uploadForm(URL_ADMIN_SQL));
        sb.append(HtmlUtil.submit(msg("Execute")));
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.textArea(ARG_QUERY, (bulkLoad
                ? ""
                : (query == null)
                  ?BLANK
                  :query),10,100));
        sb.append(HtmlUtil.p());
        sb.append("SQL File: ");
        sb.append(HtmlUtil.fileInput(ARG_SQLFILE, ""));
        sb.append(HtmlUtil.formClose());
        sb.append("<table>");
        if (query == null) {
            return makeResult(request, msg("SQL"), sb);
        }

        long t1 = System.currentTimeMillis();

        if (bulkLoad) {
            Connection connection = getDatabaseManager().getNewConnection();
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            SqlUtil.loadSql(query, statement, false, true);
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            return makeResult(request, msg("SQL"),
                              new StringBuffer("Executed SQL" + "<P>"
                                  + HtmlUtil.space(1) + sb.toString()));

        } else {
            Statement statement = null;
            try {
                statement = getDatabaseManager().execute(query, -1, 0);
            } catch (Exception exc) {
                exc.printStackTrace();
                throw exc;
            }

            SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
            ResultSet        results;
            int              cnt    = 0;
            Hashtable        map    = new Hashtable();
            int              unique = 0;
            while ((results = iter.next()) != null) {
                ResultSetMetaData rsmd = results.getMetaData();
                while (results.next()) {
                    cnt++;
                    if (cnt > 1000) {
                        continue;
                    }
                    int colcnt = 0;
                    if (cnt == 1) {
                        sb.append("<table><tr>");
                        for (int i = 0; i < rsmd.getColumnCount(); i++) {
                            sb.append(
                                HtmlUtil.col(
                                    HtmlUtil.bold(
                                        rsmd.getColumnLabel(i + 1))));
                        }
                        sb.append("</tr>");
                    }
                    sb.append("<tr>");
                    while (colcnt < rsmd.getColumnCount()) {
                        colcnt++;
                        if (rsmd.getColumnType(colcnt)
                                == java.sql.Types.TIMESTAMP) {
                            Date dttm = results.getTimestamp(colcnt,
                                            Repository.calendar);
                            sb.append(HtmlUtil.col(formatDate(request,
                                    dttm)));
                        } else {
                            sb.append(
                                HtmlUtil.col(results.getString(colcnt)));
                        }
                    }
                    sb.append("</tr>\n");
                    //                if (cnt++ > 1000) {
                    //                    sb.append(HtmlUtil.row("..."));
                    //                    break;
                    //                }
                }
            }
            sb.append("</table>");
            long t2 = System.currentTimeMillis();
            getRepository().clearCache();
            getRepository().readGlobals();
            return makeResult(request, msg("SQL"),
                              new StringBuffer(msgLabel("Fetched rows") + cnt
                                  + HtmlUtil.space(1) + msgLabel("in")
                                  + (t2 - t1) + "ms <p>" + sb.toString()));
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
    public Result adminScanForBadParents(Request request) throws Exception {
        boolean      delete = request.get("delete", false);
        StringBuffer sb     = new StringBuffer();
        Statement stmt = getDatabaseManager().execute("select "
                             + Tables.ENTRIES.COL_ID + ","
                             + Tables.ENTRIES.COL_PARENT_GROUP_ID + " from "
                             + Tables.ENTRIES.NAME, 10000000, 0);
        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        int              cnt        = 0;
        List<Entry>      badEntries = new ArrayList<Entry>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                String id       = results.getString(1);
                String parentId = results.getString(2);
                cnt++;
                if (parentId != null) {
                    Group group = getEntryManager().findGroup(request,
                                      parentId);
                    if (group == null) {
                        Entry entry = getEntryManager().getEntry(request, id);
                        sb.append("bad parent:" + entry.getName()
                                  + " parent id=" + parentId + "<br>");
                        badEntries.add(entry);
                    }
                }
            }
        }
        sb.append("Scanned " + cnt + " entries");
        if (delete) {
            getEntryManager().deleteEntries(request, badEntries, null);
            return makeResult(request, msg("Scan"),
                              new StringBuffer("Deleted"));
        }
        return makeResult(request, msg("Scan"), sb);
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
    public Result adminCleanup(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(request.form(URL_ADMIN_CLEANUP));
        if (request.defined(ACTION_STOP)) {
            runningCleanup = false;
            cleanupTS++;
            return new Result(request.url(URL_ADMIN_CLEANUP));
        } else if (request.defined(ACTION_START)) {
            Misc.run(this, "runDatabaseCleanUp", request);
            return new Result(request.url(URL_ADMIN_CLEANUP));
        } else if (request.defined(ACTION_CLEARCACHE)) {
            getRepository().clearCache();
        }
        String status = cleanupStatus.toString();
        if (runningCleanup) {
            sb.append(msg("Database clean up is running"));
            sb.append("<p>");
            sb.append(HtmlUtil.submit(msg("Stop cleanup"), ACTION_STOP));
        } else {
            sb.append(
                msg(
                "Cleanup allows you to remove all file entries from the repository database that do not exist on the local file system"));
            sb.append("<p>");
            sb.append(HtmlUtil.submit(msg("Start cleanup"), ACTION_START));

            sb.append("<p>");
            sb.append(HtmlUtil.submit(msg("Clear cache"), ACTION_CLEARCACHE));

        }
        sb.append("</form>");
        if (status.length() > 0) {
            sb.append(msgHeader("Cleanup Status"));
            sb.append(status);
        }
        //        sb.append(cnt +" files do not exist in " + (t2-t1) );
        return makeResult(request, msg("Cleanup"), sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void runDatabaseCleanUp(Request request) throws Exception {
        if (runningCleanup) {
            return;
        }
        runningCleanup = true;
        cleanupStatus  = new StringBuffer();
        int myTS = ++cleanupTS;
        try {
            Statement stmt =
                getDatabaseManager().select(
                    SqlUtil.comma(
                        Tables.ENTRIES.COL_ID, Tables.ENTRIES.COL_RESOURCE,
                        Tables.ENTRIES.COL_TYPE), Tables.ENTRIES.NAME,
                            Clause.eq(
                                Tables.ENTRIES.COL_RESOURCE_TYPE,
                                Resource.TYPE_FILE));

            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            int              cnt       = 0;
            int              deleteCnt = 0;
            long             t1        = System.currentTimeMillis();
            List<Entry>      entries   = new ArrayList<Entry>();
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    if ((cleanupTS != myTS) || !runningCleanup) {
                        runningCleanup = false;
                        break;
                    }
                    int    col = 1;
                    String id  = results.getString(col++);
                    String resource = getStorageManager().resourceFromDB(
                                          results.getString(col++));
                    Entry entry = getRepository().getTypeHandler(
                                      results.getString(col++)).createEntry(
                                      id);
                    File f = new File(resource);
                    if (f.exists()) {
                        continue;
                    }
                    //TODO: differentiate the entries that are not files
                    entries.add(entry);
                    if (entries.size() % 1000 == 0) {
                        System.err.print(".");
                    }
                    if (entries.size() > 1000) {
                        getEntryManager().deleteEntries(request, entries,
                                null);
                        entries   = new ArrayList<Entry>();
                        deleteCnt += 1000;
                        cleanupStatus = new StringBuffer("Removed "
                                + deleteCnt + " entries from database");
                    }
                }
                if ((cleanupTS != myTS) || !runningCleanup) {
                    runningCleanup = false;
                    break;
                }
            }
            if (runningCleanup) {
                getEntryManager().deleteEntries(request, entries, null);
                deleteCnt += entries.size();
                cleanupStatus = new StringBuffer(msg("Done running cleanup")
                        + "<br>" + msg("Removed") + HtmlUtil.space(1)
                        + deleteCnt + " entries from database");
            }
        } catch (Exception exc) {
            log("Running cleanup", exc);
            cleanupStatus.append("An error occurred running cleanup<pre>");
            cleanupStatus.append(LogUtil.getStackTrace(exc));
            cleanupStatus.append("</pre>");
        }
        runningCleanup = false;
        long t2 = System.currentTimeMillis();
    }



    /** _more_ */
    int ccnt = 0;





}


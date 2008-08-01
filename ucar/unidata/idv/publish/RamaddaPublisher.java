
/*
 * $Id: InfoceteraBlogger.java,v 1.13 2005/05/13 18:31:06 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.publish;



import ucar.unidata.idv.*;

import ucar.unidata.repository.Constants;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.IOUtil;



import java.awt.*;

import javax.swing.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import java.net.*;

import java.io.*;

import ucar.unidata.xml.XmlUtil;

import HTTPClient.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Note, This is very much in flux.
 * A client to an infocetera weblog (see: http://www.infocetera.com).
 *
 * @author IDV development team
 */
public class RamaddaPublisher extends ServerPublisher {



    /** Attribute name for the dirpath */
    public static final String ATTR_DIRPATH = "dirpath";


    private String sessionId;


    /** Infocetera is based on a directory structure */
    String dirPath = null;

    /**
     * Create the object
     *
     * @param idv The idv
     */
    public RamaddaPublisher(IntegratedDataViewer idv) {
        this(idv, null);
    }

    /**
     * Create the object
     *
     * @param idv The idv
     * @param element The xml element that defined us
     */
    public RamaddaPublisher(IntegratedDataViewer idv, Element element) {
        super(idv, element);
    }

    /**
     * Initialize
     */
    protected void init() {
        super.init();
        String prefix = getPropertyPrefix();
        setDirpath(
            (String) idv.getStateManager().getPreferenceOrProperty(
                prefix + ATTR_DIRPATH));
        if (dirPath == null) {
            setDirpath(XmlUtil.getAttribute(initElement, ATTR_DIRPATH,
                                            NULL_STRING));
        }
    }

    /**
     * Save opff any preferences
     */
    protected void savePreferences() {
        super.savePreferences();
        String prefix = getPropertyPrefix();
        if (dirPath != null) {
            idv.getStateManager().putPreference(prefix + ATTR_DIRPATH,
                                                dirPath);
        }
    }



    /**
     * Check the return xml from the http post
     *
     * @param xml The xml we go back from infocetera
     * @return Was this ok
     */
    public boolean resultOk(String xml) {
        if (xml.indexOf("error") >= 0) {
            return false;
        }
        if (xml.indexOf("Error") >= 0) {
            return false;
        }
        if (xml.indexOf("ERROR") >= 0) {
            return false;
        }
        return true;
    }


    /**
     * What is the infocetera directory
     *
     * @param props The properties
     * @return The infocetera directory name
     */
    private String getDir(Properties props) {
        String dir = (String) props.get("dir");
        if (dir != null) {
            return dir;
        }
        return dirPath;
    }

    /**
     * Post the given file to the infocetera weblog
     *
     * @param file The file to post
     * @param props  The properties that contain the infocetera dirpath
     * @return Was this successful
     */
    public boolean publishFile(String file, Properties props) {
        try {
            HTTPClient.NVPair[] opts = { new HTTPClient.NVPair("NAME", ""),
                                         new HTTPClient.NVPair("DESC", ""),
                                         new HTTPClient.NVPair("OVERWRITE",
                                             "1"),
                                         new HTTPClient.NVPair("_user", user),
                                         new HTTPClient.NVPair("_password",
                                             password) };

            HTTPClient.NVPair[] files = { new HTTPClient.NVPair("FILE",
                                            file) };
            HTTPClient.NVPair[] hdrs           = new HTTPClient.NVPair[1];
            byte[] formData = Codecs.mpFormDataEncode(opts, files, hdrs);
            HTTPConnection      conn           = getConnection();
            String              fileUploadPath = getDir(props) + "/files_put";
            HTTPResponse res = conn.Post(fileUploadPath, formData, hdrs);
            if (res.getStatusCode() >= 300) {
                LogUtil.userErrorMessage(new String(res.getData()));
                return false;
            }
            byte[] data = res.getData();
            String html = new String(data);
            if ( !resultOk(html)) {
                showError("An error has occurred posting the file", html);
                return false;
            }
            return true;

        } catch (Exception exc) {
            LogUtil.logException("Posting file", exc);
            return false;
        }
    }


    /**
     *  This method strips the html tags from the given html error message.
     *  It also strips out the copyright and other footer lines
     *  and shows the results to the user.
     *
     * @param msg The message
     * @param html  The html
     */
    protected void showError(String msg, String html) {
        int idx = -1;
        if (idx < 0) {
            idx = html.indexOf("error");
        }
        if (idx < 0) {
            idx = html.indexOf("Error");
        }
        if (idx < 0) {
            idx = html.indexOf("ERROR");
        }
        JComponent p = null;
        if (idx >= 0) {
            idx  = idx - 10;
            html = html.substring(idx);
            html = StringUtil.stripTags(html);
            idx  = 0;
            List lines = StringUtil.split(html, "\n");
            html = "";
            for (int i = 0; i < lines.size(); i++) {
                String line = (String) lines.get(i);
                if ((line.indexOf("Copyright") >= 0)
                        || (line.indexOf("WTS") >= 0)
                        || (line.indexOf("LLC") >= 0)) {
                    continue;
                }
                html = html + line + "\n";
            }
            p = GuiUtils.makeScrollPane(new JTextArea(html, 10, 50), 10, 50);
        } else {
            html = "";
        }
        JPanel msgPanel = GuiUtils.topCenter(new JLabel(msg), p);
        LogUtil.userErrorMessage(msgPanel);
    }


    /**
     * What is the name of this publisher
     *
     * @return The name
     */
    public String getName() {
        return "Ramadda repository";
    }

    /**
     * Have we been configured
     *
     * @return Is configured
     */
    public boolean isConfigured() {
        return super.isConfigured();
    }


    /**
     * Do the configuration
     *
     * @return Configuration ok
     */
    public boolean configure() {
        JTextField serverFld   = new JTextField((server == null)
                                                ? ""
                                                : server, 30);
        JTextField passwordFld = new JTextField((password == null)
                                                ? ""
                                                : password, 30);
        JTextField userFld     = new JTextField((user == null)
                                                ? ""
                                                : user, 30);
        List       comps       = new ArrayList();
        comps.add(GuiUtils.rLabel("Server:"));
        comps.add(GuiUtils.inset(serverFld, 4));
        comps.add(GuiUtils.rLabel("User name:"));
        comps.add(GuiUtils.inset(userFld, 4));
        comps.add(GuiUtils.rLabel("Password:"));
        comps.add(GuiUtils.inset(passwordFld, 4));
        JPanel p = GuiUtils.doLayout(comps, 2, GuiUtils.WT_Y,
                                     GuiUtils.WT_NNY);
        p = GuiUtils.topCenter(
            GuiUtils.cLabel("Please provide the following information"), p);

        while (true) {
            if ( !GuiUtils.askOkCancel(
                    "Configure access to Infocetera weblog", p)) {
                return false;
            }
            setServer(serverFld.getText());
            setUser(userFld.getText());
            setPassword(passwordFld.getText());
            savePreferences();
            idv.getStateManager().writePreferences();
            if ( !isConfigured()) {
                LogUtil.userMessage(
                    "One or more of the given values is null");
            } else {
                break;
            }
        }
        return true;
    }


    private boolean validSession() throws Exception {
        sessionId="foo";
        if(sessionId == null) return false;
        String url = "http://" + getServer() +"/repository/user/home?output=xml&sessionid="+ sessionId;
        String contents = IOUtil.readContents(url, getClass());
        Element root =XmlUtil.getRoot(contents);
        System.err.println(contents);
        return XmlUtil.getAttribute(root,"code").equals("ok");
    }

    public boolean doLogin() throws Exception {
        String url = "http://" + getServer() +"/repository/user/home?output=xml&" + "password1"+"=" + getPassword()+"&"+
            Constants.ARG_USER_ID +"=" + getUser();
        
        return true;
    }

    public void doPublish(String title, final String filePath,
                          String properties) {
        try {
            if(!validSession()) {
                if(!doLogin()) return;
            }
        System.err.println ("do publish " + title + " " + filePath);
        } catch (Exception exc) {
            LogUtil.logException("Checking session", exc);
        }

    }


    /**
     * Post the message and the file to the infocetera weblogp
     *
     * @param subject The subject
     * @param label The label for the link
     * @param msg The weblog message
     * @param filename Filename to post
     * @param props  The properties that contain the infocetera dirpath
     * @return Was this successful
     */
    public boolean publishMessage(String subject, String label, String msg,
                                  String filename, Properties props) {
        try {
            String link = "";
            if (filename != null) {
            }

            HTTPClient.NVPair[] opts = {
                new HTTPClient.NVPair("entryTitle", subject),
                new HTTPClient.NVPair("response", "xml"),
                new HTTPClient.NVPair("entryLink", link),
                new HTTPClient.NVPair("entryLabel", label),
                new HTTPClient.NVPair("entryContent", msg),
                new HTTPClient.NVPair("dopublish", "1"),
                new HTTPClient.NVPair("_user", user),
                new HTTPClient.NVPair("_password", password)
            };

            HTTPConnection conn         = getConnection();
            String         blogPostPath = getDir(props) + "/blog_change";
            HTTPResponse   res          = conn.Post(blogPostPath, opts);
            if (res.getStatusCode() >= 300) {
                LogUtil.userErrorMessage(new String(res.getData()));
                return false;
            }
            byte[] data = res.getData();
            String html = new String(data);
            System.err.println("html:" + html);
            if ( !resultOk(html)) {
                showError("An error has occurred posting to the weblog",
                          html);
                return false;
            }
        } catch (Exception exc) {
            LogUtil.logException("Posting message", exc);
            return false;
        }
        return true;
    }


    /**
     * Set the infocetera directory path
     *
     * @param value The path
     */
    public void setDirpath(String value) {
        dirPath = clean(value);
        if (dirPath != null) {
            //Check for valid path
        }
    }

    /**
     * Get the infocetera directory path
     *
     * @return The path
     */
    public String getDirpath() {
        return dirPath;
    }


}








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


import HTTPClient.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import ucar.unidata.idv.*;

import ucar.unidata.repository.RepositoryClient;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



import java.awt.*;

import java.io.*;

import java.net.*;

import java.util.ArrayList;

import java.util.List;
import java.util.Properties;

import javax.swing.*;


/**
 * Note, This is very much in flux.
 * A client to an infocetera weblog (see: http://www.infocetera.com).
 *
 * @author IDV development team
 */
public class RamaddaPublisher extends IdvPublisher {

    /** _more_          */
    private RepositoryClient client;


    /**
     * _more_
     */
    public RamaddaPublisher() {}



    /**
     * Create the object
     *
     * @param idv The idv
     */
    public RamaddaPublisher(IntegratedDataViewer idv) {
        super(idv, null);
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
     * Post the given file to the infocetera weblog
     *
     * @param file The file to post
     * @param props  The properties that contain the infocetera dirpath
     * @return Was this successful
     */
    public boolean publishFile(String file, Properties props) {
        return false;
    }

    /*

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
    */




    /**
     * Do the configuration
     *
     * @return Configuration ok
     */
    public boolean configure() {
        try {
            String     server      = ((client != null)
                                      ? client.getHostname()
                                      : "");
            String     user        = ((client != null)
                                      ? client.getUser()
                                      : "");
            String     password    = ((client != null)
                                      ? client.getPassword()
                                      : "");

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
                GuiUtils.cLabel("Please provide the following information"),
                p);

            while (true) {
                if ( !GuiUtils.askOkCancel(
                        "Configure access to Infocetera weblog", p)) {
                    return false;
                }
                client = new RepositoryClient(serverFld.getText().trim(), 80,
                        "/repository");
                client.setUser(userFld.getText());
                client.setPassword(passwordFld.getText());
                if ( !isConfigured()) {
                    LogUtil.userMessage(
                        "One or more of the given values is null");
                } else {
                    break;
                }
            }
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Doing configuration", exc);
            return false;
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isConfigured() {
        if (client == null) {
            return false;
        }
        try {
            if ( !client.getIsValidSession()) {
                return client.doLogin();
            }
        } catch (Exception exc) {
            LogUtil.logException("Doing configuration", exc);
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @param title _more_
     * @param filePath _more_
     * @param properties _more_
     */
    public void doPublish(String title, final String filePath,
                          String properties) {
        try {
            if ( !isConfigured()) {
                return;
            }
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
        return false;
    }

    /**
     *
     *   try {
     *       String link = "";
     *       if (filename != null) {
     *       }
     *
     *       HTTPClient.NVPair[] opts = {
     *           new HTTPClient.NVPair("entryTitle", subject),
     *           new HTTPClient.NVPair("response", "xml"),
     *           new HTTPClient.NVPair("entryLink", link),
     *           new HTTPClient.NVPair("entryLabel", label),
     *           new HTTPClient.NVPair("entryContent", msg),
     *           new HTTPClient.NVPair("dopublish", "1"),
     *           new HTTPClient.NVPair("_user", user),
     *           new HTTPClient.NVPair("_password", password)
     *       };
     *
     *       HTTPConnection conn         = getConnection();
     *       String         blogPostPath = getDir(props) + "/blog_change";
     *       HTTPResponse   res          = conn.Post(blogPostPath, opts);
     *       if (res.getStatusCode() >= 300) {
     *           LogUtil.userErrorMessage(new String(res.getData()));
     *           return false;
     *       }
     *       byte[] data = res.getData();
     *       String html = new String(data);
     *       System.err.println("html:" + html);
     *       if ( !resultOk(html)) {
     *           showError("An error has occurred posting to the weblog",
     *                     html);
     *           return false;
     *       }
     *   } catch (Exception exc) {
     *       LogUtil.logException("Posting message", exc);
     *       return false;
     *   }
     *   return true;
     * }
     */


}


/*
 * $Id: ServerPublisher.java,v 1.12 2005/05/13 18:31:06 jeffmc Exp $
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



import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ucar.unidata.idv.*;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.net.*;

import java.io.*;

import HTTPClient.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Note: This is very much in flux.
 * <p>
 * Class ServerPublisher is used for publishers that talk to some server.
 *
 * @author IDV development team
 */
public abstract class ServerPublisher extends IdvPublisher {


    /** More clear than doing (String) null */
    public static final String NULL_STRING = null;

    /** The port to connect to */
    int port = 80;

    /** Server hostname */
    String server = null;

    /** The user to log in as (if needed) */
    String user = null;

    /** The password to log in with (if needed) */
    String password = null;


    /** Key to look up the server */
    public static final String ATTR_SERVER = "server";

    /** Key to look up the port */
    public static final String ATTR_PORT = "port";

    /** Key to look up the user */
    public static final String ATTR_USER = "user";

    /** Key to look up the password */
    public static final String ATTR_PASSWORD = "password";


    /**
     * Create the object
     *
     * @param idv The idv
     * @param element The xml element that defined us
     */
    protected ServerPublisher(IntegratedDataViewer idv, Element element) {
        super(idv, element);
    }

    /**
     * Initialize
     */
    protected void init() {
        super.init();
        String prefix = getPropertyPrefix();

        setServer(
            (String) idv.getStateManager().getPreferenceOrProperty(
                prefix + ATTR_SERVER));
        setUser((String) idv.getStateManager().getPreferenceOrProperty(prefix
                + ATTR_USER));
        setPassword(
            (String) idv.getStateManager().getPreferenceOrProperty(
                prefix + ATTR_PASSWORD));
        setPort((String) idv.getStateManager().getPreferenceOrProperty(prefix
                + ATTR_PORT));

        if (initElement == null) {
            return;
        }
        if (server == null) {
            setServer(XmlUtil.getAttribute(initElement, ATTR_SERVER,
                                           NULL_STRING));
        }
        if (user == null) {
            setUser(XmlUtil.getAttribute(initElement, ATTR_USER,
                                         NULL_STRING));
        }
        if (password == null) {
            setPassword(XmlUtil.getAttribute(initElement, ATTR_PASSWORD,
                                             NULL_STRING));
        }
        setPort(XmlUtil.getAttribute(initElement, ATTR_PORT, NULL_STRING));
    }




    /**
     * Write out the preferences
     */
    protected void savePreferences() {
        super.savePreferences();
        String prefix = getPropertyPrefix();
        if (server != null) {
            idv.getStateManager().putPreference(prefix + ATTR_SERVER, server);
        }
        idv.getStateManager().putPreference(prefix + ATTR_PORT, "" + port);
        if (user != null) {
            idv.getStateManager().putPreference(prefix + ATTR_USER, user);
        }
        if (password != null) {
            idv.getStateManager().putPreference(prefix + ATTR_PASSWORD,
                                                password);
        }
    }

    /**
     * Make the connection
     *
     * @return The connection
     */
    protected HTTPConnection getConnection() {
        return new HTTPConnection(server, port);
    }


    /**
     * Get the given property from the props table
     *
     * @param props The properties
     * @param key Property key
     * @return The property or the empty string if not found
     */
    private String getProp(Properties props, String key) {
        String prop = (String) props.get(key);
        if (prop == null) {
            return "";
        }
        return prop;
    }

    /**
     * Publish
     *
     * @param title The title
     * @param filePath The filepath
     * @param properties The properties
     */
    public void doPublish(String title, final String filePath,
                          String properties) {
        final Properties props = Misc.parseProperties(properties);

        final JTextField subjectFld = new JTextField(getProp(props,
                                          "subject"));
        final JTextArea msgFld = new JTextArea(getProp(props, "message"), 10,
                                               40);
        final JTextField filenameFld = ((filePath != null)
                                        ? new JTextField(getProp(props,
                                            "label"))
                                        : null);
        List comps = new ArrayList();
        comps.add(GuiUtils.rLabel(" Subject:  "));
        comps.add(GuiUtils.inset(subjectFld, 4));
        if (filenameFld != null) {
            comps.add(GuiUtils.rLabel(" Save as:  "));
            comps.add(GuiUtils.inset(filenameFld, 4));
        }
        comps.add(GuiUtils.rLabel(" Message:  "));
        comps.add(GuiUtils.inset(GuiUtils.makeScrollPane(msgFld, 100, 200),
                                 4));

        JPanel p = GuiUtils.doLayout(comps, 2, GuiUtils.WT_Y,
                                     GuiUtils.WT_NNY);
        if ( !GuiUtils.askOkCancel("Publish", p)) {
            return;
        }
        String subject = subjectFld.getText();
        String message = msgFld.getText();
        String label   = ((filenameFld == null)
                          ? ""
                          : filenameFld.getText());
        publishMessageAndFile(subject, label, message, filePath, props);
    }

    /**
     * Is string defined, i.e., is it non-null and non zero length
     *
     * @param s The string
     * @return Is defined
     */
    public boolean defined(String s) {
        return ((s != null) && (s.trim().length() > 0));
    }

    /**
     * Trim the string
     *
     * @param s The string
     * @return The trimmed string
     */
    public String clean(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }

    /**
     * Is this publisher configured
     *
     * @return  Is this publisher configured
     */
    public boolean isConfigured() {
        return defined(server) && defined(user) && defined(password);
    }

    /**
     * Set the server value
     *
     * @param value The value
     */
    public void setServer(String value) {
        server = clean(value);
    }

    /**
     * Get the server value
     *
     * @return The server
     */
    public String getServer() {
        return server;
    }

    /**
     * Set the port value
     *
     * @param value The new port value
     */
    public void setPort(String value) {
        value = clean(value);
        if ((value == null) || (value.trim().length() == 0)) {
            return;
        }
        port = new Integer(value).intValue();
    }

    /**
     * Get the port value
     *
     * @return The port value
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the user value
     *
     * @param value The user
     */
    public void setUser(String value) {
        user = clean(value);
    }

    /**
     * Get the user value
     *
     * @return The user
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the password value
     *
     * @param value The password
     */
    public void setPassword(String value) {
        password = clean(value);
    }

    /**
     * Get the password value
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }







}






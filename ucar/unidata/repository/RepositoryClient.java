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


import org.w3c.dom.Element;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.List;
import java.util.ArrayList;
import java.awt.*;

import javax.swing.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryClient extends RepositoryBase {

    /** _more_          */
    private String sessionId;

    /** _more_          */
    private String user="";

    /** _more_          */
    private String password="";

    private String name = "RAMADDA Client";


    public RepositoryClient() {}


    public boolean  doConnect() {
        String[]msg = new String[]{""};

        if(!isValidSession(true, msg)) {
            if(user.length()!=0) {
                JLabel lbl = new JLabel("<html>Could not connect to RAMADDA:<blockquote>" + msg[0]+"</blockquote>Do you want to configure the connection?</html>");
                if(!GuiUtils.showOkCancelDialog(null,"RAMADDA Connection Error", GuiUtils.inset(lbl,5),null)) return false;
            }
            return showConfigDialog();
        }
        return true;
    }


    public String getSessionId() {
        return sessionId;
    }

    private boolean  showConfigDialog() {
        JTextField nameFld   = new JTextField(name, 30);
        JTextField serverFld   = new JTextField(getHostname(),30);
        JTextField pathFld   = new JTextField(getUrlBase(),30);
        JTextField portFld   = new JTextField(""+getPort());
        JTextField passwordFld = new JPasswordField(password);
        JTextField userFld     = new JTextField(user,30);
        List       comps       = new ArrayList();
        comps.add(GuiUtils.rLabel("Name:"));
        comps.add(GuiUtils.inset(nameFld, 4));
        comps.add(GuiUtils.rLabel("Server:"));
        comps.add(GuiUtils.inset(serverFld, 4));
        comps.add(GuiUtils.rLabel("Port:"));
        comps.add(GuiUtils.inset(portFld, 4));
        comps.add(GuiUtils.rLabel("Base Path:"));
        comps.add(GuiUtils.inset(pathFld, 4));
        comps.add(GuiUtils.rLabel("User name:"));
        comps.add(GuiUtils.inset(userFld, 4));
        comps.add(GuiUtils.rLabel("Password:"));
        comps.add(GuiUtils.inset(passwordFld, 4));
        JPanel contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_Y,
                                     GuiUtils.WT_NNY);
        contents = GuiUtils.topCenter(
                               GuiUtils.cLabel("Please provide the following information"),
                               contents);

        while (true) {
            if ( !GuiUtils.askOkCancel(
                                       "Configure access to RAMADDA", contents)) {
                return false;
            }
            setName(nameFld.getText());
            setHostname(serverFld.getText().trim());
            setPort(new Integer(portFld.getText().trim()).intValue());
            setUrlBase(pathFld.getText().trim());
            user = userFld.getText().trim();
            password = passwordFld.getText().trim();
            String[]msg = {""};
            if (isValidSession(true,msg)) {
                LogUtil.userMessage("Configuration succeeded");
                break;
            } 
            LogUtil.userMessage(msg[0]);
        }
        return true;
    }


    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(String hostname, int port, String base)
            throws Exception {
        super(hostname, port);
        setUrlBase(base);
    }


    public boolean isValidSession(boolean doLogin, String[]msg) {
        if(!isValidSession(msg)) {
            if(doLogin)  return doLogin(msg);
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isValidSession(String[]msg)  {
        if (sessionId == null) {
            msg[0] = "No session id";
            return false;
        }
        try {
        String url =
            HtmlUtil.url(URL_USER_HOME.getFullUrl(),
                         new String[] { ARG_OUTPUT,
                                        "xml", ARG_SESSIONID, sessionId });
        String  contents = IOUtil.readContents(url, getClass());
        Element root     = XmlUtil.getRoot(contents);
        if(responseOk(root)) {
            return true;
        } else {
            msg[0] =  XmlUtil.getChildText(root).trim();
            return false;
        }
        } catch(Exception exc) {
            msg[0] = "Could not connect to server:" + getHostname();
            return false;
        }
    }

    private boolean responseOk(Element root) {
        return XmlUtil.getAttribute(root, ATTR_CODE).equals("ok");
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean doLogin(String[]msg)  {
        try {
        String url =
            HtmlUtil.url(URL_USER_LOGIN.getFullUrl(),
                         new String[] {
                             ARG_OUTPUT, "xml", ARG_USER_PASSWORD, getPassword(),
                             ARG_USER_ID, getUser()
                         });
        String  contents = IOUtil.readContents(url, getClass());
        Element root     = XmlUtil.getRoot(contents);
        String body = XmlUtil.getChildText(root).trim();
        if (responseOk(root)) {
            sessionId = body;
            return true;
        } else {
            msg[0] = body;
            return false;
        }
        } catch(Exception exc) {
            msg[0] = "Could not connect to server:" + getHostname();
        }
        return false;
    }

    /**
     *  Set the Password property.
     *
     *  @param value The new value for Password
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     *  Get the Password property.
     *
     *  @return The Password
     */
    public String getPassword() {
        return password;
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUser() {
        return user;
    }


/**
Set the Name property.

@param value The new value for Name
**/
public void setName (String value) {
	name = value;
}

/**
Get the Name property.

@return The Name
**/
public String getName () {
	return name;
}



}


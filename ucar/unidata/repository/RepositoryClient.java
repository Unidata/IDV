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
    private String user;

    /** _more_          */
    private String password;

    public RepositoryClient() {}


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


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean getIsValidSession() throws Exception {
        if (sessionId == null) {
            return false;
        }
        String url =
            HtmlUtil.url(URL_USER_HOME.getFullUrl(),
                         new String[] { ARG_OUTPUT,
                                        "xml", ARG_SESSIONID, sessionId });
        String  contents = IOUtil.readContents(url, getClass());
        Element root     = XmlUtil.getRoot(contents);
        System.err.println(contents);
        return XmlUtil.getAttribute(root, "code").equals("ok");
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean doLogin() throws Exception {
        while (true) {
            String url =
                HtmlUtil.url(URL_USER_LOGIN.getFullUrl(),
                             new String[] {
                ARG_OUTPUT, "xml", ARG_USER_PASSWORD, getPassword(),
                ARG_USER_ID, getUser()
            });
            String  contents = IOUtil.readContents(url, getClass());
            Element root     = XmlUtil.getRoot(contents);
            if (XmlUtil.getAttribute(root, ATTR_CODE).equals("ok")) {
                sessionId = XmlUtil.getChildText(root).trim();
                return true;
            }
            String         message     = XmlUtil.getChildText(root).trim();
            JTextField     nameFld     = new JTextField(getUser());
            JPasswordField passwordFld = new JPasswordField(getPassword());
            JComponent comp =
                GuiUtils.topCenter(new JLabel("Error:" + message),
                                   GuiUtils.doLayout(new Component[] {
                                       GuiUtils.rLabel("User Name:"),
                                       nameFld, GuiUtils.rLabel("Password:"),
                                       passwordFld, }, 2, GuiUtils.WT_NY,
                                           GuiUtils.WT_N));

            if (true) {
                break;
            }
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




}


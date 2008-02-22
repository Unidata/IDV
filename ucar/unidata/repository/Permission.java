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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Permission {

    /** _more_ */
    public static final String ACTION_VIEW = "view";

    /** _more_ */
    public static final String ACTION_EDIT = "edit";

    /** _more_ */
    public static final String ACTION_NEW = "new";

    /** _more_ */
    public static final String ACTION_DELETE = "delete";

    /** _more_ */
    public static final String ACTION_COMMENT = "comment";


    /** _more_ */
    public static final String[] ACTIONS = { ACTION_VIEW, ACTION_EDIT,
                                             ACTION_NEW, ACTION_DELETE,
                                             ACTION_COMMENT };

    /** _more_ */
    public static final String[] ACTION_NAMES = { "View", "Edit", "New",
            "Delete", "Comment" };




    /** _more_ */
    String action;

    /** _more_ */
    List<String> roles;

    /**
     * _more_
     *
     * @param action _more_
     * @param role _more_
     */
    public Permission(String action, String role) {
        this.action = action;
        roles       = new ArrayList<String>();
        roles.add(role);
    }


    /**
     * _more_
     *
     * @param action _more_
     * @param roles _more_
     */
    public Permission(String action, List<String> roles) {
        this.action = action;
        this.roles  = roles;
    }



    /**
     * _more_
     *
     * @param actions _more_
     *
     * @return _more_
     */
    public static boolean isValidActions(List actions) {
        for (int i = 0; i < actions.size(); i++) {
            if ( !isValidAction((String) actions.get(i))) {
                return false;
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public static boolean isValidAction(String action) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].equals(action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "action:" + action + " roles:" + roles;
    }




    /**
     * Set the Action property.
     *
     * @param value The new value for Action
     */
    public void setAction(String value) {
        action = value;
    }

    /**
     * Get the Action property.
     *
     * @return The Action
     */
    public String getAction() {
        return action;
    }

    /**
     * Set the Roles property.
     *
     * @param value The new value for Roles
     */
    public void setRoles(List<String> value) {
        roles = value;
    }

    /**
     * Get the Roles property.
     *
     * @return The Roles
     */
    public List<String> getRoles() {
        return roles;
    }

}


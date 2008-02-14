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



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.*;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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



/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryManager implements RepositorySource, Constants,
                                          Tables, RequestHandler {




    /** _more_ */
    protected Repository repository;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public RepositoryManager(Repository repository) {
        this.repository = repository;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DatabaseManager getDatabaseManager() {
        return repository.getDatabaseManager();
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String name, String dflt) {
        return repository.getProperty(name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        return repository.getProperty(name, dflt);
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String header(String h) {
        return repository.header(h);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Admin getAdmin() {
        return repository.getAdmin();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected UserManager getUserManager() {
        return repository.getUserManager();
    }

    protected AccessManager getAccessManager() {
        return repository.getAccessManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected StorageManager getStorageManager() {
        return repository.getStorageManager();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param urls _more_
     *
     * @return _more_
     */
    protected List getSubNavLinks(Request request, RequestUrl[] urls) {
        return repository.getSubNavLinks(request, urls);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    protected void log(Request request, String message) {
        repository.log(request, message);

    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    protected void log(String message, Throwable exc) {
        repository.log(message, exc);
    }

}


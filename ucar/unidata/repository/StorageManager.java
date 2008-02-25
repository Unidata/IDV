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
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.zip.*;


import javax.swing.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class StorageManager extends RepositoryManager {


    /**
     * _more_
     *
     * @param repository _more_
     */
    public StorageManager(Repository repository) {
        super(repository);
    }

    /** _more_ */
    private List<String> downloadPrefixes = new ArrayList<String>();

    /** _more_ */
    private String repositoryDir;

    /** _more_ */
    private String tmpDir;

    /** _more_ */
    private String uploadDir;

    /** _more_ */
    private String storageDir;

    /** _more_ */
    private String thumbDir;

    /**
     * _more_
     *
     */
    protected void init() {
        repositoryDir = getRepository().getProperty(PROP_REPOSITORY_HOME,
                (String) null);
        if (repositoryDir == null) {
            repositoryDir =
                IOUtil.joinDir(Misc.getSystemProperty("user.home", "."),
                               IOUtil.joinDir(".unidata", "repository"));
        }
        IOUtil.makeDirRecursive(new File(repositoryDir));

        String htdocsDir = IOUtil.joinDir(repositoryDir, "htdocs");
        IOUtil.makeDir(htdocsDir);
        String resourcesDir = IOUtil.joinDir(repositoryDir, "resources");
        IOUtil.makeDir(resourcesDir);

        getUploadDir();
    }


    /**
     * _more_
     *
     * @param prefix _more_
     */
    public void addDownloadPrefix(String prefix) {
        prefix = prefix.replace("\\", "/");
        downloadPrefixes.add(prefix);
    }




    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public String localizePath(String path) {
        path = path.replace("%repositorydir%", getRepositoryDir());
        return path;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSystemResourcePath() {
        return "/ucar/unidata/repository/resources";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUploadDir() {
        if (uploadDir == null) {
            uploadDir = IOUtil.joinDir(getRepositoryDir(), "uploads");
            IOUtil.makeDirRecursive(new File(uploadDir));
        }
        return uploadDir;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRepositoryDir() {
        return repositoryDir;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTmpDir() {
        if (tmpDir == null) {
            tmpDir = IOUtil.joinDir(getRepositoryDir(), "tmp");
            IOUtil.makeDirRecursive(new File(tmpDir));
        }
        return tmpDir;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getStorageDir() {
        if (storageDir == null) {
            storageDir = IOUtil.joinDir(getRepositoryDir(), "storage");
            IOUtil.makeDirRecursive(new File(storageDir));
            addDownloadPrefix(storageDir);
        }
        return storageDir;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getThumbDir() {
        if (thumbDir == null) {
            thumbDir = IOUtil.joinDir(getTmpDir(), "thumbnails");
            IOUtil.makeDir(thumbDir);
        }
        return thumbDir;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     *
     * @return _more_
     */
    public File getTmpFile(Request request, String name) {
        return new File(IOUtil.joinDir(getTmpDir(),
                                       getRepository().getGUID() + "_"
                                       + name));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param original _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File moveToStorage(Request request, File original)
            throws Exception {
        File newFile = new File(IOUtil.joinDir(getStorageDir(),
                           original.getName()));
        System.err.println("From:" + original);
        System.err.println("To:" + newFile);
        IOUtil.moveFile(original, newFile);
        return newFile;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        String filePath = entry.getResource().getPath();
        filePath = filePath.replace("\\", "/");
        getRepository().checkFilePath(filePath);
        return isInDownloadArea(filePath);
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    public void removeFile(Entry entry) {
        removeFile(entry.getResource());
    }

    /**
     * _more_
     *
     * @param resource _more_
     */
    public void removeFile(Resource resource) {
        if (resource.isLocalFile()) {
            resource.getFile().delete();
        }
    }

    /**
     * _more_
     *
     * @param filePath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isInDownloadArea(String filePath) throws Exception {
        //Force the creation of the storage dir
        getStorageDir();
        filePath = filePath.replace("\\", "/");
        for (String prefix : downloadPrefixes) {
            if (filePath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }




}


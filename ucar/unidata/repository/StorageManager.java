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



import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



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




/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class StorageManager extends RepositoryManager {

    /** _more_ */
    public static final String PROP_DIRDEPTH = "ramadda.storage.dirdepth";

    /** _more_ */
    public static final String PROP_DIRRANGE = "ramadda.storage.dirrange";


    /** _more_ */
    private int dirDepth = 2;

    /** _more_ */
    private int dirRange = 10;


    /** _more_ */
    private List<String> downloadPrefixes = new ArrayList<String>();

    /** _more_ */
    private String repositoryDir;

    /** _more_ */
    private String tmpDir;

    private String anonymousDir;

    /** _more_ */
    private String uploadDir;

    /** _more_ */
    private String entriesDir;

    /** _more_ */
    private String storageDir;

    /** _more_ */
    private String thumbDir;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public StorageManager(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param resource _more_
     *
     * @return _more_
     */
    public String resourceFromDB(String resource) {
        if (resource != null) {
            resource = resource.replace("${ramadda.storagedir}",
                                        getStorageDir());
        }
        return resource;
    }

    /**
     * _more_
     *
     * @param resource _more_
     *
     * @return _more_
     */
    public String resourceToDB(String resource) {
        if (resource != null) {
            resource = resource.replace(getStorageDir(),
                                        "${ramadda.storagedir}");
        }
        return resource;
    }


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

        dirDepth = getRepository().getProperty(PROP_DIRDEPTH, dirDepth);
        dirRange = getRepository().getProperty(PROP_DIRRANGE, dirRange);
        getUploadDir();
    }


    /**
     * _more_
     *
     * @param sb _more_
     */
    protected void addInfo(StringBuffer sb) {
        sb.append(HtmlUtil.formEntry("Home Directory:", getRepositoryDir()));
        sb.append(HtmlUtil.formEntry("Storage Directory:", getStorageDir()));
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
        path = path.replace("%resourcedir%",
                            "/ucar/unidata/repository/resources");
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

    public String getAnonymousDir() {
        if (anonymousDir == null) {
            anonymousDir = IOUtil.joinDir(getStorageDir(), "anonymousupload");
            IOUtil.makeDirRecursive(new File(anonymousDir));
        }
        return anonymousDir;
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
    public String getPluginsDir() {
        String dir = IOUtil.joinDir(getRepositoryDir(), "plugins");
        IOUtil.makeDirRecursive(new File(dir));
        return dir;
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
        return moveToStorage(request, original, "");
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String cleanEntryId(String id) {
        return IOUtil.cleanFileName(id);
    }

    /**
     * _more_
     *
     *
     * @param id _more_
     * @param createIfNeeded _more_
     * @return _more_
     */
    public File getEntryDir(String id, boolean createIfNeeded) {
        id = cleanEntryId(id);
        if (entriesDir == null) {
            entriesDir = IOUtil.joinDir(getRepositoryDir(), "entries");
            IOUtil.makeDirRecursive(new File(entriesDir));
        }
        File entryDir = new File(IOUtil.joinDir(entriesDir, id));
        if (createIfNeeded) {
            IOUtil.makeDirRecursive(entryDir);
        }
        return entryDir;
    }



    /**
     * _more_
     *
     * @param id _more_
     */
    public void deleteEntryDir(final String id) {
        Misc.run(new Runnable() {
            public void run() {
                File dir = getEntryDir(id, false);
                if (dir.exists()) {
                    IOUtil.deleteDirectory(dir);
                }
            }
        });
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param original _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File moveToEntryDir(Entry entry, File original) throws Exception {
        File newFile = new File(IOUtil.joinDir(getEntryDir(entry.getId(),
                           true), original.getName()));
        IOUtil.moveFile(original, newFile);
        return newFile;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param original _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File copyToEntryDir(Entry entry, File original) throws Exception {
        File newFile = new File(IOUtil.joinDir(getEntryDir(entry.getId(),
                           true), original.getName()));
        IOUtil.copyFile(original, newFile);
        return newFile;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param original _more_
     * @param prefix _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File moveToStorage(Request request, File original, String prefix)
            throws Exception {
        String            storageDir = getStorageDir();
        String            targetName = prefix + original.getName();

        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(new Date());
        storageDir = IOUtil.joinDir(storageDir, "y" + cal.get(cal.YEAR));
        IOUtil.makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "m" + (cal.get(cal.MONTH) + 1));
        IOUtil.makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "d" + cal.get(cal.DAY_OF_MONTH));
        IOUtil.makeDir(storageDir);

        for (int depth = 0; depth < dirDepth; depth++) {
            int index = (int) (dirRange * Math.random());
            storageDir = IOUtil.joinDir(storageDir, "data" + index);
            IOUtil.makeDir(storageDir);
        }

        File newFile = new File(IOUtil.joinDir(storageDir, targetName));
        IOUtil.moveFile(original, newFile);
        return newFile;
    }



    public File moveToAnonymousStorage(Request request, File original, String prefix)
        throws Exception {
        String            storageDir = getAnonymousDir();
        File[]files =new File(storageDir).listFiles();
        long size = 0;
        for(int i=0;i<files.length;i++) {
            if(files[i].isHidden()) continue;
            size+= files[i].length();
        }

        double sizeThresholdGB = getRepository().getProperty(PROP_UPLOAD_MAXSIZEGB,10.0);
        if(size+original.length()> sizeThresholdGB*1000*1000*1000) throw new IllegalArgumentException("Anonymous upload area exceeded capacity");
        String            targetName = prefix + original.getName();
        File newFile = new File(IOUtil.joinDir(storageDir, targetName));
        IOUtil.moveFile(original, newFile);
        return newFile;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param original _more_
     * @param newName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File copyToStorage(Request request, File original, String newName)
            throws Exception {
        String            targetName = newName;
        String            storageDir = getStorageDir();

        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(new Date());

        storageDir = IOUtil.joinDir(storageDir, "y" + cal.get(cal.YEAR));
        IOUtil.makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "m" + (cal.get(cal.MONTH) + 1));
        IOUtil.makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "d" + cal.get(cal.DAY_OF_MONTH));
        IOUtil.makeDir(storageDir);


        for (int depth = 0; depth < dirDepth; depth++) {
            int index = (int) (dirRange * Math.random());
            storageDir = IOUtil.joinDir(storageDir, "data" + index);
            IOUtil.makeDir(storageDir);
        }

        File newFile = new File(IOUtil.joinDir(storageDir, targetName));
        IOUtil.copyFile(original, newFile);
        return newFile;
    }

    /**
     * _more_
     *
     * @param fileName _more_
     *
     * @return _more_
     */
    public File getUploadFilePath(String fileName) {
        return new File(IOUtil.joinDir(getUploadDir(),
                                       repository.getGUID() + "_"
                                       + fileName));
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getFileTail(Entry entry) {
        if (entry.getIsLocalFile()) {
            return IOUtil.getFileTail(entry.getResource().getPath());
        }
        return getFileTail(entry.getResource().getPath());
    }

    /**
     * _more_
     *
     * @param fileName _more_
     *
     * @return _more_
     */
    public String getFileTail(String fileName) {
        int idx = fileName.indexOf("_");
        if (idx >= 0) {
            return fileName.substring(idx + 1);
        }
        return fileName;
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
        RepositoryUtil.checkFilePath(filePath);
        if (entry.getIsLocalFile()) {
            return true;
        }
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
        if (resource.isStoredFile()) {
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


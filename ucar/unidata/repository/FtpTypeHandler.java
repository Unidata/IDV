/**
 * $Id: ,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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


import org.apache.commons.net.ftp.*;


import org.w3c.dom.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class FtpTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final int COL_SERVER = 0;

    /** _more_ */
    public static final int COL_BASEDIR = 1;

    /** _more_ */
    public static final int COL_USER = 2;

    /** _more_ */
    public static final int COL_PASSWORD = 3;

    /** _more_ */
    public static final int COL_MAXSIZE = 4;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public FtpTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
        if (getEntryManager().isSynthEntry(entry.getId())) {
            return getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
        }
        return this;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        return true;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getIconUrl(Request request, Entry entry) throws Exception {
        if (entry.isGroup()) {
            if ( !getEntryManager().isSynthEntry(entry.getId())) {
                return iconUrl(ICON_FTP);
            }
            return iconUrl(ICON_FTP);
        }
        return super.getIconUrl(request, entry);
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
        Entry mainEntry = getMainEntry(entry.getId());
        if (mainEntry == null) {
            return false;
        }
        Object[] values = mainEntry.getValues();
        if (values == null) {
            return false;
        }
        double maxSize = 0;
        if (values[COL_MAXSIZE] != null) {
            maxSize = ((Double) values[COL_MAXSIZE]).doubleValue();
        }
        return entry.getResource().getFileSize() < 1000000 * maxSize;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry getMainEntry(String id) throws Exception {
        String[] pair   = getEntryManager().getSynthId(id);
        Entry    parent = getEntryManager().getEntry(null, pair[0]);
        return parent;
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public File getFileForEntry(Entry entry) {
        try {
            File  badFile = new File("badfile");
            Entry parent  = getMainEntry(entry.getId());
            if (parent == null) {
                System.err.println("Could not find main entry");
                return badFile;
            }
            Object[] values = parent.getValues();
            if (values == null) {
                System.err.println("FtpTypeHandler: no values");
                return badFile;
            }
            double maxSize = 0;
            if (values[COL_MAXSIZE] != null) {
                maxSize = ((Double) values[COL_MAXSIZE]).doubleValue();
            }
            String server = (String) values[COL_SERVER];
            if (entry.getResource().getFileSize() > 1000000 * maxSize) {
                //                System.err.println("FtpTypeHandler: Bad size "
                //                                   + entry.getResource().getFileSize()+" " +
                //                                   entry.getResource());
                return badFile;
            }
            FTPClient ftpClient = null;
            try {
                String[] pair = getEntryManager().getSynthId(entry.getId());
                MyFTPFile myFtpFile = getFileFromId(parent, pair[1],
                                          (String) values[COL_BASEDIR]);
                ftpClient = getFtpClient(parent);
                if (ftpClient == null) {
                    System.err.println("no ftp client ");
                    return badFile;
                }
                //                String path = entry.getResource().getPath();
                String path   = myFtpFile.path;
                String prefix = "ftp://" + server;
                if (path.startsWith(prefix)) {
                    path = path.substring(prefix.length());
                }
                String cacheFileName = java.net.URLEncoder.encode("ftp:"
                                           + values[COL_SERVER] + ":"
                                           + path, "UTF-8");
                File cacheFile =
                    getStorageManager().getCacheFile(cacheFileName);
                if (cacheFile.exists()) {
                    return cacheFile;
                }

                //                System.err.println("Fetching:" + path);
                //                System.err.println("writing to:" + cacheFile);
                OutputStream fos =
                    getStorageManager().getFileOutputStream(cacheFile);
                if (ftpClient.retrieveFile(path, fos)) {
                    fos.flush();
                    fos.close();
                    return cacheFile;
                }
                //                System.err.println ("BAD FILE");
                return badFile;
            } finally {
                if (ftpClient != null) {
                    closeConnection(ftpClient);
                }
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }


    }

    /**
     * _more_
     *
     * @param id _more_
       * @param baseDir _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getPathFromId(String id, String baseDir) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return baseDir;
        }
        return new String(XmlUtil.decodeBase64(id));
    }

    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param rootDirPath _more_
     * @param parentPath _more_
     * @param file _more_
     *
     * @return _more_
     */
    public String getSynthId(Entry parentEntry, String rootDirPath,
                             String parentPath, FTPFile file) {
        String id = parentPath + "/" + file.getName();
        id = XmlUtil.encodeBase64(id.getBytes()).replace("\n", "");
        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + id;
    }


    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param rootDirPath _more_
     * @param parentPath _more_
     *
     * @return _more_
     */
    public String getSynthId(Entry parentEntry, String rootDirPath,
                             String parentPath) {
        String id = parentPath;
        id = XmlUtil.encodeBase64(id.getBytes()).replace("\n", "");
        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + id;
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getSynthIds(Request request, Group mainEntry,
                                    Group parentEntry, String synthId)
            throws Exception {
        List<String> ids     = new ArrayList<String>();

        Object[]     values  = mainEntry.getValues();
        String       baseDir = (String) values[COL_BASEDIR];
        String       path    = getPathFromId(synthId, baseDir);

        /*        boolean descending = !request.get(ARG_ASCENDING, false);
        if (request.getString(ARG_ORDERBY, "").equals("name")) {
            files = IOUtil.sortFilesOnName(files, descending);
        } else {
            files = IOUtil.sortFilesOnAge(files, descending);
            }*/
        FTPClient ftpClient = getFtpClient(mainEntry);
        if (ftpClient == null) {
            return ids;
        }

        try {
            boolean isDir = ftpClient.changeWorkingDirectory(path);
            Hashtable<String, FTPFile> cache = getCache(mainEntry);
            if (isDir) {
                boolean checkReadme = parentEntry.getDescription().length()
                                      == 0;
                FTPFile[] files = ftpClient.listFiles(path);

                for (int i = 0; i < files.length; i++) {
                    String name = files[i].getName().toLowerCase();
                    if (checkReadme) {
                        if (name.equals("readme")
                                || name.equals("readme.txt")) {
                            try {
                                InputStream fis =
                                    ftpClient.retrieveFileStream(path + "/"
                                        + files[i].getName());
                                if (fis != null) {
                                    String desc = HtmlUtil.entityEncode(
                                                      IOUtil.readInputStream(
                                                          fis));
                                    parentEntry.setDescription("<pre>" + desc
                                            + "</pre>");
                                    fis.close();
                                    ftpClient.completePendingCommand();
                                }
                            } catch (Exception exc) {
                                //                            exc.printStackTrace();
                            }
                        }
                    }


                    cache.put(path + "/" + files[i].getName(), files[i]);
                    ids.add(getSynthId(mainEntry, baseDir, path, files[i]));
                }
            }
        } finally {
            closeConnection(ftpClient);
        }
        return ids;
    }


    /**
     * _more_
     *
     * @param ftpClient _more_
     */
    private static void closeConnection(FTPClient ftpClient) {
        try {
            ftpClient.logout();
        } catch (Exception exc) {}
        try {
            ftpClient.disconnect();
        } catch (Exception exc) {}
    }

    /**
     * _more_
     *
     * @param server _more_
     * @param baseDir _more_
     * @param user _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String test(String server, String baseDir, String user,
                              String password)
            throws Exception {
        FTPClient ftpClient = new FTPClient();
        try {
            String file = baseDir;
            ftpClient.connect(server);
            //System.out.print(ftp.getReplyString());
            ftpClient.login(user, password);
            //            System.out.print(ftpClient.getReplyString());
            int reply = ftpClient.getReplyCode();
            if ( !FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                System.err.println("FTP server refused connection.");
                return null;
            }
            ftpClient.setFileType(FTP.IMAGE_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            boolean isDir = ftpClient.changeWorkingDirectory(file);
            System.err.println("file:" + file + " is dir: " + isDir);

            if (isDir) {
                FTPFile[] files = ftpClient.listFiles(file);
                for (int i = 0; i < files.length; i++) {
                    //                    System.err.println ("f:" + files[i].getName() + " " + files[i].isDirectory() + "  " + files[i].isFile());
                }
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                if (ftpClient.retrieveFile(file, bos)) {
                    //                    System.err.println(new String(bos.toByteArray()));
                } else {
                    throw new IOException("Unable to retrieve file:" + file);
                }
            }
            return "";
        } finally {
            closeConnection(ftpClient);
        }
    }



    /**
     * _more_
     *
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private FTPClient getFtpClient(Entry parentEntry) throws Exception {
        Object[] values = parentEntry.getValues();
        if (values == null) {
            return null;
        }
        String server   = (String) values[COL_SERVER];
        String baseDir  = (String) values[COL_BASEDIR];
        String user     = (String) values[COL_USER];
        String password = (String) values[COL_PASSWORD];
        if (password != null) {
            password = getRepository().processTemplate(password, false);
        } else {
            password = "";
        }
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server);
            if (user != null) {
                ftpClient.login(user, password);
            }
            int reply = ftpClient.getReplyCode();
            if ( !FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                System.err.println("FTP server refused connection.");
                return null;
            }
            ftpClient.setFileType(FTP.IMAGE_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            return ftpClient;
        } catch (Exception exc) {
            System.err.println("Could not connect to ftp server:" + exc);
            return null;
        }
    }


    /** _more_ */
    Hashtable<String, Hashtable> cache = new Hashtable<String, Hashtable>();

    /**
     * _more_
     *
     * @param parentEntry _more_
     *
     * @return _more_
     */
    private Hashtable<String, FTPFile> getCache(Entry parentEntry) {
        Hashtable<String, FTPFile> map =
            (Hashtable<String, FTPFile>) cache.get(parentEntry.getId());
        if (map == null) {
            cache.put(parentEntry.getId(),
                      map = new Hashtable<String, FTPFile>());
        }
        //TODO:CHECK SIZE and flush
        return map;
    }



    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param id _more_
     * @param baseDir _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public MyFTPFile getFileFromId(Entry parentEntry, String id,
                                   String baseDir)
            throws Exception {
        String path;
        if ((id == null) || (id.length() == 0)) {
            FTPFile file = new FTPFile();
            file.setName(baseDir);
            file.setType(FTPFile.DIRECTORY_TYPE);
            return new MyFTPFile(file, baseDir);
        } else {
            path = new String(XmlUtil.decodeBase64(id));
        }
        Hashtable<String, FTPFile> cache   = getCache(parentEntry);
        FTPFile                    ftpFile = cache.get(path);
        if (ftpFile != null) {
            return new MyFTPFile(ftpFile, path);
        }

        FTPClient ftpClient = getFtpClient(parentEntry);
        if (ftpClient == null) {
            return null;
        }


        //        System.err.println("getFileFromId path=" + path);
        try {
            boolean isDir = ftpClient.changeWorkingDirectory(path);
            if (isDir) {
                File      tmp               = new File(path);
                String    parent = tmp.getParent().replace("\\", "/");
                String    name              = tmp.getName();
                FTPFile[] files             = ftpClient.listFiles(parent);
                MyFTPFile lookingForThisOne = null;
                for (int i = 0; i < files.length; i++) {
                    String childPath = parent + "/" + files[i].getName();
                    cache.put(childPath, files[i]);
                    if (files[i].getName().equals(name)) {
                        lookingForThisOne = new MyFTPFile(files[i],
                                childPath);
                    }
                }
                if (lookingForThisOne != null) {
                    return lookingForThisOne;
                }
                System.err.println("Could not find directory:" + name
                                   + " path:" + path);
                return null;
            } else {
                FTPFile[] files = ftpClient.listFiles(path);
                if (files.length == 1) {
                    cache.put(path, files[0]);
                    return new MyFTPFile(files[0], path);
                } else {
                    System.err.println(
                        "Got bad # of files when getting file:"
                        + files.length + "  " + path);
                }

            }
            return null;

        } finally {
            closeConnection(ftpClient);
        }

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        Object[] values = parentEntry.getValues();
        if (values == null) {
            return null;
        }
        String    baseDir   = (String) values[COL_BASEDIR];
        String    server    = (String) values[COL_SERVER];
        MyFTPFile myFtpFile = getFileFromId(parentEntry, id, baseDir);
        if (myFtpFile == null) {
            return null;
        }
        if (myFtpFile.path.equals(baseDir)) {
            return parentEntry;
        }
        FTPFile     ftpFile = myFtpFile.ftpFile;
        TypeHandler handler = (ftpFile.isDirectory()
                               ? getRepository().getTypeHandler(
                                   TypeHandler.TYPE_GROUP)
                               : getRepository().getTypeHandler(
                                   TypeHandler.TYPE_FILE));
        handler = this;
        String synthId = Repository.ID_PREFIX_SYNTH + parentEntry.getId()
                         + ":" + id;

        boolean isDir = ftpFile.isDirectory();
        Entry   entry = (isDir
                         ? (Entry) new Group(synthId, handler)
                         : new Entry(synthId, handler));

        String  name  = IOUtil.getFileTail(ftpFile.getName());
        entry.setIsLocalFile(true);
        Group parent;
        if (myFtpFile.path.equals(baseDir)) {
            parent = (Group) parentEntry;
        } else {
            File   tmp        = new File(myFtpFile.path);
            String parentPath = tmp.getParent().replace("\\", "/");
            String parentId   = getSynthId(parentEntry, baseDir, parentPath);
            if (parentPath.equals(baseDir)) {
                parent = (Group) parentEntry;
            } else {
                parent = (Group) getEntryManager().getEntry(request,
                        parentId, false, false);
            }
        }



        double maxSize = 0;
        if (values[COL_MAXSIZE] != null) {
            maxSize = ((Double) values[COL_MAXSIZE]).doubleValue();
        }



        long     dttm = ftpFile.getTimestamp().getTime().getTime();
        Resource resource;
        if (isDir) {
            resource = new Resource("ftp://" + server + myFtpFile.path,
                                    Resource.TYPE_URL);
        } else {
            if (ftpFile.getSize() > 1000000 * maxSize) {
                resource = new Resource("ftp://" + server + myFtpFile.path,
                                        Resource.TYPE_URL);
            } else {
                resource = new Resource(name, Resource.TYPE_REMOTE_FILE);

            }
            resource.setFileSize(ftpFile.getSize());
        }
        entry.initEntry(name, "", parent, getUserManager().localFileUser,
                        resource, "", dttm, dttm, dttm, null);


        return entry;
    }


    /**
     * Class MyFTPFile _more_
     *
     *
     * @author IDV Development Team
     */
    public static class MyFTPFile {

        /** _more_ */
        FTPFile ftpFile;

        /** _more_ */
        String path;

        /**
         * _more_
         *
         * @param ftpFile _more_
         * @param path _more_
         */
        public MyFTPFile(FTPFile ftpFile, String path) {
            this.ftpFile = ftpFile;
            this.path    = path;
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Group(id, this);
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        test("ftp.unidata.ucar.edu", "/pub", "anonymous", "");
        System.err.println("------------");
        test("ftp.unidata.ucar.edu", "/pub/idv/README", "anonymous", "");
        System.err.println("------------");
        test("ftp.unidata.ucar.edu", "/pub/ramadda/test", "anonymous", "");
        System.err.println("------------");
        test("ftp.unidata.ucar.edu", "/pub/ramadda/test/test", "anonymous",
             "");
    }



}


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

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class LocalFileTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public LocalFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
     * @param entry _more_
     *
     * @return _more_
     */
    public String getIconUrl(Request request,Entry entry) throws Exception {
        if (entry.isGroup()) {
            return fileUrl(ICON_FOLDER_CLOSED);
        }
        return super.getIconUrl(request,entry);
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
     * @param id _more_
     * @param baseFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getFileFromId(String id, File baseFile) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return baseFile;
        }
        String subPath = new String(XmlUtil.decodeBase64(id));
        File   file    = new File(IOUtil.joinDir(baseFile, subPath));
        if ( !IOUtil.isADescendent(baseFile, file)) {
            throw new IllegalArgumentException("Bad file path:" + subPath);
        }

        boolean ok = false;
        for (File parent : getRepository().getLocalFilePaths()) {
            if (IOUtil.isADescendent(parent, file)) {
                ok = true;
                break;
            }
        }
        if ( !ok) {
            if (getRepository().getLocalFilePaths().size() == 0) {
                throw new IllegalArgumentException(
                    "For security you must specify the allowable  file paths in the Administration screen");
            }
            throw new IllegalArgumentException(
                "The specified file is not under one of the allowable file system directories");
        }


        return file;
    }

    /** _more_ */
    public static final int COL_PATH = 0;

    /** _more_ */
    public static final int COL_AGE = 1;

    /** _more_ */
    public static final int COL_INCLUDES = 2;

    /** _more_ */
    public static final int COL_EXCLUDES = 3;

    /** _more_ */
    public static final int COL_NAMES = 4;

    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    public List<String> get(Object[] values, int idx) {
        if (values[idx] == null) {
            return new ArrayList<String>();
        }
        return (List<String>) StringUtil.split(values[idx], "\n", true, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getSynthIds(Request request, Group parentEntry,
                                    String synthId)
            throws Exception {
        List<String> ids    = new ArrayList<String>();
        Object[]     values = parentEntry.getValues();
        if (values == null) {
            return ids;
        }
        File   rootDir     = new File((String) values[0]);
        String rootDirPath = rootDir.toString();
        File   childPath   = getFileFromId(synthId, rootDir);
        File[] files       = childPath.listFiles();
        files = IOUtil.sortFilesOnName(files);
        List<String> includes = get(values, COL_INCLUDES);
        List<String> excludes = get(values, COL_EXCLUDES);
        long age = (long) (1000
                           * (((Double) values[COL_AGE]).doubleValue() * 60));
        long now = System.currentTimeMillis();
        for (int i = 0; i < files.length; i++) {
            File childFile = files[i];
            if (childFile.isHidden()) {
                continue;
            }
            if ((age != 0) && (now - childFile.lastModified()) < age) {
                continue;
            }
            if ( !match(childFile, includes, true)) {
                continue;
            }
            if (match(childFile, excludes, false)) {
                continue;
            }
            ids.add(getSynthId(parentEntry, rootDirPath, childFile));
        }
        return ids;
    }


    /**
     * _more_
     *
     * @param pattern _more_
     *
     * @return _more_
     */
    private String getRegexp(String pattern) {
        if ( !pattern.startsWith("regexp:")) {
            pattern = StringUtil.wildcardToRegexp(pattern);
        } else {
            pattern = pattern.substring("regexp:".length());
        }
        return pattern;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param patterns _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private boolean match(File file, List<String> patterns, boolean dflt) {
        String  value      = file.toString();
        boolean hadPattern = false;
        for (String pattern : patterns) {
            if (pattern.startsWith("dir:")) {
                if (file.isDirectory()) {
                    hadPattern = true;
                    pattern    =
                        getRegexp(pattern.substring("dir:".length()));
                    if (StringUtil.stringMatch(value, pattern, true, false)) {
                        return true;
                    }
                }
            } else {
                if ( !file.isDirectory()) {
                    hadPattern = true;
                    if (StringUtil.stringMatch(value, getRegexp(pattern),
                            true, false)) {
                        return true;
                    }
                }
            }
        }
        if (hadPattern) {
            return false;
        }
        return dflt;
    }

    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param rootDirPath _more_
     * @param childFile _more_
     *
     * @return _more_
     */
    public String getSynthId(Entry parentEntry, String rootDirPath,
                             File childFile) {
        String subId = childFile.toString().substring(rootDirPath.length());
        subId = XmlUtil.encodeBase64(subId.getBytes()).replace("\n", "");
        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + subId;
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
        List<Metadata> metadataList = getMetadataManager().getMetadata(parentEntry);
        Object[] values = parentEntry.getValues();
        if (values == null) {
            return null;
        }
        File rootDir    = new File((String) values[0]);
        File targetFile = getFileFromId(id, rootDir);
        if ( !rootDir.exists() || !targetFile.exists()) {
            return null;
        }
        //TODO: Check the time since last change here
        List<String> includes = get(values, COL_INCLUDES);
        List<String> excludes = get(values, COL_EXCLUDES);
        if ( !match(targetFile, includes, true)) {
            throw new IllegalArgumentException("File cannot be accessed");
        }
        if (match(targetFile, excludes, false)) {
            throw new IllegalArgumentException("File cannot be accessed");
        }

        String synthId = Repository.ID_PREFIX_SYNTH + parentEntry.getId()
                         + ":" + id;
        TypeHandler handler = (targetFile.isDirectory()
                               ? getRepository().getTypeHandler(
                                   TypeHandler.TYPE_GROUP)
                               : getRepository().getTypeHandler(
                                   TypeHandler.TYPE_FILE));
        Entry        entry  = (targetFile.isDirectory()
                               ? (Entry) new Group(synthId, handler)
                               : new Entry(synthId, handler));

        Entry templateEntry = getEntryManager().getTemplateEntry(targetFile);
        String       name   = null;
        List<String> names  = get(values, COL_NAMES);
        for (String pair : names) {
            boolean doPath = false;
            if (pair.startsWith("path:")) {
                pair   = pair.substring("path:".length());
                doPath = true;
            } else if (pair.startsWith("name:")) {
                pair   = pair.substring("name:".length());
                doPath = false;
            }
            if (name == null) {
                if (doPath) {
                    name = targetFile.toString();
                } else {
                    name = IOUtil.getFileTail(targetFile.toString());
                }
            }
            String[] tuple = StringUtil.split(pair, ":", 2);
            if ((tuple == null) || (tuple.length != 2)) {
                continue;
            }
            name = name.replaceAll(".*" + tuple[0] + ".*", tuple[1]);
        }
        if (name == null) {
            name = IOUtil.getFileTail(targetFile.toString());
        }
        entry.setIsLocalFile(true);
        Group parent;
        if (targetFile.getParentFile().equals(rootDir)) {
            parent = (Group) parentEntry;
        } else {
            String parentId = getSynthId(parentEntry, rootDir.toString(),
                                         targetFile.getParentFile());
            parent = (Group) getEntryManager().getEntry(request, parentId,
                    false, false);
        }

        entry.initEntry(name, "", parent, getUserManager().localFileUser,
                        new Resource(targetFile, (targetFile.isDirectory()
                ? Resource.TYPE_LOCAL_DIRECTORY
                : Resource.TYPE_LOCAL_FILE)), "", targetFile.lastModified(),
                targetFile.lastModified(), targetFile.lastModified(), null);

        if (templateEntry != null) {
            entry.initWith(templateEntry);
        }
        //Tack on the metadata
        entry.setMetadata(metadataList);
        return entry;
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

}


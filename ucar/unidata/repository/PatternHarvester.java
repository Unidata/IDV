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
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
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
public class PatternHarvester extends Harvester {

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_FILEPATTERN = "filepattern";


    /** _more_ */
    public static final String ATTR_BASEGROUP = "basegroup";




    /** _more_ */
    private SimpleDateFormat sdf;
    //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");

    /** _more_ */
    private String groupTemplate;

    /** _more_ */
    private String tagTemplate = "";

    /** _more_ */
    private String nameTemplate;

    /** _more_ */
    private String descTemplate;

    /** _more_ */
    private List<String> columns;

    /** _more_ */
    private List<String> patternNames;

    /** _more_ */
    private List<String> patternTypes;


    /** _more_ */
    private String filePatternString;

    /** _more_ */
    private String fileNotPatternString;

    /** _more_ */
    private Pattern filePattern;

    /** _more_ */
    private Pattern fileNotPattern;

    /** _more_ */
    private TypeHandler typeHandler;


    /** _more_ */
    private String baseGroupName;


    /** _more_ */
    private List<FileInfo> dirs;


    /** _more_ */
    private Hashtable dirMap = new Hashtable();


    User     user;

    int      rootStrLen;



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public PatternHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        this.typeHandler =
            repository.getTypeHandler(XmlUtil.getAttribute(element,
                ATTR_TYPE, TypeHandler.TYPE_ANY));
        this.filePatternString = XmlUtil.getAttribute(element,
                ATTR_FILEPATTERN, (String) null);
        this.baseGroupName = XmlUtil.getAttribute(element, ATTR_BASEGROUP,
                "Files");
        columns       = split(element, "columns");
        patternNames  = split(element, "patternnames");
        patternTypes  = split(element, "patterntypes");
        groupTemplate = XmlUtil.getAttribute(element, "grouptemplate", "");
        tagTemplate   = XmlUtil.getAttribute(element, "tagtemplate", "");
        nameTemplate = XmlUtil.getAttribute(element, "nametemplate",
                                            "${filename}");
        descTemplate = XmlUtil.getAttribute(element, "desctemplate", "");
        sdf = new SimpleDateFormat(XmlUtil.getAttribute(element,
                "dateformat", "yyyyMMdd_HHmm"));
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        user = repository.getUserManager().getDefaultUser();
        rootStrLen = rootDir.toString().length();

        init();
    }

    /**
     * _more_
     *
     * @param element _more_
     * @param attr _more_
     *
     * @return _more_
     */
    private List split(Element element, String attr) {
        if ( !XmlUtil.hasAttribute(element, attr)) {
            return null;
        }
        return StringUtil.split(XmlUtil.getAttribute(element, attr), ",");
    }



    /**
     * _more_
     */
    private void init() {
        if (filePatternString != null) {
            filePattern = Pattern.compile(filePatternString);
        }
        if (fileNotPatternString != null) {
            fileNotPattern = Pattern.compile(fileNotPatternString);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getExtraInfo() {
        return "Directory:" + rootDir + "";
    }

    /**
     * _more_
     *
     * @param dir _more_
     */
    private void removeDir(FileInfo dir) {
        dirs.remove(dir);
        dirMap.remove(dir.getFile());
    }

    /**
     * _more_
     *
     * @param dir _more_
     *
     * @return _more_
     */
    private FileInfo addDir(File dir) {
        FileInfo fileInfo = new FileInfo(dir, true);
        dirs.add(fileInfo);
        dirMap.put(dir, dir);
        return fileInfo;
    }

    /**
     * _more_
     *
     * @param dir _more_
     *
     * @return _more_
     */
    private boolean hasDir(File dir) {
        return dirMap.get(dir) != null;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void runInner() throws Exception {
        if ( !getActive()) {
            return;
        }
        long tt1 = System.currentTimeMillis();
        dirs = FileInfo.collectDirs(rootDir);
        long tt2 = System.currentTimeMillis();
        System.err.println("took:" + (tt2 - tt1) + " to find initial dirs:"
                           + dirs.size());

        for (FileInfo dir : dirs) {
            dirMap.put(dir.getFile(), dir);
        }

        int cnt = 0;
        while (getActive()) {
            long t1 = System.currentTimeMillis();
            collectEntries((cnt == 0));
            long t2 = System.currentTimeMillis();
            cnt++;
            //            System.err.println("found:" + entries.size() + " files in:"
            //                               + (t2 - t1) + "ms");
            if ( !getMonitor()) {
                break;
            }
            Misc.sleep((long) (getSleepMinutes() * 60 * 1000));
        }
    }





    /**
     * _more_
     *
     * @param rootDir _more_
     * @param firstTime _more_
     * @param typeHandler _more_
     *
     *
     * @throws Exception _more_
     */
    public void collectEntries(boolean firstTime)
            throws Exception {

        long           t1         = System.currentTimeMillis();
        List<Entry>    entries    = new ArrayList<Entry>();
        List<Entry>    needToAdd  = new ArrayList<Entry>();
        List<FileInfo> tmpDirs    = new ArrayList<FileInfo>(dirs);
        for (int dirIdx = 0; dirIdx < tmpDirs.size(); dirIdx++) {
            FileInfo fileInfo = tmpDirs.get(dirIdx);
            if ( !fileInfo.exists()) {
                removeDir(fileInfo);
                continue;
            }
            if ( !firstTime && !fileInfo.hasChanged()) {
                continue;
            }
            File[] files = fileInfo.getFile().listFiles();
            if (files == null) {
                continue;
            }
            for (int fileIdx = 0; fileIdx < files.length; fileIdx++) {
                File f = files[fileIdx];
                if (f.isDirectory()) {
                    //If this is a directory then check if we already have it 
                    //in the list. If not then add it to the main list and the local list
                    if ( !hasDir(f)) {
                        FileInfo newFileInfo = addDir(f);
                        tmpDirs.add(newFileInfo);
                    }
                    continue;
                }
                Entry entry = processFile(f);
                if (entry != null) {
                    entries.add(entry);
                }


                if (entries.size() > 1000) {
                    needToAdd.addAll(repository.getUniqueEntries(entries));
                    entries = new ArrayList();
                }
                if (needToAdd.size() > 1000) {
                    repository.insertEntries(needToAdd, true, true);
                    needToAdd = new ArrayList<Entry>();
                }

                if ( !getActive()) {
                    return;
                }
            }
        }

        needToAdd.addAll(repository.getUniqueEntries(entries));
        if (needToAdd.size() > 0) {
            repository.insertEntries(needToAdd, true, true);
        }
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    private Entry processFile(File f) throws Exception {



        //check if its a hidden file
        if (f.getName().startsWith(".")) {
            return null;
        }


        String fileName = f.toString();
        fileName = fileName.replace("\\", "/");

        String dirPath = f.getParent().toString();
        dirPath = dirPath.substring(rootStrLen);
        dirPath = SqlUtil.cleanUp(dirPath);
        dirPath = dirPath.replace("\\", "/");


        Matcher matcher = filePattern.matcher(fileName);
        if ( !matcher.find()) {
            return null;
        }


        Hashtable map       = new Hashtable();
        Date      fromDate  = null;
        Date      toDate    = null;
        String    tag       = tagTemplate;
        String    groupName = groupTemplate;
        String    name      = nameTemplate;
        String    desc      = descTemplate;

        Object[]  values    = null;
        if (columns != null) {
            for (int dataIdx = 0; dataIdx < patternNames.size(); dataIdx++) {
                String dataName = patternNames.get(dataIdx);
                Object value    = matcher.group(dataIdx + 1);
                String type     = patternTypes.get(dataIdx);
                if (type.equals("date")) {
                    try {
                        value = sdf.parse((String) value);
                    } catch (Exception exc) {
                        System.err.println("value:" + value);
                        System.err.println("file:" + fileName);
                    }
                } else if (type.equals("int")) {
                    value = new Integer(value.toString());
                } else if (type.equals("double")) {
                    value = new Double(value.toString());
                }
                if (dataName.equals("fromDate")) {
                    fromDate = (Date) value;
                } else if (dataName.equals("toDate")) {
                    toDate = (Date) value;
                } else {
                    groupName = groupName.replace("${" + dataName + "}",
                            value.toString());
                    name = name.replace("${" + dataName + "}",
                                        value.toString());
                    desc = desc.replace("${" + dataName + "}",
                                        value.toString());
                    map.put(dataName, value);
                }
            }


            values = new Object[columns.size()];
            for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                String colName = columns.get(colIdx);
                Object value   = map.get(colName);
                values[colIdx] = value;
            }
        }

        Date createDate = new Date();
        if (fromDate == null) {
            fromDate = toDate;
        }
        if (toDate == null) {
            toDate = fromDate;
        }
        if (fromDate == null) {
            fromDate = createDate;
        }
        if (toDate == null) {
            toDate = createDate;
        }

        List   dirToks  = StringUtil.split(dirPath, "/", true, true);

        String dirGroup = StringUtil.join("/", dirToks);


        String ext      = IOUtil.getFileExtension(fileName);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        tag       = tag.replace("${extension}", ext);

        groupName = groupName.replace("${dirgroup}", dirGroup);

        groupName = groupName.replace("${fromDate}",
                                      Repository.fmt(fromDate));
        groupName = groupName.replace("${toDate}", Repository.fmt(toDate));

        name      = name.replace("${filename}", f.getName());
        name      = name.replace("${fromDate}", Repository.fmt(fromDate));

        name      = name.replace("${toDate}", Repository.fmt(toDate));

        desc      = desc.replace("${fromDate}", Repository.fmt(fromDate));
        desc      = desc.replace("${toDate}", Repository.fmt(toDate));
        desc      = desc.replace("${name}", name);



        Group group = repository.findGroupFromName(baseGroupName + "/"
                          + groupName, user, true);
        Entry entry = typeHandler.createEntry(repository.getGUID());
        entry.init(name, desc, group, user,
                   new Resource(fileName, Resource.TYPE_FILE),
                   createDate.getTime(), fromDate.getTime(),
                   toDate.getTime(), values);
        if (tag.length() > 0) {
            entry.setTags(StringUtil.split(tag, ","));
        }
        typeHandler.initializeNewEntry(entry);


        return entry;

    }

    /**
     * _more_
     *
     * @param type _more_
     * @param filepath _more_
     *
     * @return _more_
     */
    public Entry processFile(TypeHandler type, String filepath) throws Exception {
        if ( !this.typeHandler.equals(type)) {
            return null;
        }
        return processFile(new File(filepath));
    }




}


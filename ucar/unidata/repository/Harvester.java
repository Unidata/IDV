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
public class Harvester {

    /** _more_          */
    private Repository repository;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public Harvester(Repository repository) {
        this.repository = repository;
    }


    /**
     * _more_
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> collectDummyLevel3RadarFiles(File rootDir,
                                                      String groupName, final TypeHandler typeHandler)
        throws Exception {
        List<Entry> entries = new ArrayList();
        long  baseTime    = repository.currentTime();
        Group group = repository.findGroupFromName(groupName,true);
        User user = repository.findUser("jdoe");
        for (int stationIdx = 0; stationIdx < 50; stationIdx++) {
            String station = "station" + stationIdx;
            for (int productIdx = 0; productIdx < 10; productIdx++) {
                String product = "product" + productIdx;
                group = repository.findGroupFromName(groupName + "/"
                        + station + "/" + product,true);
                for (int timeIdx = 0; timeIdx < 10; timeIdx++) {
                    long time =  baseTime + timeIdx * 1000 * 60*60;
                    entries.add(
                        new Entry(
                            repository.getGUID(), 
                            typeHandler, 
                            "file:" + stationIdx + "_" + productIdx + "_"
                            + group, 
                            "", 
                            group,
                            user,
                            "file:" + stationIdx + "_" + productIdx + "_"
                            + group, 
                            time,new Object[]{station,product, new Double(40), new Double(-107),"Banana", new Boolean(true), new Double(stationIdx)}));

                }
            }
        }

        return entries;
    }

    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @param typeHandler _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> collectLevel3RadarFiles(File rootDir,
            final String groupName, final TypeHandler typeHandler)
            throws Exception {
        long                         t1          = System.currentTimeMillis();
        final List<Entry> entries = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User        user       = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (entries.size() % 5000 == 0) {
                    System.err.println("Found:" + entries.size());
                }
                String station = matcher.group(1);
                String product = matcher.group(2);
                Group group = repository.findGroupFromName(groupName + "/"
                                  + "NIDS" + "/" + station + "/" + product,true);
                Date dttm = sdf.parse(matcher.group(3));
                entries.add(new Entry(repository.getGUID(),
                                             typeHandler, dttm.toString(), "", group, user,
                                             f.toString(),  dttm.getTime(), new Object[]{station, product, new Double(40), new Double(-107),"Banana", new Boolean(true), new Double(5)}));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        if(entries.size()>0) {
            System.err.println("found:" + entries.size() + " in "
                               + (t2 - t1));
        }
        return entries;
    }




    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @param typeHandler _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> collectLevel2radarFiles(File rootDir,
            final String groupName, final TypeHandler typeHandler)
            throws Exception {
        long                         t1          = System.currentTimeMillis();
        final List<Entry> entries = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User        user       = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (entries.size() % 5000 == 0) {
                    System.err.println("Found:" + entries.size());
                }
                String station = matcher.group(1);
                Group group = repository.findGroupFromName(groupName + "/"
                                  + "craft" + "/" + station,true);
                Date dttm = sdf.parse(matcher.group(2));
                entries.add(new Entry(repository.getGUID(),
                        typeHandler, dttm.toString(), "", group, user,
                        f.toString(),dttm.getTime(), new Object[]{station}));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found:" + entries.size() + " in "
                           + (t2 - t1));
        return entries;
    }



    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @param typeHandler _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> collectSatelliteFiles(File rootDir,
            final String groupName, final TypeHandler typeHandler)
            throws Exception {
        long                       t1      = System.currentTimeMillis();
        final List<Entry> entries = new ArrayList();
        final SimpleDateFormat     sdf =
            new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User        user       = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (entries.size() % 5000 == 0) {
                    System.err.println("Found:" + entries.size());
                }
                String platform   = matcher.group(1);
                String resolution = matcher.group(2);
                String product    = matcher.group(3);
                Group group = repository.findGroupFromName(groupName +  "/" + platform + "/"
                                  + resolution + "/" + product,true);
                Date dttm = sdf.parse(matcher.group(4));
                entries.add(new Entry(repository.getGUID(),
                        typeHandler, dttm.toString(), "", group, user,
                        f.toString(),
                        dttm.getTime(),
                                              new Object[]{platform, resolution, product}));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        if(entries.size()>0) {
            System.err.println("found sat files:" + entries.size() + " in "
                               + (t2 - t1));
        }
        return entries;
    }


    /**
     * _more_
     *
     * @param rootDir _more_
     * @param rootGroup _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> collectFiles(File rootDir, final String rootGroup,
                                           final TypeHandler typeHandler,
                                    final List<FileInfo> directories)
            throws Exception {
        final String      rootStr    = rootDir.toString();
        final int         rootStrLen = rootStr.length();
        final List<Entry> entries    = new ArrayList();
        final User        user       = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String name = f.getName();
                //                System.err.println(name);
                if (name.startsWith(".")) {
                    return DO_DONTRECURSE;
                }
                if (f.isDirectory()) {
                    directories.add(new FileInfo(f,true));
                    return DO_CONTINUE;
                }
                //                if ( !name.endsWith(".java")) {
                //                    return DO_CONTINUE;
                //                }
                String path    = f.toString();
                String noext   = IOUtil.stripExtension(path);


                String dirPath = f.getParent().toString();
                dirPath = dirPath.substring(rootStrLen);
                List toks = StringUtil.split(dirPath, File.separator, true,
                                             true);
                toks.add(0, rootGroup);
                Group group =
                    repository.findGroupFromName(StringUtil.join("/", toks),true);
                Entry entry = new Entry(repository.getGUID(),
                                               typeHandler, name, name, group,
                                               user, f.toString(),
                                               f.lastModified(),null);
                String ext = IOUtil.getFileExtension(path);
                if (ext.startsWith(".")) {
                    ext = ext.substring(1);
                }
                if (ext.trim().length() > 0) {
                    entry.addTag(ext);
                }
                entries.add(entry);
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        return entries;
    }


    /**
     * _more_
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> collectModelFiles(File rootDir,
            final String groupName, final TypeHandler typeHandler)
            throws Exception {
        final List<Entry> entries = new ArrayList();
        final SimpleDateFormat[] sdf = { new SimpleDateFormat("yyyyMMddHH"),
                                         new SimpleDateFormat("yyyyMMdd") };
        final String      regex = "([^/]+)/[^/\\d]*(\\d+)(f\\d+)?_([^\\.]+)";
        final Pattern     pattern    = Pattern.compile(regex);
        final User        user       = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    if ( !f.isDirectory()) {
                        System.err.println(name);
                        System.err.println(regex);
                    }
                    return DO_CONTINUE;
                }
                //                    System.err.println (name);
                if (entries.size() % 5000 == 0) {
                    System.err.println("Found:" + entries.size());
                }
                String modelGroup = matcher.group(1);
                Date   dttm       = null;
                String dateString = matcher.group(2);
                for (int i = 0; i < sdf.length; i++) {
                    try {
                        dttm = sdf[i].parse(dateString);
                        break;
                    } catch (Exception exc) {}
                }
                if (dttm == null) {
                    System.err.println("Could not parse date:" + dateString);
                    return DO_CONTINUE;
                }
                String forecasrHour = matcher.group(3);
                String modelRun     = matcher.group(4);


                Group group = repository.findGroupFromName(groupName + "/"
                                  + "model" + "/" + modelGroup + "/"
                                  + modelRun,true);

                entries.add(new Entry(repository.getGUID(), typeHandler,
                                           IOUtil.getFileTail(f.toString()),
                                           "", group, user, f.toString(),
                                           dttm.getTime(),
                                             new Object[]{modelGroup, modelRun}));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        return entries;
    }




}


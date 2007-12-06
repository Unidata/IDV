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
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;
import org.w3c.dom.*;

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
import java.util.Enumeration;
import java.util.Date;
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

    private Repository repository;

    public Harvester(Repository repository) {
        this.repository = repository;
    }


    /**
     * _more_
     *
     * @param rootDir _more_
     * @param groupName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Level3RadarInfo> xxxxcollectLevel3radarFiles(File rootDir,
            String groupName)
            throws Exception {
        final List<Level3RadarInfo> radarInfos = new ArrayList();
        long                  baseTime   = repository.currentTime();
        Group                 group      = repository.findGroupFromName(groupName);
        User user = repository.findUser("jdoe");
        for (int stationIdx = 0; stationIdx < 120; stationIdx++) {
            String station = "station" + stationIdx;
            for (int productIdx = 0; productIdx < 20; productIdx++) {
                String product = "product" + productIdx;
                group = repository.findGroupFromName(groupName + "/" + station + "/" + product);
                for (int timeIdx = 0; timeIdx < 1000; timeIdx++) {
                    radarInfos.add(new Level3RadarInfo(repository.getGUID(),
                                                       "", "", group,
                                                       user,
                                                       "file" + stationIdx + "_" +productIdx
                                                       + "_" + group, station, product,
                                                       baseTime
                                                       + timeIdx*1000*60));
                    
                }
            }
        }

        return radarInfos;
    }

    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Level3RadarInfo> collectLevel3radarFiles(File rootDir,
            final String groupName)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<Level3RadarInfo>  radarInfos = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User user = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (radarInfos.size() % 5000 == 0) {
                    System.err.println("Found:" + radarInfos.size());
                }
                String station = matcher.group(1);
                String product = matcher.group(2);
                Group group = repository.findGroupFromName(groupName + "/" + "NIDS" + "/"
                                        + station + "/" + product);
                Date dttm = sdf.parse(matcher.group(3));
                radarInfos.add(new Level3RadarInfo(repository.getGUID(),
                                                   dttm.toString(), "", group, user,
                                                   f.toString(), station, product,
                                                   dttm.getTime()));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found:" + radarInfos.size() + " in " + (t2 - t1));
        return radarInfos;
    }



 
    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Level2RadarInfo> collectLevel2radarFiles(File rootDir,
            final String groupName)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<Level2RadarInfo>  radarInfos = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User user = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (radarInfos.size() % 5000 == 0) {
                    System.err.println("Found:" + radarInfos.size());
                }
                String station = matcher.group(1);
                Group group = repository.findGroupFromName(groupName + "/" + "craft" + "/"
                                        + station);
                Date dttm = sdf.parse(matcher.group(2));
                radarInfos.add(new Level2RadarInfo(repository.getGUID(),
                                                   dttm.toString(), "", group, user,
                                                   f.toString(), station, 
                                                   dttm.getTime()));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found:" + radarInfos.size() + " in " + (t2 - t1));
        return radarInfos;
    }



    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<SatelliteInfo> collectSatelliteFiles(File rootDir,
            final String groupName)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<SatelliteInfo>  infos = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User user = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (infos.size() % 5000 == 0) {
                    System.err.println("Found:" + infos.size());
                }
                String platform = matcher.group(1);
                String resolution = matcher.group(2);
                String product = matcher.group(3);
                Group group = repository.findGroupFromName(groupName + "/" + "Satellite" + "/"
                                                + platform + "/" + resolution +"/"+product);
                Date dttm = sdf.parse(matcher.group(4));
                infos.add(new SatelliteInfo(repository.getGUID(),
                                            dttm.toString(), "", group, user,
                                            f.toString(), platform, resolution, product,
                                            dttm.getTime()));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found sat files:" + infos.size() + " in " + (t2 - t1));
        return infos;
    }


    /**
     * _more_
     *
     * @param rootDir _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<FilesInfo> collectFiles(File rootDir, final String rootGroup) throws Exception {
        final String         rootStr    = rootDir.toString();
        final int            rootStrLen = rootStr.length();
        final List<FilesInfo> filesInfos  = new ArrayList();
        final User user = repository.findUser("jdoe");
        IOUtil.FileViewer    fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String name = f.getName();
                //                System.err.println(name);
                if (name.startsWith(".")) {
                    return DO_DONTRECURSE;
                }
                if (f.isDirectory()) {
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
                Group group = repository.findGroupFromName(StringUtil.join("/", toks));
                FilesInfo fileInfo = new FilesInfo(repository.getGUID(),
                                             name, name, TypeHandler.TYPE_FILE,
                                             group, user, f.toString(),
                                             f.lastModified());
                String ext = IOUtil.getFileExtension(path);
                if(ext.startsWith(".")) ext = ext.substring(1);
                if(ext.trim().length()>0) {
                    fileInfo.addTag(ext);
                }
                filesInfos.add(fileInfo);
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        return filesInfos;
    }


    public List<ModelInfo> collectModelFiles(File rootDir,
            final String groupName)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<ModelInfo>  infos = new ArrayList();
        final SimpleDateFormat[] sdf = {
            new SimpleDateFormat("yyyyMMddHH"),
            new SimpleDateFormat("yyyyMMdd")};
        final String regex= 
            "([^/]+)/[^/\\d]*(\\d+)(f\\d+)?_([^\\.]+)";
        final Pattern pattern =
            Pattern.compile(regex);


        final User user = repository.findUser("jdoe");
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    if(!f.isDirectory()) {
                        System.err.println (name);
                        System.err.println (regex);
                    }
                    return DO_CONTINUE;
                }
                //                    System.err.println (name);
                if (infos.size() % 5000 == 0) {
                    System.err.println("Found:" + infos.size());
                }
                String modelGroup = matcher.group(1);
                Date dttm=null;
                String dateString = matcher.group(2);
                for(int i=0;i<sdf.length;i++) {
                    try {
                        dttm= sdf[i].parse(dateString);
                        break;
                    } catch(Exception exc) {
                    }
                }
                if(dttm == null) {
                    System.err.println ("Could not parse date:" + dateString);
                    return DO_CONTINUE;
                }
                String forecasrHour = matcher.group(3);
                String modelRun = matcher.group(4);


                Group group = repository.findGroupFromName(groupName + "/" + "model" + "/"
                                        + modelGroup +"/"+modelRun);

                infos.add(new ModelInfo(repository.getGUID(),
                                        IOUtil.getFileTail(f.toString()), "",group, user,
                                        f.toString(), modelGroup, modelRun,
                                        dttm.getTime()));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found:" + infos.size() + " in " + (t2 - t1));
        return infos;
    }




}


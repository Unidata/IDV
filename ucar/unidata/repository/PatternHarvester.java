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

    public static final String ATTR_TYPE="type";
    public static final String ATTR_FILEPATTERN="filepattern";
    public static final String ATTR_ROOTDIR="rootdir";        
    public static final String ATTR_BASEGROUP="basegroup";        




    private SimpleDateFormat sdf;
    //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
    private String groupTemplate;
    private String nameTemplate;
    private String descTemplate;
    private List<String> columns;
    private List<String> patternNames;
    private List<String> patternTypes;


    private String filePatternString;
    private String fileNotPatternString;
    private Pattern filePattern;
    private Pattern fileNotPattern;
    private TypeHandler typeHandler;
    private File rootDir; 
    private String baseGroupName;
    private boolean running = true;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public PatternHarvester(Repository repository,Element element) throws Exception {
        super(repository,element);
        this.typeHandler = repository.getTypeHandler(XmlUtil.getAttribute(element,ATTR_TYPE,TypeHandler.TYPE_ANY));
        this.filePatternString = XmlUtil.getAttribute(element,ATTR_FILEPATTERN,(String)null);
        this.rootDir = new File(XmlUtil.getAttribute(element,ATTR_ROOTDIR));        
        this.baseGroupName = XmlUtil.getAttribute(element,ATTR_BASEGROUP,"Files");
        columns = split(element,"columns");
        patternNames = split(element,"patternnames");
        patternTypes = split(element,"patterntypes");
        groupTemplate = XmlUtil.getAttribute(element,"grouptemplate","");
        nameTemplate = XmlUtil.getAttribute(element,"nametemplate","${filename}");
        descTemplate = XmlUtil.getAttribute(element,"desctemplate","");
        sdf = new SimpleDateFormat(XmlUtil.getAttribute(element,"dateformat","yyyyMMdd_HHmm"));
        init();
    }

    private List split(Element element, String attr) {
        if(!XmlUtil.hasAttribute(element, attr)) return null;
        return StringUtil.split(XmlUtil.getAttribute(element,attr),",");
    }

    public PatternHarvester(Repository repository,
                            TypeHandler typeHandler,
                            File rootDir, 
                            String baseGroupName, 
                            String patternString,
                            String notPatternString) {

        super(repository);
        this.typeHandler = typeHandler;
        this.rootDir = rootDir;
        this.baseGroupName  = baseGroupName;
        this.filePatternString = patternString;
        this.fileNotPatternString = notPatternString;
        init();
    }


    private void init() {
        if(filePatternString!=null)
            filePattern = Pattern.compile(filePatternString);
        if(fileNotPatternString!=null)
            fileNotPattern = Pattern.compile(fileNotPatternString);
    }

    public void run()         
        throws Exception { 
        long tt1 = System.currentTimeMillis();
        List<FileInfo> dirs = FileInfo.collectDirs(rootDir);        
        long tt2 = System.currentTimeMillis();
        System.err.println ("took:" + (tt2-tt1) +" to find dirs:" + dirs.size());

        int cnt = 0;
        while(running) {
            long t1 = System.currentTimeMillis();
            List<Entry> entries;
            if(columns!=null &&  patternNames!=null  && patternTypes!=null) {
                System.err.println ("from pattern");
                entries= collectFromPattern(rootDir,  dirs, (cnt==0),
                                            baseGroupName,  typeHandler);
            } else {
                entries = collectEntries(rootDir, dirs, (cnt==0), baseGroupName, typeHandler);
            }
            long t2 = System.currentTimeMillis();
            cnt++;
            System.err.println ("found:" + entries.size() + " in:" + (t2-t1) +"ms");
            if(!repository.processEntries(this, typeHandler, entries)) break;
            if(!getMonitor()) break;
            Misc.sleep((long)(getSleepMinutes()*60*1000));
        }
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
    public List<Entry> collectFromPattern(File rootDir, List<FileInfo> dirs, boolean firstTime, 
                                          String rootGroup,
                                          TypeHandler typeHandler)
        throws Exception {
        long                         t1          = System.currentTimeMillis();
        final List<Entry> entries    = new ArrayList();
        final User        user       = repository.findUser("jdoe");
        //        System.err.println("PATTERN:" + filePatternString);
        for(FileInfo fileInfo: dirs) {
            if(!firstTime && !fileInfo.hasChanged()) continue;
            File[]files = fileInfo.getFile().listFiles();
            for(int fileIdx=0;fileIdx<files.length;fileIdx++) {
                File f = files[fileIdx];
                if(f.isDirectory())continue;
                String  fileName    = f.toString();
                fileName = fileName.replace("\\","/");
                Matcher matcher = filePattern.matcher(fileName);
                if ( !matcher.find()) {
                    continue;
                }
                if(entries.size()%1000==0) 
                    System.err.print(".");
                Hashtable map =new Hashtable();
                Date fromDate=null;                
                Date toDate=null;
                String groupName = groupTemplate;
                String name = nameTemplate;
                String desc = descTemplate;
                for(int dataIdx=0;dataIdx<patternNames.size();dataIdx++) {
                    String dataName = patternNames.get(dataIdx);
                    Object value =  matcher.group(dataIdx+1);
                    String type = patternTypes.get(dataIdx);
                    if(type.equals("date")) {
                        value   = sdf.parse((String)value);
                    } else if(type.equals("int")) {
                        value   = new Integer(value.toString());
                    } else if(type.equals("double")) {
                        value   = new Double(value.toString());
                    }
                    if(dataName.equals("fromDate")) {
                        fromDate = (Date) value;
                    } else if(dataName.equals("toDate")) {
                        toDate = (Date) value;
                    } else {
                        groupName = groupName.replace("${" + dataName +"}", value.toString());
                        name = name.replace("${" + dataName +"}", value.toString());
                        desc = desc.replace("${" + dataName +"}", value.toString());
                        map.put(dataName, value);
                    }
                }

                Date createDate = new Date();
                if(fromDate == null) fromDate = toDate;
                if(toDate == null) toDate = fromDate;
                if(fromDate == null) fromDate = createDate;
                if(toDate == null) toDate = createDate;

                groupName = groupName.replace("${fromDate}", fromDate.toString());
                groupName = groupName.replace("${toDate}", toDate.toString());

                name = name.replace("${fromDate}", fromDate.toString());
                name = name.replace("${toDate}", toDate.toString());

                desc = desc.replace("${fromDate}", fromDate.toString());
                desc = desc.replace("${toDate}", toDate.toString());
                desc = desc.replace("${name}", name);

                Object[]values = new Object[columns.size()];
                for(int colIdx=0;colIdx<columns.size();colIdx++) {
                    String colName  = columns.get(colIdx);
                    Object value = map.get(colName);
                    values[colIdx] = value;
                }

                Group group = repository.findGroupFromName(rootGroup + "/" + groupName,true);
                entries.add(new Entry(repository.getGUID(),
                                      typeHandler, name, desc, group, user,
                                      fileName,  createDate.getTime(),fromDate.getTime(),toDate.getTime(), values));
            }
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
    public List<Entry> collectEntries(File rootDir, List<FileInfo> dirs, boolean firstTime, 
                                    String rootGroup,
                                    TypeHandler typeHandler)
        throws Exception {
        final String      rootStr    = rootDir.toString();
        final int         rootStrLen = rootStr.length();
        final List<Entry> entries    = new ArrayList();
        final User        user       = repository.findUser("jdoe");
        for(FileInfo fileInfo: dirs) {
            if(!firstTime && !fileInfo.hasChanged()) continue;
            File[]files = fileInfo.getFile().listFiles();
            for(int i=0;i<files.length;i++) {
                File f = files[i];
                if (f.isDirectory()) {
                    continue;
                }
                String name = f.getName();
                String lcName = name.toLowerCase();
                if(filePattern!=null) {
                    if(!filePattern.matcher(lcName).find()) continue;
                }
                if(fileNotPattern!=null) {
                    if(fileNotPattern.matcher(lcName).find()) continue;
                }
                if (name.startsWith(".")) {
                    continue;
                }
                String path    = f.toString();
                String dirPath = f.getParent().toString();
                dirPath = dirPath.substring(rootStrLen);
                dirPath = SqlUtil.cleanUp(dirPath);
                List toks = StringUtil.split(dirPath, File.separator, true,
                                             true);

                toks.add(0, rootGroup);
                Group group =
                    repository.findGroupFromName(StringUtil.join("/", toks),true);
                Entry entry = new Entry(repository.getGUID(),
                                        typeHandler, name, name, group,
                                        user, path,
                                        f.lastModified(),null);
                String ext = IOUtil.getFileExtension(path);
                if (ext.startsWith(".")) {
                    ext = ext.substring(1);
                }
                if (ext.trim().length() > 0) {
                    entry.addTag(ext);
                }
                entries.add(entry);
            }
        }
        return entries;
    }




}


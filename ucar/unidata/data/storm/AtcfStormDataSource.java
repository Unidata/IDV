/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;

import java.util.Date;

import visad.DateTime;

import org.apache.commons.net.ftp.*;

import ucar.unidata.data.DataSourceDescriptor;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;


/**
 */
public  class  AtcfStormDataSource extends StormDataSource {

    private static String directoryPath = "ftp://anonymous:password@ftp.tpc.ncep.noaa.gov/atcf/archive";


    private List<StormInfo> stormInfos;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public AtcfStormDataSource() throws Exception {}

    /**
     * _more_
     *
     * @param descriptor _more_
     * @param name _more_
     * @param description _more_
     * @param properties _more_
     */
    public AtcfStormDataSource(DataSourceDescriptor descriptor, String url,
                               Hashtable properties) {
        super(descriptor, "ATCF Storm Data", "ATCF Storm Data", properties);
    }


    protected void initAfter() {
        try {
            stormInfos = new ArrayList<StormInfo>();
            String stormTable = readFile(directoryPath+"/storm.table");
            List lines = StringUtil.split(stormTable, "\n", true, true);

            SimpleDateFormat fmt  = new SimpleDateFormat("yyyymmddHH");
            for(int i=0;i<lines.size();i++) {
                String line = (String)lines.get(i);
                List toks = StringUtil.split(line, ",",true);
                String name  = (String)toks.get(0);
                String basin  = (String)toks.get(1);
                String number  = (String)toks.get(7);
                String year  = (String)toks.get(8);
                int y  = new Integer(year).intValue();
                //                if(y<2007) continue;
                String id = basin +"_"+number+"_" + year;
                if(name.equals("UNNAMED")) name  = id;
                String dttm =  (String)toks.get(11);
                Date date = fmt.parse(dttm);
                StormInfo si = new StormInfo(id,name, new DateTime(date));
                stormInfos.add(si);

            }
            System.err.println ("Read:" + stormInfos.size());
            
        } catch(Exception exc) {
            logException("Error initializing ATCF data",exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public  List<StormInfo> getStormInfos() {
        return stormInfos;
    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public  StormTrackCollection getTrackCollection(StormInfo stormInfo)
        throws Exception {
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public  StormInfo getStormInfo(String stormId) {
        return null;
    }

/**
Set the Directory property.

@param value The new value for Directory
**/
public void setDirectoryPath (String value) {
	directoryPath = value;
}

/**
Get the Directory property.

@return The Directory
**/
public String getDirectoryPath () {
	return directoryPath;
}


    private String readFile(String file) throws Exception {
        if(new File(file).exists()) {
            return IOUtil.readContents(file);
        }
        URL url = new URL(file);
        FTPClient ftp=new FTPClient();
        ftp.connect(url.getHost());
        ftp.login("anonymous","password");
        ftp.setFileType(FTP.IMAGE_FILE_TYPE);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ftp.enterLocalPassiveMode();
        if(!ftp.retrieveFile(url.getPath(), bos)) {
            throw new FileNotFoundException("Could not read file: " + file);
        }
        return new String(bos.toByteArray());
    }


}


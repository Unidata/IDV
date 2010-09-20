/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository.metadata;


import ucar.unidata.repository.*;


import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.imaging.jpeg.*;
import com.drew.lang.*;

import java.io.File;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class JpegMetadataHandler extends MetadataHandler {

    public static final String TYPE_CAMERA_DIRECTION = "camera.direction";


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public JpegMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {
        String path = entry.getResource().getPath();
        if(!(path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg"))) return;
        try {
            File jpegFile = new File(path); 
            com.drew.metadata.Metadata metadata = JpegMetadataReader.readMetadata(jpegFile);
            com.drew.metadata.Directory exifDir = metadata.getDirectory(ExifDirectory.class); 
            com.drew.metadata.Directory dir = metadata.getDirectory(GpsDirectory.class); 

            if(exifDir!=null) {
                //This tells ramadda that something was added
                if(exifDir.containsTag(ExifDirectory.TAG_DATETIME_ORIGINAL)) {
                    Date dttm = exifDir.getDate(ExifDirectory.TAG_DATETIME_ORIGINAL);
                    if(dttm!=null) {
                        entry.setStartDate(dttm.getTime());
                        entry.setEndDate(dttm.getTime());
                        extra.put("1","");
                    }
                }

            }

            if(dir.containsTag(GpsDirectory.TAG_GPS_IMG_DIRECTION)) {
                Metadata dirMetadata =
                    new Metadata(getRepository().getGUID(),
                                 entry.getId(), TYPE_CAMERA_DIRECTION,
                                 DFLT_INHERITED, ""+getValue(dir,GpsDirectory.TAG_GPS_IMG_DIRECTION),
                                 Metadata.DFLT_ATTR,
                                 Metadata.DFLT_ATTR,
                                 Metadata.DFLT_ATTR,
                                 Metadata.DFLT_EXTRA);
                
                metadataList.add(dirMetadata);
            }

            if(!dir.containsTag(GpsDirectory.TAG_GPS_LATITUDE)) return;
            double latitude =  getValue(dir,GpsDirectory.TAG_GPS_LATITUDE);
            double longitude =  getValue(dir,GpsDirectory.TAG_GPS_LONGITUDE);
            if(longitude>0) longitude = -longitude;
            double altitude = (dir.containsTag(GpsDirectory.TAG_GPS_ALTITUDE)?
                               getValue(dir,GpsDirectory.TAG_GPS_ALTITUDE):0);
            entry.setLocation(latitude, longitude, altitude);
            //This tells ramadda that something was added
            extra.put("1","");
        } catch(Exception exc) {
            getRepository().getLogManager().logError("Processing jpg:" + path,exc);
        }
    }


    private double getValue(Directory dir, int tag) throws Exception {
        try {
            Rational[] comps = dir.getRationalArray(tag);
            if(comps.length==3) {
                int deg = comps[0].intValue();
                float min = comps[1].floatValue();
                float sec = comps[2].floatValue();
                sec += (min % 1) * 60;
                return deg+min/60+sec/60/60;
            }
        } catch(Exception exc) {
            //Ignore this
        }
        return dir.getDouble(tag);
    }


}

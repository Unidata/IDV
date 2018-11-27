/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv;


import ucar.nc2.grib.GribVariableRenamer;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;

import java.net.URL;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


/**
 * Class to obtain build information from the jar files of
 * libraries used by the IDV.
 */
public class LibVersionUtil {

    /**
     * Display package name and version information for
     * javax.mail.internet.
     *
     * This example is a bit artificial, since examining the version of a
     * jar from Sun Microsystems is unusual.
     *
     * @return _more_
     *
     * @throws IOException _more_
     */

    private static HashMap<String, String> getBuildInfo() throws IOException {
        GribVariableRenamer     renamer   = new GribVariableRenamer();
        HashMap<String, String> buildInfo = new HashMap<String, String>();

        Enumeration<URL> resources =
            renamer.getClass().getClassLoader().getResources(
                "META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            try {
                Manifest manifest =
                    new Manifest(resources.nextElement().openStream());
                Attributes attrs = manifest.getMainAttributes();
                if (attrs != null) {
                    String implTitle = attrs.getValue("Implementation-Title");
                    if ((implTitle != null)
                            && (implTitle.contains("ncIdv"))) {
                        buildInfo.put(
                                "version",
                                attrs.getValue("Implementation-Version"));
                        String strDate = attrs.getValue("Built-On");

                        CalendarDate cd = CalendarDate.parseISOformat(null, strDate);
                        buildInfo.put("buildDate", cd.toString());

                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buildInfo;
    }

    public static String getNcidvVersion() {
        HashMap<String, String> myHash;
        try {
            myHash = getBuildInfo();
            String ncIdvBuildInfo = myHash.get("version") + "-"
                    + myHash.get("buildDate");
            return ncIdvBuildInfo;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        HashMap<String, String> myHash;
        try {
            myHash = getBuildInfo();
            String ncIdvBuildInfo = myHash.get("version") + "-"
                                    + myHash.get("buildDate");
            System.out.println(ncIdvBuildInfo);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

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



package ucar.unidata.apps.africa;


import ucar.visad.GeoUtils;
import visad.georef.LatLonPoint;
import visad.CommonUnit;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import visad.*;


/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class Converter {
    public static final String HEADER = "(index)->(Latitude,Longitude,Country(Text),State(Text),Region(Text),Time,rate)\nLatitude[ unit=\"degrees\" ],Longitude[ unit=\"degrees\" ],Country(Text),State(Text),Region(Text),Time[ fmt=\"yyyy-MM-dd\" ],rate[]";

    public static void process(String file, String country, String year) throws Exception {
	String contents = IOUtil.readContents(file, Converter.class);
	List<String> lines = StringUtil.split(contents, "\n", false,false);
	if(lines.size()==1) {
	    lines = StringUtil.split(contents, "\r", false,false);
	}
	SimpleDateFormat sdf1 =       new SimpleDateFormat("MMM dd yyyy");
	SimpleDateFormat sdf2 =       new SimpleDateFormat("yyyy-MM-dd");
	sdf1.setTimeZone(DateUtil.TIMEZONE_GMT);
	sdf2.setTimeZone(DateUtil.TIMEZONE_GMT);
	List<String> dates = new ArrayList<String>();
	for(int i=0;i<lines.size();i++) {
	    if(i==0) continue;
	    String line = lines.get(i).trim();
	    if(line.length()==0) continue;
	    
	    List<String> cols =  StringUtil.split(line, ",", false,false);

	    if(i==1) {
		//,Latitude,Longitude,Location,Population,Dec 29-Jan 4,Jan 5-Jan 11,Jan 12-Jan 18,Jan 19-Jan 25,Jan 26-Feb 1,Feb 2-Feb 8,Feb 9-Feb 15,Feb 16-Feb 22,Feb 23-Mar 1,Mar 2-Mar 8,Mar 9-Mar 15,Mar 16-Mar 22,Mar 23-Mar 29,Mar 30-Apr 5,Apr 6-Apr 12,Apr 13-Apr 19,Apr 20-Apr 26
		for(int col=5;col<cols.size();col++) {
		    String dttms = cols.get(col);
		    List<String> toks = StringUtil.split(dttms,"-",true,true);
		    //Get the second date
		    Date  date = sdf1.parse(toks.get(1)+" " + year);
		    dates.add(sdf2.format(date));
		}
		continue;
	    }
	//Kano,11.70,9.10,Albasu,209606.00,,,,,,0.48,2.86,0.48,,,3.82,6.20,9.54,20.99,2.86,,
	    int col=0;
	    String state= cols.get(col++);
	    String lats= cols.get(col++);
	    String lons= cols.get(col++);
	    String region= cols.get(col++);
	    double population = new Double(cols.get(col++)).doubleValue();
	    //	    System.err.println ("line:"+i);
	    LatLonPoint llp = GeoUtils.getLocationFromAddress(region+"," +state+"," + country,null);
            System.err.println(region+" " + llp);
	    if(llp==null && lats.trim().length()==0) {
		System.err.println("No location:" +region);
	    }
	    System.out.println("Latitude=" + llp.getLatitude().getValue(CommonUnit.degree));
	    System.out.println("Longitude=" + llp.getLongitude().getValue(CommonUnit.degree));
	    System.out.println("Country=" + country);
	    System.out.println("State=" + state);
	    System.out.println("Region=" + region);
	    for(int dateIdx=5;dateIdx<cols.size();dateIdx++) {
		System.out.print(dates.get(dateIdx-5));
		System.out.print(",");
		String value = cols.get(dateIdx);
		if(value.trim().length() == 0) 
		    value = "0.0";
		System.out.println(new Double(value).doubleValue());
	    }

	}
    }

    public static void main(String []args) throws Exception {
	System.out.println(HEADER);
	for(String arg: args) {
	    process(arg,"nigeria","2009");
	}
    }


}

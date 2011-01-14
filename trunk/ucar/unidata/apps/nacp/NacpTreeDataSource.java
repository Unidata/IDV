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



package ucar.unidata.apps.nacp;

import ucar.unidata.data.*;
import ucar.unidata.data.point.TextPointDataSource;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

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
public class NacpTreeDataSource extends TextPointDataSource {

    public static final double MISSING = -999.9;

    /**
     * Default contstructor.
     *
     * @throws VisADException
     */
    public NacpTreeDataSource() throws VisADException {
    }

    /**
     *
     * @param descriptor  <code>DataSourceDescriptor</code> for this.
     * @param source      Source URL
     * @param properties  <code>Hashtable</code> of properties for the source.
     *
     *
     * @throws Exception On badness
     */
    public NacpTreeDataSource(DataSourceDescriptor descriptor, String source,
				  Hashtable properties)
            throws Exception {
        super(descriptor, source, "NACP Tree Data", properties);
    }



    private static String header = "(index) -> (Time,Longitude,Latitude,Altitude,id(Text),Condition(Text),Species(Text),dbh,Crown_along_slope,Crown_cross_slope,Crown_form(Text),Height,Crown_base,Distance,Azimuth,Notes(Text))\nTime[fmt=\"yyyy-MM-dd\"],Longitude[unit=\"degrees\" ],Latitude[unit=\"degrees\"],Altitude[units=\"m\"],id(Text),Condition(Text),Species(Text),dbh[unit=\"cm\" missing=\"-999.9\"],Crown_along_slope[unit=\"cm\" missing=\"-999.9\"],Crown_cross_slope[unit=\"cm\" missing=\"-999.9\"],Crown_form(Text),Height[unit=\"m\"  missing=\"-999.9\"],Crown_base[unit=\"m\"  missing=\"-999.9\"],Distance[unit=\"m\" missing=\"-999.9\"],Azimuth[unit=\"m\" missing=\"-999.9\"],Notes(Text)";

    protected String getContents(String sourceFile, boolean sampleIt) throws Exception {
	return process(sourceFile, sampleIt);
    }

    public static String process(String file, boolean sampleIt) throws Exception {
	StringBuffer sb  = new StringBuffer();
	sb.append(header);
	/*1,Longitude,decimal degrees,Longitude of plot location in decimal degrees. Negative values indicate west 
2,Latitude,decimal degrees,Latitude of plot location in decimal degrees. Positive values indicate north 
3,Plot_ID,units,Plot identification
4,Subplot_ID,units,Subplot within each plot. Each 100 by 100 meter plot was divided into 9 subplots.
5,Tree_ID,units,Unique tree id within each subplot
6,Condition,none,Live or dead indicated by L and D respectively 
7,Species,none,Species are indicated with a 4 letter code made up of the first two letters of the genus and the first two letters of the species
8,DBH,cm,Diameter at breast height in centimeters (cm) measured at 1.3 meters above the ground
9,Crown_along_slope,meter,Crown diameter measured along the slope of the subplot
10,Crown_cross_slope,meter,Crown diameter measured perpindicular to the slope of the subplot
11,Crown_form,none,"Approximate geometrical shape of the crown: cone, cylinder, semi-cylinder, ellipse etc",,,,,,,,,,,,
12,Ht,meter,Total height of the tree measured in meters (m),,,,,,,,,,,,
13,Crown_base,meter,Height of the crown base measured in meters,,,,,,,,,,,,
14,Distance,meter,Distance from the center of the subplot to the tree measured in meters (m),,,,,,,,,,,,
15,Azimuth,decimal degrees,Azimuth of the tree from the center of the subplot measured in decimal degrees,,,,,,,,,,,,
16,Notes,none,Field notes ,,,,,,,,,,,,
	*/

	sb.append("\n");
	sb.append("Time=2009-10-1\n");
	List<String> lines = StringUtil.split(IOUtil.readContents(file),"\n",true,true);
	boolean seenStart = false;
	for(String line: lines) {
	    if(!seenStart) {
		seenStart = line.startsWith("Longitude,");
		continue;
	    } 
	    List<String> toks = StringUtil.split(line,",",true,true);

	    int col=0;
	    double lon = Misc.toDouble(toks.get(col++));
	    double lat = Misc.toDouble(toks.get(col++));
	    String id  = toks.get(col++)+"_" + toks.get(col++) +"_" + toks.get(col++);


	    String condition = toks.get(col++);
	    String species = toks.get(col++);
	    double DBH = Misc.toDouble(toks.get(col++));
	    double Crown_along_slope = Misc.toDouble(toks.get(col++));
	    double Crown_cross_slope = Misc.toDouble(toks.get(col++));
	    String Crown_form = toks.get(col++);
	    double ht= Misc.toDouble(toks.get(col++));
	    double Crown_base = Misc.toDouble(toks.get(col++));
	    double distance = Misc.toDouble(toks.get(col++));
	    double azimuth = Misc.toDouble(toks.get(col++));
	    String notes = toks.get(col++);


	    if(azimuth!= MISSING && distance!=MISSING) {
		LatLonPointImpl llp =  Bearing.findPoint(new LatLonPointImpl(lat,lon),azimuth,distance/1000.0,null);
		//		System.out.println(azimuth +" " + lat +" " + lon  +" " + distance +" " + llp.getLatitude());
		lat = llp.getLatitude();
		lon = llp.getLongitude();
	    }
	    sb.append(lon);
	    sb.append(",");
	    sb.append(lat);
	    sb.append(",");
	    sb.append(0.0);
	    sb.append(",");
	    sb.append(id);
	    for(int i=5;i<toks.size();i++)  {
		sb.append(",");
		sb.append(toks.get(i));
	    }

	    sb.append("\n");

	    if(sampleIt) break;
	}
	return sb.toString();
    }



    public static void main(String []args) throws Exception {
	for(int i=0;i<args.length;i++) {
	    process(args[i],false);
	}

    }

}



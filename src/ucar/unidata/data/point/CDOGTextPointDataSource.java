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

/**
 *
 */
package ucar.unidata.data.point;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.Util;

import visad.DateTime;
import visad.FieldImpl;
import visad.FunctionType;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;

import java.io.IOException;

import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;


/**
 * DataSource to handle the Comprehensive Deepwater Oil and Gass (CDOG) Blowout Model space delimited text output
 * @author Don Murray (don.murray@noaa.gov)
 */
public class CDOGTextPointDataSource extends TextPointDataSource {

    /** the time unit */
    private Unit timeUnit = null;

    /** the file with the time info */
    private static final String TIME_FILE = "series.dat";

    /**
     * Create a new data source
     * @throws VisADException
     */
    public CDOGTextPointDataSource() throws VisADException {
    }

    /**
     * Create a new data source
     * @param source  the source file
     * @throws VisADException
     */
    public CDOGTextPointDataSource(String source) throws VisADException {
        super(source);
    }

    /**
     * Create a new data source
     * @param descriptor descriptor
     * @param sources    list of sources
     * @param properties  properties
     * @throws VisADException problem creating the data
     */
    public CDOGTextPointDataSource(DataSourceDescriptor descriptor,
                                   List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, properties);
    }

    /**
     * Create a new data source
     * @param descriptor   descriptor
     * @param source       the source file
     * @param properties   the properties
     * @throws VisADException problem creating the data
     */
    public CDOGTextPointDataSource(DataSourceDescriptor descriptor,
                                   String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, properties);
    }

    /**
     * Create a new data source
     * @param descriptor   descriptor
     * @param name         name of the data
     * @param source       the source file
     * @param properties   the properties
     * @throws VisADException problem creating the data
     */
    public CDOGTextPointDataSource(DataSourceDescriptor descriptor,
                                   String source, String name,
                                   Hashtable properties)
            throws VisADException {
        super(descriptor, source, name, properties);
    }

    /**
     * make the observations from the given datachoice
     *
     * @param dataChoice the data choice
     * @param subset data selection to subset with
     * @param bbox bounding box to subset
     * @param trackParam the parameter to use for thetrack
     * @param sampleIt do we just sample or do we read the full set of obs
     * @param showAttributeGuiIfNeeded popup the gui if we have a problem
     *
     * @return the field
     *
     * @throws Exception On badness
     */
    public FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                             LatLonRect bbox, String trackParam,
                             boolean sampleIt,
                             boolean showAttributeGuiIfNeeded)
            throws Exception {
        String source = getSource(dataChoice);
        String controlFile = IOUtil.joinDir(IOUtil.getFileRoot(source),
                                            TIME_FILE);
        String timeContents = null;
        try {
            timeContents = getContents(controlFile);
        } catch (IOException ioe) {
            System.err.println("Unable to read " + TIME_FILE);
            timeContents = null;
        }
        String startTime = null;
        if (timeContents != null) {
            List lines = StringUtil.split(timeContents, "\n", true, true);
            if (lines.size() >= 3) {
                startTime = parseStartTime((String) lines.get(2));
            } else {
                System.err.println("Unable to read start time from "
                                   + TIME_FILE);
            }
        }
        if (startTime == null) {
            startTime = new DateTime().toString();
        }
        timeUnit = Util.parseUnit("hours since " + startTime);
        return super.makeObs(dataChoice, subset, bbox, trackParam, sampleIt,
                             showAttributeGuiIfNeeded);
    }

    /**
     * Parse the time field
     *
     * @param time the time from the file
     *
     * @return the formatted time
     */
    private String parseStartTime(String time) {
    	// time could be:  2010 July 02 12 30 CDT
    	//                 2010 July 02 1230 CDT start
    	//                 2010 July 02 12 30 CDT start
    	//                 2010 07 02 12 30 CDT start
    	List timeParts = StringUtil.split(time, " ", true, true);
    	if (timeParts.size() < 4) return null;
    	boolean hasStart = ((String) timeParts.get(timeParts.size()-1)).equals("start");
    	int numTokens = timeParts.size();
    	if (hasStart) numTokens--;
    	String timePattern = (numTokens == 6) ? "HH mm" : "HHmm";
        StringBuilder timeBuilder = new StringBuilder();
    	int numTimeToks = numTokens-1;
    	for (int i = 0; i < numTimeToks; i++) {
    		timeBuilder.append((String) timeParts.get(i));
    		timeBuilder.append(" ");
    	}
    	String timePart = timeBuilder.toString().trim();
    	// now build the pattern
        StringBuilder patternBuilder = new StringBuilder("yyyy ");
        String tmp = (String) timeParts.get(1);
        if (tmp.length() < 3) {   // 01,02,03, ...
        	patternBuilder.append("MM ");
        } else if (tmp.length() < 4) { // Jan, Feb, Mar, ...
        	patternBuilder.append("MMM ");
        } else { // January, February, March, ....
        	patternBuilder.append("MMMMM ");
        }
        patternBuilder.append("dd ");
        patternBuilder.append(timePattern);
    	String timeFormat = patternBuilder.toString().trim();
    	
        String timeZone = (String) timeParts.get(numTimeToks);
        if (timeZone.equals("CDT")) {  // CDT doesn't seem to work
            timeZone = "America/Chicago";
        }
        DateTime startTime = null;
        try {
            TimeZone zone = TimeZone.getTimeZone(timeZone);
            startTime = DateTime.createDateTime(timePart, timeFormat, zone);
            String startTimeString =  startTime.formattedString(DateTime.DEFAULT_TIME_FORMAT
                                             + " Z", zone);
            return startTimeString;
        } catch (VisADException ve) {
            System.err.println("unable to parse start time");
        }
        return null;
    }

    /**
     * Make obs from the text
     *
     * @param contents the contents
     * @param delimiter  the delimiter
     * @param subset  subset info
     * @param bbox  the bounding box
     * @param trackParam  the track parameter name
     * @param sampleIt  true if just need a sample
     * @param showAttributeGuiIfNeeded  true if we need to show the attribute gui
     *
     * @return the Field of obs
     *
     * @throws Exception  some problem occurred making the obs
     */
    public FieldImpl makeObs(String contents, String delimiter,
                             DataSelection subset, LatLonRect bbox,
                             String trackParam, boolean sampleIt,
                             boolean showAttributeGuiIfNeeded)
            throws Exception {
        String firstLine = contents.substring(0,
                               contents.indexOf("\n")).trim();
        double hours = Misc.parseDouble(firstLine.substring(0,
                           firstLine.indexOf(" ")));
        DateTime time = new DateTime(hours, timeUnit);

        FieldImpl myObs = super.makeObs(contents, delimiter, subset, bbox,
                                        trackParam, sampleIt,
                                        showAttributeGuiIfNeeded);
        FieldImpl obsWithTime = new FieldImpl((FunctionType) myObs.getType(),
                                    myObs.getDomainSet());
        for (int i = 0; i < myObs.getLength(); i++) {
            PointObTuple ob = (PointObTuple) myObs.getSample(i);
            PointOb newOb = new PointObTuple(ob.getEarthLocation(), time,
                                             ob.getData(),
                                             (TupleType) ob.getType(), false);
            obsWithTime.setSample(i, newOb, false, false);
        }
        return obsWithTime;
    }
}

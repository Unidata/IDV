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

package ucar.unidata.data.point;


import ucar.unidata.data.DataSourceDescriptor;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.VisADException;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Datasource for the GNOME SPLOTS (General NOAA Operational Modeling
 * Environement SPill DOTS") files.  It consists of a series of 3 files
 *  with extensions .ms3, .ms4, .ms5
 *
 * @author Don Murray
 */
public class GNOMETextPointDataSource extends TextPointDataSource {

    /** Suffix for the file containing the time */
    public static final String TIME_FILE_SUFFIX = "ms3";

    /** Suffix for the file containing the locations */
    public static final String LOCATION_FILE_SUFFIX = "ms4";

    /** Suffix for the file containing the data */
    public static final String DATA_FILE_SUFFIX = "ms5";

    /** Date line identifier */
    public static final String VALIDFOR = "VALIDFOR:";

    /**
     * Header for the contents
     * public static final String HEADER =
     *   "(index) -> (Time, Longitude, Latitude, LE, Type(Text), "
     *   + "Pollutant(Text), Depth, mass, density, age, status(Text))\n"
     *   + "Time[colspan=\"2\" fmt=\"HH:mm MM/dd/yyy\"], Longitude[unit=\"degrees_east\"], "
     *   + "Latitude[unit=\"degrees_north\"], LE, Type(Text), Pollutant(Text), "
     *   + "Depth[unit=\"m\"], mass, density[unit=\"kg/m3\"], "
     *   + "age[unit=\"hr\"], status(Text)\n";
     */

    public static final String HEADER =
        "(index) -> (Time, Longitude, Latitude, LE, Depth, mass, density, age)\n"
        + "Time[colspan=\"2\" fmt=\"HH:mm MM/dd/yy\" tz=\"America/Chicago\"], "
        + "Longitude[unit=\"degrees_east\"], Latitude[unit=\"degrees_north\"], LE, skip, skip, "
        + "Depth[unit=\"m\" scale=\"-1\"], mass, density[unit=\"kg/m3\"], "
        + "age[unit=\"hr\"], skip\n";


    /**
     * Default ctor
     *
     * @throws VisADException  gotta throw something I guess
     */
    public GNOMETextPointDataSource() throws VisADException {
        super();
    }

    /**
     * Create a GNOME data source for the specified file
     *
     * @param source  the source file
     *
     * @throws VisADException  problem making the file
     */
    public GNOMETextPointDataSource(String source) throws VisADException {
        super(source);
    }

    /**
     * Create a new GNOME data source
     *
     * @param descriptor  the data source descriptor
     * @param sources  the list of sources
     * @param properties  the properties
     *
     * @throws VisADException  problem making data
     */
    public GNOMETextPointDataSource(DataSourceDescriptor descriptor,
                                    List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, makeUniqueList(sources), properties);
    }



    /**
     *     Get the description for this data source
     *
     * @return _more_
     */
    public String getDescription() {
        return "GNOME Text Point Data";
    }

    /**
     * Create a new GNOME data source
     *
     * @param descriptor  the data source descriptor
     * @param source  the source
     * @param properties  the properties
     *
     * @throws VisADException  problem making data
     */
    public GNOMETextPointDataSource(DataSourceDescriptor descriptor,
                                    String source, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(new Object[] { source }), properties);
    }

    /**
     * Make a unique list of names in case the users selects more than one type for the same
     * time period
     *
     * @param sources list of sources
     *
     * @return a unique list of .ms3 files
     */
    private static List makeUniqueList(List sources) {
        List newSources = new ArrayList();
        for (int i = 0; i < sources.size(); i++) {
            String originalSource = (String) sources.get(i);
            String testFile = originalSource.replaceAll("\\.ms.$", ".ms3");
            if ( !(newSources.contains(testFile))) {
                newSources.add(testFile);
            }
        }
        return newSources;
    }

    /**
     * Get the contents.  Merge the info from the 3 files
     *
     * @param sourceFile  the source file
     * @param sampleIt    true to sample
     *
     * @return  the contents
     *
     * @throws Exception  problem reading the files
     */
    protected String getContents(String sourceFile, boolean sampleIt)
            throws Exception {
        // There are 3 files for each timestep ms3,ms4,ms5
        String dateFile = sourceFile.replaceAll("\\.ms.$", ".ms3");
        String locFile  = sourceFile.replaceAll("\\.ms.$", ".ms4");
        String dataFile = sourceFile.replaceAll("\\.ms.$", ".ms5");

        if ( !canFindFile(dateFile)) {
            throw new Exception("unable to find the date file: " + dateFile);
        }
        if ( !canFindFile(locFile)) {
            throw new Exception("unable to find the location file: "
                                + locFile);
        }
        if ( !canFindFile(locFile)) {
            throw new Exception("unable to find the data file: " + dataFile);
        }

        String dateContents = IOUtil.readContents(dateFile);
        List<String> dateLines = StringUtil.split(dateContents, "\n", true,
                                     true);
        String locContents = IOUtil.readContents(locFile);
        List<String> locLines = StringUtil.split(locContents, "\n", true,
                                    true);
        String dataContents = IOUtil.readContents(dataFile);
        List<String> dataLines = StringUtil.split(dataContents, "\n", true,
                                     true);

        String dateString = null;
        // parse the date.  Looking for something like:
        // 0, VALIDFOR: 12:00, 04/29/10
        for (String dateLine : dateLines) {
            if (dateLine.indexOf(VALIDFOR) < 0) {
                continue;
            }
            dateString = dateLine.substring(dateLine.indexOf(": ")
                                            + 1).trim();
            break;
        }
        if (dateString == null) {
            throw new Exception("Unable to find the date in " + dateFile);
        }
        StringBuffer buf = new StringBuffer(HEADER);
        for (int i = 0; i < dataLines.size(); i++) {
            if ((i == 1) && sampleIt) {
                break;
            }
            buf.append(dateString);
            buf.append(", ");
            List<String> locStrings = StringUtil.split(locLines.get(2 * i
                                          + 1).trim(), " ", true, true);
            for (int j = 0; j < 2; j++) {
                buf.append(locStrings.get(j));
                buf.append(", ");
            }
            buf.append(dataLines.get(i));
            buf.append("\n");
        }
        // System.out.println(buf.toString());
        return buf.toString();
    }

    /**
     * Can we find the file?
     *
     * @param filename  the name of the file
     *
     * @return  true if the file exists
     */
    private boolean canFindFile(String filename) {
        File f = new File(filename);
        return f.exists();
    }
}

/*
 * $Id: CMASoundingAdapter.java,v 1.4 2007/06/01 19:15:05 yuanho Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.sounding;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.GeopotentialAltitude;
import ucar.visad.quantities.Gravity;

import visad.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import java.util.StringTokenizer;


/**
 * This class creates VisAD compatible data objects from a
 * file of upper air soundings from CMA.
 *
 * @author IDV development team
 * @version $Revision: 1.4 $
 */
public class CMASoundingAdapter extends SoundingAdapterImpl implements SoundingAdapter {

    /** The filename */
    String filename = null;

    /** The list of levels */
    List soundingLevels;

    /** Height unit */
    Unit heightUnit;

    /** unit for geopotential */
    private static final Unit GEOPOTENTIAL_UNIT;

    static {
        try {
            GEOPOTENTIAL_UNIT = Util.parseUnit("m2/s2");
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex.toString());
        }
    }

    /**
     *  Constructor for reflection based construction
     */
    public CMASoundingAdapter() {
        super("CMASoundingAdapter");
    }

    /**
     * Read a file of decoded soundings from CMA.
     *
     * @param  filename  the fully qualified path and name of the file
     *                   to be adapted.
     *
     * @throws Exception  problem reading file
     */
    public CMASoundingAdapter(String filename) throws Exception {
        super("CMASoundingAdapter");
        this.filename = filename;
        init();
    }


    /**
     * Read a file of decoded soundings from CMA
     *
     * @param file     File to read
     *
     * @throws Exception   problem encountered
     *
     */
    public CMASoundingAdapter(File file) throws Exception {
        this(file.getAbsolutePath());
    }

    /**
     * Update the adapter (re-read the data)
     */
    public void update() {
        try {
            haveInitialized = false;
            init();
        } catch (Exception exc) {
            LogUtil.logException("Doing update", exc);
        }
    }



    /**
     * Read a file of decoded soundings from CMA.  Format of
     * file is as follows:
     *
     * @throws Exception   problem reading the file
     */
    protected void init() throws Exception {
        if (haveInitialized) {
            return;
        }
        super.init();

        InputStream    is = new FileInputStream(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        // get date and number of stations
        String          info    = br.readLine();
        String          delim   = (info.indexOf(",") >= 0)
                                  ? ","
                                  : " ";
        StringTokenizer tok     = new StringTokenizer(info, delim);
        int             numToks = tok.countTokens();
        if (numToks != 5) {
            throw new Exception("Can't find date and number of stations");
        }
        int          tokNum = 0;
        StringBuffer buf    = new StringBuffer();
        while (tokNum < 3) {
            buf.append(tok.nextToken());
            buf.append("-");
            tokNum++;
        }
        buf.append(tok.nextToken());
        // now should have something like 2007-1-27-0
        DateTime dt = DateTime.createDateTime(buf.toString(),
                          "yyyy-MM-dd-HH");
        times = new ArrayList(1);
        times.add(dt);

        int numStations = Integer.parseInt(tok.nextToken());
        // System.out.println("numStations = " + numStations);

        // get the station list and number of stations
        stations       = new ArrayList(numStations);  // array of stations
        soundings      = new ArrayList(numStations);  // array of soundings
        soundingLevels = new ArrayList(numStations);

        // fill the station and sounding lists
        SoundingStation currentStation = null;
        List            levels         = null;
        int             numFound       = 0;
        while (true) {

            String data = br.readLine();
            if (data == null) {
                break;
            }
            if (data.trim().equals("")) {
                continue;
            }
            tok     = new StringTokenizer(data, delim);
            numToks = tok.countTokens();
            if (numToks == 4) {  // new station
                if (levels != null) {
                    SoundingOb so = new SoundingOb(currentStation, dt);
                    soundings.add(so);
                    soundingLevels.add(levels);
                    numFound++;
                }
                currentStation = makeSoundingStation(tok);
                levels         = new ArrayList();
            } else {
                appendLevels(levels, tok);
            }
        }

    }

    /**
     * Append levels to the list
     *
     * @param levels  the list of levels
     * @param tok  a list of tokens for the level
     */
    private void appendLevels(List levels, StringTokenizer tok) {
        if (levels == null) {
            return;
        }
        SoundingLevelData sld      = new SoundingLevelData();
        float             pressure = getValue(tok.nextToken());
        if (Double.isNaN(pressure)) {
            return;
        }
        sld.pressure    = pressure;
        sld.height      = getValue(tok.nextToken());
        sld.temperature = getValue(tok.nextToken());
        sld.dewpoint    = getValue(tok.nextToken());
        sld.direction   = getValue(tok.nextToken());
        sld.speed       = getValue(tok.nextToken());
        try {
            // figure out if we are in Geopotential or GeopotentialMeters
            if ((heightUnit == null) && !Double.isNaN(sld.height)) {
                float expected =
                    AirPressure.getStandardAtmosphereCS().toReference(
                        new float[][] {
                    { sld.pressure }
                })[0][0];
                if (Math.abs(sld.height) > expected + 50) {
                    heightUnit = GEOPOTENTIAL_UNIT;
                } else {
                    heightUnit = GeopotentialAltitude.getGeopotentialMeter();
                }
            }
        } catch (VisADException ve) {}

        levels.add(sld);
    }

    /**
     * Get a value from a string
     *
     * @param s  the string
     *
     * @return the numeric value
     */
    private float getValue(String s) {
        double val;
        try {
            val = Misc.parseDouble(s);
            if ((val == 99999.90) || (val == 999999.0)) {
                val = Double.NaN;
            }
        } catch (NumberFormatException nfe) {
            val = Double.NaN;
        }
        return (float) val;
    }

    /**
     * Create a sounding station object from the netCDF file info
     *
     * @param tok  the tokenizer to parse
     *
     * @return the SoundingStation
     * @throws Exception   problem getting the data
     */
    private SoundingStation makeSoundingStation(StringTokenizer tok)
            throws Exception {
        String wmoID;
        double latvalue;
        double lonvalue;
        double elevvalue;
        try {
            wmoID = new Integer((int) getValue(tok.nextToken())).toString();
            latvalue  = getValue(tok.nextToken());
            lonvalue  = getValue(tok.nextToken());
            elevvalue = getValue(tok.nextToken());
        } catch (Exception ne) {
            throw new Exception(ne.toString());
        }
        SoundingStation s = new SoundingStation(wmoID, latvalue, lonvalue,
                                elevvalue);
        stations.add(s);
        return s;
    }


    /**
     * Set the data in the RAOB.
     * @param so  the SoundingOb
     * @param levels  list of SoundingLevelData
     */
    protected void setRAOBData(SoundingOb so, List levels) {
        if (levels == null) {
            return;
        }
        int     numLevels = levels.size();
        float[] pressures = new float[numLevels];
        float[] heights   = new float[numLevels];
        float[] temps     = new float[numLevels];
        float[] dewpts    = new float[numLevels];
        float[] dirs      = new float[numLevels];
        float[] speeds    = new float[numLevels];
        for (int i = 0; i < numLevels; i++) {
            SoundingLevelData sld = (SoundingLevelData) levels.get(i);
            pressures[i] = sld.pressure;
            heights[i]   = sld.height;
            temps[i]     = sld.temperature;
            dewpts[i]    = sld.dewpoint;
            dirs[i]      = sld.direction;
            speeds[i]    = sld.speed;
        }
        try {
            // not really meters, but we use this as a hack for Geopotential
            if (heightUnit.equals(GEOPOTENTIAL_UNIT)) {
                float[] newHeights =
                    GeopotentialAltitude.toAltitude(
                        heights,
                        GeopotentialAltitude.getGeopotentialUnit(
                            CommonUnit.meter), Gravity.newReal(),
                                new float[heights.length], CommonUnit.meter,
                                true);
                heights = newHeights;
            }
            RAOB r = so.getRAOB();
            r.setMandatoryPressureProfile(
                CommonUnits.MILLIBAR, pressures, CommonUnits.CELSIUS, temps,
                CommonUnits.CELSIUS, dewpts, CommonUnit.meterPerSecond,
                speeds, CommonUnit.degree, dirs,
                GeopotentialAltitude.getGeopotentialUnit(CommonUnit.meter),
                heights);
            //System.out.println("data = " + r.getMandatoryPressureProfile());
        } catch (Exception excp) {
            //System.out.println("couldn't set RAOB data");
            System.out.println(excp.getMessage());
        }
    }

    /**
     * Check to see if the RAOB has any data
     *
     * @param sound    sounding to check
     * @return  sounding with data
     */
    public SoundingOb initSoundingOb(SoundingOb sound) {
        // System.out.println("init sounding ob " + sound);
        checkInit();
        if ( !sound.hasData()) {
            int idx = soundings.indexOf(sound);
            if (idx < 0) {
                throw new IllegalArgumentException(
                    "SoundingAdapter does not contain sounding:" + sound);
            }
            setRAOBData(sound, (List) soundingLevels.get(idx));
        }
        return sound;
    }


    /**
     * Get the filename for this adapter.
     *
     * @return  name of file
     */
    public String getSource() {
        return filename;
    }

    /**
     * Set the source for this data
     *
     * @param s  new source
     */
    public void setSource(String s) {
        filename = s;
    }

    /**
     * Test it out
     *
     * @param args  filename
     *
     * @throws Exception problem opening or decoding the file
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("must supply a filename");
            System.exit(1);
        }
        CMASoundingAdapter csa = new CMASoundingAdapter(args[0]);
    }

    /**
     * A class to hold sounding level data
     *
     * @author IDV Development Team
     * @version $Revision: 1.4 $
     */
    private class SoundingLevelData {

        /** the pressure */
        public float pressure;

        /** the height */
        public float height;

        /** the temperature */
        public float temperature;

        /** the dewpoint */
        public float dewpoint;

        /** the wind direction */
        public float direction;

        /** the wind speed */
        public float speed;

        /**
         * Ctor
         */
        public SoundingLevelData() {}

        /**
         * Get a String representation of this object
         *
         * @return the String representation of this object
         */
        public String toString() {
            StringBuffer b = new StringBuffer();
            b.append("p: ");
            b.append(pressure);
            b.append(" z: ");
            b.append(height);
            b.append(" t: ");
            b.append(temperature);
            b.append(" dp: ");
            b.append(dewpoint);
            b.append(" wind: ");
            b.append(direction);
            b.append("/");
            b.append(speed);
            return b.toString();
        }
    }
}


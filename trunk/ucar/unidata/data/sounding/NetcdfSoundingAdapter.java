/*
 * $Id: NetcdfSoundingAdapter.java,v 1.20 2007/06/13 22:34:36 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


import ucar.netcdf.*;

import ucar.unidata.data.DataUtil;
import ucar.unidata.data.sounding.RAOB;
import ucar.unidata.data.sounding.SoundingOb;
import ucar.unidata.data.sounding.SoundingStation;

import ucar.unidata.metdata.Station;
import ucar.unidata.util.Defaults;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;

import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.*;

import visad.data.units.Parser;


import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This class creates VisAD compatible data objects from a netCDF
 * file of upper air soundings.
 *
 * @author IDV development team
 * @version $Revision: 1.20 $
 */
public class NetcdfSoundingAdapter extends SoundingAdapterImpl implements SoundingAdapter {

    /** netCDF sounding filename */
    private String filename;

    /** netCDF file */
    private NetcdfFile nc = null;

    /** number of stations in file */
    private int numStations;

    /** name of mandP pressure variable */
    private String prMandPVar;

    /** name of mandP height variable */
    private String htMandPVar;

    /** name of mandP temp variable */
    private String tpMandPVar;

    /** name of mandP dewpt variable */
    private String tdMandPVar;

    /** name of mandP wind speed variable */
    private String spdMandPVar;

    /** name of mandP wind dir variable */
    private String dirMandPVar;

    /** name of mandW height variable */
    private String htMandWVar;

    /** name of mandW wind speed variable */
    private String spdMandWVar;

    /** name of mandW wind dir variable */
    private String dirMandWVar;

    /** name of sig T pressure variable */
    private String prSigTVar;

    /** name of sig T temp variable */
    private String tpSigTVar;

    /** name of sig T dewpt variable */
    private String tdSigTVar;

    /** name of sig wind height variable */
    private String htSigWVar;

    /** name of sig wind wind speed variable */
    private String spdSigWVar;

    /** name of sig wind wind dir variable */
    private String dirSigWVar;

    /** name of max wind pressure variable */
    private String prMaxWVar;

    /** name of max wind wind speed variable */
    private String spdMaxWVar;

    /** name of max wind wind dir variable */
    private String dirMaxWVar;

    /** name of tropopause pressure variable */
    private String prTropVar;

    /** name of tropopause temp variable */
    private String tpTropVar;

    /** name of tropopause dewpt variable */
    private String tdTropVar;

    /** name of tropopause wind speed variable */
    private String spdTropVar;

    /** name of tropopause wind dir variable */
    private String dirTropVar;

    /** station id */
    private Variable stid;

    /** station latitude */
    private Variable lat;

    /** station longitude */
    private Variable lon;

    /** station elevation */
    private Variable elev;

    /** sounding time */
    private Variable time;

    /** number mandatory pressure levels */
    private Variable numMandP;

    /** number mandatory wind levels */
    private Variable numMandW;

    /** number significant temp levels */
    private Variable numSigT;

    /** number of significant wind levels */
    private Variable numSigW;

    /** number of max wind levels */
    private Variable numMaxW;

    /** number of tropopause levels */
    private Variable numTrop;

    /** file has mandatory pressure  data */
    private boolean hasMandP = false;

    /** file has mandatory wind data */
    private boolean hasMandW = false;

    /** file has significant temp data */
    private boolean hasSigT = false;

    /** file has significant wind data */
    private boolean hasSigW = false;

    /** file has max wind data */
    private boolean hasMaxW = false;

    /** file has tropopause data */
    private boolean hasTrop = false;

    /** file has tropopause data in old format (only one) */
    private boolean oneTrop = false;
    //     old format (only one)

    /** file stores dewpoint as value */
    private boolean dewpointIsDepression = true;

    /** default missing value */
    private float missingValue = 99999.f;

    /** time Unit */
    private Unit timeUnit = null;

    /** time fill */
    private double timeFill = Double.NaN;

    /** SoundingStation */
    private SoundingStation s;

    /**
     *  Constructor for reflection based construction
     */
    public NetcdfSoundingAdapter() {
        super("NetcdfSoundingAdapter");
    }


    /**
     * Read a netCDF file of decoded soundings.
     *
     * @param  filename  the fully qualified path and name of the file
     *                   to be adapted.
     *
     * @throws Exception  problem reading file
     */
    public NetcdfSoundingAdapter(String filename) throws Exception {
        super("NetcdfSoundingAdapter");
        this.filename = filename;
        init();
    }


    /**
     * Read a netCDF file of decoded soundings
     *
     * @param file     File to read
     *
     * @throws Exception   problem encountered
     *
     */
    public NetcdfSoundingAdapter(File file) throws Exception {
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
     * Read a netCDF file of decoded soundings.
     *
     * @throws Exception   problem reading the file
     */
    protected void init() throws Exception {
        if (haveInitialized) {
            return;
        }
        super.init();

        try {
            URL url = new URL(filename);
            nc = new NetcdfFile(url);
        } catch (MalformedURLException e) {
            //            System.err.println ("oops filename:" + filename);
            nc = new NetcdfFile(filename, /*readonly=*/ true);
        }

        // get the names of the variables to be used.
        getVariables();

        // get the station list and number of stations
        numStations = stid.getLengths()[0];
        stations    = new ArrayList<SoundingStation>(numStations);  // array of stations
        soundings   = new ArrayList<SoundingOb>(numStations);  // array of soundings
        times       = new ArrayList<DateTime>(10);
        int index[] = new int[1];  // index array to specify which value

        // fill the station and sounding lists
        for (int i = 0; i < numStations; i++) {
            index[0] = i;

            DateTime sndTime;
            // Set the station (s)
            try {
                makeSoundingStation(index);
                sndTime = getObsTime(index);
            } catch (Exception e) {
                continue;
            }

            // Set the data
            if (sndTime != null) {
                makeSoundingOb(index, s, sndTime);
            }
        }
        Collections.sort(times);
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
     * Check to see if the RAOB has any data
     *
     * @param sound    sounding to check
     * @return  sounding with data
     */
    public SoundingOb initSoundingOb(SoundingOb sound) {
        checkInit();
        if ( !sound.hasData()) {
            int idx = soundings.indexOf(sound);
            if (idx < 0) {
                throw new IllegalArgumentException(
                    "SoundingAdapter does not contain sounding:" + sound);
            }
            setRAOBData(new int[] { idx }, sound);
        }
        return sound;
    }



    /**
     * Create a sounding station object from the netCDF file info
     *
     * @param index   netCDF index
     *
     * @throws Exception   problem getting the data
     */
    private void makeSoundingStation(int[] index) throws Exception {
        String wmoID;
        double latvalue;
        double lonvalue;
        double elevvalue;
        try {
            wmoID     = Integer.toString(stid.getInt(index));
            latvalue  = lat.getDouble(index);
            lonvalue  = lon.getDouble(index);
            elevvalue = elev.getDouble(index);
        } catch (Exception ne) {
            throw new Exception(ne.toString());
        }
        s = new SoundingStation(wmoID, latvalue, lonvalue, elevvalue);
        stations.add(s);
    }


    /**
     * Creates a sounding observation with an empty raob
     *
     * @param index     index for accessing variable data
     * @param station   station information
     * @param sndTime   sounding time
     */
    private void makeSoundingOb(int[] index, SoundingStation station,
                                DateTime sndTime) {
        soundings.add(new SoundingOb(station, sndTime));
        if ( !times.contains(sndTime)) {
            times.add(sndTime);
        }
    }

    /**
     *  Fills in the data for the RAOB
     *
     * @param index    index for accessing variable data
     * @param sound    sounding to add to
     */
    private void setRAOBData(int[] index, SoundingOb sound) {

        Variable press;
        Variable height;
        Variable temp;
        Variable dewpt;
        Variable direct;
        Variable speed;
        Unit     pUnit   = null;
        Unit     tUnit   = null;
        Unit     tdUnit  = null;
        Unit     spdUnit = null;
        Unit     dirUnit = null;
        Unit     zUnit   = null;



        int      i;
        int      numLevels;

        float    p[];
        float    t[];
        float    td[];
        float    z[];
        float    spd[];
        float    dir[];

        int[]    j = new int[2];

        j[0] = index[0];
        int   levFill;
        float pFill;
        float zFill;
        float tpFill;
        float tdFill;
        float spdFill;
        float dirFill;


        dbPrint("\nNew Station:\n\t" + sound.getStation());

        // get the mandatory levels first
        if (hasMandP) {
            try {
                numLevels = numMandP.getInt(index);
                levFill   = (int) getFillValue(numMandP, missingValue);
                if ((numLevels > 0) && (numLevels != levFill)) {
                    dbPrint("Num mand pressure levels = " + numLevels);
                    // Get the variables and their units
                    press = nc.get(prMandPVar);
                    pUnit = getUnit(press);
                    if (pUnit == null) {
                        pUnit = CommonUnits.MILLIBAR;
                    }
                    pFill  = getFillValue(press, missingValue);

                    height = nc.get(htMandPVar);
                    // NB: geopotential altitudes stored in units of length
                    zUnit = GeopotentialAltitude.getGeopotentialUnit(
                        getUnit(height));
                    zFill   = getFillValue(height, missingValue);

                    temp    = nc.get(tpMandPVar);
                    tUnit   = getUnit(temp);
                    tpFill  = getFillValue(temp, missingValue);

                    dewpt   = nc.get(tdMandPVar);
                    tdUnit  = getUnit(dewpt);
                    tdFill  = getFillValue(dewpt, missingValue);

                    speed   = nc.get(spdMandPVar);
                    spdUnit = getUnit(speed);
                    spdFill = getFillValue(speed, missingValue);

                    direct  = nc.get(dirMandPVar);
                    dirUnit = getUnit(direct);
                    dirFill = getFillValue(direct, missingValue);

                    // initialize the arrays
                    p   = new float[numLevels];
                    z   = new float[numLevels];
                    t   = new float[numLevels];
                    td  = new float[numLevels];
                    spd = new float[numLevels];
                    dir = new float[numLevels];

                    // fill the arrays
                    for (i = 0; i < numLevels; i++) {
                        j[1]   = i;
                        p[i]   = (press.getFloat(j) == pFill)
                                 ? Float.NaN
                                 : press.getFloat(j);
                        z[i]   = (height.getFloat(j) == zFill)
                                 ? Float.NaN
                                 : height.getFloat(j);
                        t[i]   = (temp.getFloat(j) == tpFill)
                                 ? Float.NaN
                                 : temp.getFloat(j);
                        td[i]  = (dewpt.getFloat(j) == tdFill)
                                 ? Float.NaN
                                 : (dewpointIsDepression == true)
                                   ? t[i] - dewpt.getFloat(j)
                                   : dewpt.getFloat(j);

                        spd[i] = (speed.getFloat(j) == spdFill)
                                 ? Float.NaN
                                 : speed.getFloat(j);
                        dir[i] = (direct.getFloat(j) == dirFill)
                                 ? Float.NaN
                                 : direct.getFloat(j);
                    }
                    sound.getRAOB().setMandatoryPressureProfile(pUnit, p,
                            tUnit, t, tdUnit, td, spdUnit, spd, dirUnit, dir,
                            zUnit, z);
                } else if (debug) {
                    System.err.println(
                        "No mandatory pressure data found for this station");
                }
            } catch (Exception e) {
                LogUtil.logException(
                    "Unable to set mandatory pressure  data for station "
                    + sound.getStation(), e);
            }
        }

        // now get the mandatory wind data
        if (hasMandW) {
            try {
                numLevels = numMandW.getInt(index);
                levFill   = (int) getFillValue(numMandW, missingValue);
                if ((numLevels > 0) && (numLevels != levFill)) {
                    if (debug) {
                        System.err.println("Num mand wind levels = "
                                           + numLevels);
                    }

                    // Get the variables and their units
                    height = nc.get(htMandWVar);
                    // NB: geopotential altitudes stored in units of length
                    zUnit = GeopotentialAltitude.getGeopotentialUnit(
                        getUnit(height));
                    zFill   = getFillValue(height, missingValue);

                    speed   = nc.get(spdMandWVar);
                    spdUnit = getUnit(speed);
                    spdFill = getFillValue(speed, missingValue);

                    direct  = nc.get(dirMandWVar);
                    dirUnit = getUnit(direct);
                    dirFill = getFillValue(direct, missingValue);

                    // initialize the arrays
                    z   = new float[numLevels];
                    spd = new float[numLevels];
                    dir = new float[numLevels];

                    // fill the arrays
                    for (i = 0; i < numLevels; i++) {
                        j[1]   = i;
                        z[i]   = (height.getFloat(j) == zFill)
                                 ? Float.NaN
                                 : height.getFloat(j);
                        spd[i] = (speed.getFloat(j) == spdFill)
                                 ? Float.NaN
                                 : speed.getFloat(j);
                        dir[i] = (direct.getFloat(j) == dirFill)
                                 ? Float.NaN
                                 : direct.getFloat(j);
                    }
                    if ( !allNaNs(z)) {
                        sound.getRAOB().setMandatoryWindProfile(zUnit, z,
                                spdUnit, spd, dirUnit, dir);
                    }
                } else if (debug) {
                    System.err.println("No mandatory wind data found "
                                       + "for this station");
                }
            } catch (Exception e) {
                LogUtil.logException(
                    "Unable to set mandatory wind data for station "
                    + sound.getStation(), e);
            }
        }

        // get the significant temperature levels
        if (hasSigT) {
            try {
                numLevels = numSigT.getInt(index);
                levFill   = (int) getFillValue(numSigT, missingValue);
                if ((numLevels > 0) && (numLevels != levFill)) {
                    if (debug) {
                        System.err.println("Num sig temperature levels = "
                                           + numLevels);
                    }
                    // Get the variables and their units
                    press = nc.get(prSigTVar);
                    pUnit = getUnit(press);
                    if (pUnit == null) {
                        pUnit = CommonUnits.MILLIBAR;
                    }
                    pFill  = getFillValue(press, missingValue);

                    temp   = nc.get(tpSigTVar);
                    tUnit  = getUnit(temp);
                    tpFill = getFillValue(temp, missingValue);

                    dewpt  = nc.get(tdSigTVar);
                    tdUnit = getUnit(dewpt);
                    tdFill = getFillValue(dewpt, missingValue);

                    // initialize the arrays
                    p  = new float[numLevels];
                    t  = new float[numLevels];
                    td = new float[numLevels];

                    // fill the arrays
                    for (i = 0; i < numLevels; i++) {
                        j[1] = i;
                        p[i] = (press.getFloat(j) == pFill)
                               ? Float.NaN
                               : press.getFloat(j);
                        t[i] = (temp.getFloat(j) == tpFill)
                               ? Float.NaN
                               : temp.getFloat(j);
                        /*
                          td[i]  = dewpt.getFloat(j) == tdFill
                          ? Float.NaN
                          : dewpt.getFloat(j);
                        */
                        td[i] = (dewpt.getFloat(j) == tdFill)
                                ? Float.NaN
                                : (dewpointIsDepression == true)
                                  ? t[i] - dewpt.getFloat(j)
                                  : dewpt.getFloat(j);
                    }
                    if ( !allNaNs(p)) {
                        sound.getRAOB().setSignificantTemperatureProfile(
                            pUnit, p, tUnit, t, tdUnit, td);
                    }
                } else if (debug) {
                    System.err.println("No sig temperature data found "
                                       + "for this station");
                }
            } catch (Exception e) {
                LogUtil.logException(
                    "Unable to set significant temperature data for station "
                    + sound.getStation(), e);
            }
        }

        // get the significant levels with respect to wind 
        if (hasSigW) {
            try {
                numLevels = numSigW.getInt(index);
                levFill   = (int) getFillValue(numSigW, missingValue);
                if ((numLevels > 0) && (numLevels != levFill)) {
                    if (debug) {
                        System.err.println("Num significant wind levels = "
                                           + numLevels);
                    }
                    // Get the variables and their units
                    height = nc.get(htSigWVar);
                    // NB: geopotential altitudes stored in units of length
                    zUnit = GeopotentialAltitude.getGeopotentialUnit(
                        getUnit(height));
                    zFill   = getFillValue(height, missingValue);

                    speed   = nc.get(spdSigWVar);
                    spdUnit = getUnit(speed);
                    spdFill = getFillValue(speed, missingValue);

                    direct  = nc.get(dirSigWVar);
                    dirUnit = getUnit(direct);
                    dirFill = getFillValue(direct, missingValue);

                    // initialize the arrays
                    z   = new float[numLevels];
                    spd = new float[numLevels];
                    dir = new float[numLevels];

                    // fill the arrays
                    for (i = 0; i < numLevels; i++) {
                        j[1]   = i;
                        z[i]   = (height.getFloat(j) == zFill)
                                 ? Float.NaN
                                 : height.getFloat(j);
                        spd[i] = (speed.getFloat(j) == spdFill)
                                 ? Float.NaN
                                 : speed.getFloat(j);
                        dir[i] = (direct.getFloat(j) == dirFill)
                                 ? Float.NaN
                                 : direct.getFloat(j);
                    }
                    if ( !allNaNs(z)) {
                        sound.getRAOB().setSignificantWindProfile(zUnit, z,
                                spdUnit, spd, dirUnit, dir);
                    }
                } else {
                    dbPrint(
                        "No significant wind data found for this station");
                }
            } catch (Exception e) {
                LogUtil.logException(
                    "Unable to set significant wind data for station "
                    + sound.getStation(), e);
            }
        }

        // now get the max wind level
        if (hasMaxW) {
            boolean multiLevel = false;
            try {
                numLevels = numMaxW.getInt(index);
                levFill   = (int) getFillValue(numMaxW, missingValue);
                if ((numLevels > 0) && (numLevels != levFill)) {
                    if (debug) {
                        System.err.println("Num max wind levels = "
                                           + numLevels);
                    }
                    // Get the variables and their units
                    press = nc.get(prMaxWVar);
                    // check to see if it handles multipl levels
                    if (press.getRank() > 1) {
                        multiLevel = true;
                    }
                    pUnit = getUnit(press);
                    if (pUnit == null) {
                        pUnit = CommonUnits.MILLIBAR;
                    }
                    pFill   = getFillValue(press, missingValue);

                    speed   = nc.get(spdMaxWVar);
                    spdUnit = getUnit(speed);
                    spdFill = getFillValue(speed, missingValue);

                    direct  = nc.get(dirMaxWVar);
                    dirUnit = getUnit(direct);
                    dirFill = getFillValue(direct, missingValue);

                    // initialize the arrays
                    p   = new float[numLevels];
                    spd = new float[numLevels];
                    dir = new float[numLevels];

                    // fill the arrays
                    if ( !multiLevel) {
                        j = new int[] { index[0] };
                    }
                    for (i = 0; i < numLevels; i++) {
                        if (multiLevel) {
                            j[1] = i;
                        }
                        p[i]   = (press.getFloat(j) == pFill)
                                 ? Float.NaN
                                 : press.getFloat(j);
                        spd[i] = (speed.getFloat(j) == spdFill)
                                 ? Float.NaN
                                 : speed.getFloat(j);
                        dir[i] = (direct.getFloat(j) == dirFill)
                                 ? Float.NaN
                                 : direct.getFloat(j);
                    }
                    if ( !allNaNs(p)) {
                        sound.getRAOB().setMaximumWindProfile(pUnit, p,
                                spdUnit, spd, dirUnit, dir);
                    }
                } else if (debug) {
                    System.err.println("No maximum wind data found "
                                       + "for this station");
                }
            } catch (Exception e) {
                LogUtil.logException(
                    "Unable to set maximum wind data for station "
                    + sound.getStation(), e);
            }
        }

        // get the tropopause levels
        if (hasTrop) {
            try {
                numLevels = oneTrop
                            ? 1
                            : numTrop.getInt(index);
                levFill   = oneTrop
                            ? (int) missingValue
                            : (int) getFillValue(numTrop, missingValue);
                if ((numLevels > 0) && (numLevels != levFill)) {
                    if (debug) {
                        System.err.println("Num tropopause levels = "
                                           + numLevels);
                    }
                    // Get the variables and their units
                    press = nc.get(prTropVar);
                    pUnit = getUnit(press);
                    if (pUnit == null) {
                        pUnit = CommonUnits.MILLIBAR;
                    }
                    pFill   = getFillValue(press, missingValue);

                    temp    = nc.get(tpTropVar);
                    tUnit   = getUnit(temp);
                    tpFill  = getFillValue(temp, missingValue);

                    dewpt   = nc.get(tdTropVar);
                    tdUnit  = getUnit(dewpt);
                    tdFill  = getFillValue(dewpt, missingValue);

                    speed   = nc.get(spdTropVar);
                    spdUnit = getUnit(speed);
                    spdFill = getFillValue(speed, missingValue);

                    direct  = nc.get(dirTropVar);
                    dirUnit = getUnit(direct);
                    dirFill = getFillValue(direct, missingValue);

                    // initialize the arrays
                    p   = new float[numLevels];
                    t   = new float[numLevels];
                    td  = new float[numLevels];
                    spd = new float[numLevels];
                    dir = new float[numLevels];

                    // fill the arrays
                    if (oneTrop) {
                        j = new int[] { index[0] };
                    }
                    if ( !oneTrop
                            || (oneTrop && (press.getFloat(j) != pFill))) {
                        for (i = 0; i < numLevels; i++) {
                            if ( !oneTrop) {
                                j[1] = i;
                            }
                            p[i] = (press.getFloat(j) == pFill)
                                   ? Float.NaN
                                   : press.getFloat(j);
                            t[i] = (temp.getFloat(j) == tpFill)
                                   ? Float.NaN
                                   : temp.getFloat(j);
                            /*
                              td[i]  = dewpt.getFloat(j) == tdFill
                              ? Float.NaN
                              : dewpt.getFloat(j);
                            */
                            td[i]  = (dewpt.getFloat(j) == tdFill)
                                     ? Float.NaN
                                     : (dewpointIsDepression == true)
                                       ? t[i] - dewpt.getFloat(j)
                                       : dewpt.getFloat(j);
                            spd[i] = (speed.getFloat(j) == spdFill)
                                     ? Float.NaN
                                     : speed.getFloat(j);
                            dir[i] = (direct.getFloat(j) == dirFill)
                                     ? Float.NaN
                                     : direct.getFloat(j);
                        }
                        if ( !allNaNs(p)) {
                            sound.getRAOB().setTropopauseProfile(
                                RAOB.newTropopauseProfile(
                                    pUnit, p, tUnit, t, tdUnit, td, spdUnit,
                                    spd, dirUnit, dir));
                        }
                    }
                } else if (debug) {
                    System.err.println("No tropopause data found "
                                       + "for this station");
                }
            } catch (Exception e) {
                LogUtil.logException(
                    "Unable to set tropopause data for station "
                    + sound.getStation(), e);

            }
        }

    }

    /**
     * Create a DateTime object for the soundingob
     *
     * @param index    index for accessing variable data
     * @return  DateTime of object
     */
    private DateTime getObsTime(int[] index) {
        // if first time through, get some metadata
        try {
            if (timeUnit == null) {
                timeUnit = getUnit(time);
                if (timeUnit == null) {
                    timeUnit = RealType.Time.getDefaultUnit();
                }
                Attribute a = time.getAttribute("_FillValue");
                if (a != null) {
                    timeFill = a.getNumericValue().doubleValue();
                }
            }
        } catch (Exception ve) {
            timeUnit = RealType.Time.getDefaultUnit();
            timeFill = Double.NaN;
        }
        try {
            double val = time.getDouble(index);

            return (Double.doubleToLongBits(val)
                    == Double.doubleToLongBits(timeFill))
                   ? (DateTime) null
                   : (timeUnit == null)
                     ? new DateTime(val)
                     : new DateTime(new Real(RealType.Time, val, timeUnit));
        } catch (Exception ne) {
            LogUtil.logException("getObsTime", ne);
        }
        return null;
    }

    /**
     * Get a default value  using this Adapter's prefix
     *
     * @param name  name of property key
     * @param dflt  default value
     * @return  the default for that property or dflt if not in properties
     */
    protected String getDflt(String name, String dflt) {
        return getDflt("NetcdfSoundingAdapter.", name, dflt);
    }

    /**
     * Determines the names of the variables in the netCDF file that
     * should be used.
     *
     * @throws Exception   problem finding a variable
     */
    private void getVariables() throws Exception {

        String    idVar   = getDflt("stationIDVariable", "wmoStaNum");
        Attribute a       = nc.getAttribute("timeVariables");
        String    timeVar = (a != null)
                            ? a.getStringValue()
                            : getDflt("soundingTimeVariable", "relTime");

        Attribute version = nc.getAttribute("version");
        if ((version != null)
                && (version.getStringValue().indexOf(
                    "Forecast Systems Lab 1.3") >= 0)) {
            idVar   = "wmoStat";
            timeVar = "synTime";
        }

        stid = nc.get(idVar);
        if (stid == null) {
            throw new Exception("Unable to find station id variable");
        }

        lat = nc.get(getDflt("latitudeVariable", "staLat"));
        if (lat == null) {
            throw new Exception("Unable to find latitude variable");
        }

        lon = nc.get(getDflt("longitudeVariable", "staLon"));
        if (lon == null) {
            throw new Exception("Unable to find longitude variable");
        }

        elev = nc.get(getDflt("stationElevVariable", "staElev"));
        if (elev == null) {
            throw new Exception("Unable to find station elevation variable");
        }

        time = nc.get(timeVar);
        if (time == null) {
            throw new Exception("Unable to find sounding time variable");
        }

        numMandP = nc.get(getDflt("numMandPresLevels", "numMand"));

        if (numMandP != null) {
            hasMandP    = true;
            prMandPVar  = getDflt("mandPPressureVariable", "prMan");
            htMandPVar  = getDflt("mandPHeightVariable", "htMan");
            tpMandPVar  = getDflt("mandPTempVariable", "tpMan");
            tdMandPVar  = getDflt("mandPDewptVariable", "tdMan");
            spdMandPVar = getDflt("mandPWindSpeedVariable", "wsMan");
            dirMandPVar = getDflt("mandPWindDirVariable", "wdMan");
        }

        numMandW = nc.get(getDflt("numMandWindLevels", "numMandW"));
        if (numMandW != null) {
            hasMandW    = true;
            htMandWVar  = getDflt("mandWHeightVariable", "htMandW");
            spdMandWVar = getDflt("mandWWindSpeedVariable", "wsMandW");
            dirMandWVar = getDflt("mandWWindDirVariable", "wdMandW");
        }

        numSigT = nc.get(getDflt("numSigTempLevels", "numSigT"));
        if (numSigT != null) {
            hasSigT   = true;
            prSigTVar = getDflt("sigTPressureVariable", "prSigT");
            tpSigTVar = getDflt("sigTTempVariable", "tpSigT");
            tdSigTVar = getDflt("sigTDewptVariable", "tdSigT");
        }

        numSigW = nc.get(getDflt("numSigWindLevels", "numSigW"));
        if (numSigW != null) {
            hasSigW    = true;
            htSigWVar  = getDflt("sigWHeightVariable", "htSigW");
            spdSigWVar = getDflt("sigWWindSpeedVariable", "wsSigW");
            dirSigWVar = getDflt("sigWWindDirVariable", "wdSigW");
        }

        numMaxW = nc.get(getDflt("numMaxWindLevels", "numMwnd"));
        if (numMaxW != null) {
            hasMaxW    = true;
            prMaxWVar  = getDflt("maxWPressureVariable", "prMaxW");
            spdMaxWVar = getDflt("maxWWindSpeedVariable", "wsMaxW");
            dirMaxWVar = getDflt("maxWWindDirVariable", "wdMaxW");
        }

        numTrop = nc.get(getDflt("numTropLevels", "numTrop"));
        if (numTrop == null) {
            // see if this is the old version (one trop level)
            if (nc.contains(getDflt("prTropName", "prTrop"))) {
                hasTrop = true;
                oneTrop = true;
            }
        } else {
            hasTrop = true;
        }

        if (hasTrop) {
            prTropVar  = getDflt("tropPressureVariable", "prTrop");
            tpTropVar  = getDflt("tropTempVariable", "tpTrop");
            tdTropVar  = getDflt("tropDewptVariable", "tdTrop");
            spdTropVar = getDflt("tropWindSpeedVariable", "wsTrop");
            dirTropVar = getDflt("tropWindDirVariable", "wdTrop");
        }

        // Check to see if dewpoint is stored as a depression or actual value.
        dewpointIsDepression =
            Boolean.valueOf(getDflt("dewpointIsDepression",
                                    "true")).booleanValue();
        // See if there is a default value for missing data
        try {
            missingValue = Float.parseFloat(getDflt("missingValue", "99999"));
        } catch (NumberFormatException excp) {
            missingValue = 99999;
        }
    }

    /**
     * Gets the units of the variable.   Checks for the "units" attribute.
     *
     * @param v variable to check
     *
     * @return  corresponding Unit or null if can't be decoded
     * @see ucar.unidata.data.DataUtil#parseUnit(String)
     */
    private Unit getUnit(Variable v) {
        Unit      u = null;
        Attribute a = v.getAttribute("units");
        if (a != null) {
            u = DataUtil.parseUnit(a.getStringValue());
        }
        return u;
    }

    /**
     * Gets the fill value for the variable or if none, returns
     * a default value
     *
     * @param v                 netCDF variable
     * @param defaultValue      default value
     * @return  actual value or <code>defaultValue</code>
     */
    private float getFillValue(Variable v, float defaultValue) {
        Attribute a = v.getAttribute("_FillValue");
        return (a == null)
               ? defaultValue
               : a.getNumericValue().floatValue();

    }

    /**
     * Check  to see if the array is all NaN's
     *
     * @param values  array to check
     *
     * @return true if all NaNs otherwise false;
     */
    private boolean allNaNs(float[] values) {
        for (int i = 0; i < values.length; i++) {
            if ( !Float.isNaN(values[i])) {
                return false;
            }
        }
        return true;
    }
}


/*
 * $Id: AddeSoundingAdapter.java,v 1.27 2007/08/20 22:37:52 dmurray Exp $
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


import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddeException;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;

import ucar.unidata.beans.InvisiblePropertiedBean;
import ucar.unidata.beans.NonVetoableProperty;

import ucar.unidata.data.DataUtil;
import ucar.unidata.data.sounding.RAOB;
import ucar.unidata.data.sounding.SoundingOb;
import ucar.unidata.data.sounding.SoundingStation;

import ucar.unidata.util.Defaults;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.UtcDate;


import ucar.visad.Util;

import ucar.visad.quantities.GeopotentialAltitude;

import visad.DateTime;
import visad.Unit;
import visad.VisADException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Class for retrieving upper air data from an ADDE remote server. Creates
 * a SoundingOb for each of the stations on the remote server for the
 * latest available data.
 */
public class AddeSoundingAdapter extends SoundingAdapterImpl implements SoundingAdapter {

    /** parameter identifier */
    private static final String P_PARAM = "param";

    /** number of obs identifier */
    private static final String P_NUM = "num";

    /** all obs identifier */
    private static final String P_ALL = "all";

    /** number of obs identifier */
    private static final String P_POS = "pos";

    /** group identifier */
    private static final String P_GROUP = "group";

    /** descriptor identifier */
    private static final String P_DESCR = "descr";

    /** select identifier */
    private static final String P_SELECT = "select";

    /** URL type identifier */
    private static final String URL_ROOT = "/point";

    /** URL protocol identifier */
    private static final String URL_PROTOCOL = "adde";

    /** server propert */
    private NonVetoableProperty serverProperty;

    /** mandatory data set property */
    private NonVetoableProperty mandatoryDatasetProperty;

    /** significant data set property */
    private NonVetoableProperty significantDatasetProperty;

    /** stations property */
    private NonVetoableProperty stationsProperty;

    /** sounding times property */
    private NonVetoableProperty soundingTimesProperty;

    /** mandatory data group name */
    private String manGroup;

    /** mandatory data descriptor */
    private String manDescriptor;

    /** sig data group name */
    private String sigGroup = null;

    /** sig data descriptor */
    private String sigDescriptor = null;

    /** use main hours only */
    private boolean mainHours = false;

    /** name of mandP pressure variable */
    private String prMandPVar = "p";

    /** name of mandP height variable */
    private String htMandPVar = "z";

    /** name of mandP temp variable */
    private String tpMandPVar = "t";

    /** name of mandP dewpoint variable */
    private String tdMandPVar = "td";

    /** name of mandP wind speed variable */
    private String spdMandPVar = "spd";

    /** name of mandP wind dir variable */
    private String dirMandPVar = "dir";

    /** name of day variable */
    private String dayVar = "day";

    /** name of time variable */
    private String timeVar = "time";

    /** name of station id variable */
    private String idVar = "idn";

    /** name of station latitude variable */
    private String latVar = "lat";

    /** name of station longitude variable */
    private String lonVar = "lon";

    /** name of station elevation variable */
    private String eleVar = "zs";


    /** server name */
    private String server;

    /** mandatory dataset name */
    private String mandDataset;

    /** significant dataset name */
    private String sigDataset;

    /** default server */
    private String defaultServer = "adde.unidata.ucar.edu";

    /** default mandatory data set */
    private String defaultMandDataset = "rtptsrc/uppermand";

    /** default significant dataset */
    private String defaultSigDataset = "rtptsrc/uppersig";




    /**
     * Construct an empty AddeSoundingAdapter
     */
    public AddeSoundingAdapter() {
        super("AddeSoundingAdapter");
    }

    /**
     * Retreive upper air data from a remote ADDE server using only
     * mandatory data.
     *
     * @param    server   name or IP address of remote server
     *
     * @throws Exception (AddeException) if there is no data available or there
     *              is trouble connecting to the remote server
     */
    public AddeSoundingAdapter(String server) throws Exception {
        this(server, null);
    }

    /**
     * Retreive upper air data from a remote ADDE server using only
     * mandatory data.
     *
     * @param    server   name or IP address of remote server
     * @param    dataset  name of ADDE dataset (group/descriptor)
     *
     * @throws  Exception (AddeException) if there is no data available or there
     *              is trouble connecting to the remote server
     */
    public AddeSoundingAdapter(String server, String dataset)
            throws Exception {
        this(server, dataset, null);
    }



    /**
     * Retreive upper air data from a remote ADDE server using only
     * mandatory data.
     *
     * @param    server       name or IP address of remote server
     * @param    mandDataset  name of mandatory level upper air ADDE
     *                        dataset (group/descriptor)
     * @param    sigDataset   name of significant level upper air ADDE
     *                        dataset (group/descriptor)
     *
     * @throws Exception (AddeException) if there is no data available
     *            or there is trouble connecting to the remote server
     */
    public AddeSoundingAdapter(String server, String mandDataset,
                               String sigDataset)
            throws Exception {
        this(server, mandDataset, sigDataset, false);
    }

    /**
     * Retreive upper air data from a remote ADDE server using only
     * mandatory data.
     *
     * @param    server       name or IP address of remote server
     * @param    mandDataset  name of mandatory level upper air ADDE
     *                        dataset (group/descriptor)
     * @param    sigDataset   name of significant level upper air ADDE
     *                        dataset (group/descriptor)
     * @param    mainHours    only get data for main (00 &amp; 12Z) hours
     *
     * @throws Exception (AddeException) if there is no data available
     *            or there is trouble connecting to the remote server
     */
    public AddeSoundingAdapter(String server, String mandDataset,
                               String sigDataset, boolean mainHours)
            throws Exception {
        super("AddeSoundingAdapter");
        this.server      = server;
        this.mandDataset = mandDataset;
        this.sigDataset  = sigDataset;
        this.mainHours   = mainHours;
        init();
    }


    /**
     * Initialize the class.  Populate the variable list and get
     * the server and dataset information.
     *
     * @throws Exception   problem occurred
     */
    protected void init() throws Exception {
        if (haveInitialized) {
            return;
        }
        super.init();

        getVariables();

        if (server == null) {
            server = defaultServer;
        }

        if (mandDataset == null) {
            mandDataset = defaultMandDataset;
        }

        if (sigDataset == null) {
            sigDataset = defaultSigDataset;
        }

        // set up the properties
        addProperty(serverProperty = new NonVetoableProperty(this, "server"));
        serverProperty.setValue(server);

        addProperty(mandatoryDatasetProperty = new NonVetoableProperty(this,
                "mandatoryDataset"));
        mandatoryDatasetProperty.setValue(mandDataset);

        addProperty(significantDatasetProperty =
            new NonVetoableProperty(this, "significantDataset"));
        significantDatasetProperty.setValue(sigDataset);

        addProperty(stationsProperty = new NonVetoableProperty(this,
                "stations"));
        addProperty(soundingTimesProperty = new NonVetoableProperty(this,
                "soundingTimes"));
        loadStations();
    }


    /**
     *  Utility method that calls McIDASUtil.intBitsToString
     *  to get a string to compare to the given parameter s
     *
     * @param v    integer string value
     * @param s    string to compare
     * @return  true if they are equal
     */
    private boolean intEqual(int v, String s) {
        return (McIDASUtil.intBitsToString(v).equals(s));
    }



    /**
     * Return the given String in single quotes
     *
     * @param s   add single quotes to the string for select clauses
     * @return  single quoted string (ex:  'foo')
     */
    private String sQuote(String s) {
        return "'" + s + "'";
    }


    /**
     * Assemble the url from the given url argument array. This turns around
     * and calls makeUrl, passing in the URL_ROOT ("/point") and the
     * urlRoot to use.
     *
     * @param args  URL arguments, key value pairs
     *              (ex: arg[0]=arg[1]&arg[2]=arg[3]...)
     * @return  associated URL
     */
    private String makeUrl(String[] args) {
        return makeUrl(URL_ROOT, args);
    }

    /**
     * Assemble the url from the given url root and url argument array.
     * This returns:
     * "URL_PROTOCOL://server urlRoot ?arg[0]=arg[1]&arg[2]=arg[3]...
     *
     * @param urlRoot   root for the URL
     * @param args      key/value pair arguments
     * @return  ADDE URL
     */
    private String makeUrl(String urlRoot, String[] args) {
        return Misc.makeUrl(URL_PROTOCOL, server, urlRoot, args);
    }


    /**
     * Update this adapter for new data
     */
    public void update() {
        checkInit();
        try {
            loadStations();
        } catch (Exception exc) {
            LogUtil.logException("Error updating AddeSoundingAdapter", exc);
        }
    }


    /**
     * Initialize the times, stations and soundings lists.
     * Load the data into them.
     *
     * @throws AddeException   error accessing the data
     */

    private void loadStations() throws AddeException {
        times     = new ArrayList(8);
        stations  = new ArrayList(100);
        soundings = new ArrayList(100);

        if ((server != null) && (mandDataset != null)) {
            loadStationsInner();
        }
        stationsProperty.setValueAndNotifyListeners(stations);
        soundingTimesProperty.setValueAndNotifyListeners(times);
    }

    /**
     * Initialize the group and descriptor strings
     */
    private void initGroupAndDescriptors() {
        if (manGroup == null) {
            StringTokenizer tok = new StringTokenizer(mandDataset, "/");
            if (tok.countTokens() != 2) {
                throw new IllegalStateException(
                    "Illegal mandatory dataset name " + mandDataset);
            }
            manGroup      = tok.nextToken();
            manDescriptor = tok.nextToken();
        }
        if ((sigDataset != null) && (sigGroup == null)) {
            StringTokenizer tok = new StringTokenizer(sigDataset, "/");
            if (tok.countTokens() != 2) {
                throw new IllegalStateException(
                    "Illegal significant dataset name " + mandDataset);
            }
            sigGroup      = tok.nextToken();
            sigDescriptor = tok.nextToken();
        }
    }


    /**
     * Actually do the work of loading the stations
     *
     * @throws AddeException  problem accessing data
     */
    private void loadStationsInner() throws AddeException {
        initGroupAndDescriptors();
        String request = makeUrl(new String[] {
            P_GROUP, manGroup, P_DESCR, manDescriptor, P_PARAM,
            StringUtil.join(new String[] {
                dayVar, timeVar, idVar, latVar, lonVar, eleVar
            }), P_NUM, P_ALL, P_POS, P_ALL
        }) + getManUserProj() + getStationsSelectString();
        dbPrint(request);



        //System.err.println("loading stations: " + request);

        AddePointDataReader dataReader = new AddePointDataReader(request);
        String[]            units      = dataReader.getUnits();
        int[]               scales     = dataReader.getScales();
        int[][]             data       = dataReader.getData();

        for (int i = 0; i < data[0].length; i++) {
            int    day   = data[0][i];
            int    time  = data[1][i];
            String wmoID = Integer.toString(data[2][i]);
            double lat   = scaleValue(data[3][i], scales[3]);
            double lon   = scaleValue(data[4][i], scales[4]);
            lon = -lon;  // change from McIDAS to eastPositive
            double elev = scaleValue(data[5][i], scales[5]);
            try {
                SoundingStation s = new SoundingStation(wmoID, lat, lon,
                                        elev);
                if ( !(stations.contains(s))) {
                    stations.add(s);
                }
                DateTime dt = new DateTime(McIDASUtil.mcDayTimeToSecs(day,
                                  time));
                soundings.add(new SoundingOb(s, dt));
                if ( !times.contains(dt)) {
                    times.add(dt);
                }
            } catch (Exception vexcp) {
                LogUtil.logException("Creating sounding", vexcp);
            }
        }
        Collections.sort(times);
        if (debug) {
            System.err.println("Times:" + times);
        }
    }

    /**
     *  Set the ADDE server name
     *
     * @param  server  server name or IP address
     */
    public void setSource(String server) {
        this.server = server;
        if (serverProperty != null) {
            serverProperty.setValue(server);
        }
    }

    /**
     * Get the source of the data (server)
     *
     * @return  server name or IP address
     */
    public String getSource() {
        return server;
    }


    /**
     * Set the mandatory data set name
     *
     * @param value  mandatory data set name
     */
    public void setMandDataset(String value) {
        mandDataset = value;
    }

    /**
     * Set the mandatory data set name
     *
     * @return  the mandatory data set name
     */
    public String getMandDataset() {
        return mandDataset;
    }


    /**
     * Set the significant data set name
     *
     * @param value the significant data set name
     */
    public void setSigDataset(String value) {
        sigDataset = value;
    }

    /**
     * Get the significant data set name
     *
     * @return the significant data set name
     */
    public String getSigDataset() {
        return sigDataset;
    }


    /**
     * Check to see if the RAOB has any data
     *
     * @param sound    sounding to check
     * @return  a sounding with data
     */
    public SoundingOb initSoundingOb(SoundingOb sound) {
        if ( !sound.hasData()) {
            setRAOBData(sound);
        }
        return sound;
    }

    /**
     * Make the select string that will get this observation
     *
     * @param sound   sounding to use
     * @return  select string
     */
    private String makeSelectString(SoundingOb sound) {
        return makeSelectString(sound.getStation().getIdentifier(),
                                sound.getTimestamp());
    }


    /**
     * Make a select string for the given station id and date
     *
     * @param wmoId    station id
     * @param date     time of data
     * @return  ADDE select clause for the given parameters
     */
    private String makeSelectString(String wmoId, DateTime date) {
        String day  = UtcDate.getYMD(date);
        String time = UtcDate.getHH(date);
        //int[] daytime = McIDASUtil.mcSecsToDayTime((long) date.getValue());
        return new String(idVar + " " + wmoId + ";" + dayVar + " " + day
                          + ";" + timeVar + " " + time);
    }

    /**
     * Fills in the data for the RAOB
     *
     * @param sound   sounding ob to set
     */
    private void setRAOBData(SoundingOb sound) {

        initGroupAndDescriptors();
        int                 numLevels;
        Unit                pUnit   = null,
                            tUnit   = null,
                            tdUnit  = null,
                            spdUnit = null,
                            dirUnit = null,
                            zUnit   = null;
        float               p[], t[], td[], z[], spd[], dir[];
        AddePointDataReader apdr;

        String              request = getMandatoryURL(sound);

        dbPrint(request);
        try {
            if (sound.getMandatoryFile() != null) {
                request = "file:" + sound.getMandatoryFile();
                //                System.err.println ("using fixed mandatory url:" + request);
            }


            apdr = new AddePointDataReader(request);
            String[] params = apdr.getParams();
            int[]    scales = apdr.getScales();
            String[] units  = apdr.getUnits();
            int[][]  data   = apdr.getData();

            numLevels = data[0].length;
            if (numLevels > 0) {
                dbPrint("Num mand pressure levels = " + numLevels);
                // Get the their units
                pUnit = getUnit(units[0]);
                // NB: geopotential altitudes stored in units of length
                zUnit = GeopotentialAltitude.getGeopotentialUnit(
                    getUnit(units[1]));
                tUnit   = getUnit(units[2]);
                tdUnit  = getUnit(units[3]);
                spdUnit = getUnit(units[4]);
                dirUnit = getUnit(units[5]);

                // initialize the arrays
                p   = new float[numLevels];
                z   = new float[numLevels];
                t   = new float[numLevels];
                td  = new float[numLevels];
                spd = new float[numLevels];
                dir = new float[numLevels];

                // fill the arrays
                for (int i = 0; i < numLevels; i++) {
                    p[i]   = (float) scaleValue(data[0][i], scales[0]);
                    z[i]   = (float) scaleValue(data[1][i], scales[1]);
                    t[i]   = (float) scaleValue(data[2][i], scales[2]);
                    td[i]  = (float) scaleValue(data[3][i], scales[3]);
                    spd[i] = (float) scaleValue(data[4][i], scales[4]);
                    dir[i] = (float) scaleValue(data[5][i], scales[5]);
                }
                if (debug) {
                    System.err.println("P[" + pUnit + "]\t" + "Z[" + zUnit
                                       + "]\t" + "T[" + tUnit + "]\t" + "TD["
                                       + tdUnit + "]\t" + "SPD[" + spdUnit
                                       + "]\t" + "DIR[" + dirUnit + "]");
                    for (int i = 0; i < numLevels; i++) {
                        System.err.println(p[i] + "\t" + z[i] + "\t" + t[i]
                                           + "\t" + td[i] + "\t" + spd[i]
                                           + "\t" + dir[i]);
                    }
                }
                sound.getRAOB().setMandatoryPressureProfile(pUnit, p, tUnit,
                        t, tdUnit, td, spdUnit, spd, dirUnit, dir, zUnit, z);
            }
        } catch (Exception e) {
            LogUtil.logException(
                "Unable to set mandatory pressure data for station "
                + sound.getStation(), e);
        }

        // Done if we have no sig data
        if ((sigGroup == null) || (sigDescriptor == null)) {
            return;
        }

        request = getSigURL(sound);
        dbPrint(request);

        // get the sig data
        try {
            if (sound.getSigFile() != null) {
                request = "file:" + sound.getSigFile();
                //                System.err.println ("using fixed sig url:" + request);
            }

            apdr = new AddePointDataReader(request);
            String[] params = apdr.getParams();
            int[]    scales = apdr.getScales();
            String[] units  = apdr.getUnits();
            int[][]  data   = apdr.getData();

            numLevels = data[0].length;
            if (numLevels > 0) {
                // Determine how many of each kind of level
                int numSigW = 0;
                int numSigT = 0;
                for (int i = 0; i < data[0].length; i++) {
                    if (intEqual(data[0][i], "SIGT")) {
                        numSigT++;
                    }
                    if (intEqual(data[0][i], "SIGW")) {
                        numSigW++;
                    }
                }

                dbPrint("Num sig temperature levels = " + numSigT);
                dbPrint("Num sig wind levels = " + numSigW);


                // Get the units & initialize the arrays
                pUnit  = getUnit("mb");
                tUnit  = getUnit("k");
                tdUnit = getUnit("k");
                // NB: geopotential altitudes stored in units of length
                zUnit =
                    GeopotentialAltitude.getGeopotentialUnit(getUnit("m"));
                spdUnit = getUnit("mps");
                dirUnit = getUnit("deg");

                p       = new float[numSigT];
                t       = new float[numSigT];
                td      = new float[numSigT];
                z       = new float[numSigW];
                spd     = new float[numSigW];
                dir     = new float[numSigW];

                // fill the arrays
                int j = 0;  // counter for sigT
                int l = 0;  // counter for sigW
                for (int i = 0; i < numLevels; i++) {
                    if (intEqual(data[0][i], "SIGT")) {
                        p[j]  = (float) scaleValue(data[3][i], 1);
                        t[j]  = (float) scaleValue(data[1][i], 2);
                        td[j] = (float) scaleValue(data[2][i], 2);
                        j++;
                    } else if (intEqual(data[0][i], "SIGW")) {
                        z[l] = (data[3][i] == 0)
                               ? (float) ((SoundingStation) sound
                                   .getStation()).getAltitudeAsDouble()
                               : (float) scaleValue(data[3][i], 0);
                        spd[l] = (float) scaleValue(data[2][i], 1);
                        dir[l] = (float) scaleValue(data[1][i], 0);
                        l++;
                    }
                }
                if (numSigT > 0) {
                    try {
                        if (debug) {
                            System.err.println("P[" + pUnit + "]\tT[" + tUnit
                                    + "]\tTD[" + tdUnit + "]");
                            for (int i = 0; i < numSigT; i++) {
                                System.err.println(p[i] + "\t" + t[i] + "\t"
                                        + td[i]);
                            }
                        }
                        sound.getRAOB().setSignificantTemperatureProfile(
                            pUnit, p, tUnit, t, tdUnit, td);
                    } catch (Exception e) {
                        LogUtil.logException(
                            "Unable to set significant temperature data for station "
                            + sound.getStation(), e);
                    }
                }
                if (numSigW > 0) {
                    try {
                        if (debug) {
                            System.err.println("Z[" + zUnit + "]\tSPD["
                                    + spdUnit + "]\tDIR[" + dirUnit + "]");
                            for (int i = 0; i < numSigW; i++) {
                                System.err.println(z[i] + "\t" + spd[i]
                                        + "\t" + dir[i]);
                            }
                        }
                        sound.getRAOB().setSignificantWindProfile(zUnit, z,
                                spdUnit, spd, dirUnit, dir);
                    } catch (Exception e) {
                        LogUtil.logException(
                            "Unable to set significant wind data for station "
                            + sound.getStation(), e);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logException(
                "Unable to retrieve significant level data for station "
                + sound.getStation(), e);
        }
    }


    /**
     * scale the values returned from the server
     *
     * @param value    value to scale
     * @param scale    scale factor
     * @return   scaled value
     */
    private double scaleValue(int value, int scale) {
        return (value == McIDASUtil.MCMISSING)
               ? Double.NaN
               : (value / Math.pow(10.0, (double) scale));
    }

    /**
     * Gets the units of the variable.  Now just a passthrough to
     * ucar.unidata.data.DataUtil.parseUnit.
     *
     * @param unitName   unit name
     * @return  corresponding Unit or null if can't be decoded
     * @see ucar.unidata.data.DataUtil#parseUnit(String)
     */
    private Unit getUnit(String unitName) {
        return DataUtil.parseUnit(unitName);
    }

    /**
     * Get a default value  using this Adapter's prefix
     *
     * @param name  name of property key
     * @param dflt  default value
     * @return  the default for that property or dflt if not in properties
     */
    private String getDflt(String name, String dflt) {
        return getDflt("AddeSoundingAdapter.", name, dflt);
    }

    /**
     * Determines the names of the variables in the netCDF file that
     * should be used.
     */
    private void getVariables() {
        // initialize the defaults for this object
        try {
            defaultServer      = getDflt("serverName", defaultServer);
            defaultMandDataset = getDflt("mandDataset", defaultMandDataset);
            defaultSigDataset  = getDflt("sigDataset", defaultSigDataset);
            idVar              = getDflt("stationIDVariable", idVar);
            latVar             = getDflt("latitudeVariable", latVar);
            lonVar             = getDflt("longitudeVariable", lonVar);
            eleVar             = getDflt("stationElevVariable", eleVar);
            timeVar            = getDflt("soundingTimeVariable", timeVar);
            dayVar             = getDflt("soundingDayVariable", dayVar);

            prMandPVar         = getDflt("mandPPressureVariable", prMandPVar);
            htMandPVar         = getDflt("mandPHeightVariable", htMandPVar);
            tpMandPVar         = getDflt("mandPTempVariable", tpMandPVar);
            tdMandPVar         = getDflt("mandPDewptVariable", tdMandPVar);
            spdMandPVar = getDflt("mandPWindSpeedVariable", spdMandPVar);
            dirMandPVar        = getDflt("mandPWindDirVariable", dirMandPVar);

            // Significant Temperature data
            /*
              numSigT      = nc.get(getDflt("NetcdfSoundingAdapter.", "numSigTempLevels", "numSigT"));
              if (numSigT != null)    {
              hasSigT = true;
              prSigTVar =  getDflt ("NetcdfSoundingAdapter.", "sigTPressureVariable", "prSigT");
              tpSigTVar =  getDflt ("NetcdfSoundingAdapter.", "sigTTempVariable", "tpSigT");
              tdSigTVar =  getDflt("NetcdfSoundingAdapter.", "sigTDewptVariable", "tdSigT");
              }

              // Significant Wind data
              numSigW =  nc.get(getDflt("NetcdfSoundingAdapter.", "numSigWindLevels", "numSigW"));
              if (numSigW != null)  {
              hasSigW = true;
              htSigWVar =  getDflt ("NetcdfSoundingAdapter.", "sigWHeightVariable", "htSigW");
              spdSigWVar = getDflt("NetcdfSoundingAdapter.", "sigWWindSpeedVariable", "wsSigW");
              dirSigWVar = getDflt("NetcdfSoundingAdapter.", "sigWWindDirVariable", "wdSigW");
              }
            */
        } catch (Exception e) {
            System.err.println("Unable to initialize defaults file");
        }
    }

    /**
     * Get significant data ADDE user/project id for the data
     * @return  user/project string (ex: "id=idv proj=0")
     */
    private String getSigUserProj() {
        return getUserProj(new String(sigGroup + "/"
                                      + sigDescriptor).toUpperCase());
    }

    /**
     * Make the mandatory levels URL for the given sounding
     *
     * @param sound sounding
     *
     * @return mandatory url
     */
    public String getMandatoryURL(SoundingOb sound) {
        String select      = makeSelectString(sound);
        String paramString = StringUtil.join(new String[] {
            prMandPVar, htMandPVar, tpMandPVar, tdMandPVar, spdMandPVar,
            dirMandPVar
        });
        String request = makeUrl(new String[] {
            P_GROUP, manGroup, P_DESCR, manDescriptor, P_SELECT,
            sQuote(select), P_PARAM, paramString, P_NUM, P_ALL, P_POS, P_ALL
        }) + getManUserProj();

        return request;

    }

    /**
     * Make the url for the significant  levels for the sounding
     *
     * @param sound the sounding
     *
     * @return sig url
     */
    public String getSigURL(SoundingOb sound) {
        String select      = makeSelectString(sound);
        String paramString = StringUtil.join(new String[] {
            prMandPVar, htMandPVar, tpMandPVar, tdMandPVar, spdMandPVar,
            dirMandPVar
        });


        String request = makeUrl(new String[] {
            P_GROUP, sigGroup, P_DESCR, sigDescriptor, P_SELECT,
            sQuote(select), P_PARAM, "type p1 p2 p3", P_NUM, P_ALL, P_POS,
            P_ALL
        })  //"&param=type p1 p2 p3&num=all&pos=1" + 
        + getSigUserProj();

        return request;
    }


    /**
     * Get mandatory data ADDE user/project id for the data
     *
     * @return  user/project string (ex: "id=idv proj=0")
     */
    private String getManUserProj() {
        return getUserProj(new String(manGroup + "/"
                                      + manDescriptor).toUpperCase());
    }

    /**
     * Get the user/project string for the given key
     *
     * @param key   group/descriptor
     *
     * @return  user/project string (ex: "id=idv proj=0")  for the specified
     *          dataset
     */
    private String getUserProj(String key) {
        StringBuffer buf = new StringBuffer();
        buf.append("&proj=");
        buf.append(getDflt("", key.toUpperCase().trim() + ".proj", "0"));
        buf.append("&user=");
        buf.append(getDflt("", key.toUpperCase().trim() + ".user", "user"));
        buf.append("&compress=gzip");
        //buf.append("&debug=true");
        return buf.toString();
    }

    /**
     * Get the select string for use in loadStations
     *
     * @return  select string
     */
    private String getStationsSelectString() {
        if ( !mainHours) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("&SELECT='");
        buf.append(timeVar);
        buf.append(" 00,12'");
        return buf.toString();
    }

    /**
     * test by running java ucar.unidata.data.sounding.AddeSoundingAdapter
     *
     * @param args   array of arguments.  Takes up to 3 arguments as
     *               "server&nbsp;mandatory dataset&nbsp;significant dataset"
     *               Use "x" for any of these arguments to use the default.
     */
    public static void main(String[] args) {
        String server = "adde.unidata.ucar.edu";
        String manset = "rtptsrc/uppermand";
        String sigset = "rtptsrc/uppersig";
        if (args.length > 0) {
            server = ( !(args[0].equalsIgnoreCase("x")))
                     ? args[0]
                     : server;
            if (args.length > 1) {
                manset = ( !(args[1].equalsIgnoreCase("x")))
                         ? args[1]
                         : manset;
            }
            if (args.length > 2) {
                sigset = ( !(args[2].equalsIgnoreCase("x")))
                         ? args[2]
                         : sigset;
            }
        }
        //        try  {
        //            AddeSoundingAdapter asa = 
        //                //new AddeSoundingAdapter(server, manset, sigset);
        //                new AddeSoundingAdapter();
        /*
          Thread.sleep(5000);
          asa.setServer("hurri.kean.edu");
          Thread.sleep(5000);
          asa.setServer("adde.unidata.ucar.edu");
          Thread.sleep(5000);
          asa.setMandatoryDataset("blizzard/uppermand");
        */
        //        }
        //        catch (Exception me)   {
        //            System.err.println(me);
        //        }
    }


    /**
     * The string representation
     * @return The string
     */
    public String toString() {
        return "SoundingAdapter:" + server;

    }





}


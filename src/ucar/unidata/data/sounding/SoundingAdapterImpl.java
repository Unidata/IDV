/*
 * $Id: SoundingAdapterImpl.java,v 1.11 2007/06/13 22:34:36 dmurray Exp $
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


import ucar.unidata.beans.InvisiblePropertiedBean;
import ucar.unidata.beans.NonVetoableProperty;

import ucar.unidata.data.sounding.RAOB;
import ucar.unidata.data.sounding.SoundingOb;
import ucar.unidata.data.sounding.SoundingStation;

import ucar.unidata.util.Defaults;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.quantities.GeopotentialAltitude;

import visad.DateTime;
import visad.Unit;
import visad.VisADException;

import visad.data.units.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.StringTokenizer;


/**
 * Class for retrieving upper air data from an ADDE remote server. Creates
 * a SoundingOb for each of the stations on the remote server for the
 * latest available data.
 */
public abstract class SoundingAdapterImpl extends InvisiblePropertiedBean {

    /** Initialization flag */
    protected boolean haveInitialized = false;

    /** Debug flag */
    protected boolean debug = false;

    /** Defaults for parameters */
    protected Defaults defaults = null;  // configuration object

    /** List of stations */
    protected List<SoundingStation> stations;

    /** List of soundings */
    protected List<SoundingOb> soundings;

    /** List of times */
    protected List<DateTime> times;



    /**
     * Construct an empty AddeSoundingAdapter
     *
     * @param name   name for this adapter
     */
    public SoundingAdapterImpl(String name) {
        super(name);
    }



    /**
     * Initialize the adapter.  Set the initialization flag to true.
     * Subclasses should call super.init() after doing what they need
     * to do.
     *
     * @throws Exception       problem initializing
     */
    protected void init() throws Exception {
        haveInitialized = true;
    }

    /**
     * Check to see if we have been initialized.
     */
    protected void checkInit() {
        //Convert the AddeException to a throwable
        try {
            init();
        } catch (Exception exc) {
            throw new IllegalStateException("Error initializing: " + exc);
        }
    }

    /**
     * Get a property value from the Defaults
     *
     * @param prefix    prefix for property
     * @param name      name of property
     * @param dflt      default value
     * @return  value for prefix.name  or dflt
     */
    protected String getDflt(String prefix, String name, String dflt) {
        if (defaults == null) {
            defaults = Defaults.initialize(
                SoundingAdapter.class.getResource("Soundings.defaults"));
        }
        String key     = prefix + name;
        String sysProp = System.getProperty(key, dflt);
        if ( !sysProp.equals(dflt)) {
            return sysProp;
        }
        return defaults.getDefault(prefix + name, dflt);
    }


    /**
     * If we are in debug mode then print the string
     *
     * @param s  string to print
     */
    protected void dbPrint(String s) {
        if (debug) {
            System.err.println(s);
        }
    }

    /**
     * Retrieves a list of the stations in the dataset.
     *
     * @return   list of sounding stations or empty list if none found
     */
    public List<SoundingStation> getStations() {
        checkInit();
        List<SoundingStation> stnList = new ArrayList();
        if (stations != null) {
            stnList = new ArrayList<SoundingStation>(stations);
        }

        dbPrint(stnList.size() + " stations");
        return stnList;
    }

    /**
     * Retrieves a list of the stations in the dataset for a given time.
     *
     * @param    time       time of observation
     *
     * @return   list of sounding stations or null if none found
     */
    public List<SoundingStation> getStations(DateTime time) {
        checkInit();
        List<SoundingStation> stnList = new ArrayList();
        if (stations != null) {
            for (SoundingOb snd : soundings) {
                DateTime   e   = snd.getTimestamp();
                if (time.equals(e)) {
                    stnList.add(snd.getStation());
                }
            }
        }

        dbPrint(stnList.size() + " stations at " + time);
        return stnList;
    }

    /**
     * Initialize a sounding ob
     *
     * @param so  ob to initialize
     * @return  initialized observation
     */
    public abstract SoundingOb initSoundingOb(SoundingOb so);


    /**
     * Retrieve the first sounding observation found for the given station.
     *
     * @param    station    station to look for
     *
     * @return  first sounding observation for the given station or null
     *          if no sounding is available for this station
     */
    public SoundingOb getSoundingOb(SoundingStation station) {
        checkInit();
        if (soundings != null) {
            for (int i = 0; i < soundings.size(); i++) {
                SoundingOb      snd = (SoundingOb) soundings.get(i);
                SoundingStation sta = snd.getStation();
                if (sta.equals(station)) {
                    return initSoundingOb(snd);
                }
            }
        }
        return null;
    }



    /**
     * Retrieve all the sounding observations in the dataset
     *
     * @return  all the sounding observations in the dataset or null
     */
    public SoundingOb[] getSoundingObs() {
        checkInit();
        if ((soundings != null) && !soundings.isEmpty()) {
            SoundingOb[] returns = new SoundingOb[soundings.size()];
            for (int i = 0; i < soundings.size(); i++) {
                returns[i] = initSoundingOb((SoundingOb) soundings.get(i));
            }
            return returns;
        }
        return (SoundingOb[]) null;
    }


    /**
     *  Retrieve an array of the sounding times available in the dataset.
     *
     *  @return list of timestamps
     */
    public DateTime[] getSoundingTimes() {
        checkInit();
        return (times == null)
               ? (DateTime[]) null
               : (DateTime[]) times.toArray(new DateTime[times.size()]);
    }


    /**
     * Retrieves a list of the times in the dataset for a given station.
     *
     * @param    station   station of observation
     *
     * @return   list of times or empty list if none found
     */
    public List<DateTime> getSoundingTimes(SoundingStation station) {
        checkInit();
        List timesList = new ArrayList();
        if (soundings != null) {
            for (SoundingOb snd : soundings) {
                SoundingStation sta = snd.getStation();
                if (sta.equals(station)) {
                    timesList.add(snd.getTimestamp());
                }
            }
        }

        dbPrint(timesList.size() + " times for " + station);
        return timesList;
    }

}


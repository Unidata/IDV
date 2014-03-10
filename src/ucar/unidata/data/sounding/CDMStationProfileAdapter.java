/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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


import ucar.ma2.StructureData;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;

import ucar.unidata.util.LogUtil;

import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.CommonUnit;
import visad.DateTime;
import visad.VisADException;


import java.beans.PropertyVetoException;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;


/**
 * The Class CDMStationProfileAdapter.
 */
public class CDMStationProfileAdapter extends SoundingAdapterImpl implements SoundingAdapter {

    /** The sounding levels. */
    private List<List<SoundingLevelData>> soundingLevels;

    /** The filename. */
    private String filename;

    /**
     * Instantiates a new CDM station profile adapter.
     */
    public CDMStationProfileAdapter() {
        super("CDMStationProfileAdapter");
    }

    /**
     * Instantiates a new CDM station profile adapter.
     *
     * @param filename the filename
     */
    public CDMStationProfileAdapter(String filename) {
        super(filename);
        this.filename = filename;
    }

    /**
     * Instantiates a new CDM station profile adapter.
     *
     * @param file the file
     */
    public CDMStationProfileAdapter(File file) {
        this(file.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() throws Exception {
        // need to initialize times, stations, soundings, soundingLevels
        if (haveInitialized) {
            return;
        }
        super.init();

        times          = new ArrayList<>();
        stations       = new ArrayList<>();
        soundings      = new ArrayList<>();
        soundingLevels = new ArrayList<>();

        //TODO: Not sure what to do with this formatter quite yet.
        FeatureDatasetPoint fdp =
            (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.STATION_PROFILE, filename, null, new Formatter());
        StationProfileFeatureCollection pfc =
            (StationProfileFeatureCollection) fdp
                .getPointFeatureCollectionList().get(0);

        while (pfc.hasNext()) {  //Iterating through all profile stations, empty or not.
            StationProfileFeature profile = pfc.next();  // Have a profile location, might be empty
            while (profile.hasNext()) {  // Now iterating through observations at that profile
                ProfileFeature          pf  = profile.next();  //observations
                List<SoundingLevelData> sld = new ArrayList<>();
                while (pf.hasNext()) {                         //Iterating through each profiler level.
                    PointFeature      p    = pf.next();
                    StructureData     data = p.getData();
                    SoundingLevelData sl   = new SoundingLevelData();
                    sl.pressure = data.getScalarInt(data.findMember("PRES"));
                    sl.temperature =
                        data.getScalarInt(data.findMember("TEMP"));
                    sl.dewpoint  = data.getScalarInt(data.findMember("DWPT"));
                    sl.speed     = data.getScalarInt(data.findMember("SPED"));
                    sl.direction = data.getScalarInt(data.findMember("DRCT"));
                    sl.height    = data.getScalarInt(data.findMember("HGHT"));
                    sld.add(sl);
                }
                SoundingStation soundingStation =
                    new SoundingStation(profile.getName(),
                                        profile.getLatLon().getLatitude(),
                                        profile.getLatLon().getLongitude(),
                                        0d);
                stations.add(soundingStation);
                SoundingOb so = new SoundingOb(soundingStation,
                                    new DateTime(pf.getTime()));
                times.add(new DateTime(pf.getTime()));

                soundings.add(so);
                soundingLevels.add(sld);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update() {
        try {
            haveInitialized = false;
            init();
        } catch (Exception exc) {
            LogUtil.logException("Doing update", exc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSource() {
        return filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSource(String s) {
        filename = s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SoundingOb initSoundingOb(SoundingOb sound) {
        checkInit();
        if ( !sound.hasData()) {
            int idx = soundings.indexOf(sound);
            if (idx < 0) {
                throw new IllegalArgumentException(
                    "SoundingAdapter does not contain sounding:" + sound);
            }
            setRAOBData(sound, soundingLevels.get(idx));
        }
        return sound;
    }

    /**
     * Sets the RAOB data.
     *
     * @param sound the sound
     * @param levels the levels
     */
    private void setRAOBData(SoundingOb sound,
                             List<? extends SoundingLevelData> levels) {

        float[] pressures = new float[levels.size()];
        float[] temps     = new float[levels.size()];
        float[] dewpts    = new float[levels.size()];
        float[] speeds    = new float[levels.size()];
        float[] dirs      = new float[levels.size()];
        float[] heights   = new float[levels.size()];

        for (int i = 0; i < levels.size(); i++) {
            pressures[i] = levels.get(i).pressure;
            temps[i]     = levels.get(i).temperature;
            dewpts[i]    = levels.get(i).dewpoint;
            speeds[i]    = levels.get(i).speed;
            dirs[i]      = levels.get(i).direction;
            heights[i]   = levels.get(i).height;
        }

        RAOB r = sound.getRAOB();
        try {
            r.setMandatoryPressureProfile(
                CommonUnits.MILLIBAR, pressures, CommonUnits.CELSIUS, temps,
                CommonUnits.CELSIUS, dewpts, CommonUnit.meterPerSecond,
                speeds, CommonUnit.degree, dirs,
                GeopotentialAltitude.getGeopotentialUnit(CommonUnit.meter),
                heights);
        } catch (VisADException | RemoteException | PropertyVetoException e) {
            System.err.println("Error:");
            e.printStackTrace();
        }
    }
}

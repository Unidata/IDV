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

package ucar.unidata.data.sounding;


import ucar.ma2.Array;
import ucar.ma2.ArraySequence;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers.Member;

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
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * The Class CDMStationProfileAdapter.
 */
public class CDMStationProfileAdapter extends SoundingAdapterImpl implements SoundingAdapter {

    /** MXWC. */
    private static final String MXWC = "MXWC";

    /** TTCC. */
    private static final String TTCC = "TTCC";

    /** PPBB. */
    private static final String PPBB = "PPBB";

    /** TTBB. */
    private static final String TTBB = "TTBB";

    /** Height. */
    private static final String HGHT = "HGHT";

    /** Direction. */
    private static final String DRCT = "DRCT";

    /** Speed. */
    private static final String SPED = "SPED";

    /** Dew Point. */
    private static final String DWPT = "DWPT";

    /** Temperature. */
    private static final String TEMP = "TEMP";

    /** Pressure. */
    private static final String PRES = "PRES";

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


        Set<DateTime> timeSet = new LinkedHashSet<>();

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
                ProfileFeature          pf    = profile.next();  //observations
                List<SoundingLevelData> sld   = new ArrayList<>();
                boolean                 first = true;
                while (pf.hasNext()) {  //Iterating through each profiler level.
                    PointFeature  p    = pf.next();
                    StructureData data = p.getData();

                    sld.add(mandatoryLevels(data));
                    //Significant levels
                    //Data structure replicated at every level for some reason so just grabbing first.
                    if (first) {
                        sld.addAll(ttbb(data));
                        sld.addAll(ppbb(data));
                        sld.addAll(ttcc(data));
                        sld.addAll(mxwc(data));
                        first = false;
                    }
                }
                SoundingStation soundingStation =
                    new SoundingStation(profile.getName(),
                                        profile.getLatLon().getLatitude(),
                                        profile.getLatLon().getLongitude(),
                                        0d);
                stations.add(soundingStation);
                SoundingOb so = new SoundingOb(soundingStation,
                                    new DateTime(pf.getTime()));
                timeSet.add(new DateTime(pf.getTime()));

                soundings.add(so);
                soundingLevels.add(sld);
            }
        }
        times.addAll(timeSet);
    }

    /**
     * Mandatory levels.
     *
     * @param data the structured data
     * @return the sounding level data
     */
    private SoundingLevelData mandatoryLevels(final StructureData data) {
        SoundingLevelData sl = new SoundingLevelData();
        sl.pressure    = (data.findMember(PRES) == null)
                         ? null
                         : data.getScalarFloat(data.findMember(PRES));
        sl.temperature = (data.findMember(TEMP) == null)
                         ? null
                         : data.getScalarFloat(data.findMember(TEMP));
        sl.dewpoint    = (data.findMember(DWPT) == null)
                         ? null
                         : data.getScalarFloat(data.findMember(DWPT));
        sl.speed       = (data.findMember(SPED) == null)
                         ? null
                         : data.getScalarFloat(data.findMember(SPED));
        sl.direction   = (data.findMember(DRCT) == null)
                         ? null
                         : data.getScalarFloat(data.findMember(DRCT));
        sl.height      = (data.findMember(HGHT) == null)
                         ? null
                         : data.getScalarFloat(data.findMember(HGHT));
        return sl;
    }

    /**
     * TTBB.
     *
     * @param data the structured data
     * @return the TTBB collection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Collection<? extends SoundingLevelData> ttbb(
            final StructureData data)
            throws IOException {
        List<SoundingLevelData> sld = new LinkedList<>();
        Member                  d   = data.findMember(TTBB);
        if (d != null) {
            ArraySequence ttbb = data.getArraySequence(d);
            Array         pres = (ttbb.findMember(PRES) == null)
                                 ? null
                                 : ttbb.extractMemberArray(
                                     ttbb.findMember(PRES));
            Array         temp = (ttbb.findMember(TEMP) == null)
                                 ? null
                                 : ttbb.extractMemberArray(
                                     ttbb.findMember(TEMP));
            Array         dwpt = (ttbb.findMember(DWPT) == null)
                                 ? null
                                 : ttbb.extractMemberArray(
                                     ttbb.findMember(DWPT));
            if (pres != null) {
                for (int i = 0; i < pres.getSize(); i++) {
                    SoundingLevelData sl = new SoundingLevelData();
                    sl.pressure    = pres.getFloat(i);
                    sl.temperature = (temp == null)
                                     ? Float.NaN
                                     : temp.getFloat(i);
                    sl.dewpoint    = (dwpt == null)
                                     ? Float.NaN
                                     : dwpt.getFloat(i);
                    sl.speed       = Float.NaN;
                    sl.direction   = Float.NaN;
                    sl.height      = Float.NaN;
                    sld.add(sl);
                }
            }
        }
        return sld;
    }

    /**
     * PPBB.
     *
     * @param data the structured data
     * @return the PPBB collection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Collection<? extends SoundingLevelData> ppbb(StructureData data)
            throws IOException {
        List<SoundingLevelData> sld = new LinkedList<>();
        Member                  d   = data.findMember(PPBB);
        if (d != null) {
            ArraySequence ppbb = data.getArraySequence(d);
            Array         sped = (ppbb.findMember(SPED) == null)
                                 ? null
                                 : ppbb.extractMemberArray(
                                     ppbb.findMember(SPED));
            Array         drct = (ppbb.findMember(DRCT) == null)
                                 ? null
                                 : ppbb.extractMemberArray(
                                     ppbb.findMember(DRCT));
            Array         hght = (ppbb.findMember(HGHT) == null)
                                 ? null
                                 : ppbb.extractMemberArray(
                                     ppbb.findMember(HGHT));
            if (hght != null) {
                for (int i = 0; i < hght.getSize(); i++) {
                    SoundingLevelData sl = new SoundingLevelData();
                    sl.pressure    = hght.getFloat(i);
                    sl.temperature = Float.NaN;
                    sl.dewpoint    = Float.NaN;
                    sl.speed       = (sped == null)
                                     ? Float.NaN
                                     : sped.getFloat(i);
                    sl.direction   = (drct == null)
                                     ? Float.NaN
                                     : drct.getFloat(i);
                    sl.height      = Float.NaN;
                    sld.add(sl);
                }
            }
        }
        return sld;
    }

    /**
     * TTCC.
     *
     * @param data the structured data
     * @return the TTCC collection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Collection<? extends SoundingLevelData> ttcc(StructureData data)
            throws IOException {
        List<SoundingLevelData> sld = new LinkedList<>();
        Member                  d   = data.findMember(TTCC);
        if (d != null) {
            ArraySequence ttcc = data.getArraySequence(d);
            Array         pres = (ttcc.findMember(PRES) == null)
                                 ? null
                                 : ttcc.extractMemberArray(
                                     ttcc.findMember(PRES));
            Array         temp = (ttcc.findMember(TEMP) == null)
                                 ? null
                                 : ttcc.extractMemberArray(
                                     ttcc.findMember(TEMP));
            Array         dwpt = (ttcc.findMember(DWPT) == null)
                                 ? null
                                 : ttcc.extractMemberArray(
                                     ttcc.findMember(DWPT));
            Array         sped = (ttcc.findMember(SPED) == null)
                                 ? null
                                 : ttcc.extractMemberArray(
                                     ttcc.findMember(SPED));
            Array         drct = (ttcc.findMember(DRCT) == null)
                                 ? null
                                 : ttcc.extractMemberArray(
                                     ttcc.findMember(DRCT));
            Array         hght = (ttcc.findMember(HGHT) == null)
                                 ? null
                                 : ttcc.extractMemberArray(
                                     ttcc.findMember(HGHT));
            if (hght != null) {
                for (int i = 0; i < hght.getSize(); i++) {
                    SoundingLevelData sl = new SoundingLevelData();
                    sl.pressure    = (pres == null)
                                     ? Float.NaN
                                     : pres.getFloat(i);
                    sl.temperature = (temp == null)
                                     ? Float.NaN
                                     : temp.getFloat(i);
                    sl.dewpoint    = (dwpt == null)
                                     ? Float.NaN
                                     : dwpt.getFloat(i);
                    sl.speed       = (sped == null)
                                     ? Float.NaN
                                     : sped.getFloat(i);
                    sl.direction   = (drct == null)
                                     ? Float.NaN
                                     : drct.getFloat(i);
                    sl.height      = hght.getFloat(i);
                    sld.add(sl);
                }
            }
        }
        return sld;
    }

    /**
     * MXWC.
     *
     * @param data the structured data
     * @return the MXWC collection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Collection<? extends SoundingLevelData> mxwc(StructureData data)
            throws IOException {
        List<SoundingLevelData> sld = new LinkedList<>();
        Member                  d   = data.findMember(MXWC);
        if (d != null) {
            ArraySequence mxwc = data.getArraySequence(d);
            Array         sped = (mxwc.findMember(SPED) == null)
                                 ? null
                                 : mxwc.extractMemberArray(
                                     mxwc.findMember(SPED));
            Array         drct = (mxwc.findMember(DRCT) == null)
                                 ? null
                                 : mxwc.extractMemberArray(
                                     mxwc.findMember(DRCT));
            Array         pres = (mxwc.findMember(PRES) == null)
                                 ? null
                                 : mxwc.extractMemberArray(
                                     mxwc.findMember(PRES));
            if (pres != null) {
                for (int i = 0; i < pres.getSize(); i++) {
                    SoundingLevelData sl = new SoundingLevelData();
                    sl.pressure    = pres.getFloat(i);
                    sl.temperature = Float.NaN;
                    sl.dewpoint    = Float.NaN;
                    sl.speed       = (sped == null)
                                     ? Float.NaN
                                     : sped.getFloat(i);
                    sl.direction   = (drct == null)
                                     ? Float.NaN
                                     : drct.getFloat(i);
                    sl.height      = Float.NaN;
                    sld.add(sl);
                }

            }

        }
        return sld;
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

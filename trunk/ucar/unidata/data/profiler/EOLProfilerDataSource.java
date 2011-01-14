/*
 * $Id: EOLProfilerDataSource.java,v 1.4 2006/12/01 20:42:36 jeffmc Exp $
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

package ucar.unidata.data.profiler;


import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.metdata.NamedStation;
import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import ucar.visad.quantities.*;

import visad.*;

import visad.data.netcdf.Plain;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.awt.*;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;


import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;

import javax.swing.*;



/**
 * A data source for EOL profiler data
 *
 * @author Unidata Development Team
 * @version $Revision: 1.4 $
 */
public class EOLProfilerDataSource extends DataSourceImpl {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            EOLProfilerDataSource.class.getName());

    /** source file */
    private String fileNameOrUrl;

    /** location */
    EarthLocation location = null;

    /** data */
    FieldImpl data = null;

    /** Input for lat/lon center point */
    private LatLonWidget locWidget;

    /** flag for user set location */
    private boolean locationSetByUser = false;

    /**
     * No argument XML persistence constructor
     *
     * @throws VisADException    problem in VisAD
     */
    public EOLProfilerDataSource() throws VisADException {}


    /**
     * Create a new EOLProfilerDataSource
     *
     * @param descriptor             description of source
     * @param source                 source of the data
     * @param properties             extra properties
     *
     * @throws VisADException        problem accessing data
     */
    public EOLProfilerDataSource(DataSourceDescriptor descriptor,
                                 String source, Hashtable properties)
            throws VisADException {
        super(descriptor,
              "EOL/NCAR Profiler (" + IOUtil.getFileTail(source) + ")",
              source, properties);
        setFileNameOrUrl(source);
        initProfiler();
    }

    /**
     * Extends method in DataSourceImpl to call local initProfiler ()
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        initProfiler();
    }

    /**
     * If we are polling some directory this method gets called when
     * there is a new file. We set the file name, clear our state,
     * reload the metadata and tell listeners of the change.
     *
     * @param  f  new File to use.
     */
    public void newFileFromPolling(File f) {
        String newFilename = f.getPath();
        System.out.println("new file: " + newFilename);
        if ( !newFilename.equals(getFileNameOrUrl())) {
            locationSetByUser = false;
        }
        try {
            initProfiler();
        } catch (Exception exc) {
            LogUtil.printException(log_, "Creating new file", exc);
            return;
        }
        setFileNameOrUrl(f.getPath());
        setName(getFileNameOrUrl());
        flushCache();
        notifyDataChange();
    }

    /**
     * Get the location where we poll.
     *
     * @return File to poll on.
     */
    protected List getLocationsForPolling() {
        return Misc.newList(getFileNameOrUrl());
    }

    /**
     * Read in the data.
     *
     */
    private void initProfiler() {

        try {
            Trace.call1("initProfiler");
            Plain p = new Plain();
            Tuple t = (Tuple) p.open(getFileNameOrUrl());
            // Find lat/lon/alt
            Real      lat     = null;
            Real      lon     = null;
            Real      alt     = null;
            FieldImpl rawData = null;
            for (int i = 0; i < t.getDimension(); i++) {
                Data d = t.getComponent(i);
                if (d instanceof Real) {
                    Real r = (Real) t.getComponent(i);
                    if (r.getType().equals(RealType.Latitude)) {
                        lat = r;
                    } else if (r.getType().equals(RealType.Longitude)) {
                        lon = r;
                    } else if (r.getType().equals(RealType.Altitude)) {
                        double value = r.getValue();
                        // handle bad unit for station elevation
                        if (r.getUnit().equals(CommonUnit.meter.scale(1000))
                                && (value > 8)) {
                            value = value / 1000;
                        }
                        alt = r.cloneButValue(value);
                    }
                } else if (d instanceof FieldImpl) {
                    FieldImpl f = (FieldImpl) d;
                    if (GridUtil.isTimeSequence(f)) {
                        rawData = f;
                    }
                }
            }
            if ((lat == null) || (lon == null) || (alt == null)) {
                throw new VisADException("couldn't find location");
            }
            if (rawData == null) {
                throw new VisADException("couldn't find data");
            }
            if ( !locationSetByUser) {
                location = new EarthLocationTuple(lat, lon, alt);
            }
            // System.out.println("location at " + location);
            // munge data
            int timeIndex = 0;
            // See if this has a time_offset variable;
            String type = rawData.getType().toString();
            //System.out.println("data = " + type);
            int          offsetIndex = type.indexOf("time_offset");
            Gridded1DSet timeSet     = null;
            if (offsetIndex > 0) {
                //System.out.println("have offset time");
                FlatField offsetFI = (FlatField) rawData.extract(0);
                //System.out.println("offset data = " + offsetFI.getType());
                timeSet = new Gridded1DDoubleSet(RealType.Time,
                        offsetFI.getValues(false),
                        offsetFI.getDomainSet().getLength(),
                        (CoordinateSystem) null,
                        offsetFI.getDefaultRangeUnits(),
                        (ErrorEstimate[]) null);
                rawData = (FieldImpl) rawData.extract(1);
                //System.out.println("rawData = " + rawData.getType());
            } else {
                timeSet = (Gridded1DSet) rawData.getDomainSet();
            }
            TupleType    rangeType = null;

            Unit[]       units     = null;
            int          altIndex  = -1;
            int          spdIndex  = -1;
            int          dirIndex  = -1;
            int          wIndex    = -1;
            FunctionType newType   = null;
            boolean      has3Comps = false;
            Unit[]       origUnits = null;
            Unit         altUnit   = CommonUnit.meter;
            for (int i = 0; i < timeSet.getLength(); i++) {
                FlatField     f  = (FlatField) rawData.getSample(i, false);
                RealTupleType tt =
                    ((FunctionType) f.getType()).getFlatRange();
                // should be x->(height, wspd, wdir, ...)
                //System.out.println("flatRange = " + f.getType());
                if (i == 0) {
                    altIndex = tt.getIndex("height");
                    spdIndex = tt.getIndex("wspd");
                    dirIndex = tt.getIndex("wdir");
                    wIndex   = tt.getIndex("wvert");
                    if ((altIndex == -1) || (spdIndex == -1)
                            || (dirIndex == -1)) {
                        throw new VisADException("can't find components");
                    }
                    //has3Comps = (wIndex != -1);
                    if (has3Comps) {
                        rangeType = new TupleType(new MathType[] {
                            PolarHorizontalWind.getRealTupleType(),
                            VerticalWindComponent.getRealType() });
                    } else {
                        rangeType = PolarHorizontalWind.getRealTupleType();
                    }
                    origUnits = f.getDefaultRangeUnits();
                    if ((origUnits[altIndex] != null)
                            && !origUnits[altIndex].isDimensionless()) {
                        altUnit = origUnits[altIndex];
                    }
                    Unit spdUnit = ((origUnits[spdIndex] == null)
                                    || origUnits[spdIndex].isDimensionless())
                                   ? CommonUnit.meterPerSecond
                                   : origUnits[spdIndex];
                    Unit dirUnit = ((origUnits[dirIndex] == null)
                                    || origUnits[spdIndex].isDimensionless())
                                   ? CommonUnit.degree
                                   : origUnits[dirIndex];
                    Unit wUnit = ((origUnits[wIndex] == null)
                                  || origUnits[spdIndex].isDimensionless())
                                 ? CommonUnit.meterPerSecond
                                 : origUnits[wIndex];
                    units   = (has3Comps)
                              ? new Unit[] { spdUnit, dirUnit, wUnit }
                              : new Unit[] { spdUnit, dirUnit };
                    newType = new FunctionType(RealType.Altitude, rangeType);
                    FunctionType fiType =
                        new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            newType);
                    data = new FieldImpl(fiType, timeSet);
                }
                float[][] vals = removeMissingHeights(f.getFloats(false),
                                     altIndex);
                if (vals != null) {
                    float   stationAlt = (float) alt.getValue(altUnit);
                    float[] alts       = vals[altIndex];
                    for (int l = 0; l < alts.length; l++) {
                        alts[l] += stationAlt;
                    }
                    Gridded1DSet altSet = new Gridded1DSet(RealType.Altitude,
                                              new float[][] {
                        vals[altIndex]
                    }, vals[altIndex].length, null, new Unit[] { altUnit },
                       null);


                    FlatField ff = new FlatField(newType, altSet,
                                       (CoordinateSystem) null, (Set[]) null,
                                       units);
                    float[][] newSamples = (has3Comps)
                                           ? new float[][] {
                        vals[spdIndex], vals[dirIndex], vals[wIndex]
                    }
                                           : new float[][] {
                        vals[spdIndex], vals[dirIndex]
                    };

                    ff.setSamples(newSamples, false);
                    data.setSample(i, ff, false);
                }
            }
            //System.out.println("data = " + data.getType());
        } catch (Exception exc) {
            setInError(true);
            throw new WrapperException(exc);
        }
        Trace.call2("initProfiler");
    }


    /**
     * Remove missing altitudes
     *
     * @param samples   the samples of all data
     * @param altIndex  the index of altitudes
     *
     * @return an array with missing data removed
     */
    private float[][] removeMissingHeights(float[][] samples, int altIndex) {
        float[][] newSamples = null;
        int[]     indices    = new int[samples[0].length];
        int       counter    = 0;
        for (int i = 0; i < samples[altIndex].length; i++) {
            if ( !Float.isNaN(samples[altIndex][i])) {
                indices[counter++] = i;
            }
        }
        if (counter == 0) {
            return newSamples;
        }
        newSamples = new float[samples.length][counter];
        for (int i = 0; i < counter; i++) {
            for (int j = 0; j < samples.length; j++) {
                newSamples[j][i] = samples[j][indices[i]];
            }
        }
        return newSamples;
    }

    /**
     * Called by DataSourceImpl to make the actual DataChoice(s) and add them
     * to a list of Datachoices; the DataSourceImpl
     * then checks to see if any derived
     * datachoices are possible to derive from these, and shows them if so.
     * Used data supplied in the constructor.
     */
    public void doMakeDataChoices() {

        // to make datachoice
        // 2nd arg is url or request String for data from remote ADDE server.
        // 3rd arg is key to param defaults table 
        //                  in resources/paramdefaults.xml
        // 4th arg is "parameter" label in DataSelector gui "Fields" panel.
        // DataCategory.CATEGORY_PROFILER_ONESTA 
        //  comes from unidata/data/DataCategory.java; see alos controls.xml -
        // connections which "controls" (displays) each parameter can use.

        DataChoice choice = null;
        List singleDC = DataCategory.parseCategories(
                            DataCategory.CATEGORY_PROFILER_ONESTA, false);
        List compositeDC =
            DataCategory.parseCategories(DataCategory.CATEGORY_PROFILER_3D,
                                         false);
        compositeDC.addAll(singleDC);

        CompositeDataChoice composite = new CompositeDataChoice(this, "",
                                            "Winds", "Profiler winds",
                                            compositeDC);
        addDataChoice(composite);
        composite.addDataChoice(new DirectDataChoice(this, location, "winds",
                location.toString(), singleDC));

    }


    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        boolean singleStation = !(dataChoice instanceof CompositeDataChoice);

        //Recast the field into a different FieldImpl we can use.
        if (singleStation) {
            return data;
        } else {
            return recastProfilerMultiStationData();
        }

    }

    /**
     * Change the data into a field that includes lat/lon info
     *
     * @return field with lat/lon info
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private FieldImpl recastProfilerMultiStationData()
            throws VisADException, RemoteException {
        FieldImpl    retField  = null;
        Gridded1DSet timeSet   = (Gridded1DSet) data.getDomainSet();
        FunctionType rangeType = null;
        for (int i = 0; i < timeSet.getLength(); i++) {
            FlatField f = (FlatField) data.getSample(i);
            if (i == 0) {
                FunctionType ft = (FunctionType) f.getType();
                rangeType =
                    new FunctionType(RealTupleType.LatitudeLongitudeAltitude,
                                     ft.getRange());
                FunctionType newType =
                    new FunctionType(
                        ((SetType) timeSet.getType()).getDomain(), rangeType);
                retField = new FieldImpl(newType, timeSet);
            }
            Gridded1DSet alts   = (Gridded1DSet) f.getDomainSet();
            Unit         degree = CommonUnit.degree;
            float[][]    lla    = new float[3][alts.getLength()];
            Arrays.fill(lla[0],
                        (float) location.getLatitude().getValue(degree));
            Arrays.fill(lla[1],
                        (float) location.getLongitude().getValue(degree));
            lla[2] = alts.getSamples(false)[0];
            Gridded3DSet newDomain =
                new Gridded3DSet(RealTupleType.LatitudeLongitudeAltitude,
                                 lla, alts.getLength(),
                                 (CoordinateSystem) null, new Unit[] { degree,
                    degree, alts.getSetUnits()[0] }, (ErrorEstimate[]) null,
                        false);
            FlatField newField = new FlatField(rangeType, newDomain);
            newField.setSamples(f.getFloats(false), false);
            retField.setSample(i, newField, false);
        }
        return retField;
    }

    /**
     * Check to see if this EOLProfilerDataSource is equal to the object
     * in question.
     *
     * @param o  object in question
     *
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof EOLProfilerDataSource)) {
            return false;
        }
        EOLProfilerDataSource that = (EOLProfilerDataSource) o;
        if ( !super.equals(o)) {
            return false;
        }
        return (Misc.equals(data, that.data));
    }

    /**
     * Return the hashcode for this object
     *
     * @return  hashCode
     */
    public int hashCode() {
        return Misc.hashcode(location) ^ super.hashCode();
    }

    /**
     * Test by running "java ucar.unidata.data.profiler.EOLProfilerDataSource <filename>"
     *
     * @param args  filename
     *
     * @throws Exception  problem running this
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Must supply a file name");
            System.exit(1);
        }
        try {
            EOLProfilerDataSource ncar = new EOLProfilerDataSource(null,
                                             args[0], null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the FileNameOrUrl property.
     *
     * @param value The new value for FileNameOrUrl
     */
    public void setFileNameOrUrl(String value) {
        fileNameOrUrl = value;
    }

    /**
     * Get the FileNameOrUrl property.
     *
     * @return The FileNameOrUrl
     */
    public String getFileNameOrUrl() {
        return fileNameOrUrl;
    }

    /**
     * Add the gui components into the list for the properties dialog
     *
     * @param comps List of components
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        locWidget = new LatLonWidget("Lat: ", " Lon: ", " Alt:", null);
        JPanel locPanel = GuiUtils.hbox(new Component[] {
                              GuiUtils.leftCenter(GuiUtils.rLabel("Lat: "),
                                  locWidget.getLatField()),
                              GuiUtils.leftCenter(GuiUtils.rLabel(" Lon: "),
                                  locWidget.getLonField()),
                              GuiUtils.leftCenter(
                                  GuiUtils.rLabel(" Alt: "),
                                  GuiUtils.centerRight(
                                      locWidget.getAltField(),
                                      GuiUtils.cLabel("m"))) });
        if (location != null) {
            locWidget.setLat(location.getLatitude().getValue());
            locWidget.setLon(location.getLongitude().getValue());
            try {
                locWidget.setAlt(
                    location.getAltitude().getValue(CommonUnit.meter));
            } catch (VisADException ve) {
                locWidget.setAlt(location.getAltitude().getValue());
            }
        }
        comps.add(GuiUtils.filler());
        comps.add(getPropertiesHeader("Profiler Location"));
        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(locPanel));
    }

    /**
     * Apply properties components
     *
     * @return false if something failed and we need to keep showing the dialog
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        return setLocationFromWidgets();
    }

    /**
     * Set the location from the widgets
     *
     * @return
     */
    private boolean setLocationFromWidgets() {

        try {
            double             lat = locWidget.getLat();
            double             lon = locWidget.getLon();
            double             alt = locWidget.getAlt();
            EarthLocationTuple elt = new EarthLocationTuple(lat, lon, alt);
            if (elt.equals(location)) {
                return true;
            } else {
                locationSetByUser = true;
                location          = elt;
            }
        } catch (Exception e) {
            return false;
        }
        // update the data choices
        List l = getDataChoices();
        for (int i = 0; i < l.size(); i++) {
            DataChoice          dc       = (DataChoice) l.get(i);
            CompositeDataChoice cdc      = (CompositeDataChoice) dc;
            List                children = cdc.getDataChoices();
            for (int j = 0; j < children.size(); j++) {
                DataChoice child = (DataChoice) children.get(j);
                child.setId(location);
                child.setDescription(location.toString());
            }
        }
        reloadData();
        return true;
    }

}


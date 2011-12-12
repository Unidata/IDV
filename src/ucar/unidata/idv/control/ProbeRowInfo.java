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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataAlias;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.geoloc.Bearing;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.idv.control.chart.LineState;
import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.MidiManager;
import ucar.unidata.util.MidiProperties;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.ThreeDSize;

import ucar.unidata.util.TwoFacedObject;


import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorDisplayable;


import visad.*;

import visad.data.units.Parser;

import visad.georef.EarthLocation;


import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;



import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;




/**
 * Class ProbeRowInfo holds the state for each row in the probe control.
 *
 * @author IDV Development Team
 */
public class ProbeRowInfo {

    /** Extra label to show in table */
    private String extra = "";

    /** The raw sampled value */
    private Data timeSample;

    /** the sample from the last point sample */
    private FieldImpl pointSample;

    /** the grid to use for sampling */
    private FieldImpl workingGrid;

    /** The last point that we sampled  on */
    private EarthLocation lastPoint;


    /** the time set from the last setValue */
    private Set timeSet;

    /** The unit_ */
    private Unit unit;

    /** The level */
    private Real level;

    /** The altitude of the last sampled data */
    private Real altitude;

    /** Sampling mode */
    private int samplingMode;

    /** For playing sounds */
    private MidiProperties midiProperties;


    /** What is displayed. May be the formatted text or "missing" */
    private Object displayValue;

    /** The data instance */
    private DataInstance dataInstance;

    /** For probing on point data */
    private String pointParameter;

    /** station name */
    private String stationName = "";

    /** point index */
    private int pointIndex = -1;

    /** For playing sounds */
    private MidiManager midiManager;


    /** For the chart */
    private LineState lineState = new LineState(null, 1.0f,
                                      LineState.STROKE_SOLID);



    /**
     * ctor
     */
    public ProbeRowInfo() {}

    /**
     * ctor
     *
     * @param control The control we're in. Just use it for getting the sampling mode
     */
    public ProbeRowInfo(ProbeControl control) {
        samplingMode = control.getDefaultSamplingModeValue();
    }

    /**
     * ctor used for creating a ProbeRowInfo from legacy bundles values
     *
     * @param level level
     * @param alt altitude
     * @param mode sampling mode
     * @param unit display unit
     * @param midiProperties  sound configuartion
     */
    public ProbeRowInfo(Real level, Real alt, int mode, Unit unit,
                        MidiProperties midiProperties) {
        this.level          = level;
        this.altitude       = alt;
        this.samplingMode   = mode;
        this.unit           = unit;
        this.midiProperties = midiProperties;
    }

    /**
     * ctor
     *
     * @param control the control
     * @param dataInstance data instance
     */
    public ProbeRowInfo(ProbeControl control, DataInstance dataInstance) {
        this(control);
        this.dataInstance = dataInstance;
    }


    /**
     * Are we probing on grids
     *
     * @return Is this data a grid
     */
    public boolean isGrid() {
        return (dataInstance instanceof GridDataInstance);
    }

    /**
     * Is this for point data
     *
     * @return true if point data
     */
    public boolean isPoint() {
        return !isGrid();
    }

    /**
     * extra stuff for table
     *
     * @param s extra
     */
    protected void setExtra(String s) {
        this.extra = s;
    }

    /**
     * extra stuff for table
     *
     * @return extra
     */
    protected String getExtra() {
        return extra;
    }

    /**
     * Clear any cached samples
     */
    protected void clearCachedSamples() {
        pointSample = null;
        lastPoint   = null;
        workingGrid = null;
        tupleType   = null;
    }



    /**
     * Set the raw value from the Data. If rt is null then set raw value to null.
     * Else if rt is a RealTuple then use the first component else
     * assume it is a Real
     *
     * @param rt The data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setTimeSample(Data rt)
            throws VisADException, RemoteException {
        if (rt == null) {
            timeSample = null;
        } else if (rt.isMissing()) {
            timeSample = null;
        } else {
            timeSample = rt;
        }
    }



    /**
     * Get the time sample
     *
     * @return the time sample
     */
    protected Data getTimeSample() {
        return timeSample;
    }


    /**
     * Sort of a copy ctor
     *
     * @param that that
     */
    protected void initWith(ProbeRowInfo that) {
        this.displayValue   = that.displayValue;
        this.timeSample     = that.timeSample;
        this.unit           = that.unit;
        this.level          = that.level;
        this.altitude       = that.altitude;
        this.samplingMode   = that.samplingMode;
        this.pointParameter = that.pointParameter;
        this.stationName    = that.stationName;
        if (that.midiProperties != null) {
            this.midiProperties = new MidiProperties(that.midiProperties);
        }
        // TODO - how much of lineState should we copy?
        // copy along the lineState name  
        lineState.setChartName(that.lineState.getChartName());
        if (that.lineState.getRange() != null) {
            lineState.setRange(new Range(that.lineState.getRange()));
        }
    }



    /**
     * Show the sound dialog
     *
     * @param control control I'm in
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void showSoundDialog(ProbeControl control)
            throws VisADException, RemoteException {
        if (midiProperties == null) {
            midiProperties = new MidiProperties();
        }
        if (midiProperties.showPropertyDialog(control.getWindow())) {
            MidiManager mm = getMidiManager();
            mm.setInstrument(midiProperties.getInstrumentName());
            try {
                control.updatePosition();
            } catch (Exception exc) {
                control.logException("After changing sound", exc);
            }
        }
    }







    /**
     * to string
     *
     * @return String representation
     */
    public String toString() {
        if (dataInstance == null) {
            return "";
        }
        return "" + dataInstance.getParamName();
    }


    /**
     * Play a sound for the particular row and value
     *
     * @param value value of data
     */
    protected void playSound(double value) {
        Range range = null;
        if ((midiProperties == null) || midiProperties.getMuted()) {
            return;
        }

        if (isGrid()) {
            range = getGridDataInstance().getRange(0);
        } else {
            //For now just use a fixed data range for point data
            range = new Range(0,100);
        }
 
        int note = (int) (midiProperties.getLowNote()
                          + (range.getPercent(value)
                             * (midiProperties.getHighNote()
                                - midiProperties.getLowNote())));
        getMidiManager().play(note, 500);

    }



    /**
     * Set the DisplayValue property.
     *
     * @param value The new value for DisplayValue
     */
    protected void setDisplayValue(Object value) {
        displayValue = value;
    }

    /**
     * Get the DisplayValue property.
     *
     * @return The DisplayValue
     */
    protected Object getDisplayValue() {
        return displayValue;
    }



    /**
     * Get the data instance
     *
     * @return The data instance
     */
    public GridDataInstance getGridDataInstance() {
        if ( !isGrid()) {
            return null;
        }
        return (GridDataInstance) dataInstance;
    }

    /**
     * Get the data instance
     *
     * @return The data instance
     */
    public DataInstance getDataInstance() {
        return dataInstance;
    }


    /**
     * Set the data instance
     *
     * @param di The data instance
     */
    protected void setDataInstance(DataInstance di) {
        dataInstance = di;
    }

    /**
     * Get the midi manager. Create it if needed.
     *
     * @return midi manager
     */
    private MidiManager getMidiManager() {
        if (midiManager == null) {
            midiManager = new MidiManager(midiProperties);
        } else {
            //TODO: set sound                
        }
        return midiManager;
    }



    /** the tuple type */
    private TupleType tupleType;

    /**
     * Get the TupleType for the data
     *
     * @return the type
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    public TupleType getTupleType() throws VisADException, RemoteException {
        if (tupleType != null) {
            return tupleType;
        }
        if (isGrid()) {
            return null;
        }
        FieldImpl pointObs = (FieldImpl) getDataInstance().getData();
        if (pointObs == null) {
            return null;
        }
        int numObs = pointObs.getDomainSet().getLength();
        if (numObs == 0) {
            return null;
        }
        PointOb ob = (PointOb) pointObs.getSample(0);
        tupleType = (TupleType) ((Tuple) ob.getData()).getType();
        return tupleType;
    }



    /**
     * Get the value as a Real. We may be a Real, if so then return.
     * Else we may be a RealTuple. If so return the 0th component.
     *
     * @return My value as a Real
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Real getRealValue() throws VisADException, RemoteException {
        return getRealValue(timeSample);
    }


    /**
     * Get the value at the time sample
     *
     * @param timeSample  the time sample
     *
     * @return  the value
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Real getRealValue(Data timeSample)
            throws VisADException, RemoteException {
        if (timeSample == null) {
            return null;
        }
        //        System.err.println("timeSample:" + timeSample);
        if (timeSample instanceof Real) {
            return (Real) timeSample;
        }
        if (timeSample instanceof FieldImpl) {
            Data data = ((FieldImpl) timeSample).getSample(0);
            if (data instanceof Real) {
                return (Real) data;
            }

            if (data instanceof PointOb) {
                PointOb ob = (PointOb) data;
                Tuple   t  = (Tuple) ob.getData();
                getPointIndex();
                if (pointIndex < 0) {
                    return null;
                }
                return (Real) t.getComponent(pointIndex);
            }
            return null;
        }
        return (Real) ((RealTuple) timeSample).getComponent(0);
    }


    /**
     * Get the point parameter name
     *
     * @return  the name
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public String getPointParameterName()
            throws VisADException, RemoteException {
        if (pointParameter == null) {
            TupleType tt = getTupleType();
            if (tt == null) {
                return null;
            }
            for (int i = 0; i < tt.getDimension(); i++) {
                if ( !(tt.getComponent(i) instanceof RealType)) {
                    continue;
                }
                setPointParameter(
                    Util.cleanTypeName(tt.getComponent(i).toString()));
                break;
            }
        }
        return pointParameter;
    }

    /**
     * Get the point index
     *
     * @return  the index
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public int getPointIndex() throws VisADException, RemoteException {
        if (pointIndex >= 0) {
            return pointIndex;
        }
        getPointParameterName();
        if (pointParameter != null) {
            TupleType tt = getTupleType();
            if (tt == null) {
                return -1;
            }
            for (int i = 0; i < tt.getDimension(); i++) {
                if (Util.cleanTypeName(tt.getComponent(i).toString()).equals(
                        pointParameter)) {
                    pointIndex = i;
                    break;
                }
            }
        }
        return pointIndex;
    }


    /**
     * Set the Unit property.
     *
     * @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        Unit oldUnit = unit;
        unit = value;
        lineState.setRange(Util.convertRange(lineState.getRange(), oldUnit,
                                             value));
    }



    /**
     * Set the working grid for this row
     *
     * @param grid
     */
    protected void setWorkingGrid(FieldImpl grid) {
        this.workingGrid = grid;
    }

    /**
     * Get the sample we last used
     *
     * @return sample
     */
    public FieldImpl getWorkingGrid() {
        return this.workingGrid;
    }

    /**
     * Set the sample we last used
     *
     * @param sample sample
     * @param elt The point we sampled on
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setPointSample(FieldImpl sample, EarthLocation elt)
            throws VisADException, RemoteException {
        this.pointSample = sample;
        this.lastPoint   = elt;
    }


    /**
     * Set the station name
     *
     * @param ob  the point observation
     * @param control The display control this is part of
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setStationName(PointOb ob, DisplayControlImpl control)
            throws VisADException, RemoteException {
        Tuple  t     = (Tuple) ob.getData();
        Data[] comps = t.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof visad.Text) {
                String name =
                    StringUtil.replace(comps[i].getType().toString(),
                                       "(Text)", "").toLowerCase();
                if(name.toLowerCase().equals("station")) {
                    stationName = comps[i].toString();
                    return;
                } else {
                    String canon = DataAlias.aliasToCanonical(name);
                    if ((canon != null)
                        && (canon.equals("IDN") || canon.equals("ID"))) {
                        stationName = comps[i].toString();
                        return;
                    }
                }
            }
        }
        stationName = control.getDisplayConventions().formatLatLonPoint(
            ob.getEarthLocation().getLatLonPoint());
    }


    /**
     * time set
     *
     * @return time set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Set getTimeSet() throws VisADException, RemoteException {
        if (pointSample != null) {
            return pointSample.getDomainSet();
        }
        return null;

    }



    /**
     * Get the last point sample
     *
     * @return Point sample
     */
    public FieldImpl getPointSample() {
        return pointSample;
    }


    /**
     * Get the sample we last used. If The given earth location
     * is not the same as the location of the last point we sampled on
     * then null out the sample and return null.
     *
     *
     * @param elt The point we want to sample on
     * @return sample
     */
    public FieldImpl getPointSample(EarthLocation elt) {
        if ( !Misc.equals(elt, lastPoint)) {
            pointSample = null;
        }
        return this.pointSample;
    }

    /**
     * Get the Unit property.
     *
     * @return The Unit
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Set the Level property.
     *
     * @param value The new value for Level
     */
    public void setLevel(Real value) {
        level = value;
        clearCachedSamples();
    }

    /**
     * Get the Level property.
     *
     * @return The Level
     */
    public Real getLevel() {
        return level;
    }

    /**
     * Set the Altitude property.
     *
     * @param value The new value for Altitude
     */
    public void setAltitude(Real value) {
        altitude = value;
        clearCachedSamples();
    }

    /**
     * Get the Altitude property.
     *
     * @return The Altitude
     */
    public Real getAltitude() {
        return altitude;
    }

    /**
     * Set the SamplingMode property.
     *
     * @param value The new value for SamplingMode
     */
    public void setSamplingMode(int value) {
        samplingMode = value;
        clearCachedSamples();
    }

    /**
     * Get the SamplingMode property.
     *
     * @return The SamplingMode
     */
    public int getSamplingMode() {
        return samplingMode;
    }

    /**
     * Set the MidiProperties property.
     *
     * @param value The new value for MidiProperties
     */
    public void setMidiProperties(MidiProperties value) {
        midiProperties = value;
    }

    /**
     * Get the MidiProperties property.
     *
     * @return The MidiProperties
     */
    public MidiProperties getMidiProperties() {
        return midiProperties;
    }


    /**
     *  Set the LineState property.
     *
     *  @param value The new value for LineState
     */
    public void setLineState(LineState value) {
        lineState = value;
    }

    /**
     *  Get the LineState property.
     *
     *  @return The LineState
     */
    public LineState getLineState() {
        return lineState;
    }

    /**
     * Get the station name
     *
     * @return  the name or null
     */
    public String getStationName() {
        return stationName;
    }

    /**
     *  Set the PointParameter property.
     *
     *  @param value The new value for PointParameter
     */
    public void setPointParameter(String value) {
        unit           = null;
        pointParameter = value;
        pointIndex     = -1;
        this.lastPoint = null;
    }

    /**
     *  Get the PointParameter property.
     *
     *  @return The PointParameter
     */
    public String getPointParameter() {
        return pointParameter;
    }



}

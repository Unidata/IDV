/**
 * $Id: ProbeRowInfo.java,v 1.19 2007/07/24 13:39:22 dmurray Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
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
 *
 * @author IDV Development Team
 * @version $Revision: 1.19 $
 */
public class ProbeRowInfo {

    /** Extra label to show in table */
    private String extra = "";

    /** the sample from the last point sample */
    private FieldImpl pointSample;

    /** the grid to use for sampling */
    private FieldImpl workingGrid;

    /** The last point that we sampled  on */
    private EarthLocationTuple lastPoint;

    /** the sample from the last setValue on time */
    private FieldImpl timeSample;

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

    /** The raw sampled value */
    private Data rawValue;

    /** What is displayed. May be the formatted text or "missing" */
    private Object displayValue;

    /** The data instance */
    private DataInstance dataInstance;

    /** For probing on point data */
    private String pointParameter;

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
        timeSample  = null;
        pointSample = null;
        lastPoint   = null;
        workingGrid = null;
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
    public void setValue(Data rt) throws VisADException, RemoteException {
        if (rt == null) {
            rawValue = null;
        } else if (rt.isMissing()) {
            rawValue = null;
        } else {
            /*
            rawValue = (rt instanceof RealTuple)
                       ? (Real) ((RealTuple) rt).getComponent(0)
                       : (Real) rt;
            */
            rawValue = rt;
        }
    }


    /**
     * Sort of a copy ctor
     *
     * @param that that
     */
    protected void initWith(ProbeRowInfo that) {
        this.displayValue = that.displayValue;
        this.rawValue     = that.rawValue;
        this.unit         = that.unit;
        this.level        = that.level;
        this.altitude     = that.altitude;
        this.samplingMode = that.samplingMode;
        if (that.midiProperties != null) {
            this.midiProperties = new MidiProperties(that.midiProperties);
        }
        // TODO - how much of lineState should we copy?
        // copy along the lineState name  
        lineState.setChartName(that.lineState.getChartName());
        lineState.setRange(new Range(that.lineState.getRange()));
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
        if(dataInstance ==null)
            return "";
        return "" + dataInstance.getParamName();
    }


    /**
     * Play a sound for the particular row and value
     *
     * @param value value of data
     */
    protected void playSound(double value) {
        if ( !isGrid()) {
            return;
        }
        if ((midiProperties == null) || midiProperties.getMuted()) {
            return;
        }
        Range range = getGridDataInstance().getRange(0);
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

    /**
     * Set the RawValue property.
     *
     * @param value The new value for RawValue
     */
    protected void setRawValue(Data value) {
        rawValue = value;
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
        if (rawValue instanceof Real) {
            return (Real) rawValue;
        }
        return (Real) ((RealTuple) rawValue).getComponent(0);
    }


    /**
     * Get the RawValue property.
     *
     * @return The RawValue
     */
    protected Data getRawValue() {
        return rawValue;
    }

    /**
     * Set the Unit property.
     *
     * @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        unit = value;
    }

    /**
     * set the time set. Just a holder.
     *
     * @param s time set_
     */
    protected void setTimeSet(Set s) {
        timeSet = s;
    }

    /**
     * time set
     *
     * @return time set
     */
    protected Set getTimeSet() {
        return timeSet;

    }

    /**
     * Set the sample we last used
     *
     * @param sample sample
     */
    protected void setTimeSample(FieldImpl sample) {
        this.timeSample = sample;
    }

    /**
     * Get the sample we last used
     *
     * @return sample
     */
    public FieldImpl getTimeSample() {
        return this.timeSample;
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
     */
    protected void setPointSample(FieldImpl sample, EarthLocationTuple elt) {
        this.pointSample = sample;
        this.lastPoint   = elt;
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
    public FieldImpl getPointSample(EarthLocationTuple elt) {
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
     *  Set the PointParameter property.
     *
     *  @param value The new value for PointParameter
     */
    public void setPointParameter(String value) {
        pointParameter = value;
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


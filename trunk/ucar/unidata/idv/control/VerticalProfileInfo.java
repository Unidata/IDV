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


import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.idv.control.chart.LineState;
import ucar.unidata.util.Misc;

import visad.*;

import visad.Unit;

import visad.georef.LatLonPoint;


/**
 * Class VerticalProfileInfo holds information for the multiple vertical profiles
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class VerticalProfileInfo {

    /** The unit */
    private Unit unit;

    /** Sampling mode */
    private int samplingMode;

    /** The data instance */
    private GridDataInstance dataInstance;

    /** The default LineState */
    private LineState lineState = new LineState(null, 1.0f,
                                      LineState.STROKE_SOLID,
                                      LineState.LINETYPE_SHAPES_AND_LINES,
                                      LineState.SHAPE_POINT);

    /** the sample from the last point */
    private FieldImpl profile;

    /** The last point that we sampled  on */
    private LatLonPoint lastPoint;

    /** the time set from the last setValue */
    private Set timeSet;

    /**
     * Default Constructor
     */
    public VerticalProfileInfo() {}

    /**
     * Constructor
     *
     * @param control The control we're in. Just use it for
     *                getting the sampling mode
     */
    public VerticalProfileInfo(VerticalProfileControl control) {
        samplingMode = control.getDefaultSamplingModeValue();
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
     * Set the Unit property.
     *
     * @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        unit = value;
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
     * Set the SamplingMode property.
     *
     * @param value The new value for SamplingMode
     */
    public void setSamplingMode(int value) {
        samplingMode = value;
    }

    /**
     * Get the data instance
     *
     * @return The data instance
     */
    public GridDataInstance getDataInstance() {
        return dataInstance;
    }


    /**
     * Set the data instance
     *
     * @param di The data instance
     */
    protected void setDataInstance(GridDataInstance di) {
        dataInstance = di;
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
     * Set the profile we last used
     *
     * @param profile profile
     * @param llp The point we sampled on
     */
    protected void setProfile(FieldImpl profile, LatLonPoint llp) {
        this.profile   = profile;
        this.lastPoint = llp;
    }


    /**
     * Get the last profile
     *
     * @return profile
     */
    public FieldImpl getProfile() {
        return profile;
    }


    /**
     * Get the profile we last used. If The given earth location
     * is not the same as the location of the last point we sampled on
     * then null out the sample and return null.
     *
     *
     * @param llp The point we want to sample on
     * @return sample
     */
    public FieldImpl getProfile(LatLonPoint llp) {
        if ( !Misc.equals(llp, lastPoint)) {
            profile = null;
        }
        return this.profile;
    }

}

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


import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.TimeHeightViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.metdata.NamedStation;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import ucar.visad.display.*;

import visad.*;
import visad.Set;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.*;
import java.util.List;

import javax.swing.*;


/**
 * For  wind time/height profiles.
 *
 * Uses FieldImpl with VisAD function (Time -> ((Z) -> (DIR, SPD))
 * where time values are
 * VisAD Datetime objects, Z has RealType RealType.Altitude, DIR and
 * SPD have RealType Display.Flow1Azimuth and Display.Flow1Radial,
 *
 * The data display is in its own window, not the main IDV view manager.
 *
 * @author Unidata IDV development
 * @version $ $
 */
public class WindTimeHeightControl extends ProfilerControl {

    /** vertical spacing */
    protected Real currentVerticalInt;

    /** profile display */
    protected XYDisplay profileDisplay;

    /** _more_ */
    protected TimeHeightViewManager timeHeightView;

    /** data */
    protected FieldImpl fieldImpl;

    /** displayable for data */
    protected WindBarbDisplayable wbDisplayable;

    /** label for the plot */
    protected JLabel plotLabel;

    /** flag for sequence */
    protected boolean isSequence = false;

    /** station name */
    protected String stationName;

    /** station name label */
    protected String labelName;

    /** position */
    protected float latitude, longitude;

    /** flag for X axis ordering */
    protected boolean isLatestOnLeft = true;

    /** flag for new station */
    protected boolean haveNewStation = false;

    /** scale for X axis */
    protected AxisScale xScale;

    /** _more_ */
    protected AxisScale yScale;

    /** data choice */
    protected CompositeDataChoice compositeDataChoice;

    /** selected data choice */
    protected DataChoice currentDataChoice = null;

    /** from data choice */
    private float currentLevel;
    /**
     *  Cstr; does nothing. See init() for creation actions.
     */
    public WindTimeHeightControl() {}

    /**
     * Construct the DisplayMaster, Displayable, frame, and controls.
     * @param dataChoice the DataChoice to use
     * @return boolean true if DataChoice is ok.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        boolean result = super.init(dataChoice);
        if ( !result) {
            return false;
        }
        timeHeightView = new TimeHeightViewManager(getViewContext(),
                new ViewDescriptor("timeheight_of_" + paramName),
                "showControlLegend=false;wireframe=true;") {
        };

        addViewManager(timeHeightView);
        profileDisplay = timeHeightView.getTimeHeightDisplay();
        profileDisplay.setAspect(1.0, .6);

        //If foreground is not null  then this implies we have been unpersisted

        if (dataChoice instanceof CompositeDataChoice) {
            compositeDataChoice = (CompositeDataChoice) dataChoice;
            //If we already have one then try it
            if (currentDataChoice != null) {
                if (initStation(currentDataChoice)) {
                    return true;
                }
            }
            for (int dataChoiceIdx = 0;
                    dataChoiceIdx < compositeDataChoice.getNumChildren();
                    dataChoiceIdx++) {
                currentDataChoice =
                    (DataChoice) compositeDataChoice.getDataChoices().get(
                        dataChoiceIdx);
                if (initStation(currentDataChoice)) {
                    return true;
                }
            }
            return false;
        } else {
            return initStation(dataChoice);
        }
    }

    /**
     * Apply the properties
     *
     * @return true if successful
     */
    public boolean doApplyProperties() {
        if ( !super.doApplyProperties()) {
            return false;
        }
        if (timeHeightView != null) {
            return timeHeightView.applyProperties();
        }
        return true;
    }

    /**
     * Initialize a station from the data choice
     *
     * @param dataChoice   choice describing data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean initStation(DataChoice dataChoice)
            throws VisADException, RemoteException {
        try {
            if ( !setData(dataChoice)) {
                return false;
            }
        } catch (Exception exc) {
            logException("There was an error loading the profiler data: "
                         + dataChoice, exc);
            return false;
        }
        currentDataChoice = dataChoice;
        if (wbDisplayable == null) {
            initDisplayables();
        }
        updateLegendAndList();

        return true;
    }

    /**
     * Initialize the displayables.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void initDisplayables() throws VisADException, RemoteException {
        Data          data  = getDataInstance().getData();
        FunctionType  ftype = (FunctionType) data.getType();
        FunctionType  obFT  = (FunctionType) ftype.getRange();
        RealTupleType rtt   = (RealTupleType) obFT.getFlatRange();
        wbDisplayable = new WindBarbDisplayable("profiler station", rtt, true);
        setFlowScale(getFlowScale());
        // profileDisplay = new XYDisplay("wind time height profile",
        //                                RealType.Time, RealType.Altitude);
        //    profileDisplay.setDisplayAspect(new double[] { .65, .65, 1.0 });
        //   profileDisplay.setAspect(1.0, 1.0);
        //profileDisplay.setYRange(0,16000);
        //addDisplayMaster(profileDisplay);
        profileDisplay.showAxisScales(true);
        xScale = profileDisplay.getXAxisScale();
        xScale.setMinorTickSpacing(3600);
        xScale.setTitle("Time of day, UT");
        xScale.setSnapToBox(true);
        setXAxisValues(((FieldImpl) data).getDomainSet());
        setYAxisValues((FieldImpl) data);
        yScale.setTitle("Height above MSL, meters");
        yScale.setSnapToBox(true);
        wbDisplayable.setVisible(true);
        addAttributedDisplayable(wbDisplayable, FLAG_COLORTABLE);
        profileDisplay.setDisplayables(new Displayable[] { wbDisplayable });
        profileDisplay.draw();
    }

    /**
     * Make the UI contents
     * @return  the UI
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        plotLabel = new JLabel(" ");
        setLabel();
        return GuiUtils.topCenterBottom(plotLabel,
                                        timeHeightView.getContents(),
                                        //profileDisplay.getComponent(),
                                        doMakeWidgetComponent());
    }

    /**
     * Make any extra components for the UI
     * @return  extra component
     */
    protected JComponent doMakeExtraComponent() {
        return doMakeVerticalIntervalComponent();
    }

    /**
     * Reset the vertical spacing interval
     *
     * @param verticalInt  new interval
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void resetDataVerticalInterval(Real verticalInt)
            throws VisADException, RemoteException {
        if ((verticalInt == null)
                || (Misc.equals(verticalInt, currentVerticalInt)
                    && !haveNewStation)) {
            return;
        }
        currentVerticalInt = verticalInt;
        int numbZs = 1 + (int) (currentLevel / verticalInt.getValue());
        Linear1DSet newZset = new Linear1DSet(RealType.Altitude, 0.0,
                                  currentLevel, numbZs);
        Set       timeSet = fieldImpl.getDomainSet();
        FieldImpl rvFI    = null;
        FlatField oneTimeFF, newFF;
        for (int i = 0; i < timeSet.getLength(); i++) {
            oneTimeFF = (FlatField) fieldImpl.getSample(i);
            newFF = (FlatField) (oneTimeFF.resample(newZset,
                    Data.NEAREST_NEIGHBOR, Data.NO_ERRORS));
            if (i == 0) {
                rvFI = new FieldImpl(
                    new FunctionType(
                        ((SetType) timeSet.getType()).getDomain(),
                        ((newFF).getType())), timeSet);
            }
            rvFI.setSample(i, newFF);
        }
        wbDisplayable.loadData(rvFI);
    }

    /**
     * set the length of the wind barb
     * @param value the length of the wind barb
     */
    public void setFlowScale(float value) {
        super.setFlowScale(value);
        if (wbDisplayable != null) {
            wbDisplayable.setFlowScale(flowScaleValue * scaleFactor);
        }
    }

    /**
     * set the vertical separation of wind barbs
     * @param value the vertical separation of wind barbs
     */
    public void setVerticalInterval(float value) {
        super.setVerticalInterval(value);
        if (wbDisplayable != null) {
            try {
                resetDataVerticalInterval(new Real(value));
            } catch (Exception ve) {
                logException("setVerticalInterval", ve);
            }
        }
    }

    /**
     * Get the initial range for the color table
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        Range range = getDisplayConventions().getParamRange(paramName,
                          displayUnit);
        if (range == null) {
            range = getRangeFromColorTable();
        }

        if (range == null) {
            return new Range(0, 255);
        }
        return range;
    }

    /**
     * Get the VisAD coordinates for the lat/lon point
     *
     * @param rlat    latitude (degrees)
     * @param rlon    longitude (degrees)
     * @return  XY coordinates
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected RealTuple getRealTupleForPoint(float rlat, float rlon)
            throws VisADException, RemoteException {
        RealTuple visadTup = earthToBoxTuple(new EarthLocationTuple(rlat,
                                 rlon, 0.0));
        Real[] reals   = visadTup.getRealComponents();
        Real   altreal = new Real(((RealType) (reals[2]).getType()), 1.0);
        return new RealTuple(new Real[] { reals[0], reals[1] });
    }

    /**
     * Set the XAxis values
     *
     * @throws VisADException  couldn't set the values
     */
    protected void setXAxisValues() throws VisADException {
        try {
            Set timeSet =
                ((FieldImpl) wbDisplayable.getData()).getDomainSet();
            setXAxisValues(timeSet);
        } catch (RemoteException re) {}
    }

    /**
     * Set the YAxis values
     *
     * @throws VisADException  couldn't set the values
     */
    protected void setYAxisValues() throws VisADException {
        try {
            FieldImpl data = (FieldImpl) wbDisplayable.getData();
            setYAxisValues(data);
        } catch (RemoteException re) {}
    }


    /**
     * _more_
     *
     * @param data _more_
     *
     * @throws VisADException _more_
     */
    protected void setYAxisValues(FieldImpl data) throws VisADException {
        try {
            Set   tset = data.getDomainSet();
            int   size = tset.getLength();

            int   step = 5;  //no more than 5 labels;

            float hii  = 0;
            float loo  = 10000;
            for (int i = 0; i < size; i++) {
                FlatField    sfd  = (FlatField) data.getSample(i);
                Gridded1DSet dset = (Gridded1DSet) sfd.getDomainSet();
                float[]      hi   = dset.getHi();
                float[]      lo   = dset.getLow();
                if (hi[0] > hii) {
                    hii = hi[0];
                }
                if (lo[0] < loo) {
                    loo = lo[0];
                }
            }

            yScale = profileDisplay.getYAxisScale();

            if (hii < 5000) {
                double averageSpacing = (hii - loo) / (double) (step);
                averageSpacing = (int)(averageSpacing/100)* 100.0;
                yScale.setMajorTickSpacing(averageSpacing);
                yScale.setMinorTickSpacing(averageSpacing/step);
                yScale.createStandardLabels(hii, 0, 0, averageSpacing);
                profileDisplay.setYRange(0, hii);
            } else {  // for anything above 5000
                yScale.setMajorTickSpacing(2000);
                yScale.setMinorTickSpacing(500);
                yScale.createStandardLabels(16000, 0, 0, 2000);
                profileDisplay.setYRange(0, hii);
            }
            currentLevel = hii;
        } catch (RemoteException re) {}
    }

    /**
     * Set the XAxis values based on the timeSet
     *
     * @param timeSet   set of times to use
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void setXAxisValues(Set timeSet)
            throws VisADException, RemoteException {
        if (timeSet == null) {
            return;
        }
        double time1 = DataUtility.getSample(timeSet, 0).getValues()[0];
        double time2 = DataUtility.getSample(timeSet,
                                             timeSet.getLength()
                                             - 1).getValues()[0];

        Hashtable timeLabels       = new Hashtable();
        int       numSteps         = timeSet.getLength();
        int       step             = (numSteps < 5)
                                     ? 1
                                     : timeSet.getLength() / 5;  //no more than 5 labels;
        double    majorTickSpacing = 3600;
        for (int k = 0; k < timeSet.getLength(); k += step) {
            double time = DataUtility.getSample(timeSet, k).getValues()[0];
            if (k == step) {
                majorTickSpacing = (time - time1);
            }
            DateTime dt = new DateTime(time);
            String   hr = " ";
            hr = dt.formattedString("HH:mm", TimeZone.getTimeZone("GMT"));
            // for only hours, use
            //  hr = dt.formattedString("HH", TimeZone.getTimeZone("GMT"));
            timeLabels.put(new Double(time), hr);
        }
        xScale.setTickBase(time1);
        double averageTickSpacing = (time2 - time1) / (double) (numSteps - 1);
        xScale.setMajorTickSpacing(averageTickSpacing * step);
        xScale.setMinorTickSpacing(averageTickSpacing);
        if (timeSet.getLength() == 1) {
            if (isLatestOnLeft) {
                profileDisplay.setXRange(time1 + 3600, time1 - 3600);
            } else {
                profileDisplay.setXRange(time1 - 3600, time1 + 3600);
            }
        } else {
            if (isLatestOnLeft) {
                profileDisplay.setXRange(time2, time1);
            } else {
                profileDisplay.setXRange(time1, time2);
            }
        }
        xScale.setLabelTable(timeLabels);
    }

    /**
     * Set whether latest data is displayed on the left or right
     * side of the plot.  Used by XML persistence mainly.
     * @param yesorno  <code>true</code> if want latest is on left.
     */
    public void setLatestOnLeft(boolean yesorno) {
        isLatestOnLeft = yesorno;
    }

    /**
     * Fill the label.
     */
    protected void setLabel() {
        if (plotLabel != null) {
            plotLabel.setText("  Lat " + latitude + "  Long " + longitude);
        }
    }

    /**
     * Get whether latest data is displayed on the left or right
     * side of the plot.
     * @return  <code>true</code> if latest is on left.
     */
    public boolean getLatestOnLeft() {
        return isLatestOnLeft;
    }

    /**
     *  Set the CurrentDataChoice property.
     *  @param value The new value for CurrentDataChoice
     */
    public void setCurrentDataChoice(DataChoice value) {
        currentDataChoice = value;
    }

    /**
     *  Get the CurrentDataChoice property.
     *  @return The CurrentDataChoice
     */
    public DataChoice getCurrentDataChoice() {
        return currentDataChoice;
    }
}

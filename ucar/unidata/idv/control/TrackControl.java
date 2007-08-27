/*
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
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
import ucar.unidata.collab.SharableImpl;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;

import ucar.unidata.data.DataTimeRange;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.ui.drawing.*;


import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.ShapeUtility;

import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;



import visad.*;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/**
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class TrackControl extends GridDisplayControl {



    /** mutex */
    private final Object DATA_MUTEX = new Object();



    /** the displayable for the track */
    TrackDisplayable trackDisplay;

    /** the displayable for the track range */
    SelectRangeDisplayable selectRangeDisplay;

    /** track width */
    int trackWidth = 2;

    /** slider label */
    private JLabel sliderLabel;

    /** Shows the width */
    private JLabel widthLabel;

    /** Entire track type */
    private static final String CMD_ALL = TrackDataSource.ID_WHOLETRACE;

    /** Point track type */
    private static final String CMD_POINT = TrackDataSource.ID_POINTTRACE;

    /** range track type */
    private static final String CMD_RANGE = TrackDataSource.ID_TIMETRACE;

    /** animation end type */
    private static final String CMD_ANIMATION_TIME = "animation_time";

    /** track end type */
    private static final String CMD_TRACK_TIME = "track_time";

    /** track type */
    private String trackType = CMD_RANGE;



    /** selector point */
    private StationModelDisplayable indicator = null;

    /** text field for marker text */
    private JTextField markerTextField;


    /** combobox for maker symbol */
    private JComboBox symbolBox;

    /** marker text */
    private String markerText = "";

    /** marker symbol name */
    private String markerSymbol = ShapeUtility.NONE;


    /** The last time range we used */
    private Range lastRange;

    /** Last position */
    private EarthLocationTuple lastIndicatorPosition;



    /**
     * Create a new Track Control; set the attribute flags
     */
    public TrackControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_TIMERANGE);
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        GridDataInstance gridDataInstance = getGridDataInstance();
        if ((gridDataInstance == null) || !gridDataInstance.dataOk()) {
            return false;
        }
        trackDisplay = new TrackDisplayable("track" + dataChoice);
        setTrackWidth(trackWidth);
        addDisplayable(trackDisplay, getAttributeFlags());
        selectRangeDisplay = new SelectRangeDisplayable();
        addDisplayable(selectRangeDisplay, FLAG_DISPLAYUNIT);
        getAnimation();
        indicator = new StationModelDisplayable("indicator");
        indicator.setShouldUseAltitude(true);
        updateIndicator();
        addDisplayable(indicator);
        return setData(dataChoice);
    }


    /**
     * Update the indicator with new shapes
     */
    private void updateIndicator() {
        if (indicator != null) {
            try {
                lastIndicatorPosition = null;
                indicator.setStationModel(makeStationModel());
                applyTimeRange();
            } catch (Exception exc) {
                logException("Updating indicator", exc);
            }
        }
    }

    /**
     * Return the label that is to be used for the color widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     */
    public String getColorWidgetLabel() {
        return "Marker Color";
    }

    /**
     * Set the dataInstance and its paramName
     * in the superclass member data. Set the dataInstance in this class
     * member getGridDataInstance(). Get appropritate contour information
     * for the data.
     *
     * @param dataInstance  the data instance to use
     */
    protected void setDataInstance(DataInstance dataInstance) {
        super.setDataInstance(dataInstance);
        try {
            if (selectRangeDisplay != null) {
                selectRangeDisplay.setSelectRealType(
                    getGridDataInstance().getRealType(0));
            }
        } catch (Exception exc) {
            logException("setting range real type", exc);
        }
    }

    /**
     * Get the display center
     *
     * @return  the center point of the display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public LatLonPoint getDisplayCenter()
            throws RemoteException, VisADException {
        FlatField flatField = getFlatField();
        if (flatField == null) {
            return null;
        }
        Set domainSet = flatField.getDomainSet();
        Real[] llaR = DataUtility.getSample(domainSet,
                                            domainSet.getLength()
                                            - 1).getRealComponents();
        return new LatLonTuple(llaR[0].getValue(), llaR[1].getValue());

    }



    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        if ( !isInitDone()) {
            return;
        }
        try {
            applyTimeRange();
        } catch (Exception exc) {
            logException("applyTimeRange", exc);
        }
    }


    /**
     * Set the data in this control
     *
     * @param choice    data choice
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if (trackDisplay == null) {
            return true;
        }
        if ( !super.setData(choice)) {
            //For now don't assume null data is bad.
            //This way we can come back from a bundle that is polling
            //on as yet non-existent data
            //            return false;
        }
        Unit newUnit = getDisplayUnit();
        //TODO: use the right index
        if ((newUnit != null) && !newUnit.equals(getDisplayUnit())
                && Unit.canConvert(newUnit, getRawDataUnit())) {
            trackDisplay.setDisplayUnit(newUnit);
            selectRangeDisplay.setDisplayUnit(newUnit);
        }
        FlatField        ff   = null;
        FieldImpl        grid = null;
        GridDataInstance gdi  = getGridDataInstance();
        if ((gdi == null) || !gdi.dataOk()) {
            if (trackDisplay != null) {
                trackDisplay.setData(new Real(0));
                indicator.setVisible(false);
            }
            return true;
        }
        if (indicator != null) {
            indicator.setVisible(true);
        }
        synchronized (gdi) {
            ff = getFlatField();
            if (ff != null) {
                if (gdi != null) {
                    grid = getGridDataInstance().getGrid(false);
                }
            }
        }
        if ((ff != null) && (grid != null)) {
            updateSelectRange();
            trackDisplay.setTrack(grid);
            applyTimeRange();
        }
        return true;
    }

    /**
     * Get the data for this (without time dimension);
     *
     * @return the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FlatField getFlatField() throws VisADException, RemoteException {
        GridDataInstance gdi = getGridDataInstance();
        if ((gdi == null) || !gdi.dataOk()) {
            return null;
        }
        FieldImpl fi = gdi.getGrid(false);
        if (fi != null) {
            return (fi instanceof FlatField)
                   ? (FlatField) fi
                   : (FlatField) fi.getSample(0, false);
        }
        return null;
    }

    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JComponent contents = (JComponent) super.doMakeContents();
        return GuiUtils.top(contents);
    }


    /**
     * Add control widgets specific to this control to the list
     *
     * @param controlWidgets   list of control widgets
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        super.getControlWidgets(controlWidgets);



        sliderLabel = GuiUtils.rLabel("");
        updateSliderLabel();
        controlWidgets.add(new WrapperWidget(this, sliderLabel,
                                             doMakeWidthSlider()));


        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Marker:"),
                                             doMakeMarkerWidget()));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel(getColorWidgetLabel() + ":"),
                GuiUtils.left(
                    doMakeColorControl(getDisplayConventions().getColor()))));

    }




    /**
     * Update the slider label with the appropriate text
     */
    private void updateSliderLabel() {
        sliderLabel.setText(getTrackType().equals(CMD_POINT)
                            ? "Point Size:"
                            : "Line Width:");
    }


    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        if(getDataTimeRange()!=null) {
            dsd.addPropertyValue(getDataTimeRange(), "dataTimeRange",
                         "Time Mode", "Display");
        }
    }


    public void setDataTimeRange(DataTimeRange range) {
        super.setDataTimeRange(range);
        if(getHaveInitialized()) {
            applyTimeRange();
        }
    }


    /**
     * Handle some sort of time change.  Either the subsetting interval
     * changes or there is a new timestep.
     */
    public void applyTimeRange() {

        try {
            DataTimeRange    dataTimeRange    = getDataTimeRange(true);
            GridDataInstance gridDataInstance = getGridDataInstance();
            if ((gridDataInstance == null) || !gridDataInstance.dataOk()) {
                return;
            }
            FlatField flatField;
            Unit      dataTimeUnit;
            synchronized (gridDataInstance) {
                flatField = getFlatField();
                if (flatField == null) {
                    return;
                }
                dataTimeUnit = gridDataInstance.getRawUnit(1);
            }
            Range    r                = getRangeForSelect();
            RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
            Real startReal = new Real(dataTimeRealType, r.getMin(),
                                      dataTimeUnit);
            Real endReal = new Real(dataTimeRealType, r.getMax(),
                                    dataTimeUnit);



            Animation anime    = getAnimation();
            Real      aniValue = ((anime != null)
                                  ? anime.getAniValue()
                                  : null);

            Real[] startEnd = getDataTimeRange(true).getTimeRange(startReal,
                                  endReal, aniValue);


            double startDate = startEnd[0].getValue(dataTimeUnit);
            double endDate   = startEnd[1].getValue(dataTimeUnit);
            if ( !Misc.equals(lastRange, new Range(startDate, endDate))) {
                lastRange = new Range(startDate, endDate);
                if (trackDisplay != null) {
                    trackDisplay.setSelectedRange(startDate, endDate);
                }
            }
            int      index = 0;
            double[] times = flatField.getValues(false)[1];
            for (; index < times.length; index++) {
                if (times[index] >= endDate) {
                    index--;
                    break;
                }
                if (times[index] == endDate) {
                    break;
                }
            }
            if (index == times.length) {
                index--;
            }
            if (index < 0) {
                index = 0;
            }
            Real[] llaR = DataUtility.getSample(flatField.getDomainSet(),
                              index).getRealComponents();
            if (llaR != null) {
                EarthLocationTuple elt = new EarthLocationTuple(llaR[0],
                                             llaR[1], llaR[2]);

                if ( !Misc.equals(elt, lastIndicatorPosition)) {
                    lastIndicatorPosition = elt;
                    indicator.setStationData(
                        PointObFactory.makePointObs(elt));
                    doShare(ProbeControl.SHARE_POSITION, elt);
                }
            }
        } catch (Exception e) {
            logException("applyTimeRange", e);
        }
    }


    /**
     * Make a radio button for a particular time option
     *
     * @param name        label string
     * @param trackType   associated type
     * @param selected    true if should be selected
     *
     * @return radio button
     */
    private JRadioButton makeTrackOptionButton(String name, String trackType,
            boolean selected) {
        JRadioButton rb = new JRadioButton(name, selected);
        rb.setActionCommand(trackType);
        rb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setTrackType(
                        ((JRadioButton) e.getSource()).getActionCommand());
                    resetData();
                    updateSliderLabel();
                } catch (Exception ex) {
                    logException("resetData", ex);
                }
            }
        });
        return rb;
    }


    /**
     * Override the base class method that creates request properties
     * and add in the appropriate 2d/3d request parameter.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(TrackDataSource.PROP_TRACKTYPE, trackType);
        return props;
    }

    /**
     * Update the select range for the widget with the data's range
     */
    private void updateSelectRange() {
        try {
            Range r = getRangeForSelect();
            if (r == null) {
                return;
            }
            if (trackDisplay != null) {
                trackDisplay.setRangeForSelect(r.getMin(), r.getMax());
            }
            Range d = getColorRangeFromData();
            if (d == null) {
                return;
            }
            if (selectRangeDisplay != null) {
                selectRangeDisplay.setRangeForSelect(d.getMin(), d.getMax());
            }
        } catch (Exception e) {
            logException("updateSelectRange", e);
        }
    }

    /**
     * Set the range of the data to be displayed
     * @param r range of the data
     */
    public void setSelectRange(Range r) {
        if (selectRangeDisplay != null) {
            try {
                selectRangeDisplay.setSelectedRange(r.getMin(), r.getMax());
            } catch (Exception exc) {
                logException("setting range real type", exc);
            }
        }
    }


    /**
     * Get the range for selection.
     *
     * @return the Range
     *
     * @throws RemoteException remote data error
     * @throws VisADException  VisAD error
     */
    private Range getRangeForSelect() throws VisADException, RemoteException {
        Range            range = getRange();
        GridDataInstance gdi   = getGridDataInstance();
        if ((gdi != null) && (gdi.getNumRealTypes() > 1)) {
            range = gdi.getRange(1);
        }
        return range;
    }

    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        return false;
    }

    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges. The default is 1 though if there is not more than
     * one field in the range then we end up with the 0th value.
     * @return  0
     */
    protected int getColorRangeIndex() {
        return 0;
    }

    /**
     * Set the color for the selector. Used by persistence.
     *
     * @param c  color to use
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color in VisAD
     */
    public void setColor(Color c) throws RemoteException, VisADException {
        super.setColor(c);
        updateIndicator();
    }

    /**
     * Make a slider for setting the track width
     * @return  slider
     */
    private Component doMakeWidthSlider() {
        widthLabel = GuiUtils.getFixedWidthLabel(StringUtil.padLeft(""
                + getTrackWidth(), 3));

        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slide = (JSlider) e.getSource();
                if (slide.getValueIsAdjusting()) {
                    return;
                }
                setTrackWidth(slide.getValue());
                widthLabel.setText(StringUtil.padLeft("" + getTrackWidth(),
                        3));
            }
        };

        JComponent[] sliderComps = GuiUtils.makeSliderPopup(1, 20,
                                       getTrackWidth(), listener);
        JSlider slider = (JSlider) sliderComps[1];
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setToolTipText("Change width of track");
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        return GuiUtils.left(GuiUtils.hbox(widthLabel, new JLabel(" "),
                                           sliderComps[0]));
    }

    /**
     * Make a selection widget for markers
     * @return  the selection widget
     */
    private Component doMakeMarkerWidget() {
        markerTextField = new JTextField(markerText, 10);
        markerTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                markerText = ((JTextField) ae.getSource()).getText().trim();
                updateIndicator();
            }
        });
        symbolBox = new JComboBox(ShapeUtility.SHAPES);
        TwoFacedObject tfo   = new TwoFacedObject(markerSymbol, markerSymbol);
        int            index = Misc.toList(ShapeUtility.SHAPES).indexOf(tfo);
        if (index >= 0) {
            symbolBox.setSelectedIndex(index);
        } else {
            symbolBox.setSelectedItem(tfo);
        }
        symbolBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                markerSymbol =
                    (String) ((TwoFacedObject) ((JComboBox) ae.getSource())
                        .getSelectedItem()).getId();
                updateIndicator();
            }
        });
        return GuiUtils.left(GuiUtils.hbox(new Component[] {
            GuiUtils.rLabel("Text: "),
            markerTextField, GuiUtils.rLabel("  Symbol: "), symbolBox }));
    }

    /**
     * Method called by other classes that share the selector.
     *
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if ( !getHaveInitialized()) {
            return;
        }
        if (dataId.equals(SHARE_SELECTRANGE)
                && ((DataChoice) data[0]).basicallyEquals(getDataChoice())) {

            try {
                setSelectRange((Range) data[1]);
            } catch (Exception exc) {
                logException("receiveShareData.level", exc);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }

    /**
     * Set the track width property.  Used by persistence
     *
     * @param width  width for track (pixels)
     */
    public void setTrackWidth(int width) {
        try {
            if (trackDisplay != null) {
                trackDisplay.setLineWidth(width);
            }
            trackWidth = width;
        } catch (Exception ve) {}
    }

    /**
     * Creates a station model from the supplied parameters.
     * @return
     */
    private StationModel makeStationModel() {
        StationModel obView     = new StationModel("TrackLocation");
        LabelSymbol  textSymbol = new LabelSymbol();
        textSymbol.setValue(markerText);

        ShapeSymbol shapeSymbol = new ShapeSymbol(0, 0);
        shapeSymbol.setShape(markerSymbol);

        if (getColor() != null) {
            shapeSymbol.setForeground(getColor());
            textSymbol.setForeground(getColor());
        } else {
            shapeSymbol.setForeground(Color.magenta);
            textSymbol.setForeground(Color.magenta);
        }

        shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
        shapeSymbol.setRectPoint(Glyph.PT_MM);

        boolean showId     = !markerText.equals("");
        boolean showSymbol = !markerSymbol.equals(ShapeUtility.NONE);
        if (showId) {
            obView.addSymbol(textSymbol);
        }
        if (showSymbol) {
            obView.addSymbol(shapeSymbol);
        }
        if (showSymbol && showId) {
            textSymbol.bounds = new java.awt.Rectangle(-11, -31, 72, 24);
            textSymbol.setRectPoint(Glyph.PT_LM);
        } else if (showId) {
            textSymbol.bounds = new java.awt.Rectangle(-11, -8, 72, 24);
            textSymbol.setRectPoint(Glyph.PT_MM);
        } else if (showSymbol) {
            //Same position as above
        }
        return obView;
    }

    /**
     * Get the track width property.  Used by persistence
     * @return  width
     */
    public int getTrackWidth() {
        return trackWidth;
    }

    /**
     * Set the track type for this control.  Used by persistence
     *
     * @param type    track type
     */
    public void setTrackType(String type) {
        trackType = type;
    }

    /**
     * Get the track type for this control.  Used by persistence
     *
     * @return the track type
     */
    public String getTrackType() {
        return trackType;
    }

    /**
     * Set the TimeSubsetMinutes property.
     *
     * @param value The new value for TimeSubsetMinutes
     * @deprecated
     */
    public void setTimeSubsetMinutes(double value) {}



    /**
     * Set the TimeSubsetEnabled property.
     *
     * @deprecated
     * @param value The new value for TimeSubsetEnabled
     */
    public void setTimeSubsetEnabled(boolean value) {}


    /**
     * Set the useTrackTime property.
     *
     * @param value The new value for TimeSubsetEnabled
     * @deprecated
     */
    public void setUseTrackTime(boolean value) {}



    /**
     * Set the MarkerText property.
     * @param value The new value for MarkerText
     */
    public void setMarkerText(String value) {
        markerText = value;
    }

    /**
     * Get the MarkerText property.
     * @return The MarkerText
     */
    public String getMarkerText() {
        return markerText;
    }

    /**
     * Set the MarkerSymbol property.
     * @param value The new value for MarkerSymbol
     */
    public void setMarkerSymbol(String value) {
        markerSymbol = value;
    }

    /**
     * Get the MarkerSymbol property.
     * @return The MarkerSymbol
     */
    public String getMarkerSymbol() {
        return markerSymbol;
    }


}


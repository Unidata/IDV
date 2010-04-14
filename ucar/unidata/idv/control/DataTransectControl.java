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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.display.CrossSectionSelector;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.TrackDisplayable;
import ucar.visad.display.XSDisplay;

import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.Length;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import visad.util.DataUtility;


import java.awt.*;
import java.awt.event.*;


import java.rmi.RemoteException;

import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class for displaying transects of data along a cross section line.
 *
 * @author IDV developers
 * @version $Revision: 1.31 $
 */
public class DataTransectControl extends CrossSectionControl {


    /** The current range of the line */
    private Range lineRange;

    /** The current range of the line */
    private JLabel rangeLabel;

    /** track width */
    int lineWidth = 2;

    /**
     * Default Constructor
     */
    public DataTransectControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL | FLAG_DISPLAYUNIT);
    }


    /**
     * Make a Selector line which shows and controls where cross section is
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void createCrossSectionSelector()
            throws VisADException, RemoteException {

        // make the initial selector position across the middle of the grid from
        // side to side; 1/10 of the width in from each end.
        // (converts grid indices to VisAD internal coordinates)
        // z level at origin of grid

        // make a Selector line there
        /*
        RealTuple[] positions =
            RangeAndBearingControl.makeDefaultLinePosition(
                getNavigatedDisplay());

        EarthLocation loc1 = boxToEarth(positions[0]);
        EarthLocation loc2 = boxToEarth(positions[1]);
        */

        NavigatedDisplay mapDisplay = getNavigatedDisplay();

        double[]         right      = mapDisplay.getScreenUpperRight();
        double[]         center     = mapDisplay.getScreenCenter();
        right[1] = center[1];
        double width = right[0] - center[0];

        EarthLocationTuple loc1 =
            (EarthLocationTuple) mapDisplay.getEarthLocation(center[0],
                center[1], 0, false);
        EarthLocationTuple loc2 =
            (EarthLocationTuple) mapDisplay.getEarthLocation(center[0]
                + 0.6 * width, right[1], 0, false);




        createCrossSectionSelector(loc1, loc2);
    }


    /**
     * Return the attribute flags to apply to the cross section selector.
     *
     * @return Flags to use
     */
    protected int getSelectorAttributeFlags() {
        return FLAG_COLOR | FLAG_ZPOSITION;
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

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Line Width: "),
                                             doMakeWidthSlider()));
    }

    /**
     * Make a slider for setting the transect width
     * @return  slider
     */
    private Component doMakeWidthSlider() {
        final JLabel lineWidthLbl =
            GuiUtils.getFixedWidthLabel(StringUtil.padLeft(""
                + getLineWidth(), 3));
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slide = (JSlider) e.getSource();
                if (slide.getValueIsAdjusting()) {
                    return;
                }
                setLineWidth(slide.getValue());
                lineWidthLbl.setText(StringUtil.padLeft("" + getLineWidth(),
                        3));
            }
        };

        JComponent[] sliderComps = GuiUtils.makeSliderPopup(1, 20,
                                       getLineWidth(), listener);
        JSlider slider = (JSlider) sliderComps[1];

        sliderComps[0].setToolTipText("Change Line Width");
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setToolTipText("Change width of line");
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setSnapToTicks(true);
        return GuiUtils.left(GuiUtils.hbox(lineWidthLbl, new JLabel(" "),
                                           sliderComps[0]));
    }


    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the cross section view display.
     *
     * @return  depictor for data in main display
     *
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected DisplayableData createVCSDisplay()
            throws VisADException, RemoteException {
        TrackDisplayable line = new TrackDisplayable("dcs_" + paramName);
        line.setLineWidth(lineWidth);
        addAttributedDisplayable(line, FLAG_COLORTABLE);
        return line;
    }

    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the main display.
     *
     * @return  depictor for data in main display
     *
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected DisplayableData createXSDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable display = new Grid2DDisplayable("dcs_" + paramName,
                                        true);
        display.setLineWidth(lineWidth);
        addAttributedDisplayable(display, FLAG_COLORTABLE);
        return display;
    }

    /**
     * Initialize the class.
     *
     * @param dataChoice   data description
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.init(dataChoice)) {
            return false;
        }
        getCrossSectionViewManager().getXSDisplay().setName(
            "Data Cross Section ");
        return true;
    }

    /**
     * Load the 2D data into the appropriate display(s)
     * @param twoDData  cross section slice converted to 2D
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void load2DData(FieldImpl twoDData)
            throws VisADException, RemoteException {
        if (twoDData == null) {
            return;
        }
        try {
            //Find the range of data
            Range[] range = GridUtil.getMinMax(twoDData);
            lineRange = range[0];
            Unit dataUnit = null;
            if (getGridDataInstance() != null) {
                dataUnit = getGridDataInstance().getRawUnit(0);
            }
            if ( !Misc.equals(displayUnit, dataUnit)) {
                lineRange = convertRange(lineRange, dataUnit,
                                         getDisplayUnit());
            }

        } catch (Exception exc) {}

        if (getVerticalAxisRange() == null) {
            setVerticalAxisRange(getRange());
        }
        ((TrackDisplayable) getVerticalCSDisplay()).setTrack(twoDData);
    }


    /**
     * Add an entry into the range menu
     *
     * @param rw The widget that manages the range dialog
     * @param items List of menu items
     */
    public void addToRangeMenu(final RangeWidget rw, List items) {
        super.addToRangeMenu(rw, items);
        if (lineRange == null) {
            return;
        }
        JMenuItem mi = new JMenuItem("From Line");
        items.add(mi);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                rw.setRangeDialog(convertColorRange(lineRange));
            }
        });
    }

    /**
     * Apply the display unit to the data depictions.
     *
     * @throws VisADException error applying unit
     * @throws RemoteException error applying unit
     */
    protected void applyDisplayUnit() throws VisADException, RemoteException {
        super.applyDisplayUnit();
        getCrossSectionViewManager().getXSDisplay().setYDisplayUnit(
            getDisplayUnit());
    }

    /**
     * Apply the data range to the data depictions.
     *
     * @throws VisADException error applying unit
     * @throws RemoteException error applying unit
     */
    protected void applyRange() throws VisADException, RemoteException {
        super.applyRange();
        XSDisplay xsDisplay = getCrossSectionViewManager().getXSDisplay();
        xsDisplay.setYDisplayUnit(getDisplayUnit());
        if ( !getAutoScaleYAxis()) {
            setYAxisRange(xsDisplay, getVerticalAxisRange());
        }
    }

    /**
     * Get the label for the CrossSectionView
     * @return  return the name of the cross section view
     */
    protected String getCrossSectionViewLabel() {
        return "Data Transect";
    }

    /**
     * Set the parameters for the view.  Mostly deals with the
     * vertical scale.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void updateViewParameters()
            throws VisADException, RemoteException {
        super.updateViewParameters();
        XSDisplay xsDisplay = getCrossSectionViewManager().getXSDisplay();
        if (getGridDataInstance() != null) {
            xsDisplay.setYAxisType(getGridDataInstance().getRealType(0));
            applyRange();
            AxisScale yAxis = xsDisplay.getYAxisScale();
            yAxis.setTitle(paramName);
            yAxis.setColor(java.awt.Color.blue);
            yAxis.setSnapToBox(true);
        }
    }

    /**
     * Methods to do the things that need to be done when the data range
     * changes.
     *
     * @param display   the display to modify
     * @param range     Range of values in units of Y Axis
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void setYAxisRange(XSDisplay display, Range range)
            throws VisADException, RemoteException {
        if (range == null) {
            range = getVerticalAxisRange();
        }
        if (range == null) {
            range = getRange();
        }
        if (range != null) {
            display.setYRange(range.getMin(), range.getMax());
        }
        /* TODO: needed?
        if (ctw != null) {
            ctw.setRange(range);
        }
        */
    }

    /**
     * Make the domain for the 2D grid
     *
     * @param domainSet   the domain to be 2D'ized
     *
     * @return  the 2D ized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected GriddedSet make2DDomainSet(GriddedSet domainSet)
            throws VisADException, RemoteException {

        int sizeX = domainSet.getLengths()[0];

        // get its coordinate tranform
        CoordinateSystem transform = domainSet.getCoordinateSystem();
        int              lonIndex  = GridUtil.isLatLonOrder(domainSet)
                                     ? 1
                                     : 0;
        int              latIndex  = 1 - lonIndex;

        // get the array of the a,b,c values
        float[][] transformed = domainSet.getSamples();

        // need to do this to make sure we get data in degrees
        transformed = CoordinateSystem.transformCoordinates((lonIndex == 0)
                ? RealTupleType.SpatialEarth2DTuple
                : RealTupleType
                    .LatitudeLongitudeTuple, (CoordinateSystem) null,
                                             new Unit[] { CommonUnit.degree,
                CommonUnit.degree }, (ErrorEstimate[]) null,
                                     ((SetType) domainSet.getType())
                                         .getDomain(), transform,
                                             domainSet.getSetUnits(),
                                             (ErrorEstimate[]) null,
                                             transformed, false);


        // declare an array to hold the (distance,height) positions
        // of points in the 2D display
        float[][] plane = {
            createXFromLatLon(new float[][] {
                transformed[0], transformed[1]
            }, sizeX, lonIndex)
        };
        //Misc.printArray("2D samples", plane[0]);

        RealType xType = null;
        if (crossSectionView != null) {
            XSDisplay xs = crossSectionView.getXSDisplay();
            xType = xs.getXAxisType();
        } else {
            xType = Length.getRealType();
        }

        Gridded1DSet csDS =
            new Gridded1DSet(new RealTupleType(xType), plane,
                             plane[0].length, (CoordinateSystem) null,
                             new Unit[] { CommonUnits.KILOMETER },
                             (ErrorEstimate[]) null);
        //System.out.println("csDS = " + csDS);
        return csDS;

    }


    /**
     * Set the line width property.  Used by persistence
     *
     * @param width  width for line (pixels)
     */
    public void setLineWidth(int width) {
        try {
            if (getVerticalCSDisplay() != null) {
                getVerticalCSDisplay().setLineWidth(width);
            }
            if (getXSDisplay() != null) {
                getXSDisplay().setLineWidth(width);
            }
            lineWidth = width;
        } catch (Exception ve) {}
    }

    /**
     * Get the track width property.  Used by persistence
     * @return  width
     */
    public int getLineWidth() {
        return lineWidth;
    }
}

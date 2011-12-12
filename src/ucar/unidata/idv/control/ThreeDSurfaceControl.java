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

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;


import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;

import ucar.visad.display.Grid3DDisplayable;

import ucar.visad.display.GridDisplayable;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;



/**
 * A MetApps Display Control with Displayable and controls for
 * one 3D isosurface display of one parameter.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.106 $
 */

public class ThreeDSurfaceControl extends GridDisplayControl {

    /** displayable for the isosurface */
    Grid3DDisplayable myDisplay;

    /** last level in raw units */
    private double lastRawLevel;

    /** Property for sharing isosurface value */
    public static final String SHARE_SURFACEVALUE =
        "ThreeDSurfaceControl.SHARE_FIELDSURFACEVALUE";

    /**
     * Property for sharing transparency. Deprecated since
     * transparencey is now done through color table sharing.
     * @deprecated
     */
    public static final String SHARE_TRANSPARENCY =
        "ThreeDSurfaceControl.SHARE_TRANSPARENCY";

    /** flag for ignoring UI events */
    private boolean ignoreUIEvents = false;

    /**
     * Default transparency value is fully opaque
     */
    float alpha = 1.0f;

    /**
     * percentage of slider. We don't really use this,
     *   just keep it around for legacy bundles
     */
    double levelSliderPercent = Double.NaN;

    /** The isosurface value */
    double surfaceValue = Double.NaN;


    /** label for showing current surface value */
    private JLabel levelLabel;

    /** Field for input/output of surface value */
    private JTextField levelReadout;

    /** slider for selecting surface value */
    private JSlider levelSlider;

    /** range for surface */
    private Range levelRange = new Range(0.0, 100000.0);

    /**
     * Default constructor; does nothing.  See init() for class initialization
     */
    public ThreeDSurfaceControl() {}


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

        if ( !isDisplay3D()) {
            userMessage("Can't display IsoSurface in 2D display");
            return false;
        }
        myDisplay = new Grid3DDisplayable("3diso_" + dataChoice, true);


        //Create the widgets (which are accessed from the setData call)
        makeInitialWidgets();

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }

        //Now set up the flags and add the displayable 
        setAttributeFlags(FLAG_COLORTABLE);

        if (haveMultipleFields()) {
            //If we have multiple fields then we want both the color unit 
            //and the display unit
            addDisplayable(myDisplay,
                           FLAG_COLORTABLE | FLAG_DATACONTROL
                           | FLAG_COLORUNIT | FLAG_DISPLAYUNIT);
        } else {
            //If just one field then just the color unit
            addDisplayable(myDisplay,
                           FLAG_COLORTABLE | FLAG_DATACONTROL
                           | FLAG_DISPLAYUNIT);
        }

        return true;
    }

    /**
     * Called after init.  Adjust transparency for legacy bundles.
     */
    public void initDone() {
        super.initDone();
        if (alpha != 1.0) {
            adjustTransparency(alpha, true);
        }
    }



    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_VALUE));
        labels.addAll(Misc.newList("Isosurface Value"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template The template to use
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);
        patterns.add(MACRO_VALUE);
        values.add(levelReadout.getText());
    }



    /**
     * Method called when display unit changes.
     *
     * @param oldUnit  old unit
     * @param newUnit  new unit
     */
    protected void displayUnitChanged(Unit oldUnit, Unit newUnit) {
        super.displayUnitChanged(oldUnit, newUnit);
        try {
            //For now we don't need to set the level because the Displayable ignores the displayunit
            //setLevel (lastRawLevel);
            setSliderValues();
            adjustSliderLabel(getWholeDisplayValue(lastRawLevel));
        } catch (Exception exc) {
            logException("Handling display unit change", exc);
        }
    }


    /**
     * Set the data in the display control from the data choice
     *
     * @param choice   choice describing data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice)) {
            return false;
        }
        ((GridDisplayable) myDisplay).setColoredByAnother(
            haveMultipleFields());
        myDisplay.setGrid3D(getGrid(getGridDataInstance()));
        setSliderValues();
        double initialValue = surfaceValue;
        if (Double.isNaN(initialValue)) {
            if ( !Double.isNaN(levelSliderPercent)) {
                initialValue = getRawLevelFromSlider();
            } else {
                initialValue = getDataRange().getMid();
            }
        }

        setLevelWithRawValue(initialValue);
        return true;
    }

    /** slider components */
    private JComponent[] sliderComps;

    /**
     *  This makes the initial gui widgets
     */
    private void makeInitialWidgets() {

        // Make a JSlider to adjust value of surface's value.
        //   slider internal values -- not seen by user --
        //   are 0 to 1000; first slider position at mid range, 500
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                try {
                    if ( !getHaveInitialized() || ignoreUIEvents) {
                        return;
                    }
                    double newRawLevel = getRawLevelFromSlider();
                    if (levelSlider.getValueIsAdjusting()) {
                        adjustSliderLabel(getWholeDisplayValue(newRawLevel));
                    } else {
                        setLevelWithRawValue(newRawLevel);
                    }
                } catch (Exception exc) {
                    logException("adjust level ", exc);
                }
            }
        };

        sliderComps = GuiUtils.makeSliderPopup(0, levelRange.getMaxInt(),
                levelRange.getMaxInt() / 2, listener);

        levelSlider = new JSlider(JSlider.HORIZONTAL, 0,
                                  levelRange.getMaxInt(),
                                  levelRange.getMaxInt() / 2);
        levelSlider = (JSlider) sliderComps[1];
        sliderComps[0].setToolTipText("Change Isosurface Value");
        //TODO        GuiUtils.setSliderPercent(levelSlider, levelSliderPercent);
        levelSlider.setPaintTicks(true);
        levelSlider.setPaintLabels(true);
        levelSlider.setPaintTrack(true);
        levelSlider.setToolTipText("Reset value of isosurface");
        levelSlider.setMaximumSize(new Dimension(300, 40));
        levelSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        levelSlider.addChangeListener(listener);


        // make readout of slider value;
        levelLabel = new JLabel("   ");
        //        levelLabel.setPreferredSize(new Dimension(70, 10));
        levelLabel.setToolTipText("Click to Change Display Unit");
        levelLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                changeDisplayUnit();
            }
        });
        levelReadout = new JTextField("   ", 6);
        levelReadout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (ignoreUIEvents) {
                    return;
                }
                try {
                    double displayLevel =
                        Misc.parseNumber(levelReadout.getText().trim());
                    double rawValue = convertDisplayToRaw(displayLevel);
                    //              System.err.println ("display:" + displayLevel + " " 
                    //                                  + " raw:" + rawValue);
                    setLevelWithRawValue(rawValue);
                } catch (NumberFormatException nfe) {
                    userMessage("Incorrect format: "
                                + levelReadout.getText());
                } catch (Exception exc) {
                    logException("Setting level", exc);
                }
            }
        });

    }


    /**
     * Make the gui. Align it left
     *
     * @return The gui
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        return GuiUtils.left(doMakeWidgetComponent());
    }



    /**
     * This gets called by the DisplayControlImpl.doMakeWidgetComponent
     * (which is called by DisplayControlImpl.doMakeContents)
     * to make the GUI contents of this Control,
     * and allows this class to insert its own widgets as needed.
     * Makes color table chooser and slider for surface value
     *
     * @param controlWidgets    list of control widgets to populate
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);


        JPanel labelPanel =
            GuiUtils.wrap(GuiUtils.hflow(Misc.newList(levelReadout,
                levelLabel,
                GuiUtils.inset(sliderComps[0], new Insets(0, 5, 0, 0)))));
        // add a  composite widget with label, level slider, and readout
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Isosurface Value:"),
                GuiUtils.left(labelPanel)));


    }


    /**
     * Get the data RealType
     * @return  the data's realtype
     */
    private RealType getDataRealType() {
        return getGridDataInstance().getRealType(0);
    }

    /**
     * Get the default data unit.
     * @return  default unit
     */
    private Unit getDataUnit() {
        return getDataRealType().getDefaultUnit();
    }

    /**
     * Get the data range
     * @return  range for data
     */
    private Range getDataRange() {
        return getGridDataInstance().getRange(0);
    }


    /**
     * Convert a raw data value to the display unit
     *
     * @param rawValue   value to convert
     * @return  value in display units
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double convertRawToDisplay(double rawValue)
            throws VisADException, RemoteException {
        Real dispVal = new Real(getDataRealType(), rawValue);
        try {
            if ((getDisplayUnit() != null)
                    && Unit.canConvert(getDisplayUnit(), getDataUnit())) {
                return dispVal.getValue(getDisplayUnit());
            } else {
                return dispVal.getValue();
            }
        } catch (Exception exc) {
            logException("getting display value", exc);
        }
        return 0.0;
    }


    /**
     * Convert a display unit value to the raw data unit
     *
     * @param displayLevel      surface level in display units
     * @return  surface level in raw units
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double convertDisplayToRaw(double displayLevel)
            throws VisADException, RemoteException {
        Unit rawDataUnit = getDataUnit();
        if ((rawDataUnit != null) && (getDisplayUnit() != null)) {
            return rawDataUnit.toThis(displayLevel, getDisplayUnit());
        }
        return displayLevel;
    }



    /**
     * Convert "level" to getWhole number or integer value (for "large" values)
     * in user's display units, for use in slider label
     *
     * @param rawLevel    raw level
     * @return  level in whole values
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double getWholeDisplayValue(double rawLevel)
            throws VisADException, RemoteException {
        double displayLevel = convertRawToDisplay(rawLevel);
        //For now don't convert to a whole number
        if (true) {
            return displayLevel;
        }
        // round to getWhole number, if do not need finer precision;
        // value 50 is not special; is only a limit where integers will do
        if (getDataRange().getMax() > 50) {
            displayLevel = (double) ((int) (displayLevel + 0.5));
        }
        return displayLevel;
    }


    /**
     * Set the level value on the isosurface display and update all
     * of the UI widgets.
     *
     * @param rawLevel new isourface value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void setLevelWithRawValue(double rawLevel)
            throws VisADException, RemoteException {
        setLevelWithRawValue(rawLevel, true);
    }


    /**
     * Set the level value on the isosurface display and update all
     * of the UI widgets.
     *
     * @param rawLevel     new isourface value
     * @param andDoShare   If true then propagate this change.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setLevelWithRawValue(double rawLevel, boolean andDoShare)
            throws VisADException, RemoteException {
        lastRawLevel = rawLevel;
        // make slider value from the input "level"

        double  displayValue = getWholeDisplayValue(rawLevel);
        boolean lastIgnore   = ignoreUIEvents;
        ignoreUIEvents = true;
        adjustSliderLabel(displayValue);
        levelSliderPercent = getDataRange().getPercent(rawLevel);
        GuiUtils.setSliderPercent(levelSlider, levelSliderPercent);
        ignoreUIEvents = lastIgnore;


        // get units from input level type of parameter
        Unit rawDataUnit = getDataUnit();

        // make "newVal" from the value of displayValue - not from "level" - but
        // use units of "level"; reset "level" to the same value in its units.
        if ((rawDataUnit != null) && (getDisplayUnit() != null)) {
            Real newVal = new Real(getDataRealType(), displayValue,
                                   getDisplayUnit());
            rawLevel = newVal.getValue(rawDataUnit);
        }
        myDisplay.setSurfaceValue((float) rawLevel);
        updateDisplayList();
        if (andDoShare) {
            doShareExternal(SHARE_SURFACEVALUE, new Double(lastRawLevel));
        }
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
        try {
            if (dataId.equals(SHARE_SURFACEVALUE)) {
                setLevelWithRawValue(((Double) data[0]).doubleValue(), false);
            } else if (dataId.equals(SHARE_TRANSPARENCY)) {
                float newAlpha = ((Float) data[0]).floatValue();
                adjustTransparency(newAlpha, false);
            } else {
                super.receiveShareData(from, dataId, data);
            }
        } catch (Exception exc) {
            logException("Error in processing shared state:" + dataId, exc);
        }

    }



    /**
     * Reset the label that shows the level. The given displayValue
     * is in terms of the display unit.
     *
     * @param displayValue   value in display units
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void adjustSliderLabel(double displayValue)
            throws VisADException, RemoteException {
        String value;
        String unitLabel = "   ";
        if (getDisplayUnit() != null) {
            unitLabel = " " + getDisplayUnit();
        }
        value = getDisplayConventions().format(displayValue);
        //      System.err.println ("displayValue:" + displayValue + " formatted:" + value);
        lastReadout = value + unitLabel;
        boolean lastIgnore = ignoreUIEvents;
        ignoreUIEvents = true;
        levelReadout.setText(value);
        ignoreUIEvents = lastIgnore;
        levelLabel.setText(unitLabel);
        updateLegendLabel();
    }

    /** last readout value */
    private String lastReadout;


    /**
     * Override the base class method to add the value readout.
     *
     * @param labels   labels to add to
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        if (lastReadout != null) {
            labels.add("Value: " + lastReadout);
        }
    }

    /**
     * Get the transparency property value.  Only here for legacy bundles
     *
     * @return  alpha value.
     */
    public float getAlpha() {
        return alpha;
    }


    /**
     * Set the transparency property value, 0.0 to 1.0 only.  Only for
     * legacy bundles.
     *
     * @param alpha the transparency property value.
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }


    /*
     * Comment this out for now. We still leave around the set method
     * for legacy bundles.
     * public double getLevelSliderPercent() {
     * return levelSliderPercent;
     * }
     *
     */


    /**
     * Set the surfaces's value as a percentage of the slider range.
     *
     * @param level the surfaces's value.
     */
    public void setLevelSliderPercent(double level) {
        this.levelSliderPercent = level;
    }

    /**
     * Gets the level value (in raw data units) from the the levelSlider
     *
     * @return  surface value in raw data units
     */
    private double getRawLevelFromSlider() {
        levelSliderPercent = GuiUtils.getSliderPercent(levelSlider);
        return getDataRange().getValueOfPercent(levelSliderPercent);
    }




    /**
     * Adjust the transparency.  Not used except for legacy bundles
     *
     * @param newAlpha  new alpha value
     * @param andDoShare  true to share value
     */
    private void adjustTransparency(float newAlpha, boolean andDoShare) {
        try {
            this.alpha = newAlpha;

            float[][] table =
                ColorTable.changeTransparency(myDisplay.getColorPalette(),
                    alpha);

            ColorTable t = new ColorTable(getColorTable());
            t.setTableArray(table);
            setColorTable(t);
            if (andDoShare) {
                doShareExternal(SHARE_TRANSPARENCY, new Float(alpha));
            }
        } catch (Exception exc) {
            logException("Change transparency", exc);
        }
    }


    /**
     * Set the initial state of the JSlider of isosurface value,
     * with data limits, units, and labels.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void setSliderValues() throws VisADException, RemoteException {

        boolean lastIgnore = ignoreUIEvents;
        ignoreUIEvents = true;
        // provide default values used in the slider  
        // parms with real small ranges
        Range  r    = getDataRange();
        double span = r.getAbsSpan();

        // for switching to units preferred for display, 
        // first make these Reals
        Real dmin      = new Real(getDataRealType(), r.getMin());
        Real dmax      = new Real(getDataRealType(), r.getMax());
        Real dspan     = new Real(getDataRealType(), r.getAbsSpan());
        Unit rangeUnit = Unit.canConvert(getDisplayUnit(), getDataUnit())
                         ? getDisplayUnit()
                         : getDataUnit();
        span = dspan.getValue(rangeUnit);

        // Set JSlider values to approx. parm values; 
        // slider units are integer.
        // normal case where range of values fairly large;
        if (span > 50.0) {
            levelRange.set(dmin.getValue(rangeUnit),
                           dmax.getValue(rangeUnit) + 0.5);
            // round min value to multiple of 5 or 2 (user request)
            if (levelRange.getAbsSpan() < 40) {
                levelRange.setMin((double) (2 * (levelRange.getMinInt()
                        / 2)));
                if (levelRange.getMin() < 0) {
                    levelRange.setMin(levelRange.getMinInt() - 2);
                }
            } else {
                levelRange.setMin(5 * (levelRange.getMinInt() / 5));
                if (levelRange.getMinInt() < 0) {
                    levelRange.setMin(levelRange.getMinInt() - 5);
                }
            }

            levelSlider.setMinimum(levelRange.getMinInt());
            levelSlider.setMaximum(levelRange.getMaxInt());

            // make slider labels have 4 intervals and 5 numbers:
            int spacing = (levelRange.getSpanInt()) / 4;
            // spacing such as 4123.4 causes failure to label legibly; 
            // round as follows: 
            //   if between 10 and 100 use multiple of 10
            if ((spacing > 10) && (spacing <= 100)) {
                spacing = 10 * (spacing / 10);
            }

            //            System.err.println ("Spacing-1");
            //            spacing = spacing*100;
            //   if spacing > 100 use multiple of 100
            if (spacing > 100) {
                spacing = 100 * (spacing / 100);
            }


            levelSlider.setMajorTickSpacing(spacing);
            levelSlider.setMinorTickSpacing(spacing / 2);
            levelSlider.setPaintTicks(true);
            levelSlider.setLabelTable(
                levelSlider.createStandardLabels(spacing));
        } else {  // case of a smaller range; use float labels
            float fmin = 0.0f,
                  fmax = 100000.0f,
                  fmid = 50000.0f;
            fmin = (float) dmin.getValue(rangeUnit);
            fmax = (float) dmax.getValue(rangeUnit);
            fmid = (fmax + fmin) / 2;
            /*
            DecimalFormat valueFormat = new DecimalFormat("###.0");
            if (span < 5.0  && span >= 1.0 )
                valueFormat = new DecimalFormat("#.00");
            else if (span < 1.0 && span >= 0.1 )
                valueFormat = new DecimalFormat("#.000");
            else if  (span < 0.1)
                valueFormat = new DecimalFormat("0.#E0");
            //0.#E gives label like 2.5E-6
            */

            // make slider labels have 4 intervals and 5 numbers;
            // Hashtable argument of setLabelTable must have pairs of
            // location(), JComponent(e.g. a JLabel)
            float     spacing = (fmax - fmin) / 4;
            int       ispace  = (levelRange.getSpanInt()) / 4;
            Hashtable labels  = new Hashtable();
            for (int n = 0; n <= 4; n++) {
                labels.put(new Integer(levelRange.getMinInt() + n * ispace),
                           new JLabel(getDisplayConventions().format(fmin
                               + n * spacing)));
            }
            levelSlider.setLabelTable(labels);
        }

        GuiUtils.setSliderPercent(levelSlider, levelSliderPercent);
        ignoreUIEvents = lastIgnore;


    }





    /**
     * Add any display settings
     *
     * @param dsd the dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(new Double(getSurfaceValue()),
                             "levelWithRawValue", "Isosurface Value",
                             SETTINGS_GROUP_DISPLAY);
    }



    /**
     * Set the SurfaceValue property.
     *
     * @param value The new value for SurfaceValue
     */
    public void setSurfaceValue(double value) {
        surfaceValue = value;
    }

    /**
     * Get the SurfaceValue property.
     *
     * @return The SurfaceValue
     */
    public double getSurfaceValue() {
        if (myDisplay != null) {
            return myDisplay.getSurfaceValue();
        }
        return Double.NaN;
    }


    /**
     * Can this display control write out data.
     * @return true if it can
     */
    public boolean canExportData() {
        return true;
    }

    /**
     * Get the DisplayedData
     * @return the data or null
     *
     * @throws RemoteException problem reading remote data
     * @throws VisADException  problem gettting data
     */
    protected Data getDisplayedData() throws VisADException, RemoteException {
        if ((myDisplay == null) || (myDisplay.getData() == null)) {
            return null;
        }
        return myDisplay.getData();
    }

    /**
     * Is this a raster display?
     *
     * @return  true
     */
    public boolean getIsRaster() {
        return true;
    }

}

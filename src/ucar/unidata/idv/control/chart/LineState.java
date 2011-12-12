/*
 * $Id: LineState.java,v 1.23 2007/07/23 20:10:23 dmurray Exp $
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

package ucar.unidata.idv.control.chart;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import visad.DateTime;
import visad.Real;

import visad.Unit;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Holds  graphics state
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.23 $
 */
public class LineState {

    /** vertical position value */
    public static final int VPOS_TOP = 0;

    /** vertical position value */
    public static final int VPOS_MIDDLE = 1;

    /** vertical position value */
    public static final int VPOS_BOTTOM = 2;

    /** vertical position value */
    public static final int VPOS_NONE = 3;


    /** horizontal position value */
    public static final int HPOS_LEFT = 0;

    /** horizontal position value */
    public static final int HPOS_MIDDLE = 1;

    /** horizontal position value */
    public static final int HPOS_RIGHT = 2;

    /** horizontal position value */
    public static final int HPOS_NONE = 3;



    /** vertical positions */
    public static int[] VPOS_VALUES = { VPOS_TOP, VPOS_MIDDLE, VPOS_BOTTOM,
                                        VPOS_NONE };

    /** horizontal position labels */
    public static String[] VPOS_LABELS = { "Top", "Middle", "Bottom",
                                           "None (Use data if possible)" };

    /** horizontal positions */
    public static int[] HPOS_VALUES = { HPOS_LEFT, HPOS_MIDDLE, HPOS_RIGHT,
                                        HPOS_NONE };

    /** vertical position labels */
    public static String[] HPOS_LABELS = { "Left", "Middle", "Right",
                                           "None (Use data if possible)" };

    /** line type */
    public static final int LINETYPE_SHAPES = 1;

    /** Useful constant for specifying the type of rendering (lines only). */
    public static final int LINETYPE_LINES = 2;

    /**
     * Useful constant for specifying the type of rendering (shapes and lines).
     */
    public static final int LINETYPE_SHAPES_AND_LINES = 3;

    /** Useful constant for specifying the type of rendering (area only). */
    public static final int LINETYPE_AREA = 4;

    /**
     * Useful constant for specifying the type of rendering (area and shapes).
     */
    public static final int LINETYPE_AREA_AND_SHAPES = 5;

    /** line type */
    public static final int LINETYPE_BAR = 6;

    /** line types */
    public static final int[] LINETYPE_IDS = {
        LINETYPE_LINES, LINETYPE_SHAPES_AND_LINES, LINETYPE_SHAPES,
        LINETYPE_AREA, LINETYPE_AREA_AND_SHAPES, LINETYPE_BAR
    };

    /** line type labels */
    public static final String[] LINETYPE_LABELS = {
        "Lines", "Lines and shapes", "Shapes", "Area", "Area and shapes",
        "Bar Chart"
    };


    /** which side in legend */
    public static final int SIDE_UNDEFINED = 0;

    /** which side in legend */
    public static final int SIDE_LEFT = 1;

    /** which side in legend */
    public static final int SIDE_RIGHT = 2;

    /** which side in legend */
    public static final int[] SIDES = { SIDE_UNDEFINED, SIDE_LEFT,
                                        SIDE_RIGHT };

    /** which side in legend */
    public static final String[] SIDELABELS = { "Default", "Left", "Right" };



    /** which side in legend */
    private int side = SIDE_UNDEFINED;



    /** Stroke type */
    public static final int STROKE_SOLID = 0;

    /** Stroke type */
    public static final int STROKE_DOT = 1;

    /** Stroke type */
    public static final int STROKE_DASH = 2;

    /** Stroke type */
    public static final int STROKE_DOTDASH = 3;

    /** Stroke types */
    public static final int[] STROKE_IDS = { STROKE_SOLID, STROKE_DOT,
                                             STROKE_DASH, STROKE_DOTDASH };

    /** Stroke type labels */
    public static final String[] STROKE_LABELS = { "_______", ". . . .",
            "- - - -", ". - . - ." };


    /** shape type */
    public static final int SHAPE_PLUS = 0;

    /** shape type */
    public static final int SHAPE_X = 1;

    /** shape type */
    public static final int SHAPE_RECTANGLE = 2;

    /** shape type */
    public static final int SHAPE_POINT = 3;

    /** shape type */
    public static final int SHAPE_LARGEPOINT = 4;

    /** shape type */
    public static final int SHAPE_VLINE = 5;

    /** shape type */
    public static final int SHAPE_HLINE = 6;

    /** shape types */
    public static int[] SHAPES = {
        SHAPE_POINT, SHAPE_LARGEPOINT, SHAPE_RECTANGLE, SHAPE_PLUS, SHAPE_X,
        SHAPE_VLINE, SHAPE_HLINE
    };

    /** shape names */
    public static String[] SHAPE_NAMES = {
        "Point", "Large Point", "Rectangle", "Plus", "X", "Vertical Line",
        "Hor. Line"
    };



    /** The array of colors we cycle through */
    public static Color[] COLORS = {
        Color.red, Color.cyan, Color.magenta, Color.green, Color.orange,
        Color.yellow
    };


    /** The shape to draw */
    private int shape = SHAPE_POINT;

    /** Is this visible */
    private boolean visible = true;

    /** Is this valid. For example, in point data we may have a linestate for a paramater that does not exist */
    private boolean valid = true;

    /** Is this visible */
    private boolean axisVisible = true;

    /** The color */
    private Color color = Color.gray;

    /** The line width */
    private float width = 1.0f;

    /** The stroke type */
    private int strokeType = STROKE_SOLID;

    /** for gui */
    JCheckBox visibleCbx;

    /** for gui */
    JCheckBox axisVisibleCbx;

    /** for gui */
    JComboBox sideCbx;

    /** for gui */
    GuiUtils.ColorSwatch colorSwatch;

    /** for gui */
    JComboBox strokeCbx;

    /** for gui */
    JComboBox typeCbx;

    /** for gui */
    JComboBox verticalPositionCbx;

    /** for gui */
    JComboBox horizontalPositionCbx;

    /** for gui */
    JRadioButton linearRangeBtn;

    /** for gui */
    JRadioButton logRangeBtn;

    /** for gui */
    JTextField widthFld;

    /** for gui */
    JComboBox shapeBox;

    /** for gui */
    JComboBox chartNameBox;

    /** name field */
    JTextField nameField;

    /** for gui */
    JTextField minRangeFld;

    /** for gui */
    JTextField maxRangeFld;

    /** for gui */
    JButton popupRangeBtn;

    /** for gui */
    JCheckBox includesZeroCbx;

    /** for gui */
    boolean rangeIncludesZero = false;

    /** for gui */
    boolean useLogarithmicRange = false;

    /** for gui */
    Range range;

    /** line type */
    private int lineType = LINETYPE_LINES;

    /** vert position */
    private int verticalPosition = VPOS_BOTTOM;

    /** horizontal position */
    private int horizontalPosition = HPOS_MIDDLE;


    /** the name of the chart we draw in */
    private String chartName = null;

    /** my name */
    private String name;

    /** my name */
    private String nameMacro;

    /** axis label */
    private String axisLabel;

    /** index */
    protected int index;

    /** display unit */
    protected Unit unit;

    /** flag for visible in legend property */
    private boolean visibleInLegend = true;

    /** flag for use vertical for positions */
    private boolean useVerticalPosition = true;

    /**
     * Default ctor
     */
    public LineState() {}


    /**
     * Constructor
     *
     *
     * @param color color
     * @param width line width
     * @param strokeType stroke
     */
    public LineState(Color color, float width, int strokeType) {
        this(color, width, strokeType, LINETYPE_LINES, SHAPE_POINT);
    }


    /**
     * Constructor
     *
     * @param color color
     * @param width line width
     * @param strokeType stroke
     * @param lineType   line type
     * @param shapeType   shape type
     */
    public LineState(Color color, float width, int strokeType, int lineType,
                     int shapeType) {
        this.color      = color;
        this.width      = width;
        this.strokeType = strokeType;
        this.lineType   = lineType;
        this.shape      = shapeType;
    }



    /**
     * Create the properties contents
     *
     *
     * @return Contents for properties dialog
     */
    protected JComponent getPropertyContents() {
        return getPropertyContents(false, null, null);
    }




    /**
     * show dialog
     *
     * @param listener listens for changes
     * @param chartNames all chart names. We use this to show a combo box of charts
     * @param ranges All ranges. Some candy for the user so they can set the range values to use on multiple line states
     *
     * @return ok
     */
    public boolean showPropertiesDialog(
            final PropertyChangeListener listener, List chartNames,
            List ranges) {
        return showPropertiesDialog(listener, true, true, chartNames, ranges);
    }


    /**
     * show dialog
     *
     * @param propListener listens for changes
     * @param doVisible show visible cbx
     * @param doRange show ranges
     * @param chartNames  all chart names. We use this to show a combo box of charts
     * @param sampleRanges All ranges. Some candy for the user so they can set the range values to use on multiple line states
     *
     * @return ok
     */
    public boolean showPropertiesDialog(
            final PropertyChangeListener propListener, boolean doVisible,
            boolean doRange, List chartNames, List sampleRanges) {
        JComponent comp = getPropertyContents(true, chartNames, sampleRanges);
        final JDialog dialog = GuiUtils.createDialog(null,
                                   getName() + " " + "Properties", true);
        final boolean[] ok       = { false };
        ActionListener  listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    if ( !applyProperties()) {
                        return;
                    }
                    ok[0] = true;
                    propListener.propertyChange(new PropertyChangeEvent(this,
                            "properties", this, null));
                }

                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    dialog.dispose();
                }

            }
        };
        JComponent buttons  = GuiUtils.makeApplyOkCancelButtons(listener);
        JComponent contents = GuiUtils.centerBottom(comp, buttons);
        dialog.getContentPane().add(contents);
        GuiUtils.showInCenter(dialog);
        return ok[0];
    }


    /** list of times */
    private List<DateTime> times;

    /** list of values */
    private List<Real> values;

    /**
     * Get the list of times
     *
     * @return the list of times
     */
    public List<DateTime> getTimes() {
        return times;
    }

    /**
     * Get the list of values
     *
     * @return the list of values
     */
    public List<Real> getValues() {
        return values;
    }

    /**
     * Set the track with a list of times and values
     *
     * @param times  the times
     * @param values the values
     */
    public void setTrack(List<DateTime> times, List<Real> values) {
        this.times  = times;
        this.values = values;
    }


    /**
     * Create the properties contents
     *
     * @param full show fill dialog
     * @param chartNames chart names
     * @param sampleRanges ranges
     * @return Contents for properties dialog
     */
    protected JComponent getPropertyContents(boolean full, List chartNames,
                                             List sampleRanges) {

        List comps = new ArrayList();
        visibleCbx     = new JCheckBox((full
                                        ? "Full Plot"
                                        : "Visible"), visible);
        axisVisibleCbx = new JCheckBox("Axis", visible);
        if (full) {
            comps.add(GuiUtils.rLabel("Visiblity:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(visibleCbx, axisVisibleCbx,
                    5)));
        } else {}

        String nameToShow = name;
        if ((nameMacro != null) && (nameMacro.length() > 0)) {
            nameToShow = nameMacro;
        }

        nameField = new JTextField(nameToShow, 30);
        nameField.setToolTipText("Name of chart entry. Use \""
                                 + TimeSeriesChart.MACRO_PARAMETER
                                 + "\" for parameter name");
        comps.add(GuiUtils.rLabel("Legend Label:"));
        comps.add(GuiUtils.left(nameField));

        if (chartNames != null) {
            chartNameBox = new JComboBox(new Vector(chartNames));
            if (chartName != null) {
                chartNameBox.setSelectedItem(chartName);
            }
            chartNameBox.setEditable(true);
            comps.add(GuiUtils.rLabel("Chart Name:"));
            comps.add(GuiUtils.left(chartNameBox));
        }

        this.sideCbx = GuiUtils.makeComboBox(SIDES, SIDELABELS, getSide());
        if (full) {
            comps.add(GuiUtils.rLabel("Axis Side:"));
            comps.add(GuiUtils.left(sideCbx));
        }

        if (full) {
            popupRangeBtn = GuiUtils.makeButton("...", this,
                    "showRangePopup", sampleRanges);

            minRangeFld = new JTextField(((range != null)
                                          ? range.getMin() + ""
                                          : ""), 5);
            maxRangeFld = new JTextField(((range != null)
                                          ? range.getMax() + ""
                                          : ""), 5);
            List rangeComps = Misc.newList(new JLabel("Min:"), minRangeFld,
                                           new JLabel("Max:"), maxRangeFld);
            if ((sampleRanges != null) && (sampleRanges.size() > 0)) {
                rangeComps.add(popupRangeBtn);
            }


            rangeComps.add(includesZeroCbx = new JCheckBox("Includes Zero",
                    rangeIncludesZero));

            comps.add(GuiUtils.rLabel("Range:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(rangeComps, 5)));


            linearRangeBtn = new JRadioButton("Linear", !useLogarithmicRange);
            logRangeBtn = new JRadioButton("Logarithmic",
                                           useLogarithmicRange);
            GuiUtils.buttonGroup(linearRangeBtn, logRangeBtn);
            //            comps.add(GuiUtils.rLabel("Range Type:"));
            //            comps.add(GuiUtils.left(GuiUtils.hbox(linearRangeBtn, logRangeBtn)));





        }




        typeCbx = GuiUtils.makeComboBox(LINETYPE_IDS, LINETYPE_LABELS,
                                        lineType);
        shapeBox = GuiUtils.makeComboBox(SHAPES, SHAPE_NAMES, shape);

        if (full) {
            comps.add(GuiUtils.rLabel("Type:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(typeCbx,
                    new JLabel("  Shape:"), shapeBox, 5)));
        }


        strokeCbx = GuiUtils.makeComboBox(STROKE_IDS, STROKE_LABELS,
                                          strokeType);
        Font f = Font.decode("monospaced-BOLD");
        if (f != null) {
            strokeCbx.setFont(f);
        }
        widthFld = new JTextField("" + width, 3);
        JComponent[] bg = GuiUtils.makeColorSwatchWidget(color, "");
        colorSwatch = (GuiUtils.ColorSwatch) bg[0];

        comps.add(GuiUtils.rLabel("Line:"));
        List lineComps = new ArrayList();
        if ( !full) {
            lineComps.add(visibleCbx);
        }
        lineComps.add(new JLabel("Stroke:"));
        lineComps.add(strokeCbx);
        lineComps.add(new JLabel("  "));

        lineComps.add(new JLabel("Width:"));
        lineComps.add(widthFld);
        lineComps.add(new JLabel("  "));

        lineComps.add(new JLabel("Color:"));
        lineComps.add(colorSwatch);
        comps.add(GuiUtils.left(GuiUtils.hbox(lineComps, 5)));


        if (full) {
            if (getUseVerticalPosition()) {
                comps.add(GuiUtils.rLabel("Vertical Position:"));
                comps.add(GuiUtils.left(verticalPositionCbx =
                    GuiUtils.makeComboBox(VPOS_VALUES, VPOS_LABELS,
                                          verticalPosition)));
            } else {
                comps.add(GuiUtils.rLabel("Horizontal Position:"));
                comps.add(GuiUtils.left(horizontalPositionCbx =
                    GuiUtils.makeComboBox(HPOS_VALUES, HPOS_LABELS,
                                          horizontalPosition)));
            }
        }


        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        return GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
    }


    /**
     * popup range menu
     *
     * @param ranges ranges
     */
    public void showRangePopup(List ranges) {
        List items = new ArrayList();
        for (int i = 0; i < ranges.size(); i++) {
            Range r = (Range) ranges.get(i);
            items.add(GuiUtils.makeMenuItem(r.toString(), this,
                                            "setRangeInProperties", r));
        }
        GuiUtils.showPopupMenu(items, popupRangeBtn);
    }

    /**
     * apply range to gui
     *
     * @param r range
     */
    public void setRangeInProperties(Range r) {
        minRangeFld.setText(Misc.format(r.getMin()));
        maxRangeFld.setText(Misc.format(r.getMax()));
    }


    /**
     * Apply properties
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        if (colorSwatch != null) {
            color = colorSwatch.getSwatchColor();
        }
        if (strokeCbx != null) {
            strokeType = GuiUtils.getValueFromBox(strokeCbx);
        }
        if (typeCbx != null) {
            lineType = GuiUtils.getValueFromBox(typeCbx);
        }
        if (sideCbx != null) {
            side = GuiUtils.getValueFromBox(sideCbx);
        }
        if (verticalPositionCbx != null) {
            verticalPosition = GuiUtils.getValueFromBox(verticalPositionCbx);
        }
        if (horizontalPositionCbx != null) {
            horizontalPosition =
                GuiUtils.getValueFromBox(horizontalPositionCbx);
        }
        if (shapeBox != null) {
            shape = GuiUtils.getValueFromBox(shapeBox);
        }

        if (nameMacro != null) {
            nameMacro = nameField.getText().trim();
        } else {
            name = nameField.getText().trim();
        }

        if (chartNameBox != null) {
            chartName = (String) chartNameBox.getSelectedItem();
        }
        if (includesZeroCbx != null) {
            rangeIncludesZero = includesZeroCbx.isSelected();
        }
        if (minRangeFld != null) {
            String minS = minRangeFld.getText().trim();
            String maxS = maxRangeFld.getText().trim();
            if ((minS.length() == 0) || (maxS.length() == 0)) {
                range = null;
            } else {
                range = new Range(Misc.parseNumber(minS),
                                  Misc.parseNumber(maxS));
            }
        }

        if (linearRangeBtn != null) {
            useLogarithmicRange = !linearRangeBtn.isSelected();
        }

        try {
            if (widthFld != null) {
                width = new Float(widthFld.getText().trim()).floatValue();
            }
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad width: " + widthFld.getText());
            return false;
        }
        if (visibleCbx != null) {
            visible = visibleCbx.isSelected();
        }
        if (axisVisibleCbx != null) {
            axisVisible = axisVisibleCbx.isSelected();
        }
        return true;
    }




    /**
     * Utility to get a color in the static list of colors
     *
     * @param index which color
     *
     * @return the color
     */
    public static Color getFixedColor(int index) {
        int newIndex = index % COLORS.length;
        return COLORS[newIndex];
    }

    /**
     * Create a stroke
     *
     * @return a stroke
     */
    public BasicStroke getStroke() {
        return makeStroke(width, strokeType);
    }




    /**
     * create a stroke
     *
     * @param width line width
     * @param type stroke type
     *
     * @return a stroke
     */
    public static BasicStroke makeStroke(float width, int type) {
        float[] pattern;
        if (type == STROKE_SOLID) {
            pattern = new float[] { 1000.0f };
        } else if (type == STROKE_DASH) {
            pattern = new float[] { 5.0f, 2.0f };
        } else if (type == STROKE_DOT) {
            pattern = new float[] { 2.0f, 2.0f };
        } else {
            pattern = new float[] { 2.0f, 2.0f, 5.0f, 2.0f };
        }

        return new BasicStroke(width, BasicStroke.CAP_BUTT,
                               BasicStroke.JOIN_BEVEL, 0.0f, pattern, 0.0f);
    }



    /**
     * get the shape we use to paint
     *
     * @return paint shape
     */
    public Shape getPaintShape() {
        int         w  = 10;
        int         w2 = w / 2;
        GeneralPath path;
        switch (shape) {

          case LineState.SHAPE_VLINE :
              return new Polygon(new int[] { 0, 0 }, new int[] { -w, w }, 2);

          case LineState.SHAPE_HLINE :
              return new Polygon(new int[] { -w2, w }, new int[] { 0, 0 }, 2);

          case LineState.SHAPE_POINT :
              return new Rectangle(-1, -1, 2, 2);

          case LineState.SHAPE_LARGEPOINT :
              return new Rectangle(-2, -2, 4, 4);

          case LineState.SHAPE_RECTANGLE :
              return new Rectangle(-w2, -w2, w, w);

          case LineState.SHAPE_X :
              path = new GeneralPath();
              path.append(new Polygon(new int[] { -w2, w2 }, new int[] { -w2,
                      w2 }, 2), false);

              path.append(new Polygon(new int[] { w2, -w2 }, new int[] { -w2,
                      w2 }, 2), false);
              return path;

          case LineState.SHAPE_PLUS :
              path = new GeneralPath();
              path.append(new Polygon(new int[] { 0, 0 }, new int[] { -w2,
                      w2 }, 2), false);

              path.append(new Polygon(new int[] { -w2, w2 }, new int[] { 0,
                      0 }, 2), false);

              return path;
        }
        return null;
    }





    /**
     * Set the Visible property.
     *
     * @param value The new value for Visible
     */
    public void setVisible(boolean value) {
        visible = value;
    }

    /**
     * Get the Visible property.
     *
     * @return The Visible
     */
    public boolean getVisible() {
        return visible;
    }

    /**
     * Set the Color property.
     *
     * @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
    }

    /**
     * Get the Color property.
     *
     * @return The Color
     */
    public Color getColor() {
        return color;
    }

    /**
     * if we have a color then return it. Else get index'th color
     *
     * @param index index
     *
     * @return color
     */
    public Color getColor(int index) {
        if (color == null) {
            color = LineState.getFixedColor(index);
        }
        return color;
    }



    /**
     * Set the Width property.
     *
     * @param value The new value for Width
     */
    public void setWidth(float value) {
        width = value;
    }

    /**
     * Get the Width property.
     *
     * @return The Width
     */
    public float getWidth() {
        return width;
    }

    /**
     * set stroke
     *
     * @param value value
     */
    public void setStrokeType(Integer value) {
        setStrokeType(value.intValue());
    }


    /**
     * Set the StrokeType property.
     *
     * @param value The new value for StrokeType
     */
    public void setStrokeType(int value) {
        strokeType = value;
    }

    /**
     * Get the StrokeType property.
     *
     * @return The StrokeType
     */
    public int getStrokeType() {
        return strokeType;
    }


    /**
     *  Set the Shape property.
     *
     *  @param value The new value for Shape
     */
    public void setShape(int value) {
        shape = value;
    }

    /**
     *  Get the Shape property.
     *
     *  @return The Shape
     */
    public int getShape() {
        return shape;
    }

    /**
     * Set the RangeIncludesZero property.
     *
     * @param value The new value for RangeIncludesZero
     */
    public void setRangeIncludesZero(boolean value) {
        rangeIncludesZero = value;
    }

    /**
     * Get the RangeIncludesZero property.
     *
     * @return The RangeIncludesZero
     */
    public boolean getRangeIncludesZero() {
        return rangeIncludesZero;
    }


    /**
     * Set the Range property.
     *
     * @param value The new value for Range
     */
    public void setRange(Range value) {
        range = value;
    }

    /**
     * Get the Range property.
     *
     * @return The Range
     */
    public Range getRange() {
        return range;
    }

    /**
     * Set the UseLogarithmicRange property.
     *
     * @param value The new value for UseLogarithmicRange
     */
    public void setUseLogarithmicRange(boolean value) {
        useLogarithmicRange = value;
    }

    /**
     * Get the UseLogarithmicRange property.
     *
     * @return The UseLogarithmicRange
     */
    public boolean getUseLogarithmicRange() {
        return useLogarithmicRange;
    }

    /**
     * Set the LineType property.
     *
     * @param value The new value for LineType
     */
    public void setLineType(int value) {
        lineType = value;
    }

    /**
     * Get the LineType property.
     *
     * @return The LineType
     */
    public int getLineType() {
        return lineType;
    }

    /**
     * Set the chart name property.
     *
     * @param value The new value for chart name
     */
    public void setChartName(String value) {
        chartName = value;
    }

    /**
     * Get the ChartName property.
     *
     * @return The ChartName
     */
    public String getChartName() {
        return chartName;
    }

    /**
     * Set the name if needed
     *
     * @param value true to set the name
     */
    public void setNameIfNeeded(String value) {
        if ((name == null) || (name.length() == 0)) {
            setName(value);
        }
    }

    /**
     * Does this linestate have a name defined
     *
     * @return has a name
     */
    public boolean hasName() {
        if ((name == null) || (name.length() == 0)) {
            return false;
        }
        return true;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the NameMacro property.
     *
     *  @param value The new value for NameMacro
     */
    public void setNameMacro(String value) {
        this.nameMacro = value;
    }

    /**
     *  Get the NameMacro property.
     *
     *  @return The NameMacro
     */
    public String getNameMacro() {
        return this.nameMacro;
    }



    /**
     * Set the VerticalPosition property.
     *
     * @param value The new value for VerticalPosition
     */
    public void setVerticalPosition(int value) {
        verticalPosition = value;
    }

    /**
     * Get the VerticalPosition property.
     *
     * @return The VerticalPosition
     */
    public int getVerticalPosition() {
        return verticalPosition;
    }

    /**
     * Set the HorizontalPosition property.
     *
     * @param value The new value for HorizontalPosition
     */
    public void setHorizontalPosition(int value) {
        horizontalPosition = value;
    }

    /**
     * Get the HorizontalPosition property.
     *
     * @return The HorizontalPosition
     */
    public int getHorizontalPosition() {
        return horizontalPosition;
    }

    /**
     * Set the Side property.
     *
     * @param value The new value for Side
     */
    public void setSide(int value) {
        side = value;
    }

    /**
     * Get the Side property.
     *
     * @return The Side
     */
    public int getSide() {
        return side;
    }

    /**
     * Set the AxisVisible property.
     *
     * @param value The new value for AxisVisible
     */
    public void setAxisVisible(boolean value) {
        axisVisible = value;
    }

    /**
     * Get the AxisVisible property.
     *
     * @return The AxisVisible
     */
    public boolean getAxisVisible() {
        return axisVisible;
    }

    /**
     * Set the Valid property.
     *
     * @param value The new value for Valid
     */
    public void setValid(boolean value) {
        valid = value;
    }

    /**
     * Get the Valid property.
     *
     * @return The Valid
     */
    public boolean getValid() {
        return valid;
    }

    /**
     *  Set the AxisLabel property.
     *
     *  @param value The new value for AxisLabel
     */
    public void setAxisLabel(String value) {
        axisLabel = value;
    }

    /**
     *  Get the AxisLabel property.
     *
     *  @return The AxisLabel
     */
    public String getAxisLabel() {
        return axisLabel;
    }


    /**
     *  Set the VisibleInLegend property.
     *
     *  @param value The new value for VisibleInLegend
     */
    public void setVisibleInLegend(boolean value) {
        visibleInLegend = value;
    }

    /**
     *  Get the VisibleInLegend property.
     *
     *  @return The VisibleInLegend
     */
    public boolean getVisibleInLegend() {
        return visibleInLegend;
    }

    /**
     *  Set the UseVertical property.
     *
     *  @param value The new value for UseVertical
     */
    public void setUseVerticalPosition(boolean value) {
        useVerticalPosition = value;
    }

    /**
     *  Get the UseVertical property.
     *
     *  @return The UseVertical
     */
    public boolean getUseVerticalPosition() {
        return useVerticalPosition;
    }


}



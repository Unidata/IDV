/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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

package ucar.unidata.gis.maps;


import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.drawing.Glyph;
import ucar.unidata.util.Misc;

import ucar.visad.display.LatLonLabels;
import ucar.visad.display.TextDisplayable;

import visad.TextControl;
import visad.TextType;
import visad.VisADException;


import java.awt.Color;
import java.awt.Font;

import java.rmi.RemoteException;



/**
 * A data structure to hold display attributes for lat/lon labels
 */


public class LatLonLabelData {

    /** The lat lon labels. We create this when asked. */
    protected LatLonLabels myLatLonLabels;

    /** Is the lat/lon visible */
    private boolean visible = true;

    /** The color */
    private Color color = Color.white;

    /** The alignment point */
    private String alignment = Glyph.PT_MM;

    /** The label font */
    private Object labelFont;

    /** Is this data representing latitude or longitude */
    private boolean isLatitude = true;

    /** The labels interval */
    private float interval;

    /** The lat/lon min value */
    private float minValue;

    /** The lat/lon max value */
    private float maxValue;

    /** The lat/lon base value */
    private float baseValue;

    /** The label lines */
    private float[] labelLines;

    /** fast rendering flag */
    protected boolean fastRendering = false;

    /** sphere flag */
    private boolean useSphere;

    /** format flag */
    private String labelFormat = "DD";

    /** use360 flag */
    private boolean use360 = false;


    /**
     * Default ctor
     */
    public LatLonLabelData() {}


    /**
     * The ctor
     *
     * @param isLatitude Is it lat or lon
     * @param interval The interval
     *
     */
    public LatLonLabelData(boolean isLatitude, float interval) {
        this(isLatitude, interval, (isLatitude
                                    ? -90
                                    : -180), (isLatitude
                ? 90
                : 180), 0, new float[] { 0 }, Glyph.PT_MM, null, Color.white,
                        true);
    }

    /**
     * The ctor
     *
     * @param isLatitude Is it lat or lon
     * @param interval The interval
     * @param min minimum value
     * @param max maximum value
     * @param base base value
     *
     */
    public LatLonLabelData(boolean isLatitude, float interval, float min,
                           float max, float base) {
        this(isLatitude, interval, min, max, base, new float[] { 0 },
             Glyph.PT_MM, null, Color.white, true);
    }

    /**
     * The ctor
     *
     * @param isLatitude Is it lat or lon
     * @param interval The interval
     * @param minValue  The min value
     * @param maxValue  The max value
     * @param baseValue The base value
     * @param labelLines the label lines
     * @param alignment  the alignment #ucar.unidata.ui.drawing.Glyph
     * @param labelFont  the font (Font or HersheyFont)
     * @param color The color
     * @param fastRendering true to use fast rendering
     *
     */
    public LatLonLabelData(boolean isLatitude, float interval,
                           float minValue, float maxValue, float baseValue,
                           float[] labelLines, String alignment,
                           Object labelFont, Color color,
                           boolean fastRendering) {
        this.isLatitude    = isLatitude;
        this.interval      = interval;
        this.maxValue      = maxValue;
        this.minValue      = minValue;
        this.labelLines    = labelLines;
        this.alignment     = alignment;
        this.labelFont     = labelFont;
        this.color         = color;
        this.fastRendering = fastRendering;
    }


    /**
     * Copy constructor
     *
     * @param that the other latlondata object
     */
    protected LatLonLabelData(LatLonLabelData that) {
        initWith(that);
    }


    /**
     * Initialize this object with the state from the given LatLonLabelData
     *
     * @param that The other object
     */
    public void initWith(LatLonLabelData that) {
        if (that == null) {
            return;
        }
        this.isLatitude    = that.isLatitude;
        this.interval      = that.interval;
        this.baseValue     = that.baseValue;
        this.maxValue      = that.maxValue;
        this.minValue      = that.minValue;
        this.labelLines    = that.labelLines;
        this.alignment     = that.alignment;
        this.labelFont     = that.labelFont;
        this.color         = that.color;
        this.fastRendering = that.fastRendering;
        this.visible       = that.visible;
        this.useSphere     = that.useSphere;
        this.labelFormat   = that.labelFormat;
        this.use360        = that.use360;
    }


    /**
     *  Gets called when any of the state has been changed.
     *  Is here so a subclass can be easily notified when something changes.
     */
    protected void stateChanged() {}


    /**
     * Create, if needed, initialize and return the latlonlines object
     * @return The {@link ucar.visad.display.LatLonLabels} object
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public LatLonLabels getLatLonLabels()
            throws VisADException, RemoteException {
        if (myLatLonLabels == null) {
            myLatLonLabels = new LatLonLabels("LatLonLabels",
                    TextType.getTextType((isLatitude
                                          ? "LatLabels"
                                          : "LonLabels")), isLatitude,
                                          interval, minValue, maxValue,
                                          baseValue, labelLines);

        }
        if (color != null) {
            myLatLonLabels.setColor(color);
        }
        myLatLonLabels.setVisible(getRealVisibility());
        myLatLonLabels.setInterval(interval);
        myLatLonLabels.setMin(minValue);
        myLatLonLabels.setMax(maxValue);
        myLatLonLabels.setBase(baseValue);
        myLatLonLabels.setLabelLines(labelLines);
        setFont(myLatLonLabels, labelFont);
        setAlignment(myLatLonLabels, alignment);
        myLatLonLabels.setUseFastRendering(fastRendering);
        myLatLonLabels.setSphere(useSphere);
        myLatLonLabels.setLabelFormat(labelFormat);
        myLatLonLabels.setUse360(use360);
        return myLatLonLabels;
    }

    /**
     * Set the font on the Displayable
     *
     * @param lll   the displayable
     * @param labelFont  the font Object
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem setting value
     */
    private void setFont(LatLonLabels lll, Object labelFont)
            throws VisADException, RemoteException {
        Font f    = (Font) labelFont;
        int  size = (f == null)
                    ? 12
                    : f.getSize();
        if ((f != null) && f.getName().equals(FontSelector.DEFAULT_NAME)) {
            f = null;
        }
        lll.setFont(f);
        lll.setTextSize(size / 12.f);
    }

    /**
     * Set the alignment point
     *
     * @param lll  the Displayble
     * @param rectpoint  the alignment
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem setting value
     */
    private void setAlignment(LatLonLabels lll, String rectpoint)
            throws VisADException, RemoteException {
        if (rectpoint.equals(Glyph.PT_UL)) {
            lll.setJustification(TextControl.Justification.RIGHT);
            lll.setVerticalJustification(TextControl.Justification.BOTTOM);
        } else if (rectpoint.equals(Glyph.PT_UM)) {
            lll.setJustification(TextControl.Justification.CENTER);
            lll.setVerticalJustification(TextControl.Justification.BOTTOM);
        } else if (rectpoint.equals(Glyph.PT_UR)) {
            lll.setJustification(TextControl.Justification.LEFT);
            lll.setVerticalJustification(TextControl.Justification.BOTTOM);
        } else if (rectpoint.equals(Glyph.PT_ML)) {
            lll.setJustification(TextControl.Justification.RIGHT);
            lll.setVerticalJustification(TextControl.Justification.CENTER);
        } else if (rectpoint.equals(Glyph.PT_MM)) {
            lll.setJustification(TextControl.Justification.CENTER);
            lll.setVerticalJustification(TextControl.Justification.CENTER);
        } else if (rectpoint.equals(Glyph.PT_MR)) {
            lll.setJustification(TextControl.Justification.LEFT);
            lll.setVerticalJustification(TextControl.Justification.CENTER);
        } else if (rectpoint.equals(Glyph.PT_LL)) {
            lll.setJustification(TextControl.Justification.RIGHT);
            lll.setVerticalJustification(TextControl.Justification.TOP);
        } else if (rectpoint.equals(Glyph.PT_LM)) {
            lll.setJustification(TextControl.Justification.CENTER);
            lll.setVerticalJustification(TextControl.Justification.TOP);
        } else if (rectpoint.equals(Glyph.PT_LR)) {
            lll.setJustification(TextControl.Justification.LEFT);
            lll.setVerticalJustification(TextControl.Justification.TOP);
        }
    }

    /**
     * Meant to be overwrote by a derived class that needs to
     * determine its own visibility.
     *
     * @return The actual visibility to apply to the lines
     */
    protected boolean getRealVisibility() {
        return visible;
    }



    /**
     *  Set the Color property.
     *
     *  @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
        stateChanged();
    }

    /**
     *  Get the Color property.
     *
     *  @return The Color
     */
    public Color getColor() {
        return color;
    }

    /**
     *  Set the Interval property.
     *
     *  @param value The new value for Interval
     */
    public void setInterval(float value) {
        interval = value;
        stateChanged();
    }

    /**
     *  Get the Interval property.
     *
     *  @return The Interval
     */
    public float getInterval() {
        return interval;
    }

    /**
     *  Set the BaseValue property.
     *
     *  @param value The new value for BaseValue
     */
    public void setBaseValue(float value) {
        baseValue = value;
        stateChanged();
    }

    /**
     *  Get the BaseValue property.
     *
     *  @return The BaseValue
     */
    public float getBaseValue() {
        return baseValue;
    }

    /**
     *  Set the IsLatitude property.
     *
     *  @param value The new value for IsLatitude
     */
    public void setIsLatitude(boolean value) {
        isLatitude = value;
    }

    /**
     *  Get the IsLatitude property.
     *
     *  @return The IsLatitude
     */
    public boolean getIsLatitude() {
        return isLatitude;
    }


    /**
     *  Set the MinValue property.
     *
     *  @param value The new value for MinValue
     */
    public void setMinValue(float value) {
        minValue = value;
        stateChanged();
    }

    /**
     *  Get the MinValue property.
     *
     *  @return The MinValue
     */
    public float getMinValue() {
        return minValue;
    }

    /**
     *  Set the MaxValue property.
     *
     *  @param value The new value for MaxValue
     */
    public void setMaxValue(float value) {
        maxValue = value;
        stateChanged();
    }

    /**
     *  Get the MaxValue property.
     *
     *  @return The MaxValue
     */
    public float getMaxValue() {
        return maxValue;
    }

    /**
     *  Set the LabelLines property.
     *
     *  @param value The new value for LabelLines
     */
    public void setLabelLines(float[] value) {
        labelLines = value;
        stateChanged();
    }

    /**
     *  Set the LabelLines from a string
     *
     *  @param value The new string for LabelLines
     */
    public void setLabelsLineString(String value) {
        labelLines = parseLabelLineString(value);
        stateChanged();
    }

    /**
     * Parse the label line string
     *
     * @param llString  the label line string
     *
     * @return the array of float values
     */
    public static float[] parseLabelLineString(String llString) {
        if (llString.indexOf(";") > 0) {
            return Misc.parseFloats(llString, ";");
        } else {
            return Misc.parseFloats(llString, ",");
        }
    }

    /**
     * Format the label lines as a string
     *
     * @param vals  the values
     *
     * @return  the string
     */
    public static String formatLabelLines(float[] vals) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < vals.length; i++) {
            buf.append(vals[i]);
            if (i < vals.length - 1) {
                buf.append(";");
            }
        }
        return buf.toString();
    }

    /**
     *  Get the LabelLines property.
     *
     *  @return The LabelLines
     */
    public float[] getLabelLines() {
        return labelLines;
    }

    /**
     *  Set the Visible property.
     *
     *  @param value The new value for Visible
     */
    public void setVisible(boolean value) {
        visible = value;
        stateChanged();
    }

    /**
     *  Get the Visible property.
     *
     *  @return The Visible
     */
    public boolean getVisible() {
        return visible;
    }


    /**
     *  Set the FastRendering property.
     *
     *  @param value The new value for FastRendering
     */
    public void setFastRendering(boolean value) {
        fastRendering = value;
        stateChanged();
    }

    /**
     *  Get the FastRendering property.
     *
     *  @return The FastRendering
     */
    public boolean getFastRendering() {
        return fastRendering;
    }

    /**
     *  Set the Font property.
     *
     *  @param value The new value for Font
     */
    public void setFont(Object value) {
        labelFont = value;
        stateChanged();
    }

    /**
     *  Get the Font property.
     *
     *  @return The Font
     */
    public Object getFont() {
        return labelFont;
    }

    /**
     *  Set the Alignment property.
     *
     *  @param value The new value for Alignment
     */
    public void setAlignment(String value) {
        alignment = value;
        stateChanged();
    }

    /**
     *  Get the Alignment property.
     *
     *  @return The Alignment
     */
    public String getAlignment() {
        return alignment;
    }

    /**
     * Set the whether we're on a sphere
     * @param sphere  true if sphere
     */
    public void setSphere(boolean sphere) {
        useSphere = sphere;
        stateChanged();
    }

    /**
     * Get the sphere property
     * @return true if sphere
     */
    public boolean getSphere() {
        return useSphere;
    }

    /**
     * Set the label format (DD, DD:MM, etc)
     * @param format  the label format
     */
    public void setLabelFormat(String format) {
        labelFormat = format;
        stateChanged();
    }

    /**
     * Get the label format property
     * @return  the label format
     */
    public String getLabelFormat() {
        return labelFormat;
    }

    /**
     *  Set the use360 property.
     *
     *  @param value The new value for use360
     */
    public void setUse360(boolean value) {
        use360 = value;
        stateChanged();
    }

    /**
     *  Get the Use360 property.
     *
     *  @return The Use360
     */
    public boolean getUse360() {
        return use360;
    }

}

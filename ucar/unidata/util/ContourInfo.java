/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.unidata.util;


import java.util.List;
import java.util.StringTokenizer;


/**
 * A class to hold and transfer contour level settings, as to and from the
 * dialog box ContLevelDialog.
 */

public class ContourInfo {

    /** Default contour labeling setting */
    public final static boolean DEFAULT_LABEL = true;

    /** Default contour labeling alignment setting */
    public final static boolean DEFAULT_LABEL_ALIGNMENT = true;

    /** Default contour dashing setting */
    public final static boolean DEFAULT_DASH = false;

    /** Default contour color fill value */
    public final static boolean DEFAULT_FILL = false;

    /** Default contour line width */
    public final static int DEFAULT_LINE_WIDTH = 1;

    /** Dashed line */
    public static final int DASH_STYLE = 1;

    /** Dotted line */
    public static final int DOT_STYLE = 2;

    /** Dash-Dot line */
    public static final int DASH_DOT_STYLE = 3;

    /** Default contour line style */
    public final static int DEFAULT_DASHED_STYLE = DASH_STYLE;

    /** Default label size */
    public final static int DEFAULT_LABEL_SIZE = 12;

    /** contour interval */
    private float interval;

    /** contour base */
    private float base;

    /** Minimum contour value */
    private float min;

    /** Maximum contour value */
    private float max;

    /** label (font) size */
    private int labelSize = DEFAULT_LABEL_SIZE;

    /** Font - uses Object because font may be a HersheyFont */
    private Object font;

    /** label alignment */
    private boolean alignLabels = DEFAULT_LABEL_ALIGNMENT;

    /** Flag for labeled contours */
    private boolean isLabeled = DEFAULT_LABEL;

    /** Flag for dashed contours */
    private boolean dashOn = DEFAULT_DASH;

    /** Flag for color filled contours */
    private boolean isColorFilled = DEFAULT_FILL;

    /** Contour line width */
    private int lineWidth = DEFAULT_LINE_WIDTH;

    /** Contour dash style */
    private int dashedStyle = DEFAULT_DASHED_STYLE;

    /** interval string */
    private String levelsString = null;

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param interval       the contour interval
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param isColorFilled  flag for color filling contours
     */
    public ContourInfo(float interval, float base, float min, float max,
                       boolean labelOn, boolean dashOn,
                       boolean isColorFilled) {
        this(interval, base, min, max, labelOn, dashOn, isColorFilled,
             DEFAULT_LINE_WIDTH);
    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param interval       the contour interval
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param isColorFilled  flag for color filling contours
     * @param width          line width
     */
    public ContourInfo(float interval, float base, float min, float max,
                       boolean labelOn, boolean dashOn,
                       boolean isColorFilled, int width) {

        this(String.valueOf(interval), base, min, max, labelOn, dashOn,
             isColorFilled, width);

    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param levelsString   the contour levels as a string
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param isColorFilled  flag for color filling contours
     * @param width          line width
     */
    public ContourInfo(String levelsString, float base, float min, float max,
                       boolean labelOn, boolean dashOn,
                       boolean isColorFilled, int width) {
        this(levelsString, base, min, max, labelOn, dashOn, isColorFilled,
             width, DEFAULT_DASHED_STYLE);
    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param levelsString   the contour levels as a string
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param isColorFilled  flag for color filling contours
     * @param width          line width
     * @param dashedStyle      dashedStyle;
     */
    public ContourInfo(String levelsString, float base, float min, float max,
                       boolean labelOn, boolean dashOn,
                       boolean isColorFilled, int width, int dashedStyle) {
        this(levelsString, base, min, max, labelOn, dashOn, isColorFilled,
             width, DEFAULT_DASHED_STYLE, DEFAULT_LABEL_SIZE, null,
             DEFAULT_LABEL_ALIGNMENT);
    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param levelsString   the contour levels as a string
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param isColorFilled  flag for color filling contours
     * @param width          line width
     * @param dashedStyle    dashedStyle
     * @param labelSize      the label (font) size
     * @param font           the font - Font or HersheyFont
     * @param align      the label alignment - true to be along contours
     */
    public ContourInfo(String levelsString, float base, float min, float max,
                       boolean labelOn, boolean dashOn,
                       boolean isColorFilled, int width, int dashedStyle,
                       int labelSize, Object font, boolean align) {

        if (isIrregularInterval(levelsString)) {
            this.levelsString = levelsString;
            this.interval     = Float.NaN;
        } else {
            this.levelsString = null;
            this.interval     = Misc.parseFloat(levelsString);
        }
        this.min           = min;
        this.max           = max;
        this.base          = base;
        this.isLabeled     = labelOn;
        this.dashOn        = dashOn;
        this.isColorFilled = isColorFilled;
        this.lineWidth     = width;
        this.dashedStyle   = dashedStyle;
        this.labelSize     = labelSize;
        this.font          = font;
        this.alignLabels   = align;
    }


    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog using the
     * default values.
     */
    public ContourInfo() {
        this((float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0,
             DEFAULT_LABEL, DEFAULT_DASH, DEFAULT_FILL, DEFAULT_LINE_WIDTH);
    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param interval       the contour interval
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     */
    public ContourInfo(double interval, double base, double min, double max) {
        this((float) interval, (float) base, (float) min, (float) max);

    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param interval       the contour interval
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     */
    public ContourInfo(double interval, double base, double min, double max,
                       boolean labelOn, boolean dashOn) {
        this(String.valueOf(interval), base, min, max, labelOn, dashOn,
             DEFAULT_FILL, DEFAULT_LINE_WIDTH);
    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param levelsString   the contour levels as a string
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param isColorFilled  flag for color filling contours
     * @param width          line width
     */
    public ContourInfo(String levelsString, double base, double min,
                       double max, boolean labelOn, boolean dashOn,
                       boolean isColorFilled, double width) {
        this(levelsString, (float) base, (float) min, (float) max, labelOn,
             dashOn, isColorFilled, (int) width);
    }

    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param interval       the contour interval
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     * @param labelOn        whether labels are
     * @param dashOn         whether lines below base value are dashed or not
     * @param width          line width
     */
    public ContourInfo(double interval, double base, double min, double max,
                       boolean labelOn, boolean dashOn, int width) {
        this((float) interval, (float) base, (float) min, (float) max,
             labelOn, dashOn, DEFAULT_FILL, width);
    }


    /**
     * Construct an object to hold and transfer contour level settings,
     * such as to and from the dialog box ContLevelDialog.
     *
     * @param interval       the contour interval
     * @param base           the contour level below which one line must have
     * @param min            the lower limit of plotted contour values
     * @param max            the upper limit of same
     */
    public ContourInfo(float interval, float base, float min, float max) {
        this(interval, base, min, max, DEFAULT_LABEL, DEFAULT_DASH);
    }

    /**
     * Copy constructor.
     *
     * @param s ContourInfo to copy.
     */
    public ContourInfo(ContourInfo s) {
        set(s);
    }

    /**
     * Create a ContourInfo from a float array.
     *
     * @param values  contour values (interval, base, min, max)
     */
    public ContourInfo(float[] values) {
        this(values[0], values[1], values[2], values[3]);
    }

    /**
     * Create a contour info with the parameters encoded in the string
     *
     * @param params The string params
     */
    public ContourInfo(String params) {
        processParamString(params);
    }

    /**
     * Process the params string. It can either be of the form:
     * <pre>&interval;base;min;max;<pre>
     * or made up of any combination of name=value pairs. e.g.:
     * <pre>interval=10;min=5;max=100;base=15;dashed=true;labels=false;</pre>
     *
     * @param params The string params
     */
    public void processParamString(String params) {
        List toks = StringUtil.split(params, ";", true, true);
        // TODO: how to specify font
        //interval=5;base=6;min=0;max=5
        if (params.indexOf("=") >= 0) {
            for (int i = 0; i < toks.size(); i++) {
                List subToks = StringUtil.split(toks.get(i).toString(), "=");
                if (subToks.size() != 2) {
                    throw new IllegalArgumentException(
                        "Bad contour info format: " + params);
                }
                String name  = subToks.get(0).toString().trim();
                String value = subToks.get(1).toString().trim();
                if (name.equals("interval")) {
                    interval = new Float(value).floatValue();
                } else if(name.equals("levels")) {
                    value = value.replaceAll(",",";");
                    setLevelsString(value);
                } else if (name.equals("min")) {
                    min = new Float(value).floatValue();
                } else if (name.equals("max")) {
                    max = new Float(value).floatValue();
                } else if (name.equals("base")) {
                    base = new Float(value).floatValue();
                } else if (name.equals("dashed")) {
                    dashOn = new Boolean(value).booleanValue();
                } else if (name.equals("width")) {
                    lineWidth = new Integer(value).intValue();
                } else if (name.equals("labels")) {
                    isLabeled = new Boolean(value).booleanValue();
                } else if (name.equals("labelsize")) {
                    labelSize = new Integer(value).intValue();
                } else if (name.equals("font")) {
                    //TODO: what should go here?
                } else if (name.equals("align")) {
                    alignLabels = new Boolean(value).booleanValue();
                } else {
                    throw new IllegalArgumentException(
                        "Unknown ContourInfo parameter:" + name);
                }
            }
        } else {
            //interval;base;min;max
            if (toks.size() != 4) {
                throw new IllegalArgumentException(
                    "Bad ContourInfo parameters:" + params
                    + "\n Needs to be of the form interval;base;min;max");
            }
            interval = new Float(toks.get(0).toString()).floatValue();
            base     = new Float(toks.get(1).toString()).floatValue();
            min      = new Float(toks.get(2).toString()).floatValue();
            max      = new Float(toks.get(3).toString()).floatValue();
        }
    }


    /**
     * See if this has been defined
     *
     * @return true if int/max/min/base values have been set
     */
    public boolean isDefined() {
        return getIntervalDefined() && getBaseDefined() && getMinDefined()
               && getMaxDefined();
    }

    /**
     * See if an interval has been defined.
     * @return true if the interval is not a NaN
     */
    public boolean getIntervalDefined() {
        return !Float.isNaN(interval);
    }

    /**
     * See if the base has been defined.
     * @return true if the base is not a NaN
     */
    public boolean getBaseDefined() {
        return !Float.isNaN(base);
    }

    /**
     * See if the min has been defined.
     * @return true if the min is not a NaN
     */
    public boolean getMinDefined() {
        return ( !Float.isNaN(min));
    }

    /**
     * See if the max has been defined.
     * @return true if the max is not a NaN
     */
    public boolean getMaxDefined() {
        return !Float.isNaN(max);
    }

    /**
     * Get the contour interval
     * @return the contour interval
     */
    public float getInterval() {
        return interval;
    }

    /**
     * Get the contour base
     * @return the contour base
     */
    public float getBase() {
        return base;
    }

    /**
     * Get the contour minimum
     * @return the contour minimum
     */
    public float getMin() {
        return min;
    }

    /**
     * Get the contour maximum
     * @return the contour maximum
     */
    public float getMax() {
        return max;
    }

    /**
     * Set the labelling flag
     * @param v  true to label
     */
    public void setIsLabeled(boolean v) {
        isLabeled = v;
    }

    /**
     * Get the labelling flag
     * @return true if labeled
     */
    public boolean getIsLabeled() {
        return isLabeled;
    }

    /**
     * Set the dashing flag
     * @param v  true to dash
     */
    public void setDashOn(boolean v) {
        dashOn = v;
    }

    /**
     * Get the dashing flag
     * @return true to dash
     */
    public boolean getDashOn() {
        return dashOn;
    }

    /**
     * Set the contour interval
     *
     * @param v  new interval
     */
    public void setInterval(float v) {
        interval = v;
        if ( !Float.isNaN(v)) {
            levelsString = null;
        }
    }

    /**
     * Set the irregular contour intervals string
     *
     * @param v  new interval string
     */
    public void setLevelsString(String v) {
        if (isIrregularInterval(v)) {
            interval = Float.NaN;
        } else {
            if (v != null) {
                interval = (float) Misc.parseDouble(v);
            }
        }
        levelsString = v;
    }

    /**
     * Get the contour interval string
     *
     * @return a string representation of the levels in this
     */
    public String getLevelsString() {
        return levelsString;
    }

    /**
     * Set the contour base
     *
     * @param v  new base
     */
    public void setBase(float v) {
        base = v;
    }

    /**
     * Set the contour minimum
     *
     * @param v  new minimum
     */
    public void setMin(float v) {
        min = v;
    }

    /**
     * Set the contour maximum
     *
     * @param v  new maximum
     */
    public void setMax(float v) {
        max = v;
    }

    /**
     * Set the contour parameters for this from another contour info.
     *
     * @param that  other ContourInfo
     */
    public void set(ContourInfo that) {
        this.interval      = that.interval;
        this.levelsString  = that.levelsString;
        this.base          = that.base;
        this.min           = that.min;
        this.max           = that.max;
        this.isLabeled     = that.isLabeled;
        this.dashOn        = that.dashOn;
        this.isColorFilled = that.isColorFilled;
        this.lineWidth     = that.lineWidth;
        this.dashedStyle   = that.dashedStyle;
        this.labelSize     = that.labelSize;
        this.font          = that.font;
        this.alignLabels   = that.alignLabels;
    }


    /**
     * Set the parameters if they are defined in the other.
     *
     * @param that  other to use
     */
    public void setIfDefined(ContourInfo that) {
        if (that.getIntervalDefined()) {
            this.interval = that.interval;
        }
        if (that.getBaseDefined()) {
            this.base = that.base;
        }
        if (that.getMinDefined()) {
            this.min = that.min;
        }
        if (that.getMaxDefined()) {
            this.max = that.max;
        }
        this.levelsString = that.levelsString;
        if (isColorFilled) {
            this.isLabeled = false;
        } else {
            this.isLabeled = that.isLabeled;
        }
        this.dashOn = that.dashOn;
        //??        this.isColorFilled = that.isColorFilled;
        this.lineWidth   = that.lineWidth;
        this.dashedStyle = that.dashedStyle;
        this.labelSize   = that.labelSize;
        this.font        = that.font;
        this.alignLabels = that.alignLabels;
    }

    /**
     * Return the contour parameters as an array.
     * @return contour values (interval, base, min, max)
     */
    public float[] asArray() {
        return new float[] { interval, base, min, max };
    }


    /**
     * Tweak the interval for dashing.
     */
    public void tweakIntervalForDash() {
        if (interval < 0.0) {
            interval = -interval;
        }
        if (dashOn) {
            interval = -interval;
        }
    }

    /**
     * Get the interval as a string.  If interval is undefined,
     * return the levelsString.
     * @return interval as String in Locale format or levelsString.
     */
    public String getIntervalString() {
        return getIntervalString(false);
    }

    /**
     * Get the interval as a string.  If interval is undefined,
     * return the levelsString.
     * @param  useDecimalFormat  format as decimal for XML storage
     * @return interval as String or levelsString.
     */
    public String getIntervalString(boolean useDecimalFormat) {
        return (levelsString != null)
               ? levelsString
               : (useDecimalFormat)
                 ? String.valueOf(interval)
                 : Misc.format(interval);
    }

    /**
     * Return a String representation of this object
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" interval= ");
        sb.append(interval);
        sb.append(" irregular intervals= ");
        sb.append(levelsString);
        sb.append(" base = ");
        sb.append(base);
        sb.append(" min  = ");
        sb.append(min);
        sb.append(" max = ");
        sb.append(max);
        sb.append(" dash = ");
        sb.append(dashOn);
        sb.append(" labeling = ");
        sb.append(isLabeled);
        sb.append(" isColorFilled = ");
        sb.append(isColorFilled);
        sb.append(" lineWidth = ");
        sb.append(lineWidth);
        return sb.toString();
    }

    /**
     * Set the color fill flag
     *
     * @param fill  true to color fill
     */
    public void setIsFilled(boolean fill) {
        isColorFilled = fill;
    }

    /**
     * Get the color fill flag.
     *
     * @return true to color fill
     */
    public boolean getIsFilled() {
        return isColorFilled;
    }

    /**
     * Set the line width.
     *
     * @param width  line width (pixels)
     */
    public void setLineWidth(int width) {
        lineWidth = width;
    }

    /**
     * Get the line width.
     *
     * @return line width (pixels)
     *
     * @return line width
     */
    public int getLineWidth() {
        return lineWidth;
    }

    /**
     * Set the dash style
     *
     * @param v  new dash style
     */
    public void setDashedStyle(int v) {
        dashedStyle = v;
    }

    /**
     * Get the dash style
     *
     * @return dash style
     */
    public int getDashedStyle() {
        return dashedStyle;
    }

    /**
     * Get the contour levels. If setLevels has been called with
     * a non-null array, that array is returned, otherwise, levels
     * are calculated from the interval, max, min and base.
     *
     * @return the contour levels
     */
    public float[] getContourLevels() {
        return getContourLevels((levelsString == null)
                                ? makeLevelsString()
                                : levelsString);
    }

    /**
     * Get the contour levels from the given string.
     *
     * @param levelString string representation of the levels
     * @return the contour levels
     */
    public float[] getContourLevels(String levelString) {
        List    tokens = StringUtil.split(levelString, ";", true, true);
        float[] levels = null;
        for (int i = 0; i < tokens.size(); i++) {
            String  tok  = (String) tokens.get(i);
            float[] vals = getLevelsFromDefString(tok);
            if (vals != null) {
                levels = (levels == null)
                         ? vals
                         : appendToArray(levels, vals);
            }
        }
        if (levels == null) {
            levels = new float[] { 0 };
        }
        return levels;
    }

    /**
     * Get the label (font) size.
     *
     * @return the label (font) size
     */
    public int getLabelSize() {
        return labelSize;
    }

    /**
     * Get the label (font) size
     *
     * @param size the label (font) size
     */
    public void setLabelSize(int size) {
        this.labelSize = size;
    }

    /**
     *     Get the font.
     *
     *     @return the font - null, Font, or HersheyFont
     */
    public Object getFont() {
        return font;
    }

    /**
     * Set the font.
     *
     * @param font the font to set - must be a Font or HersheyFont
     */
    public void setFont(Object font) {
        this.font = font;
    }


    /**
     * Get the label alignment
     *
     * @return  true for along contours
     */
    public boolean getAlignLabels() {
        return alignLabels;
    }

    /**
     * Set the label alignment.
     *
     * @param align  - true for along contours
     *
     */
    public void setAlignLabels(boolean align) {
        this.alignLabels = align;
    }

    /**
     * Clean up the user entered levels string
     *
     * @param levelString string representation of the levels
     * @return The level string un-localized
     */

    public static String cleanupUserLevelString(String levelString) {
        if (levelString == null) {
            return null;
        }
        List         tokens = StringUtil.split(levelString, ";", true, true);
        StringBuffer sb     = new StringBuffer();
        for (int i = 0; i < tokens.size(); i++) {
            String tok = (String) tokens.get(i);
            //            System.err.println("tok:" + tok +":");
            List subTokens = StringUtil.split(tok, "/", true, true);
            if (i > 0) {
                sb.append(";");
            }
            for (int j = 0; j < subTokens.size(); j++) {
                String subTok = (String) subTokens.get(j);
                //        System.err.println("subtok:" + tok +":");
                if (j > 0) {
                    sb.append("/");
                }
                double d = Misc.parseDouble(subTok);
                sb.append(String.valueOf(d));
            }
        }
        return sb.toString();
    }





    /**
     * See if a levelsString is an regular or an irregular
     * interval.
     * @param intervalString to check
     * @return true if this is not a regular interval (single value)
     */
    public static boolean isIrregularInterval(String intervalString) {
        return ((intervalString != null)
                && ((intervalString.indexOf(";") > 0)
                    || (intervalString.indexOf("/") > 0)));
    }

    /**
     * Make a levels string from the interval, min, max and base.
     *
     * @return String representation of the levels
     */
    private String makeLevelsString() {
        return interval + "/" + min + "/" + max + "/" + base;
    }

    /**
     * Parse a String of the form "int/min/max/base".
     * @param defString  defining String of the form int/min/max/base.
     * @return float array of the levels[] of the string
     */
    private float[] getLevelsFromDefString(String defString) {
        StringTokenizer tok  = new StringTokenizer(defString, "/");
        float           intv = getInterval();
        float           min  = getMin();
        float           max  = getMax();
        float           base = getBase();
        if (tok.hasMoreTokens()) {
            intv = getValue(tok.nextToken(), intv);
        }
        if (tok.hasMoreTokens()) {
            min = getValue(tok.nextToken(), min);
        } else {
            return new float[] { intv };
        }
        if (tok.hasMoreTokens()) {
            max = getValue(tok.nextToken(), max);
        }
        if (tok.hasMoreTokens()) {
            base = getValue(tok.nextToken(), base);
        } else {
            base = min;
        }
        return Misc.computeTicks(max, min, base, intv);
    }

    /**
     * Get the value from a string.  Pass in a default value if the
     * string to return if the string is empty
     *
     * @param valString string to parse.  Must be Double, not local format
     * @param def default value
     *
     * @return value of the string or the default
     */
    private float getValue(String valString, float def) {
        if (valString.trim().equals("")) {
            return def;
        }
        return Misc.parseFloat(valString);
    }

    /**
     * Append one array to another
     *
     * @param orig      original array
     * @param newStuff  new stuff
     *
     * @return new, appended array
     */
    private float[] appendToArray(float[] orig, float[] newStuff) {
        float[] newArray = new float[orig.length + newStuff.length];
        System.arraycopy(orig, 0, newArray, 0, orig.length);
        System.arraycopy(newStuff, 0, newArray, orig.length, newStuff.length);
        return newArray;
    }

}

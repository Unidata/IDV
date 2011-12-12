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

package ucar.unidata.idv;


import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.idv.ui.ParamDefaultsEditor;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;


import ucar.visad.*;

import visad.*;

import visad.georef.*;

import java.awt.*;


import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.*;


/**
 * Provides a set of  display conventions (e.g., contour interval,
 * default color map, etc) typically based on parameter names
 * (e.g., "rh", "temp", etc.).
 * Some of the conventions  are hardcoded but others are defined
 * using property files.
 * @author Unidata Development Team
 * @version $Revision
 */
public class DisplayConventions extends IdvManager {


    /** The singleton */
    private static DisplayConventions displayConventions;

    /** The default double format */
    private static String DEFAULT_FORMAT = "##0.0";

    /** Lat/Lon format */
    private DecimalFormat latLonFormat = new DecimalFormat(DEFAULT_FORMAT);

    /** Distance format */
    private DecimalFormat distanceFormat = new DecimalFormat("####0.0#");

    /** Angle format */
    private DecimalFormat angleFormat = new DecimalFormat(DEFAULT_FORMAT);

    /** We keep this around so we can cycle through the colors */
    private static int nextColor = 0;

    /** The array of colors we cycle through */
    private static Color[] colors = {
        Color.red, Color.cyan, Color.magenta, Color.green, Color.orange,
        Color.yellow
    };


    /** The font to use for window labels */
    private Font windowLabelFont;

    /** The preference id to hold the list of units that the user uses when changing units */
    public static final String PREF_UNITLIST = "Control.UnitList";

    /** The list of units that is displayed in the change unit guis */
    private List unitList;

    /** Provides for synchronization when dealing with the unit list */
    private Object UNIT_MUTEX = new Object();

    /** Latitude id */
    private static final int LATITUDE = 1;

    /** Longitude id */
    private static final int LONGITUDE = 0;


    /**
     * Create this object with the given idv
     *
     * @param idv The IDV
     */
    public DisplayConventions(IntegratedDataViewer idv) {
        super(idv);
        displayConventions = this;
    }


    /**
     * Get the singleton object
     *
     * @return The singleton object
     */
    public static DisplayConventions getDisplayConventions() {
        return displayConventions;
    }




    /**
     * Format an LatLonPoint as a lat/lon string.
     *
     * @param llp  LatLonPoint to format
     * @return The formatted LatLonPoint
     */
    public String formatLatLonPoint(LatLonPoint llp) {
        return formatLatLonPoint(llp, true);
    }

    /**
     * format the latlon point
     *
     * @param llp the llp
     * @param includeLabel include the label
     *
     * @return formatted point
     */
    public String formatLatLonPoint(LatLonPoint llp, boolean includeLabel) {

        StringBuffer buf = new StringBuffer();
        if (includeLabel) {
            buf.append("Lat: ");
        }
        try {
            buf.append(
                formatLatLon(
                    Math.min(
                        Math.max(llp.getLatitude().getValue(), -90), 90)));
        } catch (Exception e) {
            buf.append(" ");
        }
        if (includeLabel) {
            buf.append(" Lon: ");
        } else {
            buf.append("/");
        }
        try {
            buf.append(
                formatLatLon(
                    Misc.normalizeLongitude(llp.getLongitude().getValue())));
        } catch (Exception e) {
            buf.append(" ");
        }
        return buf.toString();

    }



    /**
     * Format a EarthLocation as a lat/lon/(alt) string.
     *
     * @param el  EarthLocation to format
     * @param includeAlt  include Altitude in the return
     * @return The formatted lat/lon/alt
     */
    public String formatEarthLocation(EarthLocation el, boolean includeAlt) {
        return formatEarthLocation(el, includeAlt, true);
    }


    /**
     * format the earth location
     *
     * @param el the earth location
     * @param includeAlt include the altitude
     * @param includeLabel include that lat/lon label
     *
     * @return formatted earth location
     */
    public String formatEarthLocation(EarthLocation el, boolean includeAlt,
                                      boolean includeLabel) {
        StringBuffer buf = new StringBuffer();
        try {
            buf.append(formatLatLonPoint(el.getLatLonPoint(), includeLabel));
        } catch (Exception e) {
            return "";
        }
        if (includeAlt) {
            if (includeLabel) {
                buf.append(" Alt: ");
            } else {
                buf.append(" ");
            }
            try {
                buf.append(formatDistance(el.getAltitude().getValue()));
            } catch (Exception e) {
                buf.append(" ");
            }
        }
        return buf.toString();

    }


    /**
     * Format a LatLonPoint as a lat/lon string.
     *
     * @param llp  LatLonPoint to format
     * @return The formatted lat/lon
     */
    public String formatLatLonShort(LatLonPoint llp) {
        StringBuffer buf = new StringBuffer();
        try {
            buf.append(formatLatLon(llp.getLatitude().getValue()));
            buf.append("/");
            buf.append(formatLatLon(llp.getLongitude().getValue()));
        } catch (Exception e) {
            return "";
        }
        return buf.toString();
    }


    /**
     * Format a EarthLocation as a lat/lon/(alt) string.
     *
     * @param el  EarthLocation to format
     * @return The formatted lat/lon/alt
     */
    public String formatEarthLocationShort(EarthLocation el) {
        StringBuffer buf = new StringBuffer();
        try {
            LatLonPoint llp = el.getLatLonPoint();
            buf.append(formatLatLonShort(llp));
            buf.append(" ");
            buf.append(formatDistance(el.getAltitude().getValue()));
        } catch (Exception e) {
            return "";
        }
        return buf.toString();
    }


    /**
     * get the latlon formatter
     *
     * @return the latlon formatter
     */
    public DecimalFormat getLatLonFormat() {
        DecimalFormat fmt = new DecimalFormat(DEFAULT_FORMAT);
        fmt.applyPattern(getStore().get(PREF_LATLON_FORMAT, DEFAULT_FORMAT));
        return fmt;
    }


    /**
     * Format the given lat or lon using the latLonFormat
     *
     * @param d The lat or lon value (degrees?)
     * @return The formatted version
     */
    public String formatLatLon(double d) {
        if (Double.isNaN(d)) {
            return "missing";
        }
        latLonFormat.applyPattern(getStore().get(PREF_LATLON_FORMAT,
                DEFAULT_FORMAT));
        return latLonFormat.format(d);
    }

    /**
     * Format an lat or lon
     *
     * @param latorlon The lat or lon
     * @return The formatted lat or lon
     */
    public String formatLatLon(Real latorlon) {
        return formatLatLon(latorlon.getValue());
    }

    /**
     * Format the lat/lon labels with cardinal points (N,S,E,W)
     *
     * @param value
     * @param type (LATITUDE or LONGITUDE)
     *
     * @return  the formatted string
     */
    public String formatLatLonCardinal(double value, int type) {
        String retString;
        if (type == LATITUDE) {
            retString = Misc.format(Math.abs(value));
            if (value < 0) {
                retString += "S";
            } else if (value == 0.f) {
                retString = "EQ";
            } else {
                retString += "N";
            }
        } else {  // LONGITUDE
            if (value > 180) {
                value -= 360;
            }
            retString = Misc.format(Math.abs(value));
            if (value < 0) {
                retString += "W";
            } else if (value == 0.f) {
                retString = "0";
            } else {
                retString += "E";
            }
        }
        return retString;
    }

    /**
     * Format an lat or lon with cardinal id (N,S,E,W)
     *
     * @param latorlon The lat or lon
     * @param type (LATITUDE or LONGITUDE)
     * @return The formatted lat or lon
     */
    public String formatLatLonCardinal(Real latorlon, int type) {
        return formatLatLonCardinal(latorlon.getValue(), type);
    }



    /**
     * Format an Altitude
     *
     * @param alt The altitude
     * @return The formatted alt
     */
    public String formatAltitude(Real alt) {
        return formatDistance(alt.getValue());
    }



    /**
     * Format the given distance using the distanceFormat
     *
     * @param d The distance to format
     * @return The formatted version
     */
    public String formatDistance(double d) {
        if (d > 1000) {
            return "" + (int) d;
        }
        return distanceFormat.format(d);
    }

    /**
     * Format the given angle with the angleFormat
     *
     * @param d The angle to format
     * @return The formatted version
     */
    public String formatAngle(double d) {
        return angleFormat.format(d);
    }


    /**
     * Format a double to a String, for values such as -179.123
     *
     * @param ll a double (such as a lat or lon in decimal degrees)
     * @return The formatted value
     * @deprecated  use formatLatLon(double)
     */
    public String formatLatOrLon(double ll) {
        return formatLatLon(ll);
    }

    /**
     * Format of a double.
     *
     * @param v The value
     * @return Its format
     */
    public String format(double v) {
        return Misc.format(v);
    }

    /**
     * Find the default contour inf
     * @param paramName variable name from the data source
     *
     * @return The default contour info for the param
     */
    public ContourInfo findDefaultContourInfo(String paramName) {
        return findDefaultContourInfo(paramName, null);
    }


    /**
     * Find the default contour inf
     * @param paramName variable name from the data source
     * @param contourInfo The contour info to use
     *
     * @return The default contour info for the param
     */
    public ContourInfo findDefaultContourInfo(String paramName,
            ContourInfo contourInfo) {
        // Find pre-determined contour values for this parameter name
        ContourInfo dflt =
            getParamDefaultsEditor().getParamContourInfo(paramName);

        if (dflt != null) {
            if (dflt.isDefined()) {
                if (contourInfo == null) {
                    return new ContourInfo(dflt);
                }
                contourInfo.setIfDefined(dflt);
                return contourInfo;
            }
        }
        return null;
    }


    /**
     * Find pre-determined contouring values for this parameter by name
     *  from the paramdefaults.xml file, or
     *  compute reasonable values of contouring values from the data itself.
     *     min    no contour line below this value;
     *     base   a contour line must have this value (even if not seen), other values
     *            are this value +/- some multiple of the interval;
     *     max    no contour with greater value than this;
     *     interval if negative, means show dashed lines below base value.
     *
     * @param paramName variable name from the data source
     * @param rangeType one of them ViaAD RealType thingys for the data
     * @param displayUnit the unit the data will appear on screen
     * @param range The range
     * @return a ContourInfo object with appropriate contouring values
     */
    public ContourInfo findContourInfo(String paramName, RealType rangeType,
                                       Unit displayUnit, Range range) {

        return findContourInfo(paramName, rangeType, displayUnit, range,
                               null);
    }


    /**
     * Find pre-determined contouring values for this parameter by name
     *  from the paramdefaults.xml file, or
     *  compute reasonable values of contouring values from the data itself.
     *     min    no contour line below this value;
     *     base   a contour line must have this value (even if not seen), other values
     *            are this value +/- some multiple of the interval;
     *     max    no contour with greater value than this;
     *     interval if negative, means show dashed lines below base value.
     *
     * @param paramName variable name from the data source
     * @param rangeType one of them ViaAD RealType thingys for the data
     * @param displayUnit the unit the data will appear on screen
     * @param range The range
     * @param contourInfo Default contour info
     * @return a ContourInfo object with appropriate contouring values
     */
    public ContourInfo findContourInfo(String paramName, RealType rangeType,
                                       Unit displayUnit, Range range,
                                       ContourInfo contourInfo) {

        // make an empty ContourInfo object
        if (contourInfo == null) {
            contourInfo = new ContourInfo(Double.NaN, Double.NaN, Double.NaN,
                                          Double.NaN);
        }

        // Find pre-determined contour values for this parameter name
        ContourInfo dflt =
            getParamDefaultsEditor().getParamContourInfo(paramName);

        if (dflt != null) {
            //Set pre-determined values into local data "contourInfo"
            //System.out.println("  DC: findContourInfo got params contouring values "+ dflt.toString() );
            contourInfo.setIfDefined(dflt);
        }

        if (contourInfo.isDefined()) {
            return contourInfo;
        }


        float min        = 0.0f;
        float max        = 1100.0f;

        float clBase     = Float.NaN;
        float clInterval = Float.NaN;
        float clMin      = Float.NaN;
        float clMax      = Float.NaN;

        try {
            // convert data's max/min values from native units to display units
            // as seen by user in plots; use VisAD methods
            if (Unit.canConvert(rangeType.getDefaultUnit(), displayUnit)) {
                Real dispVal = new Real(rangeType, range.min);
                min     = (float) dispVal.getValue(displayUnit);
                dispVal = new Real(rangeType, range.max);
                max     = (float) dispVal.getValue(displayUnit);
            } else {
                min = (float) range.min;
                max = (float) range.max;
            }

            // use data max and min values in display units to find appropriate
            // workable values for contour interval, base, min, and max.

            double span = Math.abs(max - min);

            // GEMPAK alogrithm for 5 to 10 contours in a field from grcval.f
            int scale = (int) (Math.log(span) / Math.log(10));
            if (span < 1) {
                scale = scale - 1;
            }
            double cscale = Math.pow(10, scale);
            double crange = span / cscale;
            int    nrange = (int) crange;
            double rint   = (nrange + 1) * .1 * cscale;
            if (Double.isInfinite(rint)) {
                rint = span;
            }

            if ((span <= 300.0) && (span > 5.0)) {  /* typical case */
                clInterval = (float) rint;
                clMin      = clBase = clInterval * ((int) (min / clInterval));
                clMax      = clInterval * (1 + (int) (max / clInterval));
            } else if (span <= 5.0) {               // for max-min less than 5
                clInterval = (float) rint;
                clMin      = clBase = min;
                clMax      = max;
            } else {                                // for really big ranges, span > 300 make ints
                clInterval = (float) ((int) rint);
                clMin      = clBase = (float) ((int) min);
                clMax      = (float) ((int) max);
            }
            clMax = clMax + clInterval;
            clMin = clMin - clInterval;

        } catch (Exception exp) {
            logException("Set contour levels for " + paramName, exp);
        }

        // this should never be true; must be a leftover
        if (clInterval == 0.0f) {
            //System.out.println("  DC: findContourInfo got default contour interval of 20.0");
            clInterval = 20.0f;
        }

        // IF any contouring values were not supplied by the parameter-name-based
        // information; then load in the computed values made here.

        if ( !contourInfo.getIntervalDefined()
                && (contourInfo.getLevelsString() == null)) {
            contourInfo.setInterval(clInterval);
        }

        if ( !contourInfo.getBaseDefined()) {
            contourInfo.setBase(clBase);
        }

        if ( !contourInfo.getMinDefined()) {
            contourInfo.setMin(clMin);
        }

        if ( !contourInfo.getMaxDefined()) {
            contourInfo.setMax(clMax);
        }

        return contourInfo;
    }



    /**
     * Get the default {@link ucar.unidata.util.ColorTable}
     * that should be used for the given parameter name.
     *
     * @param paramName The name of the parameter
     * @return The color table to use
     */
    public ColorTable getParamColorTable(String paramName) {
        return getParamDefaultsEditor().getParamColorTable(paramName);
    }


    /**
     * Set range of values for lower and upper parameter values locked
     * to lower and upper entries in color table
     *
     * @param paramName name of parm to set range limits for
     * @param unit DEFAULT VisAD unit not display unit
     * @return The {@link ucar.unidata.util.Range} to use for the given parameter
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public Range getParamRange(String paramName, Unit unit)
            throws VisADException, RemoteException {
        if (paramName == null) {
            if (unit != null) {
                return getParamDefaultsEditor().getParamRange("unit:" + unit);
            }
            return null;
        }

        Range r = getParamDefaultsEditor().getParamRange(paramName);

        if (r == null) {
            paramName = paramName.toLowerCase();
            r         = getParamDefaultsEditor().getParamRange(paramName);
        }

        return r;
    }


    /**
     * Wrapper around IdvBase.getParamDefaultsEditor
     *
     * @return the param defaults editor
     */
    public ParamDefaultsEditor getParamDefaultsEditor() {
        return getIdv().getParamDefaultsEditor();
    }


    /**
     * For the given parameter name, select the common unit to display;
     * for example  Celsius replacing Kelvin.
     *
     * @param paramName the String name of data parameter
     * @param unit the VisAD Unit of the parameter (incoming or original)
     *
     * @return Unit
     */

    public Unit selectDisplayUnit(String paramName, Unit unit) {
        return getDisplayUnit(paramName, unit);
    }


    /**
     * Get the default display unit for a parameter with the given name.
     * If none is found then return the given unit.
     *
     *
     * @param paramName The parameter name
     * @param unit The default unit
     * @return The display unit
     */
    public Unit getDisplayUnit(String paramName, Unit unit) {
        if (paramName == null) {
            return null;
        }
        Unit displayUnit =
            getParamDefaultsEditor().getParamDisplayUnit(paramName);
        if (displayUnit == null) {
            displayUnit = unit;
        }
        //If no unit was passed in then just return the one we found.
        if (unit == null) {
            return displayUnit;
        }
        return Unit.canConvert(unit, displayUnit)
               ? displayUnit
               : unit;
    }



    /**
     * Return the list of color names. This is used in comboboxes
     * to choose a color.
     *
     * @return List of color names
     */
    public Vector getColorNameList() {
        return new Vector(Misc.toList(GuiUtils.COLORNAMES));
    }

    /**
     * A helper to make a color selector combo box
     *
     * @param dflt The default color value
     * @return The color selector combo box
     */
    public JComboBox makeColorSelector(Color dflt) {
        return GuiUtils.makeColorNameComboBox(dflt);
    }

    /**
     * Get the color that corresponds to the given name (e.g., red, blue, etc.)
     *
     * @param name The color name
     * @return The color (or blue if not found).
     */
    public Color getColor(String name) {
        return GuiUtils.decodeColor(name, Color.blue);
    }

    /**
     * Get the name that corresponds to the given color
     *
     * @param color The color
     * @return Its name
     */
    public String getColorName(Color color) {
        return GuiUtils.getColorName(color);
    }


    /**
     * Cycle through the color list.
     *
     * @return The next color in the list
     */
    public static Color getColor() {
        if (nextColor >= colors.length) {
            nextColor = 0;
        }
        return colors[nextColor++];
    }



    /**
     * Get the font used for window labels
     *
     * @return The window label font
     */
    public Font getWindowLabelFont() {
        if (windowLabelFont == null) {
            windowLabelFont = new Font("Monospaced", Font.BOLD, 10);
        }
        return windowLabelFont;
    }



    /**
     * Popup a unit selection gui. This will also save off
     * persistently ay new unit names typed in.
     *
     * @param unit The current unit
     * @param defaultUnit The default unit to return if the user  chooses "Default"
     * @return The new unit or null on a cancel or an error
     */
    public JComboBox makeUnitBox(Unit unit, Unit defaultUnit) {
        TwoFacedObject current = null;
        if (unit != null) {
            current = new TwoFacedObject(unit.toString(), unit);
        }
        String unitName = ((unit == null)
                           ? null
                           : unit.toString());
        List   unitList = getDefaultUnitList();
        if (unit != null) {
            String unitString = unit.toString();
            List   tmpList    = unitList;
            unitList = new ArrayList();
            for (int i = 0; i < tmpList.size(); i++) {
                Object o = tmpList.get(i);
                if (o.toString().equals(unitString)) {
                    continue;
                }
                Unit theUnit = null;
                if ((o instanceof TwoFacedObject)) {
                    theUnit = (Unit) ((TwoFacedObject) o).getId();
                } else if ((o instanceof Unit)) {
                    theUnit = (Unit) o;
                } else {
                    continue;
                }
                if (theUnit != null) {
                    if ( !Unit.canConvert(unit, theUnit)) {
                        continue;
                    }

                    if (unit.equals(theUnit)) {}
                } else {
                    continue;
                }
                unitList.add(o);
            }
        }
        return GuiUtils.getEditableBox(unitList, current);
    }



    /**
     * Popup a unit selection gui. This will also save off
     * persistently any new unit names typed in.
     *
     * @param unit The current unit
     * @param defaultUnit The default unit to return if the user  chooses "Default"
     * @return The new unit or null on a cancel or an error
     */
    public Unit selectUnit(Unit unit, Unit defaultUnit) {
        JComboBox ufld  = makeUnitBox(unit, defaultUnit);
        Component panel = GuiUtils.label(" New unit:  ", ufld);
        if ( !GuiUtils.showOkCancelDialog(null, "Change unit",
                                          GuiUtils.inset(panel, 5), null,
                                          Misc.newList(ufld))) {
            return null;
        }
        Object selected = ufld.getSelectedItem();
        String unitName = TwoFacedObject.getIdString(selected);
        if (unitName == null) {
            return defaultUnit;
        }
        try {
            Unit newUnit = Util.parseUnit(unitName);
            if ( !(selected instanceof TwoFacedObject)) {
                selected = new TwoFacedObject(selected.toString(), newUnit);
            }
            addToUnitList(selected);
            return newUnit;
        } catch (Exception exc) {
            LogUtil.userMessage("Error parsing unit:" + unitName + "\n"
                                + exc);
        }
        return null;
    }



    /**
     * Add the given object to the list of units
     *
     * @param selected Selected unit
     */
    public void addToUnitList(Object selected) {
        synchronized (UNIT_MUTEX) {
            if (unitList == null) {
                getDefaultUnitList();
            }
            if ( !Misc.containsString(selected.toString(), unitList, true)
                    && !unitList.contains(selected)) {
                unitList.add(selected);
                getStore().put(PREF_UNITLIST, unitList);
                getStore().save();
            }
        }
    }


    /**
     * Return the list of {@link ucar.unidata.util.TwoFacedObject}s
     * that make up the list of units.
     *
     * @return List of unit holding objects.
     */
    public List getDefaultUnitList() {
        synchronized (UNIT_MUTEX) {
            //TODO:
            if (unitList != null) {
                return unitList;
            }
            //First  check if we have one saved as a preference
            unitList = (List) getStore().get(PREF_UNITLIST);
            if (unitList == null) {
                unitList = new ArrayList();
                unitList.add(new TwoFacedObject("Default", null));
            }
            HashSet<String> seenName = new HashSet<String>();
            List            tmp      = new ArrayList();
            for (Object o : unitList) {
                String s = o.toString().toLowerCase();
                if (seenName.contains(s)) {
                    continue;
                }
                if (tmp.contains(o)) {
                    continue;
                }
                tmp.add(o);
                seenName.add(s);
            }
            unitList = tmp;

            String[] names = {
                //Temperature
                "Celsius", "Fahrenheit", "Kelvin",
                //Pressure
                "millibar", "hectoPascal",
                //Distance
                "m", "km", "miles", "feet", "inches", "cm", "mm",
                //speed
                "m/s", "mi/hr", "knot", "km/hr"
            };
            for (int i = 0; i < names.length; i++) {
                try {
                    TwoFacedObject tfo = new TwoFacedObject(names[i],
                                             Util.parseUnit(names[i]));
                    if ( !unitList.contains(tfo)
                            && !seenName.contains(
                                tfo.toString().toLowerCase())) {
                        unitList.add(tfo);
                    }

                } catch (Exception exc) {}
            }

            return unitList;
        }
    }

    /**
     * Get the name for the projection in question.  A little better
     * than <code>MapProjection.toString()</code>
     *
     * @param projection  MapProjection in question
     * @return name for projection
     */
    public String getMapProjectionName(MapProjection projection) {
        Object op = projection;
        if (projection instanceof ProjectionCoordinateSystem) {
            ProjectionImpl pi =
                ((ProjectionCoordinateSystem) projection).getProjection();
            String name = pi.getName();
            if ((name != null) && (name.length() > 0)) {
                return name;
            }
            // make a default
            op = pi;
        } else if (projection
                   instanceof visad.data.mcidas.AREACoordinateSystem) {
            return projection.toString();
        }
        return Misc.getClassName(op.getClass());
    }


    /**
     * Create a label for a map projection based on a
     * <code>DisplayControl</code>.
     * @param mp MapProjection  cannot be null
     * @param display DisplayControl
     * @return label String
     */
    public String getMapProjectionLabel(MapProjection mp,
                                        DisplayControl display) {
        return getMapProjectionName(mp) + " from: " + display.getLabel();
    }

}

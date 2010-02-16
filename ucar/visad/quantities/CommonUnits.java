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

package ucar.visad.quantities;



import visad.CommonUnit;

import visad.DerivedUnit;

import visad.SI;

import visad.ScaledUnit;

import visad.Unit;

import visad.VisADException;

import visad.data.units.Parser;


/**
 * Provides support for units common to meteorology.
 *
 * @author Steven R. Emmerson
 * @version $Id: CommonUnits.java,v 1.12 2005/05/13 18:35:38 jeffmc Exp $
 */
public class CommonUnits {

    /**
     * The SI unit of pressure.
     */
    public static final Unit PASCAL;

    /**
     * A common unit of pressure.
     */
    public static final Unit HECTOPASCAL;

    /**
     * A common unit of pressure.
     */
    public static final Unit MILLIBAR;

    /**
     * A common unit of temperature.
     */
    public static final Unit CELSIUS;

    /**
     * A common unit of mixing-ratio.
     */
    public static final Unit GRAMS_PER_KILOGRAM;

    /**
     * The SI unit of speed.
     */
    public static final Unit METERS_PER_SECOND;

    /**
     * A common unit of time.
     */
    public static final Unit HOUR;

    /**
     * A common unit of distance.
     */
    public static final Unit NAUTICAL_MILE;

    /**
     * A common unit of speed.
     */
    public static final Unit KNOT;

    /**
     * A common unit of plane angle.
     */
    public static final Unit DEGREE;

    /**
     * A common unit of international foot
     */
    public static final Unit FOOT;

    /**
     * A common unit of kilometer
     */
    public static final Unit KILOMETER;

    /**
     * A common unit of mile
     */
    public static final Unit MILE;


    /**
     * A common unit of millimeter
     */
    public static final Unit MILLIMETER;

    /**
     * A common unit for percent
     */
    public static final Unit PERCENT;




    static {
        Unit pascal          = null;
        Unit millibar        = null;
        Unit celsius         = null;
        Unit gPerKg          = null;
        Unit metersPerSecond = null;
        Unit nauticalMile    = null;
        Unit knot            = null;
        Unit hour            = null;
        Unit foot            = null;
        Unit kilometer       = null;
        Unit mile            = null;
        Unit millimeter      = null;
        Unit percent         = null;


        try {
            pascal = SI.kilogram.divide(SI.meter).divide(
                SI.second.pow(2)).clone("Pa");
            millibar = new ScaledUnit(100, (DerivedUnit) pascal).clone("hPa");
            celsius         = SI.kelvin.shift(273.15).clone("Cel");
            gPerKg          = new ScaledUnit(0.001).clone("g/kg");
            metersPerSecond = SI.meter.divide(SI.second).clone("m/s");
            nauticalMile    = new ScaledUnit(1.852e3, SI.meter).clone("nmi");
            hour            = new ScaledUnit(3600.0, SI.second).clone("h");
            knot            = nauticalMile.divide(hour).clone("kt");
            foot            = SI.meter.scale(.0254 * 12).clone("ft");
            kilometer       = SI.meter.scale(1000).clone("km");
            mile            = foot.scale(5280).clone("mi");
            millimeter      = SI.meter.scale(.001).clone("mm");
            percent         = Parser.parse("%").clone("%");

        } catch (Exception e) {
            String reason = e.getMessage();

            System.err.println("Couldn't initialize CommonUnits class"
                               + ((reason == null)
                                  ? ""
                                  : ": " + reason));
            e.printStackTrace();
        }

        PASCAL             = pascal;
        HECTOPASCAL        = millibar;
        MILLIBAR           = millibar;
        CELSIUS            = celsius;
        GRAMS_PER_KILOGRAM = gPerKg;
        METERS_PER_SECOND  = metersPerSecond;
        HOUR               = hour;
        NAUTICAL_MILE      = nauticalMile;
        KNOT               = knot;
        DEGREE             = CommonUnit.degree;
        FOOT               = foot;
        KILOMETER          = kilometer;
        MILE               = mile;
        MILLIMETER         = millimeter;
        PERCENT            = percent;
    }

    /**
     * Constructs from nothing.
     */
    private CommonUnits() {}
}

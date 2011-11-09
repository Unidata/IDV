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

package ucar.unidata.ui.colortable;


import edu.wisc.ssec.mcidas.EnhancementTable;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;



import visad.*;

import java.awt.Color;


import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;
import java.util.Vector;


/**
 * A class to provide color tables suitable for data displays.
 * Uses some code by Ugo Taddei. All methods are static.
 *
 * @version $Id: ColorTableDefaults.java,v 1.15 2007/05/04 19:56:06 jeffmc Exp $
 */
public class ColorTableDefaults {

    /** The name of the &quot;default&quot; color table */
    public static final String NAME_DEFAULT = "default";

    /** The name of the &quot;black&quot; color table */
    private static final String NAME_BLACK = "Black";

    /** The name of the &quot;blue&quot; color table */
    private static final String NAME_BLUE = "Blue";

    /** The name of the &quot;blues&quot; color table */
    private static final String NAME_BLUES = "Blues";

    /** The name of the &quot;bref&quot; color table */
    private static final String NAME_BREF = "Base Reflectivity";

    /** The name of the &quot;bref24&quot; color table */
    private static final String NAME_BREF24 = "Base Reflectivity (24)";

    /** The name of the &quot;bright38&quot; color table */
    private static final String NAME_BRIGHT38 = "Bright38";

    /** The name of the &quot;cref&quot; color table */
    private static final String NAME_CREF = "Composite Reflectivity";

    /** The name of the &quot;cyan&quot; color table */
    private static final String NAME_CYAN = "Cyan";

    /** The name of the &quot;dbz&quot; color table */
    private static final String NAME_DBZ = "DbZ";

    /** The name of the &quot;dbznws&quot; color table */
    private static final String NAME_DBZNWS = "DbZ(NWS)";

    /** The name of the &quot;gray&quot; color table */
    private static final String NAME_GRAY = "Gray";

    /** The name of the &quot;gray scale&quot; color table */
    private static final String NAME_GRAY_SCALE_SAT = "Light Gray scale";

    /** The name of the &quot;gray scale&quot; color table */
    private static final String NAME_GRAY_SCALE = "Gray scale";

    /** The name of the &quot;inverse gray scale&quot; color table */
    private static final String NAME_INVERSE_GRAY_SCALE =
        "Inverse Gray shade";

    /** The name of the &quot;green&quot; color table */
    private static final String NAME_GREEN = "Green";

    /** The name of the &quot;Water Vapor (Gray)&quot; color table */
    private static final String NAME_H2O = "Water Vapor (Gray)";

    /** The name of the &quot;Inverse Rainbow&quot; color table */
    private static final String NAME_INVRAINBOW = "Inverse rainbow";

    /** The name of the &quot;invvisad&quot; color table */
    private static final String NAME_INVVISAD = "Inverse VisAD";

    /** The name of the &quot;magenta&quot; color table */
    private static final String NAME_MAGENTA = "Magenta";

    /** The name of the &quot;mdr&quot; color table */
    private static final String NAME_MDR = "MDR Radar";

    /** The name of the &quot;orange&quot; color table */
    private static final String NAME_ORANGE = "Orange";

    /** The name of the &quot;pink&quot; color table */
    private static final String NAME_PINK = "Pink";

    /** The name of the &quot;precip&quot; color table */
    private static final String NAME_PRECIP = "Precip";

    /** The name of the &quot;radar_precip&quot; color table */
    private static final String NAME_RADAR_PRECIP = "Radar Precip";

    /** The name of the &quot;rainbow&quot; color table */
    private static final String NAME_RAINBOW = "Rainbow";

    /** The name of the &quot;red&quot; color table */
    private static final String NAME_RED = "Red";

    /** The name of the &quot;rh&quot; color table */
    private static final String NAME_RH = "Relative humidity";

    /** The name of the &quot;rvel&quot; color table */
    private static final String NAME_RVEL = "Radial Velocity";

    /** The name of the &quot;temperature&quot; color table */
    private static final String NAME_TEMPERATURE = "Temperature";

    /** The name of the &quot;topo&quot; color table */
    private static final String NAME_TOPO = "Topographic";

    /** The name of the &quot;topobathy&quot; color table */
    private static final String NAME_TOPOBATHY = "Topography/Bathymetry";

    /** The name of the &quot;topomdr&quot; color table */
    private static final String NAME_TOPOMDR = "TOPO/MDR Composite";

    /** The name of the &quot;toposat&quot; color table */
    private static final String NAME_TOPOSAT = "TOPO/Sat Composite";

    /** The name of the &quot;tops&quot; color table */
    private static final String NAME_TOPS = "Echo Tops";

    /** The name of the &quot;vil&quot; color table */
    private static final String NAME_VIL = "VIL";

    /** The name of the &quot;visad&quot; color table */
    private static final String NAME_VISAD = "VisAD";

    /** The name of the &quot;visible/fog&quot; color table */
    private static final String NAME_VIS_FOG = "Visible/Fog";

    /** The name of the &quot;white&quot; color table */
    private static final String NAME_WHITE = "White";

    /** The name of the &quot;windcomps&quot; color table */
    private static final String NAME_WINDCOMPS = "Wind comps";

    /** The name of the &quot;windspeed&quot; color table */
    private static final String NAME_WINDSPEED = "Windspeed";

    /** The name of the &quot;yellow&quot; color table */
    private static final String NAME_YELLOW = "Yellow";




    /**
     * Create a ColorTable and add it to the given list
     *
     * @param name The CT name
     * @param category Its category
     * @param table The actual data
     * @return The color table
     */
    public static ColorTable createColorTable(String name, String category,
            float[][] table) {
        return createColorTable(new ArrayList(), name, category, table);
    }


    /**
     * Create a ColorTable and add it to the given list
     *
     * @param l List to add the ColorTable to
     * @param name The CT name
     * @param category Its category
     * @param table The actual data
     * @return The color table
     */
    public static ColorTable createColorTable(ArrayList l, String name,
            String category, float[][] table) {
        return createColorTable(l, name, category, table, false);
    }

    /**
     * Create a ColorTable and add it to the given list
     *
     * @param l List to add the ColorTable to
     * @param name The CT name
     * @param category Its category
     * @param table The actual data
     * @param tableFlipped If true then the table data is not in row major order
     * @return The color table
     */
    public static ColorTable createColorTable(ArrayList l, String name,
            String category, float[][] table, boolean tableFlipped) {
        return createColorTable(l, name, category, table, tableFlipped, null);
    }

    /**
     * Create a ColorTable and add it to the given list
     *
     * @param l List to add the ColorTable to
     * @param name The CT name
     * @param category Its category
     * @param table The actual data
     * @param tableFlipped If true then the table data is not in row major order
     * @param range the color table range
     * @return The color table
     */
    public static ColorTable createColorTable(ArrayList l, String name,
            String category, float[][] table, boolean tableFlipped,
            Range range) {

        // ensure all R G and B values in the table are in 0.0 to 1.0 range
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[i].length; j++) {
                if (table[i][j] < 0.0) {
                    //System.out.println(" bad value "+table [i][j] );
                    table[i][j] = 0.0f;
                } else if (table[i][j] > 1.0) {
                    //System.out.println(" bad value "+table [i][j]+" for table "
                    // +name +"  comp "+i+"  pt "+j);
                    table[i][j] = 1.0f;
                }
            }
        }

        ColorTable colorTable = new ColorTable(name.toUpperCase(), name,
                                    category, table, tableFlipped);
        if (range != null) {
            colorTable.setRange(range);
        }
        l.add(colorTable);
        return colorTable;
    }

    // the order color tables are lsited here is the order that
    // they appear in the GUI.

    /**
     * Create the default color tables
     * @return List of color tables
     */
    public static ArrayList createColorTables() {
        ArrayList l = new ArrayList();

        createColorTable(l, NAME_DEFAULT, ColorTable.CATEGORY_BASIC,
                         temperatureCT4());
        createColorTable(l, NAME_BLUES, ColorTable.CATEGORY_BASIC,
                         percentBlue(30));
        createColorTable(l, NAME_BRIGHT38, ColorTable.CATEGORY_BASIC,
                         bright38());
        createColorTable(l, NAME_PRECIP, ColorTable.CATEGORY_BASIC,
                         precipCT1());
        createColorTable(l, NAME_RH, ColorTable.CATEGORY_BASIC,
                         percentYelGrnBlue(20));
        createColorTable(l, NAME_TEMPERATURE, ColorTable.CATEGORY_BASIC,
                         temperatureCT4());
        createColorTable(l, NAME_VISAD, ColorTable.CATEGORY_BASIC,
                         rainbow(30));
        createColorTable(l, NAME_INVVISAD, ColorTable.CATEGORY_BASIC,
                         invRainbow(30));

        createColorTable(l, NAME_WINDCOMPS, ColorTable.CATEGORY_BASIC,
                         windSpeed(20));
        createColorTable(l, NAME_WINDSPEED, ColorTable.CATEGORY_BASIC,
                         windspeed());
        createColorTable(l, NAME_GRAY_SCALE, ColorTable.CATEGORY_BASIC,
                         grayTable(256, false));
        createColorTable(l, NAME_INVERSE_GRAY_SCALE,
                         ColorTable.CATEGORY_BASIC, grayTable(256, true));


        createColorTable(l, NAME_DBZ, ColorTable.CATEGORY_RADAR, DZ2());
        createColorTable(l, NAME_DBZNWS, ColorTable.CATEGORY_RADAR, DZ1());

        createColorTable(l, NAME_GRAY_SCALE_SAT,
                         ColorTable.CATEGORY_SATELLITE, grayTable1(30));
        createColorTable(l, NAME_TOPOSAT, ColorTable.CATEGORY_SATELLITE,
                         makeTableFromET("TOPOSAT.ET"));
        createColorTable(l, NAME_H2O, ColorTable.CATEGORY_SATELLITE,
                         makeTableFromET("H2O.ET"));
        createColorTable(l, NAME_TOPOBATHY, ColorTable.CATEGORY_MISC,
                         makeTableFromET("TOPO.ET"), false,
                         new Range(-12458, 8791));
        createColorTable(l, NAME_RADAR_PRECIP, ColorTable.CATEGORY_RADAR,
                         makeTableFromET("PRET.ET"));
        createColorTable(l, NAME_VIS_FOG, ColorTable.CATEGORY_SATELLITE,
                         makeTableFromET("VISFOG.ET"));

        createColorTable(l, NAME_TOPO, ColorTable.CATEGORY_MISC,
                         topographyCT());

        //Add the solid colors
        Color[] colors = {
            Color.black, Color.white, Color.yellow, Color.cyan, Color.red,
            Color.blue, Color.gray, Color.green, Color.magenta, Color.orange,
            Color.pink
        };
        String[] cNames = {
            "Black", "White", "Yellow", "Cyan", "Red", "Blue", "Gray",
            "Green", "Magenta", "Orange", "Pink"
        };
        for (int i = 0; i < colors.length; i++) {
            createColorTable(l, cNames[i], ColorTable.CATEGORY_SOLID,
                             allOneColor(colors[i]));
        }

        String[] radarLabels = {
            NAME_BREF, NAME_BREF24, NAME_CREF, NAME_RVEL, NAME_TOPS, NAME_VIL,
            NAME_TOPOMDR, NAME_MDR
        };
        String[] radarNames = {
            "BREF.ET", "BREF24.ET", "CREF.ET", "RVEL.ET", "TOPS.ET", "VIL.ET",
            "TOPOMDR.ET", "MDR.ET"
        };
        for (int i = 0; i < radarLabels.length; i++) {
            createColorTable(l, radarLabels[i], ColorTable.CATEGORY_RADAR,
                             makeTableFromET(radarNames[i]));
        }

        //Do the ATD ones
        for (int i = 0; i < ATD_Data.length; i++) {
            //Pass in true because the table is flipped
            createColorTable(l, ATD_Names[i],
                             ColorTable.CATEGORY_RADAR + ">ATD", ATD_Data[i],
                             true);
        }

        return l;
    }



    /**
     * Make the standard VisAD rainbow (blue lo to red high) color table
     * with definable length.
     *
     * @param len length; how many entries desired in the color table.
     *
     * @return float[3][len] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] rainbow(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            throw new IllegalArgumentException(
                " Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        for (int i = 0; i < len; i++) {
            float a = ((float) i) / (float) (len - 1);  //fraction from 0 to 1
            table[0][i] = a;  // red amount = fraction
            if ((float) i <= ((float) (len - 1) / 2.0f)) {
                table[1][i] = 2.0f * a;  // 1st half green increases 
            } else {
                table[1][i] = 2.0f - 2.0f * a;  // 2nd half green decreases
            }
            table[2][i] = 1.0f - a;                     // blue = 1 - fraction
        }

        return table;

    }

    /**
     * Make the first temperature color table.
     * Pale blue to dark blue in lower bins, for below 0 C or 273.15 Kelvin;
     * white thru yellow, orange, red-orange to red
     * in upper bins over 0 C to 50C.
     * designed for 5-degrees-wide Celcius bands.
     * SHOULD do scalarMap.setRange -85 to +50 Celsius.
     *
     * @return float[3][27] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] temperatureCT1()
            throws IllegalArgumentException {
        int       len   = 27;

        float[][] table = new float[3][len];
        // indices 0 to 13; first 14 bands, 
        // from -85 C to -5 C
        for (int i = 0; i < 14; i++) {
            float a = ((float) i) / 15.0f;  // fraction from 0 to 1
            table[0][i] = a * 0.6f;         // red 
            table[1][i] = a * 0.60f;        // green
            table[2][i] = 1.0f;             // full blue on always here
        }

        // cell index 14; -5 to 0 C; near white
        table[0][14] = 0.85f;  // red 
        table[1][14] = 0.85f;  // green
        table[2][14] = 0.95f;  // blue 

        // indices 15 thru 26 span bands from 0 to 50 C 
        for (int i = 15; i < len; i++) {
            float a = ((float) (i - 17)) / 9.0f;  // fraction from 0 to 1
            table[0][i] = 1.0f;                   // red (all on)
            table[1][i] = 1.0f - 1.0f * a;        // green
            table[2][i] = 0.0f;                   // 0.9f - 2.0f * a; // blue
            if (table[1][i] < 0.0) {
                table[1][i] = 0.0f;
            }
            if (table[2][i] < 0.0) {
                table[2][i] = 0.0f;
            }
        }

        return table;
    }

    /**
     * Make the fourth temperature color table.
     * This is notably better than temperature color tables CT1, CT2, or CT3.
     * SHOULD also set the data's scalarMap.setRange -90 to +45 Celsius.
     * for temperature to lock blue transition to freezing point.
     *
     * also happens to make a good color table for wind speed 0 to 60 m/s or so.
     *
     * @return float[3][135] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] temperatureCT4()
            throws IllegalArgumentException {
        int       temp, i,
                  len   = 135;  //max index 134

        float[][] table = new float[3][len];

        // temperatures -90 to -1 C  
        for (temp = -89; temp < 0; temp++) {
            i = temp + 89;
            float a = ((float) (temp + 89)) / 89.0f;  // fraction
            table[0][i] = 0.0f;                       // red 
            table[1][i] = 0.0f + a * 250.0f;          // green
            table[2][i] = 250.0f;                     // blue
        }

        // temperatures 0 to 4 C  bluegreen to green
        for (temp = 0; temp < 5; temp++) {
            i = temp + 89;
            float a = ((float) (temp)) / 4.0f;  // fraction
            table[0][i] = 0.0f;                 // red 
            table[1][i] = 255.0f;               // green
            table[2][i] = 255 - 205 * a;        // blue
        }

        // temperatures 5 to 14 C   green to yellow
        for (temp = 5; temp < 15; temp++) {
            i = temp + 89;
            float a = ((float) (temp - 5)) / 9.0f;  // fraction
            table[0][i] = 0.0f + 205.0f * a;        // red 
            table[1][i] = 255.0f;                   // green
            table[2][i] = 0.0f;                     // blue
        }

        // temperatures 15 to 45 C  yellow to red
        for (temp = 15; temp < 46;
                temp++)                               // max temp 45
        {
            i = temp + 89;                            // max i 134
            float a = ((float) (temp - 15)) / 30.0f;  // fraction
            table[0][i] = 255.0f;                     // red 
            table[1][i] = 255.0f - 225.0f * a;        // green
            table[2][i] = 0.0f;                       // blue
        }

        // normalize from 0-255 values to 0.0 to 1.0 values
        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 255.0f;
            }
        }

        //System.out.println ("    VC: made color table CT4 "+table);
        return table;
    }  // end temp CT4

    /**
     * Make a bright temperature spectrum; blue (lo) to red (hi)
     * 5 deg C steps or bands; some duplicate colors below 0
     * Length is fixed at 21 for now.
     * setRange() to -55 to 50 Celsius
     * This looks better than tempCT1 in VisAD 3D surfaces; worse in 2D.
     *
     * @return float[3][21] the color table
     *
     */
    public static final float[][] temperatureCT2() {
        int       len   = 21;
        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values


        table[0][0]  = 0f;
        table[1][0]  = 0f;
        table[2][0]  = 255f;  // -55 - -50
        table[0][1]  = 0f;
        table[1][1]  = 0f;
        table[2][1]  = 255f;  // -50 -
        table[0][2]  = 0f;
        table[1][2]  = 0f;
        table[2][2]  = 255f;  //     - -40
        table[0][3]  = 0f;
        table[1][3]  = 0f;
        table[2][3]  = 255f;  // -40 -
        table[0][4]  = 0f;
        table[1][4]  = 0f;
        table[2][4]  = 255f;  //     - -30

        table[0][5]  = 51f;
        table[1][5]  = 51f;
        table[2][5]  = 255f;  // -30 - 
        table[0][6]  = 51f;
        table[1][6]  = 51f;
        table[2][6]  = 255f;  //     - -20


        table[0][7]  = 90f;
        table[1][7]  = 90f;
        table[2][7]  = 255f;  // -20  -15
        table[0][8]  = 111f;
        table[1][9]  = 111f;
        table[2][9]  = 255f;  // -15  -10
        table[0][9]  = 132f;
        table[1][9]  = 132f;
        table[2][9]  = 255f;  // -10   -5
        table[0][10] = 153f;
        table[1][10] = 153f;
        table[2][10] = 255f;  // -5    0
        //  0 Celsius
        table[0][11] = 153f;
        table[1][11] = 153f;
        table[2][11] = 255f;  //  0- 5
        table[0][12] = 153f;
        table[1][12] = 255f;
        table[2][12] = 255f;  //  5-10
        table[0][13] = 127f;
        table[1][13] = 255f;
        table[2][13] = 177f;  // 10-15
        table[0][14] = 102f;
        table[1][14] = 255f;
        table[2][14] = 102f;  // 15-20
        table[0][15] = 177f;
        table[1][15] = 255f;
        table[2][15] = 51f;   // 20-25
        table[0][16] = 255f;
        table[1][16] = 255f;
        table[2][16] = 102f;  // 25-30 
        table[0][17] = 255f;
        table[1][17] = 201f;
        table[2][17] = 51f;   // 30 35
        table[0][18] = 255f;
        table[1][18] = 153f;
        table[2][18] = 0f;    // 35 40
        table[0][19] = 255f;
        table[1][19] = 140f;
        table[2][19] = 0f;    // 40 45
        table[0][20] = 255f;
        table[1][20] = 102f;
        table[2][20] = 0f;    // 45 50

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 256.0f;
            }
        }

        return table;
    }  // end temperatureCT2


    /**
     * Make a third temperature spectrum; NEEDS WORK
     * setRange() to -55 to 50 Celsius
     *
     * @return float[2][21] the color table
     *
     */
    public static final float[][] temperatureCT3() {
        int       len   = 21;
        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values

        table[0][0]  = 0f;
        table[1][0]  = 0f;
        table[2][0]  = 245f;  // -55 - -50
        table[0][1]  = 0f;
        table[1][1]  = 70f;
        table[2][1]  = 220f;  // -50 -
        table[0][2]  = 0f;
        table[1][2]  = 110f;
        table[2][2]  = 195f;  //     - -40
        table[0][3]  = 0f;
        table[1][3]  = 135f;
        table[2][3]  = 170f;  // -40 -
        table[0][4]  = 0f;
        table[1][4]  = 175f;
        table[2][4]  = 145f;  //     - -30

        table[0][5]  = 145f;
        table[1][5]  = 215f;
        table[2][5]  = 0f;    // -30 - 
        table[0][6]  = 150f;
        table[1][6]  = 255f;
        table[2][6]  = 0f;    //     - -20

        table[0][7]  = 170f;
        table[1][7]  = 255f;
        table[2][7]  = 0f;    // -20 - 
        table[0][9]  = 190f;
        table[1][9]  = 255f;
        table[2][9]  = 0f;    //     - -10

        table[0][10] = 215f;
        table[1][10] = 255f;
        table[2][10] = 0f;    // -10 - 
        table[0][11] = 235f;
        table[1][11] = 255f;
        table[2][11] = 0f;    //    - 0

        // above 0 yellow to red
        table[0][12] = 255f;
        table[1][12] = 255f;
        table[2][12] = 0f;  // 0 - 5
        table[0][13] = 255f;
        table[1][13] = 230f;
        table[2][13] = 0f;  // 5 - 10
        table[0][14] = 255f;
        table[1][14] = 205f;
        table[2][14] = 0f;  // 10 - 15
        table[0][15] = 255f;
        table[1][15] = 180f;
        table[2][15] = 0f;  // 15 - 20
        table[0][16] = 255f;
        table[1][16] = 155f;
        table[2][16] = 0f;  // 20 - 25
        table[0][17] = 255f;
        table[1][17] = 130f;
        table[2][17] = 0f;  // 25 - 30
        table[0][18] = 255f;
        table[1][18] = 105f;
        table[2][18] = 0f;  // 30 - 35
        table[0][19] = 255f;
        table[1][19] = 80f;
        table[2][19] = 0f;  // 40 - 45
        table[0][20] = 255f;
        table[1][20] = 55f;
        table[2][20] = 0f;  // 45 - 50

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 256.0f;
            }
        }

        return table;
    }

    /**
     * Make The first standard Unidata MetApps liquid precip amount
     * color table.  Pale blue to dark blue to green to
     * yellow, to orange, to red-orange to red. Length 100.
     * SHOULD also do Contour2DDisplayable::setRangeforColor(0.0, 25.0)
     * so that first color band (above black) starts at 25/100 = 0.25.
     *
     * @return float[3][100] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] precipCT1()
            throws IllegalArgumentException {
        int       len   = 100;
        float[][] table = new float[3][len];

        // lowest interval is for no precip or less than "trace": white
        // to match default white background;
        // this means there is nothing shown where there is no precip,
        // as desired.
        table[0][0] = 1.0f;
        table[1][0] = 1.0f;
        table[2][0] = 1.0f;

        // indices 0 to 19; first 20 bands, blue to -
        for (int i = 1; i < 20; i++) {
            float a = ((float) i) / 19.0f;  // fraction from 0 to 1
            table[0][i] = a * 0.5f;         // red 
            table[1][i] = a * 1.0f;         // green
            table[2][i] = 1.0f;             // full blue on always here
        }
        for (int i = 20; i < len; i++) {
            float a = ((float) (i - 20)) / 79.0f;  // fraction from 0 to 1
            table[0][i] = 0.5f + a * 0.5f;         // red 
            table[1][i] = 1.0f - 0.6f * a;         // green
            table[2][i] = 0.9f - 2.0f * a;         // blue
            if (table[2][i] < 0.0) {
                table[2][i] = 0.0f;
            }
        }

        return table;
    }

    /**
     * Make the standard VisAD inverse rainbow (red to blue) table
     * with definable length; created by by Ugo Taddei.
     *
     * @param len length; how many entries desired in the color table.
     *
     * @return float[3][len] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] invRainbow(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            throw new IllegalArgumentException(
                " Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        for (int i = 0; i < len; i++) {
            float a = ((float) i) / (float) (len - 1);
            table[2][i] = a;
            if ((float) i <= ((float) (len - 1) / 2.0f)) {
                table[1][i] = 2.0f * a;
            } else {
                table[1][i] = 2.0f - 2.0f * a;
            }
            table[0][i] = 1.0f - a;
        }

        return table;

    }

    /**
     * Make  a color table ranging from white to dark blue,
     * typically for parms with 0 to 100%.
     * setRange of values 0.0 to 100.0
     * Used for relative humidity, probability of precip (rain, snow); fog.
     *
     * @param len length; how many entries desired in the color table.
     *
     * @return float[3][len] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] percentBlue(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            throw new IllegalArgumentException(
                " Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        for (int i = 0; i < len; i++) {
            float a = ((float) i) / (float) (len - 1);
            table[0][i] = 1.0f - a;  // Red amount
            table[1][i] = 1.0f - a;  // Green
            table[2][i] = 1.0f;      // Blue
        }
        return table;
    }


    /**
     * Make a color table ranging from black to white; linear changes
     * in RGB amounts.
     *
     * @param  numColors   number of descrete colors for the table
     * @param  inverse   true for white to black instead of black to white
     * @return float[3][len] the color table
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] grayTable(int numColors, boolean inverse)
            throws IllegalArgumentException {

        float[][] table = new float[3][numColors];

        float     scale = (float) (1.0f / (float) (numColors - 1));
        for (int i = 0; i < numColors; i++) {
            float a = (inverse)
                      ? ((float) numColors - i)
                      : ((float) i);
            table[0][i] = a * scale;  // Red amount
            table[1][i] = a * scale;  // Green
            table[2][i] = a * scale;  // Blue
        }
        return table;
    }

    /**
     * Make a color table ranging from black to white; linear changes
     * in RGB amounts.
     * Offset; begins at X gray25, RGB = 64, 64, 64 out of 255
     *
     * @param len length; how many entries desired in the color table.
     * @return float[3][len] the color table
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] grayTable1(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            throw new IllegalArgumentException(
                " Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        for (int i = 0; i < len; i++) {
            //float a =((float) i) / ((float) (len-1));// pure linear from 0
            float a = (float) (0.245
                               + ((float) i) / (1.33f * (float) (len - 1)));
            table[0][i] = a;  // Red amount
            table[1][i] = a;  // Green
            table[2][i] = a;  // Blue
        }
        return table;
    }

    /**
     * Make a color table ranging from yellow to dark blue,
     * typically for parms with 0 to 100%.
     * setRange of values 0.0 to 100.0
     * Can use for relative humidity, probability of precip.
     *
     * Problem: 50% RH color is dull grey; see percentYelGrnBlue()
     *
     * @param len length; how many entries desired in the color table.
     *
     * @return float[3][len] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] percentYellowBlue(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            throw new IllegalArgumentException(
                " Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        for (int i = 0; i < len; i++) {
            float a = ((float) i) / (float) (len - 1);
            table[0][i] = 1.0f - a;  // Red amount
            table[1][i] = 1.0f - a;  // Green
            table[2][i] = a;         // Blue  
        }
        return table;
    }


    /**
     * Make a color table ranging from yellow to blue, through green;
     * typically for parms with values spanning 0 to 100 %.
     * setRange of values 0.0 to 100.0.
     * Can use for relative humidity, probability of precip, etc.
     * Espcially good where a higher percewnt is associated with mnore moisture.
     *
     * @param len length; how many entries desired in the color table.
     *
     * @return float[3][len] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] percentYelGrnBlue(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            throw new IllegalArgumentException(
                " Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        for (int i = 0; i < len; i++) {
            float a = ((float) i) / (float) (len - 1);
            table[0][i] = 1.0f - 2 * a;   // Red amount
            if (table[0][i] < 0.0) {
                table[0][i] = 0.0f;
            }

            table[1][i] = 1.0f;           // Green
            if (a >= 0.5) {
                table[1][i] = 1.5f - a;
            }

            table[2][i] = -1.0f + 2 * a;  // Blue 
            if (table[2][i] < 0.0) {
                table[2][i] = 0.0f;
            }
        }
        return table;
    }


    /**
     * Make a color table ranging from white to dark blue (negatives);
     * white to green going 0 to large positive.
     *
     * center SetRange of values on 0.0 for proper alignment with colors.
     * Used for speed of wind components u and v, which can have negative speeds.
     *
     * @param len length; how many entries desired in the color table.
     *
     * @return float[3][len] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] windSpeed(int len)
            throws IllegalArgumentException {

        if (len <= 0) {
            len = 30;
            //System.out.println(
            //  " Color Table length must be greater than 0");
        }

        float[][] table = new float[3][len];

        // blue end: pale blue at 0.0 to dark blue at low (negative) end
        for (int i = 0; i < len / 2; i++) {
            float frac = 1.75f * (((float) i) / (float) (len / 2));
            table[0][i] = frac * 60f;       // Red amount  
            table[1][i] = frac * 140;       // Green
            table[2][i] = 180 + frac * 45;  // Blue
        }
        for (int i = (len / 2); i < len; i++) {
            float frac = 2.0f * (-0.5f + ((float) i) / (float) (len - 1));
            table[0][i] = 65.0f;                   // Red amount  
            table[1][i] = 206.0f + 49.0f * frac;   // Green
            table[2][i] = 255.0f - 200.0f * frac;  // Blue  
        }

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] = table[n][m] / 256.0f;
            }
        }

        return table;
    }

    /**
     *  Make a 38-color bright spectrum; blue (low values) to red (high values).
     *  These are close to or at the brightest possible colors.
     *  Good for general purpose with higher values needing warmer colors.
     *
     *  @return float[3][38] the color table
     *
     */
    public static final float[][] bright38() {

        int       len   = 38;
        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values
        //             red              green               blue
        table[0][0]  = 255f;
        table[1][0]  = 0f;
        table[2][0]  = 226f;  // violet
        table[0][1]  = 189f;
        table[1][1]  = 1f;
        table[2][1]  = 255f;
        table[0][2]  = 166f;
        table[1][2]  = 1f;
        table[2][2]  = 255f;
        table[0][3]  = 135f;
        table[1][3]  = 1f;
        table[2][3]  = 255f;
        table[0][4]  = 112f;
        table[1][4]  = 1f;
        table[2][4]  = 255f;
        table[0][5]  = 82f;
        table[1][5]  = 1f;
        table[2][5]  = 255f;
        table[0][6]  = 59f;
        table[1][6]  = 1f;
        table[2][6]  = 255f;
        table[0][7]  = 29f;
        table[1][7]  = 1f;
        table[2][7]  = 255f;
        table[0][8]  = 1f;
        table[1][8]  = 3f;
        table[2][8]  = 255f;  // blue
        table[0][9]  = 1f;
        table[1][9]  = 34f;
        table[2][9]  = 255f;
        table[0][10] = 1f;
        table[1][10] = 57f;
        table[2][10] = 255f;
        table[0][11] = 1f;
        table[1][11] = 79f;
        table[2][11] = 255f;
        table[0][12] = 1f;
        table[1][12] = 140f;
        table[2][12] = 255f;
        table[0][13] = 0f;
        table[1][13] = 170f;
        table[2][13] = 255f;
        table[0][14] = 0f;
        table[1][14] = 209f;
        table[2][14] = 255f;
        table[0][15] = 0f;
        table[1][15] = 232f;
        table[2][15] = 255f;  //blugr
        table[0][16] = 1f;
        table[1][16] = 255f;
        table[2][16] = 232f;
        table[0][17] = 0f;
        table[1][17] = 255f;
        table[2][17] = 201f;
        table[0][18] = 0f;
        table[1][18] = 255f;
        table[2][18] = 170f;
        table[0][19] = 0f;
        table[1][19] = 255f;
        table[2][19] = 140f;
        table[0][20] = 0f;
        table[1][20] = 255f;
        table[2][20] = 110f;
        table[0][21] = 0f;
        table[1][21] = 255f;
        table[2][21] = 80f;
        table[0][22] = 0f;
        table[1][22] = 255f;
        table[2][22] = 40f;
        table[0][23] = 0f;
        table[1][23] = 255f;
        table[2][23] = 0f;    // green
        table[0][24] = 43f;
        table[1][24] = 255f;
        table[2][24] = 0f;
        table[0][25] = 89f;
        table[1][25] = 255f;
        table[2][25] = 0f;
        table[0][26] = 127f;
        table[1][26] = 255f;
        table[2][26] = 0f;
        table[0][27] = 165f;
        table[1][27] = 255f;
        table[2][27] = 0f;
        table[0][28] = 196f;
        table[1][28] = 255f;
        table[2][28] = 1f;
        table[0][29] = 227f;
        table[1][29] = 255f;
        table[2][29] = 1f;
        table[0][30] = 255f;
        table[1][30] = 244f;
        table[2][30] = 0f;    // nr yel
        table[0][31] = 255f;
        table[1][31] = 200f;
        table[2][31] = 0f;
        table[0][32] = 255f;
        table[1][32] = 168f;
        table[2][32] = 0f;
        table[0][33] = 255f;
        table[1][33] = 138f;
        table[2][33] = 0f;
        table[0][34] = 255f;
        table[1][34] = 107f;
        table[2][34] = 0f;
        table[0][35] = 255f;
        table[1][35] = 69f;
        table[2][35] = 0f;
        table[0][36] = 255f;
        table[1][36] = 31f;
        table[2][36] = 0f;
        table[0][37] = 255f;
        table[1][37] = 0f;
        table[2][37] = 0f;    // red

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 256.0f;
            }
        }

        return table;
    }

    /**
     * Make 16 color bright spectrum; blue (lo) to red (hi)
     *
     * @return float[3][16] the color table
     *
     */
    public static final float[][] bright16() {
        int       len   = 16;
        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values

        table[0][0]  = 0f;
        table[1][0]  = 0f;
        table[2][0]  = 255f;  // blue end 
        table[0][1]  = 51f;
        table[1][1]  = 51f;
        table[2][1]  = 255f;
        table[0][2]  = 102f;
        table[1][2]  = 102f;
        table[2][2]  = 255f;
        table[0][3]  = 153f;
        table[1][3]  = 153f;
        table[2][3]  = 255f;
        table[0][4]  = 153f;
        table[1][4]  = 255f;
        table[2][4]  = 255f;
        table[0][5]  = 102f;
        table[1][5]  = 255f;
        table[2][5]  = 255f;
        table[0][6]  = 51f;
        table[1][6]  = 255f;
        table[2][6]  = 255f;
        table[0][7]  = 102f;
        table[1][7]  = 255f;
        table[2][7]  = 102f;
        table[0][8]  = 51f;
        table[1][8]  = 255f;
        table[2][8]  = 51f;
        table[0][9]  = 255f;
        table[1][9]  = 255f;
        table[2][9]  = 102f;
        table[0][10] = 255f;
        table[1][10] = 255f;
        table[2][10] = 51f;
        table[0][11] = 255f;
        table[1][11] = 255f;
        table[2][11] = 0f;
        table[0][12] = 255f;
        table[1][12] = 204f;
        table[2][12] = 102f;
        table[0][13] = 255f;
        table[1][13] = 153f;
        table[2][13] = 0f;
        table[0][14] = 255f;
        table[1][14] = 102f;
        table[2][14] = 102f;
        table[0][15] = 255f;
        table[1][15] = 51f;
        table[2][15] = 51f;   // RED END

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 256.0f;
            }
        }

        return table;
    }

    /**
     * Make 14 color bright spectrum; blue (lo) to red (hi)
     *
     * @return float[3][14] the color table
     *
     */
    public static final float[][] bright14() {
        int       len   = 14;
        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values

        table[0][0]  = 0f;
        table[1][0]  = 0f;
        table[2][0]  = 255f;  // blue end 
        table[0][1]  = 51f;
        table[1][1]  = 51f;
        table[2][1]  = 255f;
        table[0][2]  = 102f;
        table[1][2]  = 102f;
        table[2][2]  = 255f;
        table[0][3]  = 153f;
        table[1][3]  = 153f;
        table[2][3]  = 255f;
        table[0][4]  = 153f;
        table[1][4]  = 255f;
        table[2][4]  = 255f;
        table[0][5]  = 102f;
        table[1][5]  = 255f;
        table[2][5]  = 255f;
        table[0][6]  = 51f;
        table[1][6]  = 255f;
        table[2][6]  = 255f;
        table[0][7]  = 102f;
        table[1][7]  = 255f;
        table[2][7]  = 102f;
        table[0][8]  = 51f;
        table[1][8]  = 255f;
        table[2][8]  = 51f;
        table[0][9]  = 255f;
        table[1][9]  = 255f;
        table[2][9]  = 102f;
        table[0][10] = 255f;
        table[1][10] = 255f;
        table[2][10] = 51f;
        table[0][11] = 255f;
        table[1][11] = 255f;
        table[2][11] = 0f;
        table[0][12] = 255f;
        table[1][12] = 204f;
        table[2][12] = 102f;
        table[0][13] = 255f;
        table[1][13] = 153f;
        table[2][13] = 0f;

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 256.0f;
            }
        }

        return table;
    }

    /**
     * Make 12 color bright spectrum; blue (lo) to red (hi)
     *
     * @return float[3][12] the color table
     *
     */
    public static final float[][] bright12() {
        int       len   = 12;
        float[][] table = new float[3][len];

        table[0][0]  = 1f;
        table[1][0]  = 57f;
        table[2][0]  = 255f;  // R,G,B in first []
        table[0][1]  = 1f;
        table[1][1]  = 140f;
        table[2][1]  = 255f;  // RGB
        table[0][2]  = 1f;
        table[1][2]  = 209f;
        table[2][2]  = 255f;  // RGB
        table[0][3]  = 1f;
        table[1][3]  = 255f;
        table[2][3]  = 232f;  // RGB
        table[0][4]  = 0f;
        table[1][4]  = 255f;
        table[2][4]  = 201f;  // RGB
        table[0][5]  = 0f;
        table[1][5]  = 255f;
        table[2][5]  = 80f;   // RGB
        table[0][6]  = 43f;
        table[1][6]  = 255f;
        table[2][6]  = 0f;    // RGB
        table[0][7]  = 165f;
        table[1][7]  = 255f;
        table[2][7]  = 0f;    // RGB
        table[0][8]  = 227f;
        table[1][8]  = 255f;
        table[2][8]  = 1f;    // RGB
        table[0][9]  = 255f;
        table[1][9]  = 199f;
        table[2][9]  = 0f;    // RGB
        table[0][10] = 255f;
        table[1][10] = 138f;
        table[2][10] = 0f;    // RGB
        table[0][11] = 255f;
        table[1][11] = 69f;
        table[2][11] = 0f;    // RGB

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] = table[n][m] / 256.0f;
            }
        }

        return table;
    }

    /**
     * Provide a color table suitable for topography
     *
     * @return float[3][256] the color table
     *
     */
    public static final float[][] topographyCT() {
        /*
          // blue high yellow low
        int len = 9;
        float[][] table = new float[3][len];

        // set values in 0 to 255 range
        table[0][8]= 1f; table[1][8]= 57f; table[2][8]= 255f; // R,G,B in first []
        table[0][7]= 1f; table[1][7]= 140f; table[2][7]= 255f; // RGB
        table[0][6]= 1f; table[1][6]= 209f; table[2][6]= 255f; // RGB
        table[0][5]= 1f; table[1][5]=255f; table[2][5]= 232f; // RGB
        table[0][4]= 0f; table[1][4]= 255f; table[2][4]= 201f; // RGB
        table[0][3]= 0f; table[1][3]= 255f; table[2][3]= 80f; // RGB  GREENish
        table[0][2]= 43f; table[1][2]= 255f; table[2][2]= 0f; // RGB
        table[0][1]= 165f; table[1][1]= 255f; table[2][1]= 0f; // RGB
        table[0][0]= 227f; table[1][0]= 255f; table[2][0]= 1f; // RGB YELLOWish

        // convert to floats in 0.0 to 1.0 range
        for (int n=0; n<3; n++)
            for (int m=0; m<len; m++)
                table[n][m] = table [n][m]/256.0f;

        return table;
        */

        // by Don Murray
        int       len   = 256;
        float[][] table = new float[3][len];
        float[]   red   = new float[] {
            25.0f, 25.0f, 20.0f, 20.0f, 70.0f, 165.0f, 200.0f
        };
        /* revised green factor creates tan at high altitudes: */
        float[] green = new float[] {
            25.0f, 25.0f, 170.0f, 170.0f, 200.0f, 165.0f, 200.0f
        };
        /* original verison is strongly red at high altitudes: */
        /*new float[] { 25.0f, 25.0f,170.0f, 170.0f, 200.0f,  42.0f, 200.0f};*/
        float[] blue = new float[] {
            255.0f, 255.0f, 42.0f, 42.0f, 0.0f, 42.0f, 200.0f
        };
        // limits of altitude ranges in km used for color bands:
        float[] range =  // km
            new float[] {
            0.0f, 0.010f, 0.015f, 0.03f, 0.1f, 1.0f, 2.8f
        };
        float   x0, x1;
        float   r, g, b, dr, dg, db;

        float   minhgt = range[0];
        float   maxhgt = range[range.length - 1];
        for (int i = 0; i < 6; i++) {
            x0 = (range[i] - minhgt) / (maxhgt - minhgt) * (float) (len - 1);
            x1 = (range[i + 1] - minhgt) / (maxhgt - minhgt)
                 * (float) (len - 1);
            dr = (red[i + 1] - red[i]) / (x1 - x0);
            dg = (green[i + 1] - green[i]) / (x1 - x0);
            db = (blue[i + 1] - blue[i]) / (x1 - x0);
            r  = red[i];
            g  = green[i];
            b  = blue[i];
            for (int j = (int) x0; j < (int) x1; j++) {
                if ((j >= 0) && (j < (len - 1))) {
                    table[0][j] = r / 255.f;
                    table[1][j] = g / 255.f;
                    table[2][j] = b / 255.f;
                }
                r += dr;
                g += dg;
                b += db;
            }
        }
        table[0][len - 1] = 1f;
        table[1][len - 1] = 1f;
        table[2][len - 1] = 1f;
        return table;
    }



    /**
     * Make a color table representing this color.
     *
     * @param  color  color to use
     * @return the color table this color
     */
    public static final float[][] allOneColor(Color color) {
        return allOneColor(color, false);
    }

    /**
     * Make a color table representing this color.
     *
     * @param  color  color to use
     * @return the color table this color
     * @param addAlpha true to add an alpha channel
     *
     * @return the table
     */
    public static final float[][] allOneColor(Color color, boolean addAlpha) {
        int       len   = 5;
        float[][] table = new float[(addAlpha
                                     ? 4
                                     : 3)][len];
        for (int m = 0; m < len; m++) {
            table[0][m] = color.getRed() / 255.f;    // Red amount  
            table[1][m] = color.getGreen() / 255.f;  // Green
            table[2][m] = color.getBlue() / 255.f;   // Blue  
            if (addAlpha) {
                table[3][m] = 1.0f;
            }
        }
        return table;
    }

    /**
     * Make a color table for radar reflectivity as close as possible
     * to the one used by the NWS in their web page displays of same.
     *
     * It turns out that this color table used in VisAD makes a crummy
     * appearence (due to VisAD blurring colors between values?).
     * Smoother transistions of color do much better in VisAD displays,
     * such as in color table DZ2 below.
     *
     * for data displayed, setRange() 0 to 80 (dbz); 16 bins
     *
     * @return float[3][16] the color table
     *
     */
    public static final float[][] DZ1() {
        int       len   = 16;

        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values

        table[0][0]  = 0f;
        table[1][0]  = 0f;
        table[2][0]  = 0f;    //  0-5

        table[0][1]  = 0f;
        table[1][1]  = 255f;
        table[2][1]  = 255f;  //  5-10
        table[0][2]  = 135f;
        table[1][2]  = 206f;
        table[2][2]  = 235f;  // 10-15
        table[0][3]  = 0f;
        table[1][3]  = 0f;
        table[2][3]  = 255f;  // 15-20
        table[0][4]  = 0f;
        table[1][4]  = 255f;
        table[2][4]  = 0f;    // 20-25
        table[0][5]  = 50f;
        table[1][5]  = 205f;
        table[2][5]  = 50f;   // 25-30

        table[0][6]  = 34f;
        table[1][6]  = 139f;
        table[2][6]  = 34f;   // 30-35
        table[0][7]  = 238f;
        table[1][7]  = 238f;
        table[2][7]  = 0f;    // 35-40
        table[0][8]  = 238f;
        table[1][8]  = 220f;
        table[2][8]  = 130f;  // 40-45
        table[0][9]  = 238f;
        table[1][9]  = 118f;
        table[2][9]  = 33f;   // 45-50
        table[0][10] = 255f;
        table[1][10] = 48f;
        table[2][10] = 48f;   // 50-55

        table[0][11] = 176f;
        table[1][11] = 48f;
        table[2][11] = 96f;   // 55-60
        table[0][12] = 176f;
        table[1][12] = 48f;
        table[2][12] = 96f;   // 60-65
        table[0][13] = 186f;
        table[1][13] = 85f;
        table[2][13] = 211f;  // 65-70
        table[0][14] = 255f;
        table[1][14] = 0f;
        table[2][14] = 255f;  // 70-75
        table[0][15] = 255f;
        table[1][15] = 255f;
        table[2][15] = 255f;  // 75-80

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 255.0f;
            }
        }

        return table;
    }

    /**
     * A 2nd color table for radar reflectivity, better than DZ1 for VisAD.
     *
     * for data displayed, setRange() -10.0 to 60.0 (dbz); 16 bins. by Stu Wier.
     *
     * @return float[3][16] the color table
     *
     */
    public static final float[][] DZ2() {
        int       len   = 16;

        float[][] table = new float[3][len];  // [0][] [1][] [2][] = RGB values

        table[0][0]  = 1f;
        table[1][0]  = 57f;
        table[2][0]  = 255f;  //  

        table[0][1]  = 0f;
        table[1][1]  = 140f;
        table[2][1]  = 255f;  //  
        table[0][2]  = 1f;
        table[1][2]  = 209f;
        table[2][2]  = 255f;  // 
        table[0][3]  = 1f;
        table[1][3]  = 255f;
        table[2][3]  = 232f;  // 
        table[0][4]  = 1f;
        table[1][4]  = 255f;
        table[2][4]  = 171f;  // 
        table[0][5]  = 1f;
        table[1][5]  = 255f;
        table[2][5]  = 79f;   // 

        table[0][6]  = 43f;
        table[1][6]  = 255f;
        table[2][6]  = 0f;    // 
        table[0][7]  = 166f;
        table[1][7]  = 255f;
        table[2][7]  = 2f;    // 
        table[0][8]  = 227f;
        table[1][8]  = 255f;
        table[2][8]  = 1f;    // 
        table[0][9]  = 255f;
        table[1][9]  = 198f;
        table[2][9]  = 0f;    // 
        table[0][10] = 255f;
        table[1][10] = 168f;
        table[2][10] = 1f;    // 

        table[0][11] = 255;
        table[1][11] = 145f;
        table[2][11] = 1f;    // 
        table[0][12] = 255f;
        table[1][12] = 130f;
        table[2][12] = 1f;    // 
        table[0][13] = 255f;
        table[1][13] = 107f;
        table[2][13] = 0f;    // 
        table[0][14] = 255f;
        table[1][14] = 84f;
        table[2][14] = 0f;    // 
        table[0][15] = 255f;
        table[1][15] = 7f;
        table[2][15] = 0f;    // 

        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 255.0f;
            }
        }

        return table;
    }

    /**
     * make the nominal wind speed color table, based on color table temperatureCT4.
     * set the data's scalarMap.setRange from 0.0 to say 70 or 80.0 m/s
     *
     * @return float[3][70] the color table
     *
     * @exception IllegalArgumentException  from construction of VisAd objects
     */
    public static final float[][] windspeed()
            throws IllegalArgumentException {
        int       temp, i,
                  len   = 70;  // max index is 69

        float[][] table = new float[3][len];

        //     20 colors from blue to blue-green
        for (temp = 0; temp < 20; temp++) {
            i = temp;
            float a = ((float) (temp)) / 20.0f;  // fraction
            table[0][i] = 0.0f;                  // red 
            table[1][i] = 0.0f + a * 250.0f;     // green
            table[2][i] = 250.0f;                // blue
        }

        //  10 colors  from blue-green to green
        for (temp = 20; temp < 30; temp++) {
            i = temp;
            float a = ((float) (temp - 20)) / 10.0f;  // fraction
            table[0][i] = 0.0f;                       // red 
            table[1][i] = 255.0f;                     // green
            table[2][i] = 255 - 205 * a;              // blue
        }

        //   10  colors from green to slightly-greenish yellow
        for (temp = 30; temp < 40; temp++) {
            i = temp;
            float a = ((float) (temp - 30)) / 10.0f;  // fraction
            table[0][i] = 0.0f + 205.0f * a;          // red 
            table[1][i] = 255.0f;                     // green
            table[2][i] = 0.0f;                       // blue
        }

        //          30 colors from yellow to red
        for (temp = 40; temp < 70; temp++) {
            i = temp;
            float a = ((float) (temp - 40)) / 30.0f;  // fraction
            table[0][i] = 255.0f;                     // red 
            table[1][i] = 255.0f - 225.0f * a;        // green
            table[2][i] = 0.0f;                       // blue
        }

        // normalize from 0-255 values to 0.0 to 1.0 values
        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                table[n][m] /= 255.0f;
            }
        }

        return table;
    }  // end windspeed

    /**
     * Read in and process the mcidas color table
     *
     * @param name File name
     * @return The processed CT data
     *
     * @throws IllegalArgumentException
     */
    public static final float[][] makeTableFromET(String name)
            throws IllegalArgumentException {
        return makeTableFromET(name, true);
    }

    /**
     * Read in and process the mcidas color table
     *
     * @param name File name
     * @param fromAuxdata Is this file from the auxdata dir
     * @return The processed CT data
     *
     * @throws IllegalArgumentException When bad things happen
     */
    public static final float[][] makeTableFromET(String name,
            boolean fromAuxdata)
            throws IllegalArgumentException {
        float colorTable[][] = new float[3][256];
        try {
            EnhancementTable et;
            if (fromAuxdata) {
                URL enhancement = Resource.getURL("/auxdata/ui/colortables/"
                                      + name);
                et = new EnhancementTable(enhancement);
            } else {
                URL url = IOUtil.getURL(name, ColorTableDefaults.class);
                //                et = new EnhancementTable(name);
                et = new EnhancementTable(url);
            }
            int[][] rgbVals = et.getRGBValues();
            for (int j = 0; j < 256; j++) {
                colorTable[0][j] = ((float) rgbVals[0][j]) / 255.f;
                colorTable[1][j] = ((float) rgbVals[1][j]) / 255.f;
                colorTable[2][j] = ((float) rgbVals[2][j]) / 255.f;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
        return colorTable;
    }


    /**
     * Utility to convert list of colors to float array
     *
     * @param colors colors
     *
     * @return color array
     */
    private static float[][] toArray(List colors) {
        float colorTable[][] = new float[3][colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            Color c = (Color) colors.get(i);
            colorTable[0][i] = ((float) c.getRed()) / 255.f;
            colorTable[1][i] = ((float) c.getGreen()) / 255.f;
            colorTable[2][i] = ((float) c.getBlue()) / 255.f;
        }
        return colorTable;
    }

    /**
     * Read in and process the pal1 color table
     *
     * @param name File name
     * @return The processed CT data
     *
     * @throws IOException On badness
     */
    public static final float[][] makeTableFromPal1(String name)
            throws IOException {
        byte[] b = IOUtil.readBytes(IOUtil.getInputStream(name));
        if (b.length < 768) {
            throw new IOException("Not a valid PAL-1 color table");
        }
        List colors = new ArrayList();
        int  minBin = 1;
        int  maxBin = 254;
        for (int i = minBin; i <= maxBin; i++) {
            colors.add(new Color(0xff & b[i], 0xff & b[i + 256],
                                 0xff & b[i + 512]));
        }
        return toArray(colors);
    }




    /**
     * Read in and process the pal2 color table
     *
     * @param name File name
     * @return The processed CT data
     *
     * @throws IOException On badness
     */
    public static final float[][] makeTableFromPal2(String name)
            throws IOException {
        byte[] b = IOUtil.readBytes(IOUtil.getInputStream(name));
        if (b.length < 768) {
            throw new IOException("Not a valid PAL-2 color table");
        }
        List colors = new ArrayList();
        int  minBin = 2;
        int  maxBin = 253;
        for (int i = minBin; i <= maxBin; i++) {
            colors.add(new Color(0xff & b[i], 0xff & b[i + 256],
                                 0xff & b[i + 512]));
        }
        return toArray(colors);
    }


    /**
     * Make GEMPAK color tables
     *
     * @param name  name of the table
     * @param category  the category
     * @param file  the file to read
     *
     * @return  a list of colors in the table (why a list, I don't know)
     */
    public static List makeGempakColorTables(String name, String category,
                                             String file) {
        try {
            List lines = StringUtil.split(IOUtil.readContents(file), "\n",
                                          true, true);
            List colors = new ArrayList();
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                List symbols = StringUtil.split(lines.get(lineIdx), " ",
                                   true, true);
                int   colorCnt   = 0;
                int[] colorArray = new int[] { 0, 0, 0 };
                for (int symbolIdx = 0;
                        (colorCnt < 3) && (symbolIdx < symbols.size());
                        symbolIdx++) {
                    String symbol = (String) symbols.get(symbolIdx);
                    if ((symbolIdx == 0)
                            && (symbol.startsWith("!")
                                || symbol.startsWith("#"))) {
                        continue;
                    }
                    try {
                        int value = toInt(symbol);
                        if ((value < 0) || (value > 255)) {
                            continue;
                        }
                        colorArray[colorCnt++] = value;
                    } catch (NumberFormatException nfe) {}
                }
                if (colorCnt == 3) {
                    colors.add(colorArray);
                }
            }

            float[][] table = new float[3][colors.size()];
            for (int colorIdx = 0; colorIdx < colors.size(); colorIdx++) {
                int[] ca = (int[]) colors.get(colorIdx);
                for (int i = 0; i < 3; i++) {
                    table[i][colorIdx] = ((float) ca[i]) / 255.0f;
                }
            }
            ColorTable ct = new ColorTable(name, category, table);
            ct.getColorList();
            return Misc.newList(ct);
        } catch (Exception exc) {
            LogUtil.logException("Error reading color table file: " + file,
                                 exc);
        }
        return new ArrayList();
    }





    /**
     * Main method
     *
     * @param args arguments
     *
     * @throws Exception  something bad happened
     */
    public static void main(String[] args) throws Exception {

        String line = args[0];



        //        makeRgbColorTables("", "",args[0]);
    }


    /**
     * Make ColorTables from NCAR Command Language (NCL) RGB files
     *
     * @param name  name of table
     * @param cat  category
     * @param file  the file
     * @param contents  the file contents
     *
     * @return  the color table in a List
     *
     * @throws IOException problem opening file
     */
    public static List makeNclRgbColorTables(String name, String cat,
                                             String file, String contents)
            throws IOException {
        if (contents == null) {
            contents = IOUtil.readContents(file);
        }
        List      lines  = StringUtil.split(contents, "\n", true, true);
        ArrayList colors = new ArrayList();
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = (String) lines.get(lineIdx);
            line = line.trim();
            if ((line.length() == 0) || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("ncolors") || line.startsWith("ntsc")) {
                continue;
            }
            List rgb = StringUtil.split(line, " ", true, true);
            if (rgb.size() != 3) {
                throw new IllegalStateException("Bad rgb values in:" + line);
            }
            try {
                colors.add(new Color(toInt(rgb.get(0)), toInt(rgb.get(1)),
                                     toInt(rgb.get(2).toString())));
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException("Bad number format on:"
                        + line + " in file:" + file);
            }
        }
        if ((colors != null) && (colors.size() > 0)) {
            return Misc.newList(makeColorTable(name, cat, colors));
        }
        return null;
    }


    /**
     * Make color tables from RGB tuples in a file
     *
     * @param name  the name of the color table
     * @param cat  the category
     * @param file  the file to read
     * @param lines  the lines
     * @param delimiter  the rgb tuple delimiters
     *
     * @return  the color table in a list
     */
    public static List makeRgbColorTables(String name, String cat,
                                          String file, List lines,
                                          String delimiter) {
        ArrayList colors = new ArrayList();
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = (String) lines.get(lineIdx);
            line = stripComments(line);
            if (line.startsWith("ncolors") || line.startsWith("ntsc")) {
                continue;
            }
            if ((line.length() == 0) || line.startsWith("#")) {
                continue;
            }

            List toks = StringUtil.split(line, delimiter, true, true);
            try {
                colors.add(new Color(toInt(toks.get(0)), toInt(toks.get(1)),
                                     toInt(toks.get(2))));
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException("Bad number format in line:"
                        + line + " from file:" + file);
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Bad color value in line:"
                        + line + " from file:" + file);
            }
        }
        if ((colors != null) && (colors.size() > 0)) {
            return Misc.newList(makeColorTable(name, cat, colors));
        }
        return null;
    }


    /**
     * Turn the object into an int
     *
     * @param o  the object
     *
     * @return  as an int
     */
    private static int toInt(Object o) {
        String s = o.toString().trim();
        return (int) (new Double(s).doubleValue());
    }

    /**
     * Strip comments from a line
     *
     * @param line  the line.
     *
     * @return  the stripped down line
     */
    private static String stripComments(String line) {
        int commentIdx = line.indexOf("/*");
        if (commentIdx >= 0) {
            int end = line.indexOf("*/");
            if (end < 0) {
                return "";
            }
            line = line.substring(0, commentIdx)
                   + line.substring(end + 2, line.length());
        }
        line = line.trim();
        if (line.startsWith("#")) {
            return "";
        }
        return line;

    }

    /**
     * Make color tables from RGB files
     *
     *
     * @param name  name of the color table
     * @param cat   category
     * @param file  the file to read
     *
     * @return  the color table in a List
     *
     * @throws IOException  problem reading the file
     */
    public static List makeRgbColorTables(String name, String cat,
                                          String file)
            throws IOException {
        List   tables   = new ArrayList();
        String contents = IOUtil.readContents(file);
        if (contents.indexOf("ncolors=") >= 0) {
            return makeNclRgbColorTables(name, cat, file, contents);
        }


        List      lines     = StringUtil.split(contents, "\n", true, true);
        ArrayList colors    = null;


        String    delimiter = null;
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = (String) lines.get(lineIdx);

            line = stripComments(line);
            if ((line.length() == 0) || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("*")) {
                if (colors != null) {
                    tables.add(makeColorTable(name, cat, colors));
                    //                    break;
                }
                colors = new ArrayList();
                name   = line.substring(1);
                int index = name.indexOf("#");
                if (index >= 0) {
                    name = name.substring(0, index).trim();
                }
                continue;
            }
            if (delimiter == null) {
                if (line.indexOf(",") >= 0) {
                    delimiter = ",";
                } else {
                    delimiter = " ";
                }
            }
            List toks = StringUtil.split(line, delimiter, true, true);
            //Handle simple rgb

            if (toks.size() == 3) {
                return makeRgbColorTables(name, cat, file, lines, delimiter);
            }
            if (toks.size() != 8) {
                throw new IllegalArgumentException(
                    "Incorrect number of tokens in:" + file + " Read:"
                    + line);
            }

            int idx = 0;
            try {
                int from       = toInt(toks.get(idx++));
                int to         = toInt(toks.get(idx++));
                int fromRed    = toInt(toks.get(idx++));
                int toRed      = toInt(toks.get(idx++));
                int redWidth   = toRed - fromRed + 1;
                int fromGreen  = toInt(toks.get(idx++));
                int toGreen    = toInt(toks.get(idx++));
                int greenWidth = toGreen - fromGreen + 1;
                int fromBlue   = toInt(toks.get(idx++));
                int toBlue     = toInt(toks.get(idx));
                int blueWidth  = toBlue - fromBlue + 1;

                int width      = to - from + 1;
                for (int i = 0; i < width; i++) {
                    double percent = i / (double) width;
                    Color c = new Color((int) (fromRed + redWidth * percent),
                                        (int) (fromGreen
                                            + greenWidth
                                              * percent), (int) (fromBlue
                                                  + blueWidth * percent));

                    colors.add(c);
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException("Bad number format in line:"
                        + line + " from file:" + file);
            }
        }
        if ((colors != null) && (colors.size() > 0)) {
            tables.add(makeColorTable(name, cat, colors));
        }
        return tables;
    }


    /**
     * Make a color table from a list of colors
     *
     * @param name  name for the table
     * @param cat   category for the table
     * @param colors  the list of colors
     *
     * @return  the corresponding ColorTable
     */
    private static ColorTable makeColorTable(String name, String cat,
                                             ArrayList colors) {
        ColorTable ct = new ColorTable(name, cat, null);
        ct.setTable(colors);
        return ct;
    }




    /**
     * Read in and process the act color table
     *
     * @param name File name
     * @return The processed CT data
     *
     * @throws IOException On badness
     */
    public static final float[][] makeTableFromAct(String name)
            throws IOException {
        byte[] b = IOUtil.readBytes(IOUtil.getInputStream(name));
        if (b.length < 768) {
            throw new IOException("Not a valid ACT color table");
        }
        int colorCount = 256;
        if (b.length == 772) {
            if (b[769] > 0) {
                colorCount = b[769];
            } else {
                colorCount = 256 + b[769];
            }
        }
        List colors = new ArrayList();
        for (int i = 0; i < colorCount; i++) {
            colors.add(new Color(0xff & b[i * 3], 0xff & b[i * 3 + 1],
                                 0xff & b[i * 3 + 2]));
        }
        return toArray(colors);
    }




    /**
     * Read in and process the pal color table
     *
     * @param name File name
     * @return The processed CT data
     *
     * @throws IOException On badness
     */
    public static final float[][] makeTableFromPal(String name)
            throws IOException {
        byte[] b = IOUtil.readBytes(IOUtil.getInputStream(name));
        if (b.length < 768) {
            throw new IOException("Not a valid ACT color table");
        }
        List colors = new ArrayList();
        for (int i = 0; i < 256; i++) {
            colors.add(new Color(0xff & b[i], 0xff & b[i + 256],
                                 0xff & b[i + 512]));

        }
        return toArray(colors);
    }




    /** Fixed values for ATD_Scook color table */
    private static float[][] ATD_Scook = {
        { 0.926f, 0.012f, 0.938f }, { 0.898f, 0.035f, 0.941f },
        { 0.875f, 0.055f, 0.941f }, { 0.836f, 0.074f, 0.938f },
        { 0.801f, 0.094f, 0.934f }, { 0.762f, 0.113f, 0.930f },
        { 0.727f, 0.133f, 0.930f }, { 0.688f, 0.152f, 0.926f },
        { 0.652f, 0.172f, 0.922f }, { 0.613f, 0.195f, 0.918f },
        { 0.578f, 0.215f, 0.914f }, { 0.543f, 0.234f, 0.910f },
        { 0.504f, 0.254f, 0.906f }, { 0.469f, 0.277f, 0.902f },
        { 0.434f, 0.297f, 0.898f }, { 0.398f, 0.320f, 0.895f },
        { 0.363f, 0.340f, 0.891f }, { 0.328f, 0.363f, 0.887f },
        { 0.293f, 0.387f, 0.883f }, { 0.258f, 0.410f, 0.879f },
        { 0.223f, 0.430f, 0.871f }, { 0.188f, 0.453f, 0.867f },
        { 0.156f, 0.477f, 0.863f }, { 0.121f, 0.500f, 0.859f },
        { 0.086f, 0.523f, 0.855f }, { 0.055f, 0.547f, 0.848f },
        { 0.020f, 0.570f, 0.844f }, { 0.016f, 0.605f, 0.816f },
        { 0.012f, 0.637f, 0.781f }, { 0.012f, 0.660f, 0.742f },
        { 0.008f, 0.688f, 0.699f }, { 0.008f, 0.711f, 0.656f },
        { 0.008f, 0.730f, 0.613f }, { 0.008f, 0.754f, 0.570f },
        { 0.004f, 0.777f, 0.523f }, { 0.004f, 0.797f, 0.480f },
        { 0.004f, 0.820f, 0.434f }, { 0.004f, 0.840f, 0.391f },
        { 0.000f, 0.855f, 0.348f }, { 0.000f, 0.867f, 0.301f },
        { 0.000f, 0.875f, 0.258f }, { 0.000f, 0.871f, 0.215f },
        { 0.000f, 0.863f, 0.176f }, { 0.000f, 0.852f, 0.141f },
        { 0.000f, 0.832f, 0.109f }, { 0.000f, 0.812f, 0.086f },
        { 0.000f, 0.789f, 0.062f }, { 0.000f, 0.766f, 0.047f },
        { 0.000f, 0.742f, 0.031f }, { 0.000f, 0.719f, 0.023f },
        { 0.000f, 0.695f, 0.016f }, { 0.000f, 0.676f, 0.012f },
        { 0.004f, 0.652f, 0.012f }, { 0.016f, 0.637f, 0.020f },
        { 0.031f, 0.621f, 0.035f }, { 0.062f, 0.613f, 0.062f },
        { 0.105f, 0.609f, 0.109f }, { 0.168f, 0.613f, 0.168f },
        { 0.242f, 0.617f, 0.242f }, { 0.324f, 0.629f, 0.324f },
        { 0.414f, 0.641f, 0.414f }, { 0.504f, 0.652f, 0.504f },
        { 0.598f, 0.664f, 0.598f }, { 0.781f, 0.781f, 0.781f },
        { 0.691f, 0.645f, 0.598f }, { 0.695f, 0.621f, 0.551f },
        { 0.699f, 0.602f, 0.500f }, { 0.703f, 0.582f, 0.453f },
        { 0.711f, 0.562f, 0.414f }, { 0.715f, 0.551f, 0.379f },
        { 0.723f, 0.543f, 0.355f }, { 0.730f, 0.539f, 0.340f },
        { 0.738f, 0.539f, 0.328f }, { 0.746f, 0.539f, 0.320f },
        { 0.754f, 0.543f, 0.312f }, { 0.770f, 0.547f, 0.309f },
        { 0.781f, 0.551f, 0.301f }, { 0.797f, 0.555f, 0.289f },
        { 0.812f, 0.555f, 0.273f }, { 0.828f, 0.559f, 0.258f },
        { 0.844f, 0.559f, 0.238f }, { 0.859f, 0.559f, 0.215f },
        { 0.875f, 0.562f, 0.191f }, { 0.895f, 0.562f, 0.168f },
        { 0.910f, 0.566f, 0.145f }, { 0.922f, 0.570f, 0.125f },
        { 0.934f, 0.574f, 0.105f }, { 0.945f, 0.582f, 0.086f },
        { 0.953f, 0.590f, 0.070f }, { 0.961f, 0.602f, 0.059f },
        { 0.965f, 0.613f, 0.047f }, { 0.969f, 0.625f, 0.043f },
        { 0.973f, 0.637f, 0.039f }, { 0.973f, 0.648f, 0.039f },
        { 0.973f, 0.660f, 0.043f }, { 0.973f, 0.672f, 0.055f },
        { 0.973f, 0.684f, 0.066f }, { 0.973f, 0.691f, 0.082f },
        { 0.969f, 0.695f, 0.102f }, { 0.969f, 0.699f, 0.121f },
        { 0.969f, 0.699f, 0.148f }, { 0.969f, 0.695f, 0.176f },
        { 0.969f, 0.691f, 0.207f }, { 0.973f, 0.680f, 0.238f },
        { 0.973f, 0.668f, 0.270f }, { 0.977f, 0.652f, 0.301f },
        { 0.977f, 0.637f, 0.324f }, { 0.980f, 0.613f, 0.352f },
        { 0.980f, 0.590f, 0.367f }, { 0.984f, 0.566f, 0.383f },
        { 0.988f, 0.539f, 0.391f }, { 0.988f, 0.512f, 0.395f },
        { 0.992f, 0.480f, 0.391f }, { 0.992f, 0.449f, 0.379f },
        { 0.992f, 0.418f, 0.363f }, { 0.996f, 0.383f, 0.344f },
        { 0.996f, 0.348f, 0.320f }, { 0.996f, 0.312f, 0.293f },
        { 0.996f, 0.277f, 0.266f }, { 0.996f, 0.242f, 0.234f },
        { 0.996f, 0.207f, 0.203f }, { 0.996f, 0.172f, 0.168f },
        { 0.996f, 0.141f, 0.137f }, { 0.996f, 0.105f, 0.105f },
        { 0.996f, 0.074f, 0.074f }, { 0.996f, 0.047f, 0.047f },
        { 0.996f, 0.016f, 0.016f }
    };


    /** Fixed values for ATD color table */
    private static float[][] ATD_Reflectivity = {
        { 0 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 60 / 255.0f, 60 / 255.0f, 60 / 255.0f },
        { 60 / 255.0f, 60 / 255.0f, 60 / 255.0f },
        { 0 / 255.0f, 69 / 255.0f, 0 / 255.0f },
        { 0 / 255.0f, 69 / 255.0f, 0 / 255.0f },
        { 0 / 255.0f, 101 / 255.0f, 10 / 255.0f },
        { 0 / 255.0f, 101 / 255.0f, 10 / 255.0f },
        { 0 / 255.0f, 158 / 255.0f, 30 / 255.0f },
        { 0 / 255.0f, 158 / 255.0f, 30 / 255.0f },
        { 0 / 255.0f, 177 / 255.0f, 59 / 255.0f },
        { 0 / 255.0f, 177 / 255.0f, 59 / 255.0f },
        { 0 / 255.0f, 205 / 255.0f, 116 / 255.0f },
        { 0 / 255.0f, 205 / 255.0f, 116 / 255.0f },
        { 0 / 255.0f, 191 / 255.0f, 150 / 255.0f },
        { 0 / 255.0f, 191 / 255.0f, 150 / 255.0f },
        { 0 / 255.0f, 159 / 255.0f, 206 / 255.0f },
        { 0 / 255.0f, 159 / 255.0f, 206 / 255.0f },
        { 8 / 255.0f, 127 / 255.0f, 219 / 255.0f },
        { 8 / 255.0f, 127 / 255.0f, 219 / 255.0f },
        { 28 / 255.0f, 71 / 255.0f, 232 / 255.0f },
        { 28 / 255.0f, 71 / 255.0f, 232 / 255.0f },
        { 56 / 255.0f, 48 / 255.0f, 222 / 255.0f },
        { 56 / 255.0f, 48 / 255.0f, 222 / 255.0f },
        { 110 / 255.0f, 13 / 255.0f, 198 / 255.0f },
        { 110 / 255.0f, 13 / 255.0f, 198 / 255.0f },
        { 144 / 255.0f, 12 / 255.0f, 174 / 255.0f },
        { 144 / 255.0f, 12 / 255.0f, 174 / 255.0f },
        { 200 / 255.0f, 15 / 255.0f, 134 / 255.0f },
        { 200 / 255.0f, 15 / 255.0f, 134 / 255.0f },
        { 196 / 255.0f, 67 / 255.0f, 134 / 255.0f },
        { 196 / 255.0f, 67 / 255.0f, 134 / 255.0f },
        { 192 / 255.0f, 100 / 255.0f, 135 / 255.0f },
        { 192 / 255.0f, 100 / 255.0f, 135 / 255.0f },
        { 191 / 255.0f, 104 / 255.0f, 101 / 255.0f },
        { 191 / 255.0f, 104 / 255.0f, 101 / 255.0f },
        { 190 / 255.0f, 108 / 255.0f, 68 / 255.0f },
        { 190 / 255.0f, 108 / 255.0f, 68 / 255.0f },
        { 210 / 255.0f, 136 / 255.0f, 59 / 255.0f },
        { 210 / 255.0f, 136 / 255.0f, 59 / 255.0f },
        { 250 / 255.0f, 196 / 255.0f, 49 / 255.0f },
        { 250 / 255.0f, 196 / 255.0f, 49 / 255.0f },
        { 254 / 255.0f, 217 / 255.0f, 33 / 255.0f },
        { 254 / 255.0f, 217 / 255.0f, 33 / 255.0f },
        { 254 / 255.0f, 250 / 255.0f, 3 / 255.0f },
        { 254 / 255.0f, 250 / 255.0f, 3 / 255.0f },
        { 254 / 255.0f, 221 / 255.0f, 28 / 255.0f },
        { 254 / 255.0f, 221 / 255.0f, 28 / 255.0f },
        { 254 / 255.0f, 154 / 255.0f, 88 / 255.0f },
        { 254 / 255.0f, 154 / 255.0f, 88 / 255.0f },
        { 254 / 255.0f, 130 / 255.0f, 64 / 255.0f },
        { 254 / 255.0f, 130 / 255.0f, 64 / 255.0f },
        { 254 / 255.0f, 95 / 255.0f, 5 / 255.0f },
        { 254 / 255.0f, 95 / 255.0f, 5 / 255.0f },
        { 249 / 255.0f, 79 / 255.0f, 8 / 255.0f },
        { 249 / 255.0f, 79 / 255.0f, 8 / 255.0f },
        { 253 / 255.0f, 52 / 255.0f, 28 / 255.0f },
        { 253 / 255.0f, 52 / 255.0f, 28 / 255.0f },
        { 200 / 255.0f, 117 / 255.0f, 104 / 255.0f },
        { 200 / 255.0f, 117 / 255.0f, 104 / 255.0f },
        { 215 / 255.0f, 183 / 255.0f, 181 / 255.0f },
        { 215 / 255.0f, 183 / 255.0f, 181 / 255.0f },
        { 210 / 255.0f, 210 / 255.0f, 210 / 255.0f },
        { 210 / 255.0f, 210 / 255.0f, 210 / 255.0f },
        { 0 / 255.0f, 0 / 255.0f, 0 / 255.0f }
    };


    /** Fixed values for ATD_ color table */
    private static float[][] ATD_Velocity = {
        { 0 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 254 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 253 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 253 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 248 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 248 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 222 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 222 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 186 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 186 / 255.0f, 0 / 255.0f, 254 / 255.0f },
        { 175 / 255.0f, 0 / 255.0f, 253 / 255.0f },
        { 175 / 255.0f, 0 / 255.0f, 253 / 255.0f },
        { 165 / 255.0f, 0 / 255.0f, 252 / 255.0f },
        { 165 / 255.0f, 0 / 255.0f, 252 / 255.0f },
        { 139 / 255.0f, 0 / 255.0f, 248 / 255.0f },
        { 139 / 255.0f, 0 / 255.0f, 248 / 255.0f },
        { 113 / 255.0f, 1 / 255.0f, 242 / 255.0f },
        { 113 / 255.0f, 1 / 255.0f, 242 / 255.0f },
        { 71 / 255.0f, 19 / 255.0f, 236 / 255.0f },
        { 71 / 255.0f, 19 / 255.0f, 236 / 255.0f },
        { 19 / 255.0f, 55 / 255.0f, 229 / 255.0f },
        { 19 / 255.0f, 55 / 255.0f, 229 / 255.0f },
        { 0 / 255.0f, 110 / 255.0f, 229 / 255.0f },
        { 0 / 255.0f, 110 / 255.0f, 229 / 255.0f },
        { 0 / 255.0f, 182 / 255.0f, 228 / 255.0f },
        { 0 / 255.0f, 182 / 255.0f, 228 / 255.0f },
        { 4 / 255.0f, 232 / 255.0f, 152 / 255.0f },
        { 4 / 255.0f, 232 / 255.0f, 152 / 255.0f },
        { 2 / 255.0f, 116 / 255.0f, 76 / 255.0f },
        { 2 / 255.0f, 116 / 255.0f, 76 / 255.0f },
        { 125 / 255.0f, 125 / 255.0f, 125 / 255.0f },
        { 125 / 255.0f, 125 / 255.0f, 125 / 255.0f },
        { 226 / 255.0f, 193 / 255.0f, 133 / 255.0f },
        { 226 / 255.0f, 193 / 255.0f, 133 / 255.0f },
        { 217 / 255.0f, 149 / 255.0f, 49 / 255.0f },
        { 217 / 255.0f, 149 / 255.0f, 49 / 255.0f },
        { 238 / 255.0f, 184 / 255.0f, 31 / 255.0f },
        { 238 / 255.0f, 184 / 255.0f, 31 / 255.0f },
        { 252 / 255.0f, 218 / 255.0f, 18 / 255.0f },
        { 252 / 255.0f, 218 / 255.0f, 18 / 255.0f },
        { 254 / 255.0f, 218 / 255.0f, 33 / 255.0f },
        { 254 / 255.0f, 218 / 255.0f, 33 / 255.0f },
        { 254 / 255.0f, 177 / 255.0f, 100 / 255.0f },
        { 254 / 255.0f, 177 / 255.0f, 100 / 255.0f },
        { 254 / 255.0f, 145 / 255.0f, 150 / 255.0f },
        { 254 / 255.0f, 145 / 255.0f, 150 / 255.0f },
        { 254 / 255.0f, 131 / 255.0f, 131 / 255.0f },
        { 254 / 255.0f, 131 / 255.0f, 131 / 255.0f },
        { 254 / 255.0f, 108 / 255.0f, 58 / 255.0f },
        { 254 / 255.0f, 108 / 255.0f, 58 / 255.0f },
        { 254 / 255.0f, 93 / 255.0f, 7 / 255.0f },
        { 254 / 255.0f, 93 / 255.0f, 7 / 255.0f },
        { 254 / 255.0f, 86 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 86 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 55 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 55 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 13 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 13 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 254 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 255 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 255 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 0 / 255.0f, 0 / 255.0f, 0 / 255.0f }
    };


    /** Fixed values for ATD_ color table */
    private static float[][] ATD_Wild = {
        { 110 / 255.0f, 110 / 255.0f, 110 / 255.0f },
        { 85 / 255.0f, 85 / 255.0f, 125 / 255.0f },
        { 138 / 255.0f, 138 / 255.0f, 177 / 255.0f },
        { 1 / 255.0f, 1 / 255.0f, 125 / 255.0f },
        { 1 / 255.0f, 23 / 255.0f, 170 / 255.0f },
        { 1 / 255.0f, 1 / 255.0f, 210 / 255.0f },
        { 4 / 255.0f, 128 / 255.0f, 255 / 255.0f },
        { 4 / 255.0f, 193 / 255.0f, 255 / 255.0f },
        { 4 / 255.0f, 252 / 255.0f, 17 / 255.0f },
        { 4 / 255.0f, 169 / 255.0f, 17 / 255.0f },
        { 4 / 255.0f, 132 / 255.0f, 17 / 255.0f },
        { 4 / 255.0f, 83 / 255.0f, 17 / 255.0f },
        { 200 / 255.0f, 200 / 255.0f, 200 / 255.0f },
        { 167 / 255.0f, 72 / 255.0f, 3 / 255.0f },
        { 217 / 255.0f, 149 / 255.0f, 96 / 255.0f },
        { 255 / 255.0f, 184 / 255.0f, 31 / 255.0f },
        { 255 / 255.0f, 255 / 255.0f, 3 / 255.0f },
        { 254 / 255.0f, 190 / 255.0f, 190 / 255.0f },
        { 254 / 255.0f, 136 / 255.0f, 136 / 255.0f },
        { 254 / 255.0f, 87 / 255.0f, 87 / 255.0f },
        { 254 / 255.0f, 119 / 255.0f, 0 / 255.0f },
        { 223 / 255.0f, 80 / 255.0f, 0 / 255.0f },
        { 138 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 210 / 255.0f, 0 / 255.0f, 0 / 255.0f },
        { 255 / 255.0f, 0 / 255.0f, 0 / 255.0f }
    };

    /** Fixed values for ATD_ color tables */
    private static float[][][] ATD_Data = {
        ATD_Scook, ATD_Reflectivity, ATD_Velocity, ATD_Wild
    };

    /** Fixed names for ATD_ color tables */
    private static String[] ATD_Names = { "ATD-Scook", "ATD-Reflectivity",
                                          "ATD-Velocity", "ATD-Wild" };



}

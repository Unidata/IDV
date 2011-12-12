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

package ucar.visad;


import visad.meteorology.WeatherSymbols;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;



/**
 * Class WxSymbolGroup holds information about the different symbol groups and symbols
 * available from the visad. meteorology.WeatherSymbols class
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class WxSymbolGroup {

    /** maps symbol name to the index in WeatherSymbols */
    private static Hashtable indexMap;

    /** maps symbol name to its description */
    private static Hashtable nameMap;

    /** All WxSymbolGroup objects created */
    private static List symbolGroups;

    //TODO: make the WeatherSymbols. values public

    /** Cut and paste from WeatherSymbols */
    private final static int PRESENTWX_INDEX = 0;

    /** Cut and paste from WeatherSymbols */
    private final static int LOCLD_INDEX = 104;

    /** Cut and paste from WeatherSymbols */
    private final static int MIDCLD_INDEX = 113;

    /** Cut and paste from WeatherSymbols */
    private final static int HICLD_INDEX = 122;

    /** Cut and paste from WeatherSymbols */
    private final static int TNDCY_INDEX = 142;

    /** Cut and paste from WeatherSymbols */
    private final static int SKY_INDEX = 131;

    /** Cut and paste from WeatherSymbols */
    private final static int TURB_INDEX = 151;

    /** Cut and paste from WeatherSymbols */
    private final static int ICING_INDEX = 160;

    /** Cut and paste from WeatherSymbols */
    private final static int MISC_INDEX = 171;



    /** The human readable name */
    String name;


    /** The group */
    String group;

    /** List of TwoFacedObjects with the id and name of each symbol */
    List symbols = new ArrayList();



    /**
     * ctor
     *
     *
     * @param name human readable name
     * @param group group id
     * @param startIndex index into WeatherSymbols list
     * @param offset 0 or 1
     * @param values values
     */
    private WxSymbolGroup(String name, String group, int startIndex,
                          int offset, String[][] values) {
        this.name = name;
        symbolGroups.add(this);
        String prefix = "WX_" + group + "_";
        //        System.out.println ("<li> " + name +"<ul>");
        for (int i = 0; i < values.length; i++) {
            String  desc     = values[i][0];
            Integer index    = new Integer(startIndex + i);
            String  symbolId = prefix + (i + offset);
            //            System.out.println ("<li><b> " + symbolId+"</b> - "+ desc);
            symbols.add(new ucar.unidata.util.TwoFacedObject(desc, symbolId));
            nameMap.put(symbolId, name + "-" + desc);
            indexMap.put(symbolId, index);
            for (int j = 1; j < values[i].length; j++) {
                indexMap.put(prefix + values[i][j], index);
            }
            /*            if(lat++>60) {
                lat = 0;
                lon-= 10;
            }
            System.out.println ("<symbol points=\"" + (lat++) +","+lon+"\"  symbol=\"" + prefix + (i + offset) +"\"/>\n");
            */
        }
        //        System.out.println ("</ul>");
    }



    /**
     * Get the TwoFacedObjects that describe each symbol
     *
     * @return symbols
     */
    public List getSymbols() {
        return symbols;
    }


    /**
     * get groups
     *
     * @return All of the WxSymbolGroups
     */
    public static List getSymbolGroups() {
        init();
        return symbolGroups;
    }


    /**
     * Get the index into the WeatherSymbols list for the given symbol name
     *
     * @param symbolName symbol
     *
     * @return index or -1 if not found
     */
    public static int getIndex(String symbolName) {
        init();
        Integer i = (Integer) indexMap.get(symbolName);
        if (i == null) {
            return -1;
        }
        return i.intValue();
    }

    /**
     * Get the long name for the given symbol
     *
     * @param symbolName symbol
     *
     * @return name or null if not found
     */
    public static String getName(String symbolName) {
        init();
        return (String) nameMap.get(symbolName);
    }


    /**
     * Get name of this group
     *
     * @return name
     */
    public String getName() {
        return name;
    }


    /**
     * Initialize
     */
    private static void init() {

        if (indexMap != null) {
            return;
        }

        /*

        System.out.println("<ul>");
        System.out.println("<li> General Symbols <ul>");
        for (int i = 0; i < ShapeUtility.SHAPES.length; i++) {
            System.out.println("<li><b> " + ShapeUtility.SHAPES[i].getId() +"</b> - " + ShapeUtility.SHAPES[i]);

        }
        System.out.println("</ul>");
        */



        indexMap     = new Hashtable();
        nameMap      = new Hashtable();
        symbolGroups = new ArrayList();

        new WxSymbolGroup("Present Weather", "PRESENTWX",  /*WeatherSymbols.*/
                          PRESENTWX_INDEX, 0, new String[][] {
            { "Cloud development not observed or not observable" },
            { "Clouds generally dissolving or becoming less developed" },
            { "State of sky on the whole unchanged" },
            { "Clouds generally forming or developing" },
            { "Visibility reduced by smoke" }, { "Haze" },
            { "Widespread dpust in suspension in the air, not raised by wind" },
            { "Dust or sand (or spray) raised by wind" },
            { "Well developed dust or sand whirl(s)" },
            { "Duststorm or sandstorm within sight or during preceding hour" },
            { "Mist" },
            { "Patches of shallow fog or ice fog (less than 2m land, 10m sea)" },
            { "Continuous shallow fog or ice fog (less than 2m land, 10m sea)" },
            { "Lightning visible, no thunder heard" },
            { "Precipitation in sight, not reaching the ground" },
            { "Precipitation in sight, reaching the ground, more than 5km away" },
            { "Precipitation in sight, reaching the ground nearby" },
            { "Thunderstorm, but no precipitation at time of observation" },
            { "Squalls within sight during the preceding hour" },
            { "Funnel cloud(s) within sight during the preceding hour" },
            { "Recent drizzle or snow grains" }, { "Recent rain" },
            { "Recent snow" }, { "Recent rain and snow or ice pellets" },
            { "Recent freezing drizzle or freezing rain" },
            { "Recent rain showers" },
            { "Recent showers of snow, or of rain and snow" },
            { "Recent showers of hail, or of rain and hail" },
            { "Recent fog" }, { "Recent thunderstorm" },
            { "Slight or moderate duststorm or sandstorm, has decreased in last hour" },
            { "Slight or moderate duststorm or sandstorm, no change in last hour" },
            { "Slight or moderate duststorm or sandstorm, has increased in last hour" },
            { "Severe duststorm or sandstorm, has decreased in last hour" },
            { "Severe duststorm or sandstorm, no change in last hour" },
            { "Severe duststorm or sandstorm, has increased in last hour" },
            { "Slight or moderate drifting snow (below eye level)" },
            { "Heavy drifting snow (below eye level)" },
            { "Slight or moderate blowing snow (above eye level)" },
            { "Heavy blowing snow (above eye level)" },
            { "Fog extending above observer in distance" },
            { "Fog in patches" },
            { "Fog has become thinner in last hour, sky visible" },
            { "Fog has become thinner in last hour, sky invisible" },
            { "Fog, no change in last hour, sky visible" },
            { "Fog, no change in last hour, sky invisible" },
            { "Fog has become thicker in last hour, sky visible" },
            { "Fog has become thicker in last hour, sky invisible" },
            { "Fog depositing rime, sky visible" },
            { "Fog depositing rime, sky invisible" },
            { "Slight intermittent drizzle" },
            { "Slight continuous drizzle" },
            { "Moderate intermittent drizzle" },
            { "Moderate continuous drizzle" },
            { "Heavy intermittent drizzle" }, { "Heavy continuous drizzle" },
            { "Slight freezing drizzle" },
            { "Moderate or heavy freezing drizzle" },
            { "Slight rain and drizzle" },
            { "Moderate or heavy rain and drizzle" },
            { "Slight intermittent rain" }, { "Slight continuous rain" },
            { "Moderate intermittent rain" }, { "Moderate continuous rain" },
            { "Heavy intermittent rain" }, { "Heavy continuous rain" },
            { "Slight freezing rain" }, { "Moderate or heavy freezing rain" },
            { "Slight rain or drizzle and snow" },
            { "Moderate or heavy rain or drizzle and snow" },
            { "Slight intermittent fall of snow flakes" },
            { "Slight continuous fall of snow flakes" },
            { "Moderate intermittent fall of snow flakes" },
            { "Moderate continuous fall of snow flakes" },
            { "Heavy intermittent fall of snow flakes" },
            { "Heavy continuous fall of snow flakes" }, { "Ice prisms" },
            { "Snow grains" }, { "Isolated starlike snow crystals" },
            { "Ice pellets" }, { "Slight rain showers" },
            { "Moderate or heavy rain showers" }, { "Violent rain showers" },
            { "Slight showers of rain and snow mixed" },
            { "Moderate or heavy showers of rain and snow mixed" },
            { "Slight snow showers" }, { "Moderate or heavy snow showers" },
            { "Slight showers of ice or snow pellets" },
            { "Moderate or heavy showers of ice or snow pellets" },
            { "Slight hail showers" }, { "Moderate or heavy hail showers" },
            { "Slight rain, thunderstorm in last hour" },
            { "Moderate or heavy rain, thunderstorm in last hour" },
            { "Slight rain and/or snow or hail, thunderstorm in last hour" },
            { "Moderate or heavy rain and/or snow or hail, thunderstorm in last hour" },
            { "Slight or moderate thunderstorm with rain and/or snow" },
            { "Slight or moderate thunderstorm with hail" },
            { "Heavy thunderstorm with rain and/or snow" },
            { "Thunderstorm combined with duststorm or sandstorm" },
            { "Heavy thunderstorm with hail" }
        });








        new WxSymbolGroup("Low Cloud", "LOCLD",  /*WeatherSymbols.*/
                          LOCLD_INDEX, 1, new String[][] {
            { "Cu of fair weather, little vertical development, seemingly flattened" },
            { "Cu of considerable development, generally towering,   with or without other Cu or Sc bases all at same level" },
            { "Cb with tops lacking clear cut outlines, but distinctly  no cirriform or anvil-shaped; with or without Cu, Sc, or St" },
            { "Sc formed by spreading out of Cu; Cu often present also" },
            { "Sc not formed by spreading out of Cu" },
            { "St or Fs or both, but no Fs of bad weather" },
            { "Fs and/or Fc of bad weather (scud)" },
            { "Cu and Sc (not formed by spreading out of Cu) with bases at different levels" },
            { "Cb having a clearly fibrous (cirriform) top, often anvil-shaped, with or without Cu, Sc, St, or scud" }
        });



        new WxSymbolGroup("Cloud Coverage", "SKY",  /*WeatherSymbols.*/
                          SKY_INDEX, 0, new String[][] {
            { "No clouds" }, { "Less than one-tenth or one-tenth" },
            { "two-tenths or three-tenths" }, { "four-tenths" },
            { "five-tenths" }, { "six-tenths" },
            { "seven-tenths or eight-tenths" },
            { "nine-tenths or overcast with openings" },
            { "completely overcast" }, { "sky obscured" }, { "missing" }
        });






        new WxSymbolGroup("Mid Clouds", "MIDCLD",  /*WeatherSymbols.*/
                          MIDCLD_INDEX, 1, new String[][] {
            { "Thin As (most of cloud layer semi-transparent)" },
            { "Thick As, greater part sufficiently dense to hide sun  (or moon), or Ns" },
            { "Thin Ac, mostly semi-transparent; cloud elements not changing much and at a single level" },
            { "Thin Ac in patches; cloud elements continually changing and/or occurring at more than one level" },
            { "Thin Ac in bands or in a layer gradually spreading over sky and usually thickening as a whole" },
            { "Ac formed by spreading out of Cu" },
            { "Double-layered Ac, or a thick layer of Ac, not increasing; or Ac with As and/or Ns" },
            { "Ac in the form of Cu-shaped tufts or Ac with turrets" },
            { "Ac of a chaotic sky, usually at different levels; patches of dense Ci are usually present also" }
        });






        new WxSymbolGroup("High Clouds", "HICLD",  /*WeatherSymbols.*/
                          HICLD_INDEX, 1, new String[][] {
            { "Filaments of Ci, or \"}mares tails\", scattered and not increasing" },
            { "Dense Ci in patches or twisted sheaves, usually not increasing, sometimes like remains of Cb; or towers or tufts" },
            { "Dense Ci, often anvil shaped derived from or associated with Cb" },
            { "Ci, often hook shaped, spreading over the sky and usually thickening as a whole" },
            { "Ci and Cs, often in converging bands, or Cs alone; generally overspreading and growing denser; the continuous layer not reaching 45 degrees altitude" },
            { "Ci and Cs, often in converging bands, or Cs alone; generally overspreading and growing denser; the continuous layer exceeding 45 degrees altitude" },
            { "Veil of Cs covering the entire sky" },
            { "Cs not increasing and not covering the entire sky" },
            { "Cc alone or Cc with some Ci or Cs, but the Cc being the main cirriform cloud" }
        });





        new WxSymbolGroup("Pressure Tendency", "TNDCY",  /*WeatherSymbols.*/
                          TNDCY_INDEX, 0, new String[][] {
            { "rising then falling" },
            { "rising then steady; or rising, then rising more slowly" },
            { "rising steadily or unsteadily" },
            { "falling or steady, then rising; or rising, then rising more quickly" },
            { "steady, same as 3 hours ago" },
            { "falling then rising, same or lower than 3 hours ago" },
            { "falling then steady; or falling, then falling more slowly" },
            { "falling steadily, or unsteadily" },
            { "steady or rising, then falling; or falling, then falling more quickly" }
        });




        new WxSymbolGroup("Icing", "ICING", /*WeatherSymbols.*/ ICING_INDEX,
                          0, new String[][] {
            { "No icing" }, { "Trace icing" }, { "Trace to light icing" },
            { "Light icing" }, { "Light to moderate icing" },
            { "Moderate icing" }, { "Moderate to heavy icing" },
            { "Heavy or moderate to severe icing" }, { "Severe icing" },
            { "Light superstructure icing" }, { "Heavy superstructure icing" }
        });







        new WxSymbolGroup("Turbulence", "TURBULENCE",  /*WeatherSymbols.*/
                          TURB_INDEX, 0, new String[][] {
            { "No turbulence" }, { "Light turbulence" },
            { "Light turbulence" }, { "Light to moderate turbulence" },
            { "Moderate turbulence" }, { "Moderate to severe turbulence" },
            { "Severe turbulence" }, { "Extreme turbulence" },
            { "Extreme turbulence" }
        });



        new WxSymbolGroup("Miscellaneous", "MISC",  /*WeatherSymbols.*/
                          MISC_INDEX, 0, new String[][] {
            { "Square (outline)" }, { "Square (filled) " },
            { "Circle (outline)" }, { "Circle (filled) " },
            { "Triangle (outline)" }, { "Triangle (filled)" },
            { "Diamond (outline)" }, { "Diamond (filled)" },
            { "Star (outline)" }, { "Star (filled)" },
            { "High Pressure (outline)" }, { "Low Pressure (outline)" },
            { "High Pressure (filled)" }, { "Low Pressure (filled)" },
            { "Plus sign" }, { "Minus sign" }, { "Tropical Storm (NH)" },
            { "Hurricane (NH)" }, { "Tropical Storm (SH)" },
            { "Hurricane (SH)" }, { "Triangle with antenna" },
            { "Mountain obscuration" }, { "Slash" }, { "Storm Center" },
            { "Tropical Depression" }, { "Tropical Cyclone" }, { "Flame" },
            { "X Cross" }, { "Low pressure with X (outline)" },
            { "Low pressure with X (filled)" }, { "Tropical Storm (NH)" },
            { "Tropical Storm (SH)" }, { "Volcanic activity" },
            { "Blowing spray" }
        });



        /*        System.out.println("</ul>");*/



    }
}

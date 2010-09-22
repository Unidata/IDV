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


import org.w3c.dom.Element;

import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import java.util.regex.*;

import javax.swing.*;


/**
 * Earth-centric utilities
 *
 * @author IDV Development Team
 */
public class GeoUtils {

    /** Keep around the last address */
    public static String lastAddress = "";

    /** list of addresses */
    private static List addresses = new ArrayList();

    /** hashtable of addresses visited */
    private static Hashtable addressMap = new Hashtable();

    /** Address tooltip */
    public static final String addressToolTip =
        "Examples:<br>12345 oak street, my town, my state<br>Or: my town, my state<br>Or: 80303 (zip code)<br>Or: latitude longitude<br>Or: \"ip\" for the location of this computer";

    /** default ctor */
    private GeoUtils() {}

    /**
     * Create an EarthLocation with altitude  0 from the given llp
     *
     * @param llp Location
     * @return Earth location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static EarthLocation toEarthLocation(LatLonPoint llp)
            throws VisADException, RemoteException {
        return new EarthLocationTuple(llp.getLatitude(), llp.getLongitude(),
                                      new Real(RealType.Altitude, 0.0));
    }


    /**
     * Prompt the user for a street address and try to find its location.
     * This supports addresses, zip codes, city/state and simple lat/lon entries
     *
     * @return Location of address or null
     */
    public static LatLonPoint getLocationOfAddress() {
        return getLocationOfAddress(null);
    }


    /**
     * Convert a LatLonRect to a LinearLatLonSet.
     * @param rect the LatLonRect
     * @return the corresponding LinearLatLonSet
     *
     * @throws VisADException Problem creating the set
     */
    public static LinearLatLonSet latLonRectToSet(LatLonRect rect)
            throws VisADException {
        LinearLatLonSet bounds =
            new LinearLatLonSet(RealTupleType.LatitudeLongitudeTuple,
                                rect.getLatMin(),
                                rect.getLatMin() + rect.getHeight(), 11,
                                rect.getLonMin(),
                                rect.getLonMin() + rect.getWidth(), 11);
        return bounds;
    }


    /**
     * Get the location of an address.
     *
     * @param extraComp the component to put the address in.
     *
     * @return the location as a LatLonPoint
     */
    public static LatLonPoint getLocationOfAddress(JComponent extraComp) {
        String     address = lastAddress;
        JComponent bottom  = new JLabel("<html>" + addressToolTip
                                        + "</html>");
        if (extraComp != null) {
            bottom = GuiUtils.vbox(bottom, extraComp);
        }
        if ( !addresses.contains("ipaddress")) {
            //      addresses.add("ipaddress");
        }
        JComboBox addressBox = new JComboBox(new Vector(addresses));
        addressBox.setToolTipText("<html>" + addressToolTip + "</html>");

        addressBox.setEditable(true);
        JComponent contents = GuiUtils.label("Address: ", addressBox);
        contents = GuiUtils.inset(contents, 5);
        if (extraComp != null) {
            contents = GuiUtils.vbox(contents, extraComp);
            contents = GuiUtils.inset(contents, 5);
        }

        while (true) {
            if ( !GuiUtils.showOkCancelDialog(null, "Go To Address",
                    contents, null, Misc.newList(addressBox))) {
                return null;
            }
            //Sometimes we get null here. So check the editor
            address = (String) addressBox.getSelectedItem();
            //            System.err.println ("address:" + address);
            if (address == null) {
                address = (String) addressBox.getEditor().getItem();
                //                System.err.println ("***address:" + address);
            }
            //              GuiUtils.getInput("Please enter the address to go to",
            //                                "Address: ", address, null, bottom);
            if (address == null) {
                return null;
            }
            lastAddress = address;
            LatLonPoint llp = getLocationFromAddress(address, null);
            if (llp != null) {
                return llp;
            }
            if ( !GuiUtils.askYesNo(
                    "Address lookup error",
                    "<html>Could not find the given address.<p>Do you want to try again?</html>")) {
                return null;
            }
        }
    }


    /**
     * Look up the location of the given address
     *
     * @param address The address
     * @param master  master
     *
     * @return The location or null if not found
     */
    public static LatLonPoint getLocationFromAddress(String address,
            int[] master) {

        try {
            int timestamp = 0;
            if (master != null) {
                timestamp = master[0];
            }
            if (address == null) {
                return null;
            }
            address = address.trim();
            if (address.length() == 0) {
                return null;
            }
            while (address.indexOf(" ,") >= 0) {
                address = StringUtil.replace(address, " ,", ",");
            }
            while (address.indexOf(", ") >= 0) {
                address = StringUtil.replace(address, ", ", ",");
            }

            LatLonPoint llp = (LatLonPoint) addressMap.get(address);
            if (llp != null) {
                return llp;
            }


            String latString      = null;
            String lonString      = null;
            String encodedAddress = StringUtil.replace(address, " ", "%20");

            //Try it as lat/lon
            if ((latString == null) || (lonString == null)) {
                String tmp = address;
                while (tmp.indexOf("  ") >= 0) {
                    tmp = StringUtil.replace(address, "  ", " ");
                }
                List toks = StringUtil.split(tmp, " ");
                if ((toks != null) && (toks.size() == 2)) {
                    try {
                        double latValue =
                            Misc.decodeLatLon((String) toks.get(0));
                        double lonValue =
                            Misc.decodeLatLon((String) toks.get(1));
                        if ( !Double.isNaN(latValue)
                                && !Double.isNaN(lonValue)) {
                            latString = "" + latValue;
                            lonString = "" + lonValue;
                        }

                    } catch (NumberFormatException nfe) {
                        //ignore
                    }
                }
            }

            //Try yahoo
            if (address.equals("ip") || address.equals("ipaddress")
                    || address.equals("mymachine")) {
                String url =
                    "http://api.hostip.info/get_html.php?position=true";
                String result = IOUtil.readContents(url, GeoUtils.class);
                if ((master != null) && (master[0] != timestamp)) {
                    return null;
                }
                //                System.err.println ("result:" + result);
                result = result.toLowerCase();
                Pattern pattern;
                Matcher matcher;
                pattern = Pattern.compile("latitude: *([0-9\\.-]+)");
                matcher = pattern.matcher(result);
                if (matcher.find()) {
                    latString = matcher.group(1);
                    pattern   = Pattern.compile("longitude: *([0-9\\.-]+)");
                    matcher   = pattern.matcher(result);
                    if (matcher.find()) {
                        lonString = matcher.group(1);
                    }
                } else {
                    return null;
                }
            }
            if ((latString == null) || (lonString == null)) {
                try {
                    String url =
                        "http://api.local.yahoo.com/MapsService/V1/geocode?appid=idvunidata&location="
                        + encodedAddress;
                    String result = IOUtil.readContents(url, GeoUtils.class);
                    if ((master != null) && (master[0] != timestamp)) {
                        return null;
                    }
                    Element root    = XmlUtil.getRoot(result);
                    Element latNode = XmlUtil.findDescendant(root,
                                          "Latitude");
                    Element lonNode = XmlUtil.findDescendant(root,
                                          "Longitude");
                    if ((latNode != null) && (lonNode != null)) {
                        latString = XmlUtil.getChildText(latNode);
                        lonString = XmlUtil.getChildText(lonNode);
                    }
                } catch (Exception exc) {}
            }




            /* Don't do these. yahoo seems pretty good
            //Maybe a zip code
            if ((latString == null) || (lonString == null)) {
                if ((address.length() == 5)
                        && Pattern.compile("\\d\\d\\d\\d\\d").matcher(
                            address).find()) {
                    String url =
                        "http://www.census.gov/cgi-bin/gazetteer?city=&state=&zip="
                        + address;
                    String result = IOUtil.readContents(url, GeoUtils.class);
                    if ((master != null) && (master[0] != timestamp)) {
                        return null;
                    }
                    //Location: 39.991381 N, 105.239178 W<br>
                    Pattern pattern =
                        Pattern.compile("Location:\\s*([^,]+),([^<]+)<br>");
                    Matcher matcher = pattern.matcher(result);
                    if (matcher.find()) {
                        latString = matcher.group(1);
                        lonString = matcher.group(2);
                    }
                }
            }

            if ((master != null) && (master[0] != timestamp)) {
                return null;
            }

            if ((latString == null) || (lonString == null)) {
                String url = "http://rpc.geocoder.us/service/rest?address="
                             + encodedAddress;
                String result = IOUtil.readContents(url, GeoUtils.class);
                if ((master != null) && (master[0] != timestamp)) {
                    return null;
                }
                if (result.indexOf("<geo:long>") >= 0) {
                    Element root    = XmlUtil.getRoot(result);
                    Element latNode = XmlUtil.findDescendant(root, "geo:lat");
                    Element lonNode = XmlUtil.findDescendant(root,
                                          "geo:long");
                    if ((latNode == null) || (lonNode == null)) {
                        LogUtil.userErrorMessage("Error: Malformed response");
                        return null;
                    }
                    latString = XmlUtil.getChildText(latNode);
                    lonString = XmlUtil.getChildText(lonNode);
                }
            }


            //Try the gazeteer
            if ((latString == null) || (lonString == null)) {
                String url = "http://www.census.gov/cgi-bin/gazetteer?"
                             + encodedAddress;
                String result = IOUtil.readContents(url, GeoUtils.class);
                if ((master != null) && (master[0] != timestamp)) {
                    return null;
                }
                Pattern pattern =
                    Pattern.compile("Location:\\s*([^,]+),([^<]+)<br>");
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    latString = matcher.group(1);
                    lonString = matcher.group(2);
                }
            }
            */

            if ((latString != null) && (lonString != null)) {
                double lat = Misc.decodeLatLon(latString.trim());
                double lon = Misc.decodeLatLon(lonString.trim());
                EarthLocation el =
                    new EarthLocationTuple(new Real(RealType.Latitude, lat),
                                           new Real(RealType.Longitude, lon),
                                           new Real(RealType.Altitude, 0.0));

                lastAddress = address;
                addresses.remove(address);
                addresses.add(0, address);
                addressMap.put(address, el.getLatLonPoint());
                while (addresses.size() > 20) {
                    addresses.remove(addresses.size() - 1);
                }
                return el.getLatLonPoint();
            }
        } catch (Exception exc) {
            LogUtil.logException(
                "An error occurred reading address location", exc);
            return null;
        }
        return null;
    }


    /**
     * Get saved addresses
     *
     * @return list of addresses
     */
    public static List getSavedAddresses() {
        return addresses;
    }


    /**
     * set saved addresses
     *
     * @param add list of addresses
     */
    public static void setSavedAddresses(List add) {
        if (add == null) {
            return;
        }
        addresses = new ArrayList(add);
    }

    /**
     * Initialize the list of addresses in box
     *
     * @param box  the box to add to.
     */
    public static void initAddressBox(JComboBox box) {
        Object selected = box.getSelectedItem();
        GuiUtils.setListData(box, new Vector(addresses));
        if (selected != null) {
            box.setSelectedItem(selected);
        }
    }


    /**
     * Test main
     *
     * @param args args
     */
    public static void main(String[] args) {
        //        System.err.println("lat/lon:" + getLocationOfAddress());
        for (String arg : args) {
            System.err.println(arg + " " + getLocationFromAddress(arg, null));
        }


    }

    /**
     * Normalize a longitude value to the range between -180 and 180.
     *
     * @param lonValue  longitude value to adjust (in degrees)
     * @return adjusted value.
     */
    public static double normalizeLongitude(double lonValue) {
        if (lonValue == lonValue) {
            while ((lonValue < -180.) || (lonValue > 180.)) {
                lonValue = Math.IEEEremainder(lonValue, 360.0);
            }
        }
        return lonValue;
    }

    /**
     * Normalize longitude values to the range between -180 and 180.
     *
     * @param lonValues  longitude values to adjust (in degrees)
     * @return adjusted values.
     */
    public static double[] normalizeLongitude(double[] lonValues) {
        for (int i = 0; i < lonValues.length; i++) {
            lonValues[i] = normalizeLongitude(lonValues[i]);
        }
        return lonValues;
    }
    
    /**
     *  Normalize a longitude between the range of the max/min
     *  @param lonRange the range of the longitude
     *  @param value the longitude value
     *  return longitude normalized to range
     */
    public static double normalizeLongitude(Range lonRange, double value) {
    	if (value > 180 && (lonRange.getMin() < 0 || lonRange.getMax() < 0)) {
    		return normalizeLongitude(value);
    	} else if (value < 0 && (lonRange.getMin() > 180 || lonRange.getMax() > 180))	{
    		return normalizeLongitude360(value);
    	} 
    	return value;
    }

    /**
     *  Normalize a longitude between the range of the max/min
     *  @param lonRange the range of the longitude
     *  @param value the longitude value
     *  return longitude normalized to range
     */
    public static float normalizeLongitude(Range lonRange, float value) {
    	if (value > 180.f && (lonRange.getMin() < 0 || lonRange.getMax() < 0)) {
    		return (float) normalizeLongitude(value);
    	} else if (value < 0 && (lonRange.getMin() > 180 || lonRange.getMax() > 180))	{
    		return (float) normalizeLongitude360(value);
    	} 
    	return value;
    }

    /**
     * Normalize longitude values to the range between -180 and 180.
     *
     * @param lonValues  longitude values to adjust (in degrees)
     *
     * @return adjusted values.
     */
    public static float[] normalizeLongitude(float[] lonValues) {
        for (int i = 0; i < lonValues.length; i++) {
            lonValues[i] = (float) normalizeLongitude(lonValues[i]);
        }
        return lonValues;
    }

    /**
     * Normalize a longitude value to the range between 0 and 360.
     *
     * @param lonValue  longitude value to adjust (in degrees)
     * @return adjusted value.
     */
    public static double normalizeLongitude360(double lonValue) {
        if (lonValue == lonValue) {
            while ((lonValue < 0.) || (lonValue > 361.)) {
                lonValue = 180. + Math.IEEEremainder(lonValue - 180., 360.0);
            }
        }
        return lonValue;
    }

    /**
     * Normalize longitude values to the range between 0 and 360.
     *
     * @param lonValues  longitude values to adjust (in degrees)
     *
     * @return adjusted values.
     */
    public static double[] normalizeLongitude360(double[] lonValues) {
        for (int i = 0; i < lonValues.length; i++) {
            lonValues[i] = normalizeLongitude360(lonValues[i]);
        }
        return lonValues;
    }


    /**
     * Normalize longitude values to the range between 0 and 360.
     *
     * @param lonValues  longitude values to adjust (in degrees)
     *
     * @return adjusted value.
     */
    public static float[] normalizeLongitude360(float[] lonValues) {
        for (int i = 0; i < lonValues.length; i++) {
            lonValues[i] = (float) normalizeLongitude360(lonValues[i]);
        }
        return lonValues;
    }

}

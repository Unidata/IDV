/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.imagery;


import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created with IntelliJ IDEA.
 * User: yuanho
 * Date: 12/7/13
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImageURLInfo extends AddeImageURL {

    /** _more_ */
    public double locationLat;

    /** _more_ */
    public double locationLon;

    /** _more_ */
    public int locationLine;

    /** _more_ */
    public int locationElem;

    /**
     * Construct a AddeImageURLInfo 
     */
    public AddeImageURLInfo() {}

    public AddeImageURLInfo(String host, String requestType, String group,
                        String descriptor, String locateKey,
                        String locateValue, String placeValue, int lines,
                        int elements, int lmag, int emag, String band,
                        String unit, int spacing) {
        super(host, requestType, group, descriptor,locateKey, locateValue, placeValue,
                lines, elements, lmag, emag, band, unit, spacing);

    }

    /**
     * Construct a AddeImageURLInfo 
     *
     * @param sourceURL _more_
     */
    public AddeImageURLInfo(String sourceURL) {
        super(null, null, null, null, null, null, null,
                0, 0, 0, 0, null, null, 0);
        URL url = null;
        try {
          url = new URL(sourceURL);
        } catch (MalformedURLException mue) {}

        setHost(url.getHost());
        setRequestType(url.getPath().substring(1).toLowerCase());
        String query = url.getQuery();

        List<String> tokens    = StringUtil.split(query, "&", true, true);
        for (String token : tokens) {
            String[] keyValue = StringUtil.split(token, "=", 2);
            if (keyValue == null) {
                continue;
            }
            String key   = keyValue[0].toLowerCase();
            String value = keyValue[1];
            if ((key == null) || (value == null)) {
                continue;
            }

            if (key.startsWith("band")) {  // band
                setBand(value);
            } else if (key.startsWith("unit")) {   // unit
                setUnit(value);
            } else if (key.startsWith("grop")) {   // group
                setGroup(value);
            } else if (key.startsWith("desc")) {  // descriptor
                setDescriptor(value);
            } else if (key.startsWith("spac")) {  // spacing
                setSpacing(Integer.parseInt(value));
            } else if (key.startsWith("linele")) {  // location  "700 864"
                setLocateKey("LINELE");
                setLocateValue(value);
                List<String> lList = StringUtil.split(value, " ");
                setLocationLine(Integer.parseInt(lList.get(0)));
                setLocationElem(Integer.parseInt(lList.get(1)));
            } else if (key.startsWith("latlon")) {  // location  "700 864"
                setLocateKey("LATLON") ;
                setLocateValue(value);
                List<String> lList = StringUtil.split(value, " ");
                setLocationLat(Double.parseDouble(lList.get(0)));
                setLocationLon(Double.parseDouble(lList.get(1)));
            } else if (key.startsWith("place")) {   // place
                setPlaceValue(value);
            } else if (key.startsWith("size")) {   // 700 800
                List<String> lList = StringUtil.split(value, " ");
                setLines(Integer.parseInt(lList.get(0)));
                setElements(Integer.parseInt(lList.get(1)));
            } else if (key.startsWith("mag")) {  // =-2 -2
                List<String> lList = StringUtil.split(value, " ");
                setLineMag(Integer.parseInt(lList.get(0)));
                setElementMag(Integer.parseInt(lList.get(1)));
            }

        }

    }

    /**
     * _more_
     *
     * @param baseURL _more_
     */
    private void decodeSourceURL(String baseURL) {
        try {
            URL url = new URL(baseURL);
            setHost(url.getHost());
            setRequestType(url.getPath().substring(1).toLowerCase());
            String query = url.getQuery();
            setExtraKeys(query);
        } catch (MalformedURLException mue) {}
    }

    /**
     * _more_
     *
     * @param lat _more_
     */
    public void setLocationLat(double lat) {

        this.locationLat = lat;
    }

    /**
     * _more_
     *
     * @param lon _more_
     */
    public void setLocationLon(double lon) {

        this.locationLon = lon;
    }

    /**
     * _more_
     *
     * @param line _more_
     */
    public void setLocationLine(int line) {

        this.locationLine = line;
    }

    /**
     * _more_
     *
     * @param elem _more_
     */
    public void setLocationElem(int elem) {

        this.locationElem = elem;
    }

    /**
     * Set the locate value
     *
     * @param value the locate value
     */
    public void setLocateValue(String value) {
        super.setLocateValue(value);
        String locKey = getLocateKey();
        List<String> locList = StringUtil.split(value, " ");
        if (locKey.equals(AddeImageURL.KEY_LINEELE)) {
            this.locationLine = Integer.parseInt(locList.get(0));
            this.locationLine = Integer.parseInt(locList.get(1));
        } else {
            this.locationLat = Double.parseDouble(locList.get(0));
            this.locationLat = Double.parseDouble(locList.get(1));
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getLocationLat() {
        return locationLat;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getLocationon() {
        return locationLon;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLocationLine() {
        return locationLine;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLocationElem() {
        return locationElem;
    }


}

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
public class ImageDataSelectionInfo {

    /** _more_ */
    public double locationLat;

    /** _more_ */
    public double locationLon;

    /** _more_ */
    public int locationLine;

    /** _more_ */
    public int locationElem;

    /** _more_ */
    public String host;

    /** _more_ */
    public String requestType;

    /** _more_ */
    public String band;

    /** _more_ */
    public String navType;

    /** _more_ */
    public String unit;

    /** _more_ */
    public String group;

    /** _more_ */
    public String descriptor;

    /** _more_ */
    public int spacing;

    /** _more_ */
    public String placeValue;

    /** _more_ */
    public String locateValue;

    /** _more_ */
    public String locateKey;

    /** _more_ */
    public int line;

    /** _more_ */
    public int elem;

    /** _more_ */
    public int lines;

    /** _more_ */
    public int elememts;

    /** _more_ */
    public int lineMag;

    /** _more_ */
    public int elementMag;

    /** _more_ */
    public int trace;

    /** _more_ */
    public boolean debug;

    /** _more_ */
    public String compress;

    /** _more_ */
    public String user;

    /** _more_ */
    public int port;

    /** _more_ */
    public int project;

    /** _more_ */
    public String version;

    /** _more_ */
    List<String> leftovers = new ArrayList<String>();

    /**
     * Construct a ImageDataSelectionInfo
     */
    public ImageDataSelectionInfo() {}

    /**
     * Construct a ImageDataSelectionInfo
     *
     * @param sourceURL _more_
     */
    public ImageDataSelectionInfo(String sourceURL) {

        URL url = null;
        try {
            url = new URL(sourceURL);
        } catch (MalformedURLException mue) {}

        setHost(url.getHost());
        setRequestType(url.getPath().substring(1).toLowerCase());
        String       query  = url.getQuery();

        List<String> tokens = StringUtil.split(query, "&", true, true);
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

            if (key.startsWith("port")) {           // port
                setPort(Integer.parseInt(value));
            } else if (key.startsWith("comp")) {    // compress type
                setCompression(value);
            } else if (key.startsWith("user")) {    // user
                setUser(value);
            } else if (key.startsWith("proj")) {    // projection
                setProject(Integer.parseInt(value));
            } else if (key.startsWith("vers")) {    // version
                setVersion(value);
            } else if (key.startsWith("debug")) {   // version
                setDebug(Boolean.parseBoolean(value));
            } else if (key.startsWith("trace")) {   // trace
                setTrace(Integer.parseInt(value));
            } else if (key.startsWith("band")) {    // band
                setBand(value);
            } else if (key.startsWith("unit")) {    // unit
                setUnit(value);
            } else if (key.startsWith("group")) {   // group
                setGroup(value);
            } else if (key.startsWith("desc")) {    // descriptor
                setDescriptor(value);
            } else if (key.startsWith("spac")) {    // spacing
                setSpacing(Integer.parseInt(value));
            } else if (key.startsWith("linele")) {  // location  "700 864"
                setLocateKey("LINELE");
                setLocate(value);
                List<String> lList = StringUtil.split(value, " ");
                setLocationLine(Integer.parseInt(lList.get(0)));
                setLocationElem(Integer.parseInt(lList.get(1)));
            } else if (key.startsWith("latlon")) {  // location  "700 864"
                setLocateKey("LATLON");
                setLocate(value);
                List<String> lList = StringUtil.split(value, " ");
                setLocationLat(Double.parseDouble(lList.get(0)));
                setLocationLon(Double.parseDouble(lList.get(1)));
            } else if (key.startsWith("place")) {   // place
                setPlaceValue(value);
            } else if (key.startsWith("size")) {    // 700 800
                List<String> lList = StringUtil.split(value, " ");
                setLines(Integer.parseInt(lList.get(0)));
                setElements(Integer.parseInt(lList.get(1)));
            } else if (key.startsWith("mag")) {     // =-2 -2
                List<String> lList = StringUtil.split(value, " ");
                setLineMag(Integer.parseInt(lList.get(0)));
                setElementMag(Integer.parseInt(lList.get(1)));
            } else if (key.startsWith("nav")) {    // navigator type
                setNavType(value);
            } else {
                leftovers.add(token);
            }

        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ImageDataSelectionInfo cloneMe() {
        return new ImageDataSelectionInfo(this.getURLString());
    }

    /**
     * _more_
     *
     * @param port _more_
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getPort() {
        return port;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getCompression() {
        return compress;
    }


    /**
     * _more_
     *
     * @param compress _more_
     */
    public void setCompression(String compress) {
        this.compress = compress;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getProject() {
        return project;
    }

    /**
     * _more_
     *
     * @param project _more_
     */
    public void setProject(int project) {
        this.project = project;
    }

    /**
     * _more_
     *
     * @param host _more_
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHost() {
        return this.host;
    }

    /**
     * _more_
     *
     * @param band _more_
     */
    public void setBand(String band) {
        this.band = band;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getBand() {
        return this.band;
    }

    /**
     * _more_
     *
     * @param navType _more_
     */
    public void setNavType(String navType) {
        this.navType = navType;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getNavType() {
        return this.navType;
    }

    /**
     * _more_
     *
     * @param unit _more_
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * _more_
     *
     * @param group _more_
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getGroup() {
        return this.group;
    }

    /**
     * _more_
     *
     * @param requestType _more_
     */
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRequestType() {
        return this.requestType;
    }

    /**
     * _more_
     *
     * @param descriptor _more_
     */
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescriptor() {
        return this.descriptor;
    }

    /**
     * _more_
     *
     * @param spacing _more_
     */
    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSpacing() {
        return this.spacing;
    }

    /**
     * _more_
     *
     * @param placeValue _more_
     */
    public void setPlaceValue(String placeValue) {
        this.placeValue = placeValue;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPlaceValue() {
        return this.placeValue;
    }

    /**
     * _more_
     *
     * @param line _more_
     */
    public void setLine(int line) {
        this.line = line;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLine() {
        return this.line;
    }

    /**
     * _more_
     *
     * @param lines _more_
     */
    public void setLines(int lines) {
        this.lines = lines;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLines() {
        return this.lines;
    }

    /**
     * _more_
     *
     * @param elem _more_
     */
    public void setElement(int elem) {
        this.elem = elem;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getElement() {
        return this.elem;
    }

    /**
     * _more_
     *
     * @param elems _more_
     */
    public void setElements(int elems) {
        this.elememts = elems;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getElements() {
        return this.elememts;
    }

    /**
     * _more_
     *
     * @param lineMag _more_
     */
    public void setLineMag(int lineMag) {
        this.lineMag = lineMag;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLineMag() {
        return this.lineMag;
    }

    /**
     * _more_
     *
     * @param elementMag _more_
     */
    public void setElementMag(int elementMag) {
        this.elementMag = elementMag;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getElementMag() {
        return this.elementMag;
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
    public void setLocate(String value) {
        //super.setLocateValue(value);
        String       locKey  = getLocateKey();
        List<String> locList = StringUtil.split(value, " ");
        if ((locKey != null) && locKey.equals(AddeImageURL.KEY_LINEELE)) {
            this.locationLine = Integer.parseInt(locList.get(0));
            this.locationElem = Integer.parseInt(locList.get(1));
            this.locateValue  = this.locationLine + " " + this.locationElem;
        } else {
            this.locationLat = Double.parseDouble(locList.get(0));
            this.locationLon = Double.parseDouble(locList.get(1));
            this.locateValue = Misc.format(this.locationLat) + " "
                               + Misc.format(this.locationLon);
        }

    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setLocateValue(String value) {
        this.locateValue = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLocate() {
        String locKey = getLocateKey();

        if ((locKey != null) && locKey.equals(AddeImageURL.KEY_LINEELE)) {

            this.locateValue = this.locationLine + " " + this.locationElem;
        } else {

            this.locateValue = Misc.format(this.locationLat) + " "
                               + Misc.format(this.locationLon);
        }
        return this.locateValue;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLocateValue() {
        return this.locateValue;
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
    public double getLocationLon() {
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

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setTrace(int value) {
        this.trace = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getTrace() {
        return this.trace;
    }

    /**
     * _more_
     *
     * @param debug _more_
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getDebug() {
        return this.debug;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setUser(String value) {
        this.user = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUser() {
        return this.user;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getVersion() {
        return version;
    }


    /**
     * _more_
     *
     * @param version _more_
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSizeValue() {
        return getLines() + " " + getElements();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getMagValue() {
        return getLineMag() + " " + getElementMag();
    }

    /**
     * Set the locate key
     *
     * @param value  the locate key
     */
    public void setLocateKey(String value) {
        this.locateKey = value;
        /*   if(value.equals(AddeImageURL.KEY_LATLON)){
               String locateValue = Misc.format(getLocationLat()) + " "
                       + Misc.format(getLocationLon());
               setLocateValue(locateValue);
           } else {
               String locateValue = getLocationLine() + " " + getLocationElem();
               setLocateValue(locateValue);
           }   */
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLocateKey() {
        return this.locateKey;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getURLString() {
        StringBuffer buf = new StringBuffer(AddeImageURL.ADDE_PROTOCOL);
        buf.append("://");
        buf.append(host);
        buf.append("/");
        buf.append(requestType);
        buf.append("?");
        appendKeyValue(buf, AddeImageURL.KEY_PORT, "" + getPort());
        appendKeyValue(buf, AddeImageURL.KEY_COMPRESS, getCompression());
        appendKeyValue(buf, AddeImageURL.KEY_USER, getUser());
        appendKeyValue(buf, AddeImageURL.KEY_PROJ, "" + getProject());
        appendKeyValue(buf, AddeImageURL.KEY_VERSION, getVersion());
        appendKeyValue(buf, AddeImageURL.KEY_DEBUG,
                       Boolean.toString(getDebug()));
        appendKeyValue(buf, AddeImageURL.KEY_TRACE, "" + getTrace());
        appendKeyValue(buf, AddeImageURL.KEY_GROUP, "" + getGroup());
        appendKeyValue(buf, AddeImageURL.KEY_DESCRIPTOR,
                       "" + getDescriptor());
        appendKeyValue(buf, AddeImageURL.KEY_BAND, "" + getBand());
        if (getLocateKey().equals(AddeImageURL.KEY_LINEELE)) {
            appendKeyValue(buf, AddeImageURL.KEY_LINEELE,
                           "" + getLocate());
        } else {
            appendKeyValue(buf, AddeImageURL.KEY_LATLON,
                           "" + getLocate());
        }

        appendKeyValue(buf, AddeImageURL.KEY_PLACE, "" + getPlaceValue());
        appendKeyValue(buf, AddeImageURL.KEY_SIZE, "" + getSizeValue());
        appendKeyValue(buf, AddeImageURL.KEY_UNIT, "" + getUnit());
        appendKeyValue(buf, AddeImageURL.KEY_MAG, "" + getMagValue());
        appendKeyValue(buf, AddeImageURL.KEY_SPAC, "" + getSpacing());
        appendKeyValue(buf, AddeImageURL.KEY_NAV, "" + getNavType());

        if ( !leftovers.isEmpty()) {
            for (String leftover : leftovers) {
                buf.append("&");
                buf.append(leftover);
            }
        }
        return buf.toString();
    }

    /**
     * _more_
     *
     * @param buf _more_
     * @param name _more_
     * @param value _more_
     */
    protected void appendKeyValue(StringBuffer buf, String name,
                                  String value) {
        if ((buf.length() == 0) || (buf.charAt(buf.length() - 1) != '?')) {
            buf.append("&");
        }
        buf.append(name);
        buf.append("=");
        buf.append(value);
    }
}

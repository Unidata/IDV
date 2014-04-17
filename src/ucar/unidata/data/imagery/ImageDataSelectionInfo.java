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

    public String host;

    public String requestType;

    public String band;

    public String unit;

    public String group;

    public String descriptor;

    public int spacing;

    public String placeValue;

    public String locateValue;

    public String locateKey;

    public int line;

    public int elem;

    public int lines;

    public int elememts;

    public int lineMag;

    public int elementMag;

    public int trace;

    public boolean debug;

    public String compress;

    public String user;

    public int port;

    public int project;

    public String version;

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

            if (key.startsWith("port")) {  // port
                setPort(Integer.parseInt(value));
            } else if (key.startsWith("comp")) {   // compress type
                setCompression(value);
            } else if (key.startsWith("user")) {   // user
                setUser(value);
            } else if (key.startsWith("proj")) {  // projection
                setProject(Integer.parseInt(value));
            } else if (key.startsWith("vers")) {  // version
                setVersion(value);
            } else if (key.startsWith("debug")) {  // version
                setDebug(Boolean.parseBoolean(value));
            } else if (key.startsWith("trace")) {   // trace
                setTrace(Integer.parseInt(value));
            } else if (key.startsWith("band")) {  // band
                setBand(value);
            } else if (key.startsWith("unit")) {   // unit
                setUnit(value);
            } else if (key.startsWith("group")) {   // group
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
            } else {
                leftovers.add(token);
            }

        }

    }

    public ImageDataSelectionInfo cloneMe(){
        return new ImageDataSelectionInfo(this.getURLString());
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getCompression() {
        return compress;
    }


    public void setCompression(String compress) {
        this.compress = compress;
    }

    public int getProject() {
        return project;
    }

    public void setProject(int project) {
        this.project = project;
    }

    public void setHost(String host){
        this.host = host;
    }

    public String getHost(){
        return this.host;
    }

    public void setBand(String band){
        this.band = band;
    }

    public String getBand(){
        return this.band;
    }

    public void setUnit(String unit){
        this.unit = unit;
    }

    public String getUnit(){
        return this.unit;
    }

    public void setGroup(String group){
        this.group = group;
    }

    public String getGroup(){
        return this.group;
    }

    public void setRequestType(String requestType){
        this.requestType = requestType;
    }

    public String getRequestType(){
        return this.requestType;
    }

    public void setDescriptor(String descriptor){
        this.descriptor = descriptor;
    }

    public String getDescriptor(){
        return this.descriptor;
    }

    public void setSpacing(int spacing){
        this.spacing = spacing;
    }

    public int getSpacing(){
        return this.spacing;
    }

    public void setPlaceValue(String placeValue){
        this.placeValue = placeValue;
    }

    public String getPlaceValue(){
        return this.placeValue;
    }

    public void setLine(int line){
        this.line = line;
    }

    public int getLine(){
        return this.line;
    }

    public void setLines(int lines){
        this.lines = lines;
    }

    public int getLines(){
        return this.lines;
    }

    public void setElement(int elem){
        this.elem = elem;
    }

    public int getElement(){
        return this.elem;
    }

    public void setElements(int elems){
        this.elememts = elems;
    }

    public int getElements(){
        return this.elememts;
    }

    public void setLineMag(int lineMag){
        this.lineMag = lineMag;
    }

    public int getLineMag(){
        return this.lineMag;
    }

    public void setElementMag(int elementMag){
        this.elementMag = elementMag;
    }

    public int getElementMag(){
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
    public void setLocateValue(String value) {
        //super.setLocateValue(value);
        String locKey = getLocateKey();
        List<String> locList = StringUtil.split(value, " ");
        if (locKey != null && locKey.equals(AddeImageURL.KEY_LINEELE)) {
            this.locationLine = Integer.parseInt(locList.get(0));
            this.locationElem = Integer.parseInt(locList.get(1));
            this.locateValue = this.locationLine + " " +  this.locationElem;
        } else {
            this.locationLat = Double.parseDouble(locList.get(0));
            this.locationLon = Double.parseDouble(locList.get(1));
            this.locateValue = Misc.format(this.locationLat) + " " + Misc.format(this.locationLon);
        }

    }

    public String getLocateValue() {
        String locKey = getLocateKey();

        if (locKey.equals(AddeImageURL.KEY_LINEELE)) {

            this.locateValue = this.locationLine + " " +  this.locationElem;
        } else {

            this.locateValue = Misc.format(this.locationLat) + " " + Misc.format(this.locationLon);
        }
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

    public void setTrace(int value){
        this.trace = value;
    }

    public int getTrace(){
        return this.trace;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean getDebug() {
        return this.debug;
    }

    public void setUser(String value){
        this.user = value;
    }

    public String getUser(){
        return this.user;
    }

    public String getVersion() {
        return version;
    }


    public void setVersion(String version) {
        this.version = version;
    }

    public String getSizeValue(){
        return getLines() + " " + getElements();
    }

    public String getMagValue(){
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

    public String getLocateKey(){
        return this.locateKey;
    }

    public String getURLString(){
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
        appendKeyValue(buf, AddeImageURL.KEY_DEBUG, Boolean.toString(getDebug()));
        appendKeyValue(buf, AddeImageURL.KEY_TRACE, "" + getTrace());
        appendKeyValue(buf, AddeImageURL.KEY_GROUP, "" + getGroup());
        appendKeyValue(buf, AddeImageURL.KEY_DESCRIPTOR, "" + getDescriptor());
        appendKeyValue(buf, AddeImageURL.KEY_BAND, "" + getBand());
        if(getLocateKey().equals(AddeImageURL.KEY_LINEELE))
            appendKeyValue(buf, AddeImageURL.KEY_LINEELE, "" + getLocateValue());
        else
            appendKeyValue(buf, AddeImageURL.KEY_LATLON, "" + getLocateValue());

        appendKeyValue(buf, AddeImageURL.KEY_PLACE, "" + getPlaceValue());
        appendKeyValue(buf, AddeImageURL.KEY_SIZE, "" + getSizeValue());
        appendKeyValue(buf, AddeImageURL.KEY_UNIT, "" + getUnit());
        appendKeyValue(buf, AddeImageURL.KEY_MAG, "" + getMagValue());
        appendKeyValue(buf, AddeImageURL.KEY_SPAC, "" + getSpacing());

        if ( !leftovers.isEmpty()) {
            for (String leftover : leftovers) {
                buf.append("&");
                buf.append(leftover);
            }
        }
        return buf.toString();
    }

    protected void appendKeyValue(StringBuffer buf, String name, String value) {
        if ((buf.length() == 0) || (buf.charAt(buf.length() - 1) != '?')) {
            buf.append("&");
        }
        buf.append(name);
        buf.append("=");
        buf.append(value);
    }
}

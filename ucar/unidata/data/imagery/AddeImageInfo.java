/*
 * $Id: AddeImageInfo.java,v 1.7 2007/07/02 21:52:47 dmurray Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

import ucar.visad.UtcDate;

import visad.DateTime;
import visad.VisADException;

import java.net.MalformedURLException;

import java.net.URL;


/**
 * A class for holding the information about an ADDE image request
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.7 $
 */
public class AddeImageInfo extends AddeImageURL {

    /** base url */
    private String baseUrl;

    /** no arg constructor */
    public AddeImageInfo() {}

    /**
     * Create an AddeImageURL.
     *
     * @param host host to send to
     * @param requestType   type of request (REQ_IMAGEDATA, REQ_IMAGEDIR)
     * @param group   ADDE group
     * @param descriptor   ADDE descriptor
     */
    public AddeImageInfo(String host, String requestType, String group,
                         String descriptor) {
        this(host, requestType, group, descriptor, null);
    }

    /**
     * Create an ADDE Image URL from the given specs.
     *
     * @param host host to send to
     * @param requestType   type of request (REQ_IMAGEDATA, REQ_IMAGEDIR)
     * @param group   ADDE group (may be null)
     * @param descriptor   ADDE descriptor (may be null)
     * @param query   query string (key/value pairs)
     */
    public AddeImageInfo(String host, String requestType, String group,
                         String descriptor, String query) {
        super(host, requestType, group, descriptor, query);
    }

    /**
     * Create an AddeImageInfo.
     *
     * @param baseUrl    base url (server, request type, etc)
     * @param locateKey  locate key
     * @param locateValue  locate value
     * @param placeValue    PLACE value
     * @param lines      number of lines
     * @param elements   number of elements
     * @param lmag       line magnification
     * @param emag       element magnification
     */
    public AddeImageInfo(String baseUrl, String locateKey,
                         String locateValue, String placeValue, int lines,
                         int elements, int lmag, int emag) {
        super(null, null, null, null, locateKey, locateValue, placeValue,
              lines, elements, lmag, emag, DEFAULT_VALUE, DEFAULT_VALUE, -1);
        this.baseUrl = baseUrl;
        decodeBaseURL(baseUrl);
    }

    /**
     * Get the base ADDE URL
     *
     * @return the base ADDE URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Set the base ADDE URL
     *
     * @param value the base ADDE URL
     */
    public void setBaseUrl(String value) {
        baseUrl = value;
        decodeBaseURL(baseUrl);
    }

    /**
     * Create the ADDE URL
     * @return a Adde URL
     * @deprecated  use getURLString() instead
     */
    public String makeAddeUrl() {
        return getURLString();
    }

    /**
     * Decode the baseURL for legacy bundles
     *
     * @param baseURL the base url of form adde:/host/request?query
     */
    private void decodeBaseURL(String baseURL) {
        try {
            URL url = new URL(baseURL);
            setHost(url.getHost());
            setRequestType(url.getPath().substring(1).toLowerCase());
            String query = url.getQuery();
            setExtraKeys(query);
        } catch (MalformedURLException mue) {}
    }

    /**
     * Create a DAY/TIME or POS string.  Override superclass so we can
     * make human readable as opposed to McIDAS formatted dates
     * @param buf  buffer to append to
     */
    protected void appendDateOrPosString(StringBuffer buf) {
        if ((getStartDate() == null) && (getEndDate() == null)) {
            appendKeyValue(buf, KEY_POS, "" + getDatasetPosition());
        } else {
            DateTime start = null;
            DateTime end   = null;
            if (getStartDate() != null) {
                try {
                    start = new DateTime(getStartDate());
                } catch (VisADException ve) {}
            }
            if (getEndDate() != null) {
                try {
                    end = new DateTime(getEndDate());
                } catch (VisADException ve) {}
            }
            StringBuffer day  = new StringBuffer();
            StringBuffer time = new StringBuffer();
            if (start != null) {
                day.append(UtcDate.getYMD(start));
                time.append(UtcDate.getHMS(start));
            }
            day.append(" ");
            time.append(" ");
            if (end != null) {
                if (getRequestType().equals(REQ_IMAGEDIR)) {
                    day.append(UtcDate.getYMD(end));
                }
                time.append(UtcDate.getHMS(end));
            }
            appendKeyValue(buf, KEY_DAY, day.toString().trim());
            appendKeyValue(buf, KEY_TIME, time.toString().trim());
        }
    }
}


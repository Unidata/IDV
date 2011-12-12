/*
 * $Id: FrontDataSource.java,v 1.15 2007/04/17 22:22:52 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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





package ucar.unidata.data.text;


import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Holds a named product
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */

public class Product implements Comparable {

    /** the product date */
    private Date date;

    /** the content */
    private String content;

    /** the station identifier */
    private String station;

    /**
     * Create a product
     *
     * @param station  station
     * @param content  content
     * @param date  the date
     */
    public Product(String station, String content, Date date) {
        this.station = station;
        this.content = content;
        this.date    = date;
    }


    /**
     * Compare this to o
     *
     * @param o the object to compare to
     *
     * @return the comparison
     */
    public int compareTo(Object o) {
        Product that = (Product) o;
        if (date == null) {
            if (that.date == null) {
                return 0;
            }
            return -1;
        }
        if (that.date == null) {
            return 1;
        }
        return (date.compareTo(that.date));
    }

    /**
     * Get a string representation
     *
     * @return a string representation
     */
    public String toString() {
        return station;
    }

    /**
     * Set the Date property.
     *
     * @param value The new value for Date
     */
    public void setDate(Date value) {
        date = value;
    }

    /**
     * Get the Date property.
     *
     * @return The Date
     */
    public Date getDate() {
        return date;
    }



    /**
     * Set the Content property.
     *
     * @param value The new value for Content
     */
    public void setContent(String value) {
        content = value;
    }

    /**
     * Get the Content property.
     *
     * @return The Content
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the Station property.
     *
     * @param value The new value for Station
     */
    public void setStation(String value) {
        station = value;
    }

    /**
     * Get the Station property.
     *
     * @return The Station
     */
    public String getStation() {
        return station;
    }


}


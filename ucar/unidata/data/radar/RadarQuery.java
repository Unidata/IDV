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

package ucar.unidata.data.radar;


import ucar.unidata.util.DateSelection;


/**
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class RadarQuery {

    /** ur to the radar collection */
    private String collectionUrl;

    /** The station */
    private String station;

    /** The product */
    private String product;

    /** The list of dates */
    private DateSelection dateSelection;


    /**
     * ctor for encoding
     */
    public RadarQuery() {}

    /**
     * ctor
     *
     * @param collectionUrl the collection url
     * @param station the station
     * @param dateSelection the date selection_
     */
    public RadarQuery(String collectionUrl, String station,
                      DateSelection dateSelection) {
        this.collectionUrl = collectionUrl;
        this.station       = station;
        this.dateSelection = dateSelection;
    }

    /**
     * ctor
     *
     * @param collectionUrl the collection url
     * @param station the station
     * @param product  the product
     * @param dateSelection the date selection_
     */
    public RadarQuery(String collectionUrl, String station, String product,
                      DateSelection dateSelection) {
        this.collectionUrl = collectionUrl;
        this.station       = station;
        this.product       = product;
        this.dateSelection = dateSelection;
    }

    /**
     * Set the CollectionUrl property.
     *
     * @param value The new value for CollectionUrl
     */
    public void setCollectionUrl(String value) {
        collectionUrl = value;
    }

    /**
     * Get the CollectionUrl property.
     *
     * @return The CollectionUrl
     */
    public String getCollectionUrl() {
        return collectionUrl;
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
        //Uncomment this if you want to do a server stress test
        /*
        if(stationName ==  null) {
            stationName = stations[nextStation++];
            if(nextStation>= stations.length)
                nextStation = 0;
        }
        System.err.println("station:" + stationName);
        return stationName;
        */
        return station;
    }

    /** _more_ */
    private static int nextStation = 0;

    /** _more_ */
    private int stationIdx = -1;

    /** _more_ */
    private String stationName = null;


    /** _more_ */
    private static String[] stations = {
        "KTYX", "KPAH", "KFDR", "KAMA", "KFDX", "KICX", "KCLX", "KATX",
        "KBHX", "KJKL", "KMVX", "KGRB", "KMOB", "KVWX", "KHDX", "KGRK",
        "KLSX", "KSFX", "KICT", "KDIX", "KGRR", "KRIW", "KFSD", "KILN",
        "KUDX", "KGLD", "KMUX", "KILX", "KCLE", "KESX", "KPBZ", "KLCH",
        "NOP3", "KCBW", "KLTX", "KCBX", "KFCX", "KBYX", "KGSP", "KDFX",
        "KBOX", "KNQA", "KARX", "KTWX", "KDOX", "KDYX", "KDDC", "KGJX",
        "KMPX", "KSHV", "KAKQ", "KHNX", "KCCX", "KABX", "KRAX", "KABR",
        "KDGX", "KOHX", "KCRP", "KSGF", "KSOX", "KBIS", "KRGX", "KYUX",
        "KBRO", "KLNX", "KTBW", "KEAX", "KLVX", "KFFC", "KMHX", "KJGX",
        "KMAF", "KIWX", "KMRX", "KDAX", "KOAX", "KEMX", "KLWX", "KCXX",
        "KBLX", "KSRX", "KDVN", "KSJT", "KEVX", "KMAX", "KIWA", "KDMX",
        "KLOT", "KMQT", "KGWX", "KVTX", "KFWS", "KPUX", "KBBX", "KBUF",
        "TJUA", "KENX", "KHGX", "KGGW", "KEWX", "KAPX", "KCYS", "KOTX",
        "KTLH", "KLZK", "KMTX", "KDLH", "KUEX", "KOKX", "KDTX", "KFSX",
        "KTLX", "KRLX", "KLBB", "KFTG", "KHTX", "KIND", "KBGM", "KLIX",
        "KGYX", "KTFX", "KINX", "KMSX", "KAMX", "KMLB", "KCAE", "KVNX",
        "KNKX", "KBMX", "KEPZ", "KRTX", "KJAX", "KPDT", "KLRX", "KMKX"
    };







    /**
     * Set the Product property.
     *
     * @param value The new value for Station
     */
    public void setProduct(String value) {
        product = value;
    }

    /**
     * Get the Station property.
     *
     * @return The Station
     */
    public String getProduct() {
        return product;
    }

    /**
     *  Set the DateSelection property.
     *
     *  @param value The new value for DateSelection
     */
    public void setDateSelection(DateSelection value) {
        dateSelection = value;
    }

    /**
     *  Get the DateSelection property.
     *
     *  @return The DateSelection
     */
    public DateSelection getDateSelection() {
        return dateSelection;
    }


}

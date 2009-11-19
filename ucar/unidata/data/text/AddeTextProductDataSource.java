/*
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


import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidas.adde.WxTextProduct;

import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


/**
 * A class for handling text ADDE test
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class AddeTextProductDataSource extends NwxTextProductDataSource {

    /**
     * properties for warnings
     */
    private Properties warningProps;

    /**
     * waring properties file name
     */
    private static final String WARN_PROP_FILE = "warnings.search.properties";

    /**
     * path to table text
     */
    public static final String PROP_TABLE_PATH = "text.table.path";

    /**
     * path to table text
     */
    public static final String PROP_WARN_PATH = "text.warning.path";

    /**
     * Default bean constructor; does nothing.
     *
     */
    public AddeTextProductDataSource() {}

    /**
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public AddeTextProductDataSource(DataSourceDescriptor descriptor,
                                     String filename, Hashtable properties) {
        this(descriptor, Misc.newList(filename), properties);
    }


    /**
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     */
    public AddeTextProductDataSource(DataSourceDescriptor descriptor,
                                     List files, Hashtable properties) {
        super(descriptor, files, "ADDE Text Products", "ADDE Text Products",
              properties);
    }

    /**
     * Read products for the station
     *
     * @param ti  the table information
     * @param stations list of stations
     * @param dateSelection the date selection
     *
     * @return  the list of products
     */
    protected List<Product> readProducts(TableInfo ti,
                                         List<NamedStationImpl> stations,
                                         DateSelection dateSelection) {
        List<Product> products = new ArrayList<Product>();
        if ( !canHandleType(ti)) {
            return products;
        }
        /*
        if ((stations == null) || (stations.size() == 0)) {
            return products;
        }
        */

        String base = "adde://"
                      + getDataContext().getIdv().getProperty("textserver",
                          "adde.ucar.edu") + "/";

        try {
            if ( !ti.flag.equals(ti.FLAG_O)) {
                if (stations == null) {
                    stations = new ArrayList<NamedStationImpl>();
                    stations.add((NamedStationImpl) null);
                }
                for (NamedStationImpl station : stations) {
                    String url = base
                                 + getWxTextRequest(ti, station,
                                     dateSelection);
                    // System.out.println("url = " + url);
                    AddeTextReader      atr   = new AddeTextReader(url);
                    List<WxTextProduct> prods = atr.getWxTextProducts();
                    for (Iterator itera =
                            prods.iterator(); itera.hasNext(); ) {
                        WxTextProduct wtp = (WxTextProduct) itera.next();
                        products.add(new Product(wtp.getWstn(),
                                wtp.getText(), wtp.getDate()));
                    }
                }
            } else {
                String url = base
                             + getObTextRequest(ti, stations, dateSelection);
                //System.out.println("url = " + url);
                AddeTextReader atr = new AddeTextReader(url);
                String         obs = atr.getText();
                products.add(new Product(stations.toString(), atr.getText(),
                                         new Date()));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return products;
    }

    /**
     * Get the weather text request
     *
     * @param ti  table info
     * @param station  station
     * @param dateSelection the date selection
     *
     * @return the request string
     */
    private String getWxTextRequest(TableInfo ti, NamedStationImpl station,
                                    DateSelection dateSelection) {
        Date[] dateRange = (((dateSelection == null)
                             || dateSelection.isLatest()
                             || dateSelection.isAll())
                            ? null
                            : dateSelection.getRange());

        Date  endTime = (dateRange == null)
                        ? new Date()
                        : dateRange[1];
        int[] endDT   = McIDASUtil.mcSecsToDayTime(endTime.getTime() / 1000);
        int maxCount = (((dateSelection == null) || dateSelection.isLatest())
                        ? 1
                        : dateSelection.getCount());
        int   dtime   = 720;
        if (dateRange != null) {
            int hours = (int) (Math.abs(dateRange[1].getTime()
                                        - dateRange[0].getTime()) / (1000
                                            * 3600));
            if (hours < 1) {
                hours = 1;
            }
            dtime = hours;
        }

        StringBuilder buf = new StringBuilder("wxtext?");
        if (ti.flag.equals(ti.FLAG_W)) {
            if (warningProps != null) {
                String search = warningProps.getProperty(ti.type);
                if (search != null) {
                    buf.append(search);
                }
                if (station != null) {
                    buf.append("&WSTN=");
                    buf.append(station.getID());
                }
            } else {

                // Major  hack
                String apro = ti.fileExtension;
                if (apro.length() == 3 && StringUtil.isUpperCase(apro)) {
                    buf.append("APRO=");
                    buf.append(apro);
                    if (station != null) {
                        buf.append("&WSTN=");
                        buf.append(station.getID());
                    }
                }
            }
        } else if (ti.flag.equals(ti.FLAG_F)) {
            String afos = station.getID();
            if ((afos == null) || afos.equals("")) {
                return "";
            }
            buf.append("APRO=");
            buf.append(afos.substring(0, 3));
            buf.append("&ASTN=");
            buf.append(afos.substring(3));
        } else {
            buf.append("WMO=");
            buf.append(station.getProperty(NamedStationTable.KEY_BULLETIN,
                                           "NONE"));
            buf.append("&WSTN=");
            buf.append(station.getID());
            // further refine the search; Major  hack
            String apro = ti.fileExtension;
            if (apro.length() == 3 && StringUtil.isUpperCase(apro)) {
                buf.append("&APRO=");
                buf.append(apro);
            }
        }
        buf.append("&dtime=");
        buf.append(dtime);
        buf.append("&num=");
        buf.append(maxCount);
        buf.append("&day=");
        buf.append(endDT[0]);
        //buf.append("&debug=true");
        return buf.toString();
    }


    /**
     * Get the ob text request
     *
     * @param ti  table info
     * @param stations  stations
     * @param dateSelection the date selection
     *
     * @return  the request string
     */
    private String getObTextRequest(TableInfo ti,
                                    List<NamedStationImpl> stations,
                                    DateSelection dateSelection) {
        Date[] dateRange = (((dateSelection == null)
                             || dateSelection.isLatest()
                             || dateSelection.isAll())
                            ? null
                            : dateSelection.getRange());
        Date          start = (dateRange == null)
                              ? null
                              : dateRange[0];
        Date          end   = (dateRange == null)
                              ? null
                              : dateRange[1];
        int maxCount = (((dateSelection == null) || dateSelection.isLatest())
                        ? 1
                        : dateSelection.getCount());

        StringBuilder buf   = new StringBuilder("obtext?");
        buf.append("&ID=");
        for (NamedStationImpl station : stations) {
            String id = station.getID();
            String idn =
                (String) station.getProperty(NamedStationTable.KEY_IDNUMBER,
                                             "");
            if ( !idn.equals("")) {
                idn = idn.substring(0, 5);
            }
            if (ti.type.equals("SND_DATA")) {
                id = idn;
            } else if (ti.type.equals("SYN_DATA")) {
                id = idn;
            } else {  // (ti.type.equals("SFC_HRLY")) 
                // uses 3 letter ids
                if (id.length() < 4) {
                    id = "K" + id;
                }
            }
            buf.append(id);
            buf.append(" ");
        }

        int hourMod = 1;
        buf.append("&descriptor=");
        if (ti.type.equals("SND_DATA")) {
            buf.append("UPPERAIR");
            hourMod = 3;
        } else if (ti.type.equals("SYN_DATA")) {
            buf.append("SYNOPTIC");
            hourMod = 3;
        } else if (ti.type.equals("TAFS_DEC")) {
            buf.append("TERMFCST");
        } else {  // (ti.type.equals("SFC_HRLY")) {
            buf.append("SFCHOURLY");
        }
        // set the times
        // TODO:  this needs some work
        // contrary to the docs, the time in newest/oldest is HH not HHMMSS
        if (dateRange != null) {
            int[] endDT   = McIDASUtil.mcSecsToDayTime(end.getTime() / 1000);
            int   endHour = endDT[1] / 10000;
            endHour = endHour - endHour % hourMod;
            buf.append("&newest=");
            buf.append(endDT[0]);
            buf.append(" ");
            buf.append(endHour);
            int[] startDT = McIDASUtil.mcSecsToDayTime(start.getTime()
                                / 1000);
            int startHour = startDT[1] / 10000;
            startHour = startHour - startHour % hourMod;
            buf.append("&oldest=");
            buf.append(startDT[0]);
            buf.append(" ");
            buf.append(startHour);
        }
        buf.append("&num=");
        buf.append(maxCount);

        return buf.toString();
    }


    /**
     * Get the table path
     * @return the base path of the data.
     */
    protected String getTablePath() {
        return getIdvProperty(
            PROP_TABLE_PATH,
            "http://www.unidata.ucar.edu/software/idv/resources");
    }

    /**
     * Set the additional resources needed for this to work.
     * @return  true if resources set okay
     */
    protected boolean setAdditionalResources() {
        return true;
    }

    /**
     * Get the error message if additional resources aren't available
     * @return  error messaage
     */
    public String getAdditionalResourcesError() {
        return "";
    }

    /**
     * Initialize after opening.
     */
    protected void initAfter() {
        super.initAfter();
        String warningPropFile = null;
        try {
            warningPropFile = getIdvProperty(PROP_WARN_PATH,
                                             getTablePath() + "/nwx/"
                                             + WARN_PROP_FILE);
            warningProps = Misc.readProperties(warningPropFile, warningProps,
                    getClass());
        } catch (Exception e) {
            System.err.println("Couldn't read warning property file: "
                               + warningPropFile);
        }  // doesn't matter if we can't get it
    }

    /**
     * Can we handle this type of data?
     *
     * @param ti  the table info
     *
     * @return true if we can handle it.
     */
    protected boolean canHandleType(TableInfo ti) {
        return ti.flag.equals(TableInfo.FLAG_B)
               || ti.flag.equals(TableInfo.FLAG_F)
               || ti.flag.equals(TableInfo.FLAG_W)
               || ti.flag.equals(TableInfo.FLAG_O);
    }

    /**
     * Get an IDV property
     *
     * @param name  name of property
     * @param def   default value
     *
     * @return  the property if found or the default value
     */
    private String getIdvProperty(String name, String def) {
        return getDataContext().getIdv().getProperty(name, def);
    }

}


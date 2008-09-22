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


import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.FilesDataSource;

import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;


import visad.*;

import java.io.File;

import java.rmi.RemoteException;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import java.util.regex.*;


/**
 * A class for handling text products configured from NWX tables
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class NwxTextProductDataSource extends TextProductDataSource {

    /** the table map */
    private Hashtable tableMap = new Hashtable();

    /** the path to the tables */
    private String tablePath;

    /** text data path */
    private String textDataPath;

    /** gemdata path */
    private String gemDataPath;

    /** GEMTBL property */
    private static final String PROP_GEMTBL = "GEMTBL";

    /** TEXT_DATA property */
    private static final String PROP_TEXT_DATA = "TEXT_DATA";

    /** GEMDATA property */
    private static final String PROP_GEMDATA = "GEMDATA";

    /** list of table infos */
    private List<TableInfo> tables = new ArrayList<TableInfo>();

    /** the product groups */
    private List<ProductGroup> productGroups;

    /** the master table */
    protected static String MASTER_TABLE = "master.tbl";

    /** the gui table */
    protected static String GUI_TABLE = "guidata.tbl";

    /**
     * Default bean constructor; does nothing.
     *
     */
    public NwxTextProductDataSource() {}

    /**
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public NwxTextProductDataSource(DataSourceDescriptor descriptor,
                                    String filename, Hashtable properties) {
        this(descriptor, Misc.newList(filename), properties);
    }


    /**
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     */
    public NwxTextProductDataSource(DataSourceDescriptor descriptor,
                                    List files, Hashtable properties) {
        this(descriptor, files, "NWX Text Products", "NWX Text Products",
             properties);
    }

    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    data source descriptor
     * @param newSources       List of sources of data (filename/URL)
     * @param name my name
     * @param description   description of the data
     * @param properties    extra properties for initialization
     */
    public NwxTextProductDataSource(DataSourceDescriptor descriptor,
                                    List newSources, String name,
                                    String description,
                                    Hashtable properties) {
        super(descriptor, newSources, name, description, properties);
    }


    /**
     * Get the table for the product type
     *
     * @param productType  the product type
     *
     * @return  the table or null if one can't be found
     */
    public TableInfo getTableInfo(ProductType productType) {
        if (productType == null) {
            return null;
        }
        for (TableInfo tableInfo : tables) {
            if (tableInfo.type.equals(productType.getId())) {
                return tableInfo;
            }
        }
        return null;
    }


    /**
     * Get the stations for a productType
     *
     * @param productType  the product type
     *
     * @return  the list of stations
     *
     * @throws Exception problem reading the stations
     */
    public NamedStationTable getStations(ProductType productType,DateSelection dateSelection)
            throws Exception {
        TableInfo tableInfo = getTableInfo(productType);
        if (tableInfo != null) {
            return getStations(tableInfo);
        }
        return null;
    }

    /**
     * Read products
     *
     * @param productType  the product type
     * @param station  the station
     *
     * @return the list of products
     */
    public List<Product> readProducts(ProductType productType,
                                      NamedStationImpl station,DateSelection dateSelection) {
        TableInfo tableInfo = getTableInfo(productType);
        if ((tableInfo != null) && canHandleType(tableInfo)) {
            return readProducts(tableInfo, station, dateSelection);
        }
        return new ArrayList<Product>();
    }


    /**
     * Read the table file
     * @param file name
     *
     * @return the  table as a string
     *
     * @throws Exception problem reading the table
     */
    private String readTableFile(String file) throws Exception {
        String contents = IOUtil.readContents(tablePath + "/nwx/" + file,
                              getClass(), null);
        return contents;
    }


    /**
     * Get the stations for the table info
     *
     * @param tableInfo  the table info
     *
     * @return the station table
     *
     * @throws Exception problem getting the stations
     */
    public NamedStationTable getStations(TableInfo tableInfo)
            throws Exception {
        String contents = readTableFile(tableInfo.locationFile);
        if (contents == null) {
            return null;
        }
        try {
            //            System.err.println (tableInfo.locationFile + " " +tableInfo.flag);
            NamedStationTable table =
                (NamedStationTable) tableMap.get(tableInfo.locationFile);

            if (table == null) {
                table = new NamedStationTable("Stations for "
                        + tableInfo.type);
                if (tableInfo.flag.equals(TableInfo.FLAG_B)
                        || tableInfo.flag.equals(TableInfo.FLAG_S)
                        || tableInfo.flag.equals(TableInfo.FLAG_W)) {
                    table.createStationTableFromBulletin(contents);
                } else {
                    table.createStationTableFromGempak(contents);

                }


                tableMap.put(tableInfo.locationFile, table);
            }
            return table;
        } catch (Exception exc) {
            logException("Error reading table", exc);
        }
        return null;
    }

    /**
     * Get the list of product groups
     *
     * @return  the list of product groups
     */
    public List<ProductGroup> getProductGroups() {
        return productGroups;
    }


    /**
     * Get the table path.  Subclasses can overrid
     * @return the base path of the data.
     */
    protected String getTablePath() {
        return System.getenv(PROP_GEMTBL);
    }

    /**
     * Set the additional resources needed for this to work.
     * @return  true if resources set okay
     */
    protected boolean setAdditionalResources() {
        textDataPath = System.getenv(PROP_TEXT_DATA);
        gemDataPath  = System.getenv(PROP_GEMDATA);
        return ((textDataPath != null) && (gemDataPath != null));
    }

    /**
     * Get the error message if additional resources aren't available
     * @return  error messaage
     */
    public String getAdditionalResourcesError() {
        return "You must have the GEMPAK environment variables "
               + PROP_GEMDATA + " and " + PROP_TEXT_DATA + " defined";
    }

    /**
     * Initialize after opening.
     */
    protected void initAfter() {
        tablePath = getTablePath();
        if (tablePath == null) {
            setInError(true, "Couldn't find path to tables");
            return;
        }
        if ( !setAdditionalResources()) {
            setInError(true, getAdditionalResourcesError());
            return;
        }
        try {
            String masterTable = readTableFile(MASTER_TABLE);
            if (masterTable == null) {
                throw new BadDataException("Unable to read " + MASTER_TABLE);
            }
            //!(12)        (12)       (1) (40)                                     (8)
            //SFC_HRLY     sfstns.tbl   O $GEMDATA/surface                         _sao.gem

            List<String[]> tableInfo = StringUtil.parseLineWords(masterTable,
                                           new int[] { 0,
                    13, 26, 28, 69 }, new int[] { 12, 12, 1, 40, 8 }, "\n",
                                      "!", true);

            for (String[] tuple : tableInfo) {
                tables.add(new TableInfo(tuple[0], tuple[1], tuple[2],
                                         tuple[3], tuple[4]));
            }

            String gui = readTableFile(GUI_TABLE);
            if (gui == null) {
                throw new BadDataException("Unable to read " + GUI_TABLE);
            }
            productGroups = parseProductGroups(gui);
        } catch (Exception exc) {
            logException("Error initializing table based products", exc);
            setInError(true, "Error initializing table based products");
        }
    }


    /**
     * Parse the product groups
     *
     * @param contents  the table contents
     *
     * @return the list of ProductGroup-s
     * @throws Exception  problem parsing
     */
    private List<ProductGroup> parseProductGroups(String contents)
            throws Exception {

        contents = contents.replace("{", "\n{\n");
        contents = contents.replace("}", "\n}\n");
        List<String> lines = (List<String>) StringUtil.split(contents, "\n",
                                 true, true);
        List<ProductGroup> products     = new ArrayList<ProductGroup>();
        ProductGroup       productGroup = null;
        boolean            inProduct    = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (productGroup != null) {
                if (line.equals("}")) {  // we're done with this group
                    if ( !productGroup.getProductTypes().isEmpty()) {
                        products.add(productGroup);
                    }
                    productGroup = null;
                } else if (line.equals("{")) {
                    //NOOP
                } else {
                    String[] toks = StringUtil.split(line, "|", 2);
                    if ((toks == null) || (toks.length != 2)) {
                        throw new IllegalArgumentException("Bad line:"
                                + line);
                    }
                    if (toks[0].startsWith("(")) {
                        continue;
                    }
                    if (toks[0].startsWith("!")) {
                        continue;
                    }
                    ProductType pt = new ProductType(toks[0].replace("_",
                                         " "), toks[1]);
                    TableInfo ti = getTableInfo(pt);
                    if ((ti != null) && canHandleType(ti)) {
                        productGroup.addProduct(pt);
                    }
                }
            } else if (line.equals("{")) {
                productGroup = null;
            } else {
                productGroup = new ProductGroup(line.replace("_", " "));
            }
        }
        return products;
    }

    /**
     * Read the products for defined by the table info.  Subclasses need
     * to override this method for their particular stuff
     *
     * @param tableInfo the table info
     * @param station  the station
     *
     * @return the products
     */
    protected List<Product> readProducts(TableInfo tableInfo,
                                         NamedStationImpl station,DateSelection dateSelection) {
        String path = tableInfo.dataDir;
        path = path.replace("$TEXT_DATA", textDataPath);
        path = path.replace("$GEMDATA", gemDataPath);
        File dir = new File(path);
        File[] files =
            dir.listFiles((java.io.FileFilter) new PatternFileFilter(".*\\."
                + tableInfo.fileExtension));
        List<Product> products = new ArrayList<Product>();
        if ((files == null) || (files.length == 0)) {
            return products;
        }
        Date[] dateRange = (dateSelection==null?null:dateSelection.getRange());
        int maxCount = (dateSelection==null?Integer.MAX_VALUE: dateSelection.getCount());

        List<DatedObject> datedObjects = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            File f= files[i];
            Date  fileDate = getDateFromFileName(f.toString());
            if(fileDate==null) {
                fileDate = new Date(f.lastModified());
            }
            datedObjects.add(new DatedObject(fileDate,f));
        }
        datedObjects = (List<DatedObject>)DatedObject.sort(datedObjects, false);
                

        files = IOUtil.sortFilesOnAge(files, true);
        int count = 0;
        boolean ok = true;
        for (DatedObject datedObject: datedObjects) {
            if(!ok) break;
            try {
                Date fileDate = datedObject.getDate();
                File f= (File)datedObject.getObject();
                if(dateRange!=null) {
                    if(!(dateRange[0].getTime()<=fileDate.getTime() && fileDate.getTime()<=dateRange[1].getTime())) {
                        //                        System.err.println ("\tskipping file:" + f + " " + fileDate + " " + dateRange[0] +" " + dateRange[1]);
                        continue;
                    }
                }
                List<Product> productsInFile = parseProduct(f.toString(), true,
                                                   station);
                for(Product product: productsInFile) {
                    products.add(product);
                    count++;
                    if(count>=maxCount) {
                        ok = false;
                        break;
                    }
                }
            } catch (Exception exc) {
                //                return "Error reading text product file:" + exc;
            }

        }
        return products;
    }


    private static SimpleDateFormat sdf;

    private static Date getDateFromFileName(String path)  {
        if(sdf==null) {
            sdf      = new SimpleDateFormat("yyyyMMddkk");
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        }
        String tmp = IOUtil.getFileTail(IOUtil.stripExtension(path));
        try {
            synchronized(sdf) {
                return  sdf.parse(tmp);
            }
        } catch (Exception exc) {
            System.err.println("no file date:" + tmp + ": " + exc);
        }
        return null;
    }

    /**
     * Parse the product from the files
     *
     * @param path  path to the files
     * @param recordType  the record type
     * @param station  the station to search for
     *
     * @return the list of products
     *
     * @throws Exception problem reading or parsing
     */
    private static List<Product> parseProduct(String path,
            boolean recordType, NamedStationImpl station)
            throws Exception {

        List<Product> products = new ArrayList<Product>();
        String contents = IOUtil.readContents(path,
                              NwxTextProductDataSource.class);
        String           prefix   = (recordType
                                     ? ""
                                     : "");
        String           suffix   = (recordType
                                     ? ""
                                     : "");
        int              idx      = 0;
        String           id       = ((station != null)
                                     ? station.getID()
                                     : null);
        Date             fileDate = getDateFromFileName(path);
        //        System.err.println ("contents:" + contents);
        while (true) {
            int idx1 = contents.indexOf(prefix, idx);
            if (idx1 < 0) {
                break;
            }
            int idx2 = contents.indexOf(suffix, idx1);
            if (idx2 < 0) {
                break;
            }
            idx = idx2 + 1;
            String  product          = contents.substring(idx1 + 1, idx2 - 1);
            int     lineCnt          = 0;
            int     startLineIdx     = 0;
            String  stationLine      = null;
            boolean seenNonBlankLine = false;
            Date    date             = null;


            SimpleDateFormat sdf1 =
                new SimpleDateFormat("hhm a z EEE MMM d yyyy");
            SimpleDateFormat sdfNoAmPm =
                new SimpleDateFormat("hhm z EEE MMM d yyyy");
            List lines = new ArrayList();
            while (true) {
                int endLineIdx = product.indexOf("\n", startLineIdx);
                if (endLineIdx < 0) {
                    break;
                }
                String line = product.substring(startLineIdx, endLineIdx);
                startLineIdx = endLineIdx + 1;
                lines.add(line);
                String tline = line.trim();
                if (seenNonBlankLine || (tline.length() > 0)) {
                    seenNonBlankLine = true;
                    lineCnt++;
                    if (lineCnt == 2) {
                        stationLine = line;
                        //                        break;
                    } else if (lineCnt > 2) {
                        if (tline.length() > 10) {
                            String[] toks = StringUtil.split(tline, " ", 2);
                            if ((toks == null) || (toks.length != 2)) {
                                continue;
                            }
                            String hhmm = toks[0];
                            try {
                                new Integer(hhmm);
                            } catch (Exception exc) {
                                continue;
                            }
                            if (hhmm.length() == 3) {
                                hhmm = "0" + hhmm;
                            } else if (hhmm.length() == 1) {
                                hhmm = "0" + hhmm + "00";
                            }

                            String dttm = hhmm + " " + toks[1];

                            try {
                                date = sdf1.parse(dttm);
                            } catch (Exception exc) {}
                            if (date == null) {
                                try {
                                    date = sdfNoAmPm.parse(dttm);
                                } catch (Exception exc) {
                                    //                                    System.err.println ("BAD:" +dttm);
                                }
                            }
                            //                            System.err.println (tline + " : " + date);
                            if (date != null) {
                                break;
                            }
                        }
                    }
                }
                if (lineCnt > 8) {
                    break;
                }
            }
            if (stationLine == null) {
                continue;
            }
            if (date == null) {
                date = fileDate;
            }
            List toks = StringUtil.split(stationLine, " ", true, true);
            if (toks.size() < 2) {
                continue;
            }
            String stationString = (String) toks.get(1);
            //            System.err.println("ID:"+ id +":" + stationString+" line:" + stationLine);
            if ((id == null) || Misc.equals(stationString, id)) {
                products.add(new Product(stationString, product, date));
            }
            //            System.out.println("************");
            //            if(true) break;
        }
        return products;

    }

    /**
     * _more_
     *
     * @param ti _more_
     *
     * @return _more_
     */
    protected boolean canHandleType(TableInfo ti) {
        return ti.flag.equals(TableInfo.FLAG_B);
    }

    /**
     * Class to hold the table information
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    protected class TableInfo {
        /*
         * !       B - Regular bulletin type ('^A'... text ... '^C'),
         * !               use combination of WMO header & stn ID for search,
         * !               use LOC_TBL to plot station location markers,
         * !               display entire bulletin.
         * !       S - Same as B type except use stn ID only for search.
         * !       W - Watch/Warning, same as B type except search data first to
         * !               get station IDs for marker plotting.
         * !       R - Record type ('^^/^A' ... text ... '^^/^C'),
         * !               search for "stnID".
         * !       M - Same as R type except search for "^^stnID ".
         * !       F - Same as R type except "stnID" is formatted to be a left-
         * !               justified 6-character ( not including NULL ) string.
         * !       Z - Plot data (contours or markers), e.g. UVI, QPF discussion.
         * !       O - Observed data.*
         */


        /** B type products */
        public static final String FLAG_B = "B";

        /** S type products */
        public static final String FLAG_S = "S";

        /** W type products */
        public static final String FLAG_W = "W";

        /** R type products */
        public static final String FLAG_R = "R";

        /** M type products */
        public static final String FLAG_M = "M";

        /** F type products */
        public static final String FLAG_F = "F";

        /** Z type products */
        public static final String FLAG_Z = "Z";

        /** O type products */
        public static final String FLAG_O = "O";


        /** type */
        String type;

        /** location file */
        String locationFile;

        /** flag */
        String flag;

        /** the data dir */
        String dataDir;

        /** file extension */
        String fileExtension;

        /**
         * Create a new TableInfo
         *
         * @param type   the type of products
         * @param locationFile  the file holding location info
         * @param flag  the flag
         * @param dataDir  the data directory
         * @param fileExtension  the file extension
         */
        public TableInfo(String type, String locationFile, String flag,
                         String dataDir, String fileExtension) {
            this.type          = type;
            this.locationFile  = locationFile;
            this.flag          = flag;
            this.dataDir       = dataDir;
            this.fileExtension = fileExtension;
        }

        /**
         * Should we use this type of data
         *
         * @return  true for the type
         */
        public boolean useStationTable() {
            return !flag.equals(FLAG_W);
        }


        /**
         * Get a String representation of the  table info
         *
         * @return a String representation of the  table info
         */
        public String toString() {
            return type + ":" + locationFile + ":" + flag + ":" + dataDir
                   + ":" + fileExtension;
        }


    }

}


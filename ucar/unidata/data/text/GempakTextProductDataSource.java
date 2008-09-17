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


import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;

import ucar.unidata.data.DirectDataChoice;


import ucar.unidata.data.FilesDataSource;

import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.idv.control.drawing.DrawingGlyph;
import ucar.unidata.idv.control.drawing.FrontGlyph;
import ucar.unidata.idv.control.drawing.HighLowGlyph;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.display.FrontDrawer;



import visad.*;

import java.io.ByteArrayInputStream;



import java.io.File;
import java.io.FileInputStream;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A class for handling text (and HTML) classes
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class GempakTextProductDataSource extends FilesDataSource {

    /** _more_          */
    private Hashtable tableMap = new Hashtable();

    /** _more_ */
    private String tablePath;

    /** _more_ */
    private String textDataPath;

    /** _more_          */
    private String gemDataPath;

    /** _more_ */
    private static final String PROP_GEMTBL = "GEMTBL";

    /** _more_ */
    private static final String PROP_TEXT_DATA = "TEXT_DATA";

    /** _more_ */
    private static final String PROP_GEMDATA = "GEMDATA";

    /** _more_ */
    private List<TableInfo> tables = new ArrayList<TableInfo>();

    /** _more_          */
    private List<ProductGroup> productGroups;

    /**
     * Default bean constructor; does nothing.
     *
     */
    public GempakTextProductDataSource() {}

    /**
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public GempakTextProductDataSource(DataSourceDescriptor descriptor,
                                       String filename,
                                       Hashtable properties) {
        this(descriptor, Misc.newList(filename), properties);
    }


    /**
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     */
    public GempakTextProductDataSource(DataSourceDescriptor descriptor,
                                       List files, Hashtable properties) {
        super(descriptor, files, "GEMPAK Text Products",
              "GEMPAK Text Products", properties);
    }


    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    public TableInfo getTable(ProductType productType) {
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
     * _more_
     *
     * @param productType _more_
     *
     * @return _more_
     */
    public String readProduct(ProductType productType) {
        TableInfo tableInfo = getTable(productType);
        if (tableInfo != null) {
            return readProduct(tableInfo);
        }
        return "Could not find text";
    }


    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    public NamedStationTable getStations(ProductType productType) {
        TableInfo tableInfo = getTable(productType);
        if (tableInfo != null) {
            return getStations(tableInfo);
        }
        return null;
    }

    /**
     * _more_
     *
     * @param tableInfo _more_
     *
     * @return _more_
     */
    public NamedStationTable getStations(TableInfo tableInfo) {
        String file = IOUtil.joinDir(tablePath,
                                     "nwx/" + tableInfo.locationFile);

        if ( !new File(file).exists()) {
            file = IOUtil.joinDir(tablePath,
                                  "stns/" + tableInfo.locationFile);
        }
        //        System.err.println (new File(file).exists() + " " + file);
        if ( !new File(file).exists()) {
            return null;
        }



        try {
            NamedStationTable table = (NamedStationTable) tableMap.get(file);
            if (table == null) {
                table = new NamedStationTable("Stations for "
                        + tableInfo.type);
                table.createStationTableFromGempak(IOUtil.readContents(file,
                        getClass()));
                tableMap.put(file, table);
            }
            return table;
        } catch (Exception exc) {
            logException("Error reading table", exc);
        }
        return null;
    }


    /**
     * _more_
     *
     * @param tableInfo _more_
     *
     * @return _more_
     */
    public String readProduct(TableInfo tableInfo) {
        String path = tableInfo.dataDir;
        path = path.replace("$TEXT_DATA", textDataPath);
        path = path.replace("$GEMDATA", gemDataPath);
        File dir = new File(path);
        File[] files =
            dir.listFiles((java.io.FileFilter) new PatternFileFilter(".*\\."
                + tableInfo.fileExtension));
        if ((files == null) || (files.length == 0)) {
            return "No text products found";
        }
        files = IOUtil.sortFilesOnAge(files, true);
        for (int i = 0; i < files.length; i++) {
            System.err.println("file:" + files[i]);
        }
        try {
            return IOUtil.readContents(files[0].toString(), getClass());
        } catch (Exception exc) {
            return "Error reading text product file:" + exc;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<ProductGroup> getProductGroups() {
        return productGroups;
    }


    /**
     * _more_
     */
    protected void initAfter() {
        tablePath    = System.getenv(PROP_GEMTBL);
        textDataPath = System.getenv(PROP_TEXT_DATA);
        gemDataPath  = System.getenv(PROP_GEMDATA);
        if ((tablePath == null) || (textDataPath == null)
                || (gemDataPath == null)) {
            setInError(true,
                       "You must have the GEMPAK environment variables "
                       + PROP_GEMTBL + " and " + PROP_TEXT_DATA + " defined");
            return;
        }
        try {
            String masterTable =
                IOUtil.readContents(IOUtil.joinDir(tablePath,
                    "nwx/master.tbl"), getClass());
            //!(12)        (12)       (1) (40)                                     (8)
            //SFC_HRLY     sfstns.tbl   O $GEMDATA/surface                         _sao.gem

            List<String[]> tableInfo = StringUtil.parseLineWords(masterTable,
                                           new int[] { 0,
                    13, 26, 28, 69 }, new int[] { 12, 12, 1, 40, 8 }, "\n",
                                      "!", true);

            for (String[] tuple : tableInfo) {
                tables.add(new TableInfo(tuple[0], tuple[1], tuple[2],
                                         tuple[3], tuple[4]));
                System.err.println(new TableInfo(tuple[0], tuple[1],
                        tuple[2], tuple[3], tuple[4]));
            }

            productGroups = ProductGroup.parse(IOUtil.joinDir(tablePath,
                    "nwx/guidata.tbl"));

        } catch (Exception exc) {
            logException("Error initializing GEMPAK products", exc);
            setInError(true, "Error initializing GEMPAK products");
        }
    }


    /**
     * _more_
     */
    protected void doMakeDataChoices() {
        String category = "textproducts";
        String docName  = getName();
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
    }



    /**
     * Class TableInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class TableInfo {

        /**
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


        public static final String TYPE_B = "B";

        /** _more_          */
        public static final String TYPE_S = "S";

        /** _more_          */
        public static final String TYPE_W = "W";

        /** _more_          */
        public static final String TYPE_R = "R";

        /** _more_          */
        public static final String TYPE_M = "M";

        /** _more_          */
        public static final String TYPE_F = "F";

        /** _more_          */
        public static final String TYPE_Z = "Z";

        /** _more_          */
        public static final String TYPE_O = "O";


        /** _more_ */
        String type;

        /** _more_ */
        String locationFile;

        /** _more_ */
        String flag;

        /** _more_ */
        String dataDir;

        /** _more_ */
        String fileExtension;

        /**
         * _more_
         *
         * @param type _more_
         * @param locationFile _more_
         * @param flag _more_
         * @param dataDir _more_
         * @param fileExtension _more_
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
         * _more_
         *
         * @return _more_
         */
        public boolean useStationTable() {
            return !type.equals(TYPE_W);
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return type + ":" + locationFile + ":" + flag + ":" + dataDir
                   + ":" + fileExtension;
        }


    }
    public static List<Product> parseProduct(String path, boolean recordType) throws Exception {
        List<Product> products = new ArrayList<Product>();
        String contents  = IOUtil.readContents(path, GempakTextProductDataSource.class);
        String prefix =(recordType? "":"");
        String suffix = (recordType?"":"");
        int idx=0;
        while(true) {
            int idx1 = contents.indexOf(prefix,idx);
            if(idx1<0) break;
            int idx2 = contents.indexOf(suffix,idx1);
            if(idx2<0) break;
            idx=idx2+1;
            String product = contents.substring(idx1+1,idx2-1);
            int lineCnt = 0;
            int startLineIdx=0;
            String stationLine=null;
            while(true) {
                int endLineIdx =product.indexOf("\n",startLineIdx);
                if(endLineIdx<0) break;
                String line = product.substring(startLineIdx,endLineIdx);
                if(line.trim().length()>0) {
                    lineCnt++;
                    if(lineCnt==2) {
                        stationLine = line;
                        break;
                    }
                }
                startLineIdx=endLineIdx+1;
            }
            if(stationLine==null) continue;
            List toks = StringUtil.split(stationLine," ",true,true);
            if(toks.size()<2) continue;
            String station = (String)toks.get(1);
            products.add(new Product(station,product));
            //            System.out.println("************");
            //            if(true) break;
        }
        return products;
    }




    public static void main(String[]args) throws Exception {
        long tt1 = System.currentTimeMillis();
        for(int i=0;i<args.length;i++) {
            long t1 = System.currentTimeMillis();
            List<Product> products = parseProduct(args[i],true);
            System.err.println(args[i]+ " " + products);
            //            System.err.println("");
            long t2 = System.currentTimeMillis();
            //            if(true) break;
            //            System.err.println ("time:" + (t2-t1));
        }
        long tt2 = System.currentTimeMillis();
        System.err.println ("total:" + args.length + " " + (tt2-tt1)+"ms");
    }



}


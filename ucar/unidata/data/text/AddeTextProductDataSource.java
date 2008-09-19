/*
 * $Id: AddeTextProductDataSource.java,v 1.15 2007/04/17 22:22:52 jeffmc Exp $
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

import java.text.SimpleDateFormat;

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



import java.util.regex.*;
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
public class AddeTextProductDataSource extends TextProductDataSource {

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
    public AddeTextProductDataSource() {}

    /**
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public AddeTextProductDataSource(DataSourceDescriptor descriptor,
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
    public AddeTextProductDataSource(DataSourceDescriptor descriptor,
                                       List files, Hashtable properties) {
        super(descriptor, files, "ADDE Text Products",
              "ADDE Text Products", properties);
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
    public List<Product> readProducts(ProductType productType,NamedStationImpl station) {
        List<Product> products = new ArrayList<Product>();
        if (station == null) return products;
        String url = "adde://adde.ucar.edu/wxtext?debug=true&num=3&"+ getSearch(productType, station);
        try {
        AddeTextReader atr      = new AddeTextReader(url);
        System.out.println("url = " + url);
        String text = atr.getText();
        products.add(new Product(station.getID(),text, new  Date()));
        } catch (Exception e) {
           System.out.println(e.getMessage());
        }
        return products;
    }

    private String getSearch(ProductType pt, NamedStationImpl station) {
        if (station == null) return "";
        StringBuilder buf = new StringBuilder("WMO=");
        buf.append(station.getProperty("BULLETIN", ""));
        buf.append("&WSTN=");
        buf.append(station.getID());
        return buf.toString();
    }

    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    public NamedStationTable getStations(ProductType productType) throws Exception {
        TableInfo tableInfo = getTable(productType);
        if (tableInfo != null) {
            return getStations(tableInfo);
        }
        return null;
    }


    private String readTableFile(String file) throws Exception {
        String contents =
                IOUtil.readContents(IOUtil.joinDir(tablePath,
                    "nwx/" + file), getClass(),null);
        if(contents == null) {
            contents =
                IOUtil.readContents(IOUtil.joinDir(tablePath,
                                                   "stns/" + file), getClass(),null);
        }
        return contents;
    }



    /**
     * _more_
     *
     * @param tableInfo _more_
     *
     * @return _more_
     */
    public NamedStationTable getStations(TableInfo tableInfo) throws Exception {
        String contents = readTableFile(tableInfo.locationFile);
        if(contents == null) return null;
        try {
            //            System.err.println (tableInfo.locationFile + " " +tableInfo.flag);
            NamedStationTable table = (NamedStationTable) tableMap.get(tableInfo.locationFile);

            if (table == null) {
                table = new NamedStationTable("Stations for "
                        + tableInfo.type);
                if(tableInfo.flag.equals(TableInfo.TYPE_B) ||
                   tableInfo.flag.equals(TableInfo.TYPE_S) ||
                   tableInfo.flag.equals(TableInfo.TYPE_W)) {
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
     * _more_
     *
     * @param tableInfo _more_
     *
     * @return _more_
     */
    public List<Product> readProducts(TableInfo tableInfo,NamedStationImpl station) {
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
        files = IOUtil.sortFilesOnAge(files, true);
        for (int i = 0; i < files.length; i++) {
            try {
                products.addAll(parseProduct(files[i].toString(),true, station));
            } catch (Exception exc) {
                //                return "Error reading text product file:" + exc;
            }
        }
        return products;
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
        //textDataPath = System.getenv(PROP_TEXT_DATA);
        //gemDataPath  = System.getenv(PROP_GEMDATA);
        //if ((tablePath == null) || (textDataPath == null)
        //        || (gemDataPath == null)) {
        if (tablePath == null) {
            setInError(true,
                       "You must have the GEMPAK environment variables "
                       + PROP_GEMTBL + " and " + PROP_TEXT_DATA + " defined");
            return;
        }
        try {
            String masterTable = readTableFile("master.tbl");
            if(masterTable==null) throw new BadDataException("Unable to read master.tbl");
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

            String gui = readTableFile("guidata.tbl");
            if(gui==null) throw new BadDataException("Unable to read guidata.tbl");
            productGroups = parseProductGroups(gui);
        } catch (Exception exc) {
            logException("Error initializing GEMPAK products", exc);
            setInError(true, "Error initializing GEMPAK products");
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public static List<ProductGroup> parseProductGroups(String contents) throws Exception {

        contents = contents.replace("{", "\n{\n");
        contents = contents.replace("}", "\n}\n");
        List<String> lines = (List<String>) StringUtil.split(contents, "\n",
                                 true, true);
        List<ProductGroup>   products     = new ArrayList<ProductGroup>();
        ProductGroup productGroup = null;
        boolean      inProduct    = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (productGroup != null) {
                if (line.equals("}")) {
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
                    productGroup.addProduct(new ProductType(toks[0].replace("_",
                            " "), toks[1]));
                }
            } else if (line.equals("{")) {
                productGroup = null;
            } else {
                productGroup = new ProductGroup(line.replace("_"," "));
                products.add(productGroup);
            }
        }
        return products;
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

    public static List<Product> parseProduct(String path, boolean recordType, NamedStationImpl station) throws Exception {
        List<Product> products = new ArrayList<Product>();
        String contents  = IOUtil.readContents(path, AddeTextProductDataSource.class);
        String prefix =(recordType? "":"");
        String suffix = (recordType?"":"");
        int idx=0;
        Date fileDate = null;
        String id = (station!=null?station.getID():null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyymmddh");
        try {
            String tmp = IOUtil.getFileTail(IOUtil.stripExtension(path));
            fileDate = sdf.parse(tmp);
            
        } catch(Exception exc) {
            System.err.println ("no file date:" + path);
        }

        //        System.err.println ("contents:" + contents);
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
            boolean seenNonBlankLine = false;
            Date date = null;


            SimpleDateFormat sdf1 = new SimpleDateFormat("hhm a z EEE MMM d yyyy");
            SimpleDateFormat sdfNoAmPm = new SimpleDateFormat("hhm z EEE MMM d yyyy");
            List lines = new ArrayList(); 
            while(true) {
                int endLineIdx =product.indexOf("\n",startLineIdx);
                if(endLineIdx<0) {
                    break;
                }
                String line = product.substring(startLineIdx,endLineIdx);
                startLineIdx=endLineIdx+1;
                lines.add(line);
                String tline = line.trim();
                if(seenNonBlankLine || tline.length()>0) {
                    seenNonBlankLine = true;
                    lineCnt++;
                    if(lineCnt==2) {
                        stationLine = line;
                        //                        break;
                    } else  if(lineCnt>2) {
                        if(tline.length()>10) { 
                            String[]toks = StringUtil.split(tline," ",2);
                            if(toks == null || toks.length!=2) continue;
                            String hhmm = toks[0];
                            try {
                                new Integer(hhmm);
                            } catch(Exception exc) {
                                continue;
                            }
                            if(hhmm.length()==3)
                                hhmm = "0" + hhmm;
                            else if(hhmm.length()==1)
                                hhmm="0" + hhmm+"00";

                            String dttm = hhmm+" " + toks[1];

                            try {
                                date = sdf1.parse(dttm);
                            } catch(Exception exc){}
                            if(date==null) {
                                try {
                                    date = sdfNoAmPm.parse(dttm);
                                } catch(Exception exc){
                                    //                                    System.err.println ("BAD:" +dttm);
                                }
                            }
                            //                            System.err.println (tline + " : " + date);
                            if(date!=null) {
                                break;
                            } 
                        }
                    }
                }
                if(lineCnt>8) {
                    break;
                }
            }
            if(stationLine==null) continue;
            if(date==null) {
                date = fileDate;
            }
            List toks = StringUtil.split(stationLine," ",true,true);
            if(toks.size()<2) continue;
            String stationString = (String)toks.get(1);
            //            System.err.println("ID:"+ id +":" + stationString+" line:" + stationLine);
            if(id==null || Misc.equals(stationString, id)) {
                products.add(new Product(stationString,product,date));
            }
            //            System.out.println("************");
            //            if(true) break;
        }
        return products;
    }


    public static void main(String[]args) throws Exception {
        /*
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH:MM a z EEE MMM d yyyy");
        String test = "3:00 AM MDT WED SEP 10 2008";
        if(args.length>0)test = args[0];
        System.err.println(sdf1.parse(test));
        if(true) return;
        */

        long tt1 = System.currentTimeMillis();
        
        for(int i=0;i<args.length;i++) {
            long t1 = System.currentTimeMillis();
            List<Product> products = parseProduct(args[i],true,null);
            //            System.err.println("");
            long t2 = System.currentTimeMillis();
            //            if(true) break;
            //            System.err.println (args[i] +" " + products.size() + " " +(t2-t1)+"ms");
            //            if(true) break;
            if((i%100)==0) System.err.println(""+i);
        }
        long tt2 = System.currentTimeMillis();
        System.err.println ("total:" + args.length + " " + (tt2-tt1)+"ms");
    }



}


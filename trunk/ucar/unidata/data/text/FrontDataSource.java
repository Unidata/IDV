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

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.display.FrontDrawer;


import visad.*;



import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;



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
public class FrontDataSource extends FilesDataSource {


    /** time format */
    private static final String TIME_FORMAT = "yyyy_MM_dd_HH_mm";


    /** Property to show the time selection window */
    public static final String PROP_TIMEWINDOW = "front.timewindow";

    /** front file value */
    private static final String TYPE_HIGHS = "HIGHS";

    /** front file value */
    private static final String TYPE_LOWS = "LOWS";

    /** front file value */
    private static final String TYPE_STNRY = "STNRY";

    /** front file value */
    private static final String TYPE_COLD = "COLD";

    /** front file value */
    private static final String TYPE_WARM = "WARM";

    /** front file value */
    private static final String TYPE_TROF = "TROF";

    /** front file value */
    private static final String TYPE_OCFNT = "OCFNT";

    /** front file value */
    private static final List TYPES = new ArrayList();


    /** The time window */
    private double timeWindow = 3.0;

    /** For setting the time window */
    private JTextField timeWindowField;

    /** When we parse the file this holds any errors */
    private List errors;

    /** When we parse the file this holds any warnings */
    private List warnings;


    /** data formatter */
    private SimpleDateFormat sdf;


    /** for parsing */
    private DateTime baseTime = null;

    /** for parsing */
    static private final GregorianCalendar calendar = new GregorianCalendar();

    /**
     * Default bean constructor; does nothing.
     *
     */
    public FrontDataSource() {}

    /**
     * Create a new FrontDataSource
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public FrontDataSource(DataSourceDescriptor descriptor, String filename,
                           Hashtable properties) {
        this(descriptor, Misc.newList(filename), properties);
    }


    /**
     * Create a new FrontDataSource
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     */
    public FrontDataSource(DataSourceDescriptor descriptor, List files,
                           Hashtable properties) {
        super(descriptor, files, (String) files.get(0), "Front data source",
              properties);
        timeWindow = getProperty(PROP_TIMEWINDOW, timeWindow);
    }


    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }

    /**
     * Get the file extension for saving to local disk
     *
     * @param file The file
     *
     * @return its extension
     */
    protected String getDataFileExtension(String file) {
        return "fcst";
    }

    /**
     * get the prefix to use when saving to local disk
     *
     * @param file file
     *
     * @return prefix
     */
    protected String getDataFilePrefix(String file) {
        return "front";
    }

    /**
     * Add to the properties component list
     *
     * @param comps List of components for the properties dialog
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        timeWindowField = new JTextField("" + timeWindow, 6);
        comps.add(GuiUtils.rLabel("Time Window:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(timeWindowField,
                new JLabel(" hours"))));
    }

    /**
     * Apply the properties
     *
     * @return successful
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        timeWindow =
            new Double(timeWindowField.getText().trim()).doubleValue();
        flushCache();
        return true;
    }

    /**
     * Make the data choices associated with this source
     */
    protected void doMakeDataChoices() {
        String category = "fronts";
        String docName  = getName();
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * p     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        try {
            String xml = parseContents();
            if (xml == null) {
                return null;
            }
            return new visad.Text(xml);
        } catch (Exception exc) {
            logException("Could not process front file contents: " + sources,
                         exc);
        }
        return null;
    }



    /**
     * Process the file name to get a file that we can use to write to local disk
     *
     * @param filename Filename
     * @param index Which file it it. This can be used by derived classes to add more info to the file name
     *
     * @return The processed filename
     */
    protected String processDataFilename(String filename, int index) {
        return convertFilename(filename);
    }


    /**
     * This converts the possibly templateized filename, replacing the %DAY- macro with todays date
     *
     * @param filename filename
     *
     * @return converted filename
     */
    private static String convertFilename(String filename) {
        int index = filename.indexOf("%DAY-");
        if (index >= 0) {
            String            tok = filename.substring(index + 1);
            GregorianCalendar now = new GregorianCalendar();
            now.setTime(new Date());
            int year   = now.get(java.util.Calendar.YEAR);
            int index2 = tok.indexOf("%");
            tok = tok.substring(0, index2);
            String deltas = tok.substring(4, index2);
            int    delta  = new Integer(deltas).intValue();
            now.add(java.util.Calendar.DAY_OF_YEAR, -delta);
            String days = "" + now.get(java.util.Calendar.DAY_OF_YEAR);
            days = StringUtil.padLeft(days, 3, "0");
            String yearDay = now.get(java.util.Calendar.YEAR) + "" + days;
            filename = StringUtil.replace(filename, "%DAY-" + delta + "%",
                                          yearDay);
            //            System.err.println("file:" + filename);
        }
        return filename;
    }



    /**
     * Parse the file
     *
     * @return  The xgrf xml that defines the shapes
     *
     * @throws Exception On badness
     */
    private String parseContents() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<shapes "
                  + XmlUtil.attrs("title", "Fronts",
                                  DrawingControl.ATTR_USETIMESINANIMATION,
                                  "true", DrawingControl.ATTR_FRONTDISPLAY,
                                  "true") + ">\n");

        errors   = new ArrayList();
        warnings = new ArrayList();
        List    contentsList = new ArrayList();
        boolean anyOk        = false;
        for (int i = 0; i < sources.size(); i++) {
            String filename = (String) sources.get(i);
            filename = convertFilename(filename);
            LogUtil.message("Reading front data: " + filename);
            String contents = getContents(filename);
            if (contents != null) {
                try {
                    parseContents(sb, contents);
                    anyOk = true;
                    //              System.err.println(contents);
                } catch (BadDataException bde) {
                    errors.add(bde.getMessage());
                }
            }
        }
        LogUtil.message("");


        if ( !anyOk) {
            errors.addAll(warnings);
        }

        if (errors.size() > 0) {
            LogUtil.userErrorMessage(
                "There were errors processing the front file:\n"
                + StringUtil.join("\n", errors));
        }
        if (anyOk && (warnings.size() > 0)) {
            System.err.println(
                "There were warnings processing the front file:\n"
                + StringUtil.join("\n", warnings));
        }
        //      System.err.println (sb);

        if ( !anyOk) {
            return null;
        }

        sb.append("</shapes>\n");
        return sb.toString();
    }

    /**
     * Get the list of input streams used to make data local
     *
     * @param processedSources Source paths
     *
     * @return List of input streams
     *
     * @throws Exception On badness
     */
    protected List getInputStreams(List processedSources) throws Exception {
        List is = new ArrayList();
        for (int i = 0; i < processedSources.size(); i++) {
            is.add(new ByteArrayInputStream(
                getContents(processedSources.get(i).toString()).getBytes()));
        }
        return is;
    }


    /**
     * Read the contents of the url or file. If its an adde url handle it with the AddeTextReader
     *
     * @param filename url or file
     *
     * @return The contents
     *
     * @throws Exception On badness
     */
    private String getContents(String filename) throws Exception {
        if (filename.startsWith("adde:")) {
            AddeTextReader reader = new AddeTextReader(filename);
            if ( !reader.getStatus().equals("OK")) {
                errors.add("Unable to read adde url: " + filename);
            }
            return reader.getText();
        }
        return IOUtil.readContents(filename);
    }



    /**
     * Parse the forecast contents, the c parameter (c for contents)
     *
     * @param sb  Holds the xgrf xml
     * @param c The contents
     *
     * @throws Exception On badness
     */
    private void parseContents(StringBuffer sb, String c) throws Exception {
        List       lines = StringUtil.split(c, "\n", true, true);
        int        idx   = 0;
        ValidBlock block = null;
        for (; idx < lines.size(); idx++) {
            String line = (String) lines.get(idx);
            if (block == null) {
                //Look for base time
                if ((line.indexOf(" PM ") >= 0)
                        || (line.indexOf(" AM ") >= 0)) {
                    List   timeToks = StringUtil.split(line, " ", true, true);
                    String ts       = StringUtil.join(" ", timeToks);
                    //529 PM EST WED DEC 27 2006                                                       
                    if (timeToks.size() == 7) {
                        try {
                            baseTime = DateTime.createDateTime(ts,
                                    "hmm a z EEE MMM dd yyyy");
                        } catch (Exception exc) {
                            errors.add("Could not parse base time:" + ts);
                        }
                    } else {
                        errors.add("Could not parse base time:" + ts);
                    }
                }
            }

            if (line.indexOf("$$") >= 0) {
                if (block != null) {
                    processBlock(block, sb);
                }
                baseTime = null;
                block    = null;
                continue;
            }

            if (line.indexOf("VALID") >= 0) {
                if (block != null) {
                    processBlock(block, sb);
                }
                block = new ValidBlock(line);
            } else if (block != null) {
                List toks = StringUtil.split(line, " ", true, true);
                toks.remove("$$");
                block.toks.addAll(toks);
            }
        }
        if (block != null) {
            processBlock(block, sb);
        }
    }

    /**
     * process the given block
     *
     * @param block The block
     * @param sb Holds xgrf xml
     *
     * @throws Exception On badness
     */
    private void processBlock(ValidBlock block, StringBuffer sb)
            throws Exception {

        if (TYPES.size() == 0) {
            TYPES.addAll(Misc.newList(TYPE_HIGHS, TYPE_LOWS, TYPE_STNRY,
                                      TYPE_COLD));
            TYPES.addAll(Misc.newList(TYPE_WARM, TYPE_TROF, TYPE_OCFNT));
        }


        Date validTime = null;

        List validToks = StringUtil.split(block.validLine, " ", true, true);
        if ((validToks.size() == 2) || (validToks.size() == 4)) {
            //  VALID 122721Z    OR  12HR PROG VALID 280600Z                                                          
            String format;
            String ts;
            if (validToks.size() == 2) {
                format = "yyyyMMddHH";
                ts     = (String) validToks.get(1);
            } else {
                ts     = (String) validToks.get(3);
                format = "yyyyMMddHHmm";
            }
            //      System.err.println ("ts:" + ts);
            if (ts.endsWith("Z")) {
                ts = ts.substring(0, ts.length() - 1);
            }
            try {
                if ((ts.length() <= 6) && (baseTime != null)) {
                    calendar.setTime(new Date(1000
                            * (long) baseTime.getValue()));
                    if (validToks.size() == 2) {
                        ts = calendar.get(java.util.Calendar.YEAR) + ts;
                    } else {
                        String month =
                            "" + (calendar.get(java.util.Calendar.MONTH) + 1);
                        if (month.length() == 1) {
                            month = "0" + month;
                        }
                        ts = calendar.get(java.util.Calendar.YEAR) + month
                             + ts;
                    }
                }
                DateTime dttm = DateTime.createDateTime(ts, format);
                validTime = new Date(1000 * (long) dttm.getValue());
                //              System.err.println("Time:" +validTime);
            } catch (Exception exc) {
                errors.add("Could not parse time:" + ts);
            }
        } else {
            errors.add("Could not parse time:" + block.validLine);
        }


        List   toks        = block.toks;
        String currentType = null;
        List   subToks     = null;
        for (int i = 0; i < toks.size(); i++) {
            String tok = (String) toks.get(i);
            if (TYPES.contains(tok)) {
                if ((currentType != null) && (subToks.size() > 0)) {
                    try {
                        processType(currentType, subToks, sb, validTime);
                    } catch (BadDataException bde) {
                        errors.add(bde.getMessage());
                    }
                }
                currentType = tok;
                subToks     = new ArrayList();
            } else if ((subToks != null) && (currentType != null)) {
                subToks.add(tok);
            } else {
                throw new BadDataException("Unknown token block:" + toks);
            }
        }
        if ((currentType != null) && (subToks.size() > 0)) {
            try {
                processType(currentType, subToks, sb, validTime);
            } catch (BadDataException bde) {
                errors.add(bde.getMessage());
            }
        }
    }

    /**
     * Convert the list of tokens to a double array
     *
     * @param toks tokens
     *
     * @return double arrray
     */
    private double[] parse(List toks) {
        toks.remove("$$");
        return Misc.parseDoubles(StringUtil.join(",", toks), ",");
    }

    /**
     * parse lat/lon
     *
     * @param s string
     *
     * @return lat/lon
     */
    double[] getLatLon(String s) {
        int    splitIndex = 2;
        double divisor    = 1.;
        if (s.length() > 5) {  // new format
            splitIndex = 3;
            divisor    = 10.;
        }
        double lat = new Double(s.substring(0, splitIndex)).doubleValue()
                     / divisor;
        double lon = -new Double(s.substring(splitIndex)).doubleValue()
                     / divisor;
        return new double[] { lat, lon };
    }

    /**
     * More parsing
     *
     * @param type Shape type
     * @param toks tokens
     * @param sb _holds xgrf
     * @param validTime time
     *
     * @throws Exception On badness
     */
    private void processType(String type, List toks, StringBuffer sb,
                             Date validTime)
            throws Exception {

        //        sb.append("\tTYPE:" + type +" toks: " + toks+"\n");
        StringBuffer dttms = new StringBuffer();


        if (validTime != null) {
            if (sdf == null) {
                sdf = new SimpleDateFormat();
                sdf.applyPattern(TIME_FORMAT);
                sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
            }


            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(validTime);
            dttms.append(" " + DrawingGlyph.ATTR_TIMEFORMAT + "=\""
                         + TIME_FORMAT + "\" ");

            dttms.append(" " + DrawingGlyph.ATTR_TIMES + "=\"");

            int delta = (int) (60 * timeWindow / 2);

            cal.add(java.util.Calendar.MINUTE, -delta);
            dttms.append(sdf.format(cal.getTime()));
            dttms.append(",");


            cal.add(java.util.Calendar.MINUTE, 2 * delta);
            dttms.append(sdf.format(cal.getTime()));

            dttms.append("\" ");
        }

        if (type.equals(TYPE_HIGHS) || type.equals(TYPE_LOWS)) {
            String  color = (type.equals(TYPE_HIGHS)
                             ? "0,0,255"
                             : "255,0,0");
            boolean high  = type.equals(TYPE_HIGHS);

            for (int i = 0; i < toks.size(); i++) {
                //Make sure we have a location token
                if (i == toks.size() - 1) {
                    warnings.add("Missing location value in " + type
                                 + " list: " + StringUtil.join(" ", toks));
                    continue;
                }
                String pressure = (String) toks.get(i);
                try {
                    double pd = new Double(pressure).doubleValue();
                    //Check for bad values
                    if ((pd < 800) || (pd > 1100)) {
                        warnings.add("Bad pressure data: " + pressure
                                     + " in " + type + " list: "
                                     + StringUtil.join(" ", toks));
                        continue;
                    }
                } catch (NumberFormatException nfe) {
                    errors.add("Bad pressure value: " + pressure + " in "
                               + type + " list: "
                               + StringUtil.join(" ", toks));
                    continue;
                }

                i++;

                //Now check the location
                String pos = (String) toks.get(i);
                if ((pos.length() < 4) || (pos.length() > 7)) {
                    warnings.add("Bad location value: " + pos + " in " + type
                                 + " list: " + StringUtil.join(" ", toks));
                    continue;
                }
                double
                    lat = 0,
                    lon = 0;
                try {
                    double[] ll = getLatLon(pos);
                    lat = ll[0];
                    if (Math.abs(lat) > 90) {
                        warnings.add("Bad latitude value: " + pos + " in "
                                     + type + " list: "
                                     + StringUtil.join(" ", toks));
                        continue;
                    }
                    lon = ll[1];
                    if (Math.abs(lon) > 180) {
                        warnings.add("Bad longitude value: " + pos + " in "
                                     + type + " list: "
                                     + StringUtil.join(" ", toks));
                        continue;
                    }
                } catch (Exception exc) {
                    warnings.add("Bad location value: " + pos + " in " + type
                                 + " list: " + StringUtil.join(" ", toks));
                    continue;
                }
                sb.append("<" + (high
                                 ? DrawingGlyph.TAG_HIGH
                                 : DrawingGlyph.TAG_LOW) + " "
                                 + dttms.toString());
                sb.append(XmlUtil.attr(DrawingGlyph.ATTR_COORDTYPE,
                                       "LATLON"));

                sb.append(XmlUtil.attr(HighLowGlyph.ATTR_PRESSURE, pressure));
                sb.append(XmlUtil.attr(DrawingGlyph.ATTR_POINTS,
                                       lat + "," + lon));
                sb.append("/>\n");
            }
        } else {
            StringBuffer attrs = new StringBuffer();
            attrs.append(XmlUtil.attr(DrawingGlyph.ATTR_COORDTYPE, "LATLON"));
            StringBuffer points  = new StringBuffer();
            int          cnt     = 0;
            String       subType = null;
            for (int i = 0; i < toks.size(); i++) {
                String tok  = (String) toks.get(i);
                int    size = tok.length();
                //Now check the location and possible split line
                if (size < 4) {
                    if (i < toks.size() - 1) {
                        String newTok  = (String) toks.get(i + 1);
                        int    newSize = size + newTok.length();
                        if ((newSize >= 4) && (newSize < 8)) {  // have split bulletin
                            tok = tok + newTok;
                            i++;
                        }
                    }
                }
                try {
                    double[] ll = getLatLon(tok);
                    if (cnt > 0) {
                        points.append(",");
                    }
                    points.append(ll[0]);
                    points.append(",");
                    points.append(ll[1]);
                    cnt++;
                } catch (NumberFormatException nfe) {
                    if (subType == null) {
                        subType = tok;
                    } else {
                        throw new BadDataException("Bad front state: " + tok
                                + " " + toks);
                    }
                }
            }
            attrs.append(XmlUtil.attr(DrawingGlyph.ATTR_POINTS,
                                      points.toString()));
            String frontType = getFrontType(type, subType);
            attrs.append(XmlUtil.attr(FrontGlyph.ATTR_FRONTTYPE, frontType));

            sb.append("<" + DrawingGlyph.TAG_FRONT + " " + dttms.toString());
            sb.append(attrs);
            sb.append("/>\n");
        }

    }

    /**
     * Maps the type name to the FrontDrawer type
     *
     * @param type  type
     * @param subType  sub type
     *
     * @return Our internal type name
     */
    private String getFrontType(String type, String subType) {
        if (type.equals(TYPE_COLD)) {
            return FrontDrawer.TYPE_COLD_FRONT;
        }
        if (type.equals(TYPE_WARM)) {
            return FrontDrawer.TYPE_WARM_FRONT;
        }
        if (type.equals(TYPE_OCFNT)) {
            return FrontDrawer.TYPE_OCCLUDED_FRONT;
        }
        if (type.equals(TYPE_TROF)) {
            return FrontDrawer.TYPE_TROUGH;
        }
        if (type.equals(TYPE_STNRY)) {
            return FrontDrawer.TYPE_STATIONARY_FRONT;
        }
        throw new BadDataException("Unknown front type:" + type);
    }


    /**
     *  Set the TimeWindow property.
     *
     *  @param value The new value for TimeWindow
     */
    public void setTimeWindow(double value) {
        timeWindow = value;
    }

    /**
     *  Get the TimeWindow property.
     *
     *  @return The TimeWindow
     */
    public double getTimeWindow() {
        return timeWindow;
    }




    /**
     * Class ValidBlock holds a block of tokens
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.15 $
     */
    private static class ValidBlock {

        /** The line from the file */
        String validLine;

        /** tokens */
        List toks = new ArrayList();

        /**
         * ctor
         *
         * @param line the input line
         */
        ValidBlock(String line) {
            validLine = line;
        }
    }

    /**
     * test main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20; i++) {
            convertFilename("foo%DAY-" + i + "%");
        }
    }
}

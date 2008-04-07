/**
 * $Id: TextPointDataSource.java,v 1.22 2007/08/16 12:00:50 jeffmc Exp $
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


package ucar.unidata.data.point;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.unidata.data.*;


import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.ui.GraphPaperLayout;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.*;

import visad.data.text.TextAdapter;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.EarthLocationTuple;

import java.awt.*;
import java.awt.event.*;

import java.io.*;


import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A data source for point data from a Text file
 *
 * @author IDV Development Team
 * @version $Revision: 1.22 $ $Date: 2007/08/16 12:00:50 $
 */
public class TextPointDataSource extends PointDataSource {

    /** Where to write out the saved meta data listing */
    public static final String PREF_METADATAMAP =
        "pref.textpointdatasource.metadatamap.xml";

    /** The visad textadapter map. We have this here if the data file does not have it */
    private String map;

    /** skip rows */
    private int skipRows = 0;

    /** metadata okay flag */
    private boolean metaDataOk = false;


    /** The visad textadapter map params line. We have this here if the data file does not have it */
    private String params;

    private Real      dfltReal;

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(TextPointDataSource.class.getName());

    /** variables for time */
    private String[] timeVars = {
        "time_nominal", "time_Nominal", "timeNominal", "timeObs",
        "reportTime", "time", "nominal_time", "Time"
    };



    /** variables for index */
    private String[] recNumVars = { "recNum", "recnum", "index", "Index" };


    /** Holds lists of text fields for the metadata gui */
    List metaDataFields = new ArrayList();


    /** for the metadata gui */
    List paramRows = new ArrayList();

    /** for the metadata gui */
    List extraParamRows = new ArrayList();

    /** for the metadata gui */
    JComponent metaDataComp;

    /** metadata properties wrapper */
    JComponent metaDataPropertiesWrapper;

    /** widget panel */
    JComponent widgetPanel;


    /** apply names button */
    private JButton applyNamesBtn;

    /** group var name */
    private String groupVarName = null;

    /**
     * Default constructor
     *
     * @throws VisADException  problem creating the object
     */
    public TextPointDataSource() throws VisADException {
        init();
    }

    /**
     * Create a new TextPointDataSource
     *
     * @param descriptor    data source descriptor
     * @param source        source of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public TextPointDataSource(DataSourceDescriptor descriptor,
                               String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, "Text Point Data", properties);
    }




    /**
     * Make PointObs from the raw VisAD data
     *
     * @param dataChoice   choice for data (source of data)
     * @param subset       subsetting parameters
     * @param bbox The area subset. May be null.
     * @return  data of the form index -> (EarthLocation, Time, value_tuple)
     *
     * @throws Exception  problem creating data
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {
        //        System.err.println("MAKE OBS");
        return makeObs(dataChoice, subset, bbox, null, false, true);
    }

    /**
     * _more_
     *
     * @param dataChoice _more_
     *
     * @return _more_
     */
    protected String getSource(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        if ((id instanceof String) && (id.toString().startsWith("track:"))) {
            return (String) sources.get(0);
        }
        return super.getSource(dataChoice);
    }

    /**
     * _more_
     *
     * @param contents _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private InputStream getInputStream(String contents) throws Exception {
        InputStream is = new ByteArrayInputStream(contents.getBytes());
        for (int i = 0; i < skipRows; i++) {
            while (is.available() > 0) {
                int c = is.read();
                if (c == '\n') {
                    break;
                }
                if (c == '\r') {
                    if (is.available() > 0) {
                        is.mark(10);
                        c = is.read();
                        if (c != '\n') {
                            is.reset();
                        }
                    }
                    break;
                }
            }
        }
        return is;

    }


    /**
     * _more_
     *
     * @param dataChoice _more_
     * @param subset _more_
     * @param bbox _more_
     * @param trackParam _more_
     * @param sampleIt _more_
     * @param showAttributeGuiIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox, String trackParam,
                                boolean sampleIt,
                                boolean showAttributeGuiIfNeeded)
            throws Exception {
        String source = getSource(dataChoice);
        String contents;
        String delimiter;
        if (source.endsWith(".xls")) {
            contents  = DataUtil.xlsToCsv(source);
            delimiter = ",";
        } else {
            contents  = IOUtil.readContents(source, getClass());
            delimiter = TextAdapter.getDelimiter(source);
        }

        FieldImpl obs = null;
        //        FieldImpl obs = (FieldImpl) getCache (source);
        if (obs == null) {
            TextAdapter ta = null;
            try {
                ta = new TextAdapter(getInputStream(contents), delimiter,
                                     map, params, sampleIt);
            } catch (visad.data.BadFormException bfe) {
                //Probably don't have the header info
                //If we already have a map and params then we have problems
                if ((map != null) && (params != null)) {
                    throw bfe;
                }
                if ( !showAttributeGuiIfNeeded) {
                    return null;
                }
                if ( !showAttributeGui(contents)) {
                    return null;
                }
                ta = new TextAdapter(getInputStream(contents), delimiter,
                                     map, params, sampleIt);
            }
            try {
                Data d = ta.getData();
                obs        = makePointObs(d, trackParam);
                metaDataOk = true;
            } catch (Exception exc) {
                map    = null;
                params = null;
                throw exc;
            }
            //putCache (source, obs);
        }
        return obs;
    }

    /**
     * _more_
     *
     * @param source _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Data test(String source) throws Exception {
        String      contents  = IOUtil.readContents(source, getClass());
        String      delimiter = TextAdapter.getDelimiter(source);
        TextAdapter ta        = null;
        //        long m1 = Misc.gc();

        long t1 = System.currentTimeMillis();
        try {
            ta = new TextAdapter(
                new ByteArrayInputStream(contents.getBytes()), delimiter,
                map, params, false);
        } catch (visad.data.BadFormException bfe) {
            throw bfe;
        }

        Data data = ta.getData();
        //            long m2 =             Misc.gc();
        //            System.err.println ("data mem:" + (m2-m1));
        for (int i = 0; i < 10; i++) {
            long t2 = System.currentTimeMillis();
            Data d  = makePointObs(data, "magnitude");
            long t3 = System.currentTimeMillis();
            System.err.println("time:" + (t3 - t2));
            if (true) {
                break;
            }
        }

        //            System.err.println ("time:" + (t2-t1) + " " + (t3-t2));




        return null;

    }



    /**
     * Show the metadata gui
     *
     * @throws IOException On badness
     */
    public void changeMetadata() throws IOException {
        try {
            if (showAttributeGui(null)) {
                Misc.run(this, "reloadData");
            }
        } catch (Exception exc) {
            logException("Setting metadata", exc);
        }
    }


    /**
     * Get the delimiter used in the text file
     *
     * @return delimiter
     */
    private String getDelimiter() {
        String delim = TextAdapter.getDelimiter(getFilePath());
        return (delim == null)
               ? ","
               : delim;
    }



    /**
     * _more_
     *
     * @param lbl _more_
     * @param index _more_
     * @param lines _more_
     */
    private void setLineText(JLabel lbl, int index, List lines) {
        //        if(true) {
        //            lbl.setText("HELLO THERE");
        //            return;
        //        }
        StringBuffer sb =
            new StringBuffer(
                "<html><body style=\"margin:0;\"><table width=\"100%\" border=\"0\">");
        int[] indices;
        if (index == 0) {
            indices = new int[] { index, index + 1, index + 2 };
        } else {
            indices = new int[] { index - 1, index, index + 1 };
        }
        for (int i = 0; i < indices.length; i++) {
            String line;
            if ((indices[i] < 0) || (indices[i] >= lines.size())) {
                line = "&nbsp;";
            } else {
                line = (String) lines.get(indices[i]);
            }
            sb.append("<tr valign=top><td width=5>");
            if (indices[i] == index) {
                sb.append("<b>&gt;</b></td><td><b><u>" + line
                          + "</u></b></td></tr>");
            } else {
                sb.append("</td><td>" + line + "</td></tr>");
            }
        }
        sb.append("</table>");
        sb.append("</body></html>");
        lbl.setText(sb.toString());


        String line  = (String) lines.get(index);
        List   toks  = StringUtil.split(line, getDelimiter(), false, false);
        List   comps = new ArrayList();
        /*        comps.add(
                  new GraphPaperLayout.Location(
                                                new JLabel("Sample Value"), 0, 0));
        comps.add(new GraphPaperLayout.Location(new JLabel("Name"), 1,
                                                0));
        comps.add(
                  new GraphPaperLayout.Location(
                                                new JLabel("Unit/Date Format"), 2, 0));
        comps.add(
                  new GraphPaperLayout.Location(
                                                new JLabel("Missing Value"), 3, 0));
        comps.add(
                  new GraphPaperLayout.Location(
                                                new JLabel("Extra (e.g., colspan)"), 4, 0));

*/

        comps.add(GuiUtils.leftRight(new JLabel("Value"), applyNamesBtn));
        comps.add(new JLabel("Name"));
        comps.add(new JLabel("Unit/Date Format"));
        comps.add(new JLabel("Missing Value"));
        comps.add(new JLabel("Extra (e.g., colspan)"));




        for (int i = 0; i < paramRows.size(); i++) {
            ParamRow paramRow = (ParamRow) paramRows.get(i);
            extraParamRows.add(0 + i, paramRow);
        }

        paramRows = new ArrayList();
        for (int tokIdx = 0; tokIdx < toks.size(); tokIdx++) {
            ParamRow paramRow;
            if (extraParamRows.size() > 0) {
                paramRow = (ParamRow) extraParamRows.remove(0);
            } else {
                paramRow = new ParamRow();
            }
            paramRow.init(tokIdx, toks, comps);
            paramRows.add(paramRow);
        }


        applySavedMetaData(metaDataFields);
        GuiUtils.tmpInsets = new Insets(5, 5, 0, 0);
        double[]   stretch = { 0.0, 1.0, 0.5, 0.0, 0.5 };
        JComponent panel = GuiUtils.doLayout(comps, 5, stretch,
                                             GuiUtils.WT_N);
        widgetPanel.removeAll();
        JScrollPane widgetSP =
            GuiUtils.makeScrollPane(GuiUtils.top(GuiUtils.inset(panel, 5)),
                                    200, 200);
        widgetPanel.add(BorderLayout.CENTER, widgetSP);
    }

    /**
     * _more_
     *
     * @param line _more_
     */
    public void applyNames(String line) {
        List toks = StringUtil.split(line, getDelimiter(), false, false);
        for (int i = 0; (i < paramRows.size()) && (i < toks.size()); i++) {
            ParamRow paramRow = (ParamRow) paramRows.get(i);
            paramRow.setName((String) toks.get(i));
        }
    }



    /**
     * Make, if needed, and return the gui metadata component
     *
     * @param contents The contents of the point file
     *
     * @return The component
     *
     * @throws IOException On badness
     */
    private JComponent getMetaDataComponent(String contents)
            throws IOException {
        if (metaDataComp == null) {
            if (contents == null) {
                contents = IOUtil.readContents(getFilePath(), getClass());
            }
            final BufferedReader bis = new BufferedReader(
                                           new InputStreamReader(
                                               new ByteArrayInputStream(
                                                   contents.getBytes())));
            final List lines = new ArrayList();
            for (int i = 0; i < 100; i++) {
                String line = bis.readLine();
                if (line == null) {
                    break;
                }
                List toks = StringUtil.split(line, getDelimiter(), false,
                                             false);
                lines.add(line);
            }


            final JLabel lineLbl = new JLabel(" ");
            lineLbl.setVerticalAlignment(SwingConstants.TOP);
            lineLbl.setPreferredSize(new Dimension(700, 150));
            applyNamesBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/HorArrow16.gif",
                                        getClass());
            applyNamesBtn.setToolTipText(
                "Use column values as the field names");
            applyNamesBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    applyNames(lines.get(skipRows).toString());
                }
            });


            JButton nextBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/Down.gif",
                                        getClass());
            JButton prevBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/Up.gif",
                                        getClass());
            nextBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    skipRows++;
                    if (skipRows >= lines.size()) {
                        skipRows = lines.size() - 1;
                    }
                    setLineText(lineLbl, skipRows, lines);
                }
            });
            prevBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    skipRows--;
                    if (skipRows < 0) {
                        skipRows = 0;
                    }
                    setLineText(lineLbl, skipRows, lines);
                }
            });
            JComponent buttons = GuiUtils.vbox(prevBtn, nextBtn);
            JComponent skipInner = GuiUtils.leftCenter(GuiUtils.top(buttons),
                                       lineLbl);


            JComponent skipContents =
                GuiUtils.topCenter(new JLabel("Start line:"), skipInner);


            widgetPanel = new JPanel(new BorderLayout());
            JLabel lbl =
                new JLabel(
                    "Enter the field names and units. Leave name field blank to skip the field    ");

            JComponent wrapper = new JPanel(new BorderLayout());
            JButton saveBtn = GuiUtils.makeButton("Preferences", this,
                                  "popupMetaDataMenu", wrapper);
            wrapper.add(BorderLayout.CENTER, saveBtn);

            metaDataComp = GuiUtils.topCenter(
                GuiUtils.vbox(
                    skipContents, GuiUtils.leftRight(
                        lbl, GuiUtils.right(wrapper))), widgetPanel);
            metaDataComp = GuiUtils.inset(metaDataComp, 5);
            setLineText(lineLbl, skipRows, lines);

        }
        return metaDataComp;

    }


    /**
     * Get the persistent mapping of named preference to list of values
     *
     * @return preference mapping
     */
    private Hashtable getMetaDataMap() {
        Hashtable pointMetaDataMap =
            (Hashtable) getDataContext().getIdv().getStore().getEncodedFile(
                PREF_METADATAMAP);
        if (pointMetaDataMap == null) {
            pointMetaDataMap = new Hashtable();
        }
        return pointMetaDataMap;
    }


    /**
     * Remove the given key from the saved mapping. Write out the file
     *
     * @param key key
     */
    public void deleteMetaData(String key) {
        Hashtable pointMetaDataMap = getMetaDataMap();
        pointMetaDataMap.remove(key);
        getDataContext().getIdv().getStore().putEncodedFile(PREF_METADATAMAP,
                pointMetaDataMap);
    }


    /**
     * Show the metadata preference menu
     *
     * @param near The component to show the menu near
     */
    public void popupMetaDataMenu(JComponent near) {

        List items = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Save Currrent", this,
                                        "saveMetaDataMap"));
        Hashtable pointMetaDataMap = getMetaDataMap();
        if (pointMetaDataMap.size() > 0) {
            List delitems = new ArrayList();
            items.add(GuiUtils.MENU_SEPARATOR);
            for (Enumeration keys = pointMetaDataMap.keys();
                    keys.hasMoreElements(); ) {
                String     key  = (String) keys.nextElement();
                final List list = (List) pointMetaDataMap.get(key);
                items.add(GuiUtils.makeMenuItem(key, this,
                        "applySavedMetaData", list));
                delitems.add(GuiUtils.makeMenuItem(key, this,
                        "deleteMetaData", key));
            }
            items.add(GuiUtils.makeMenu("Delete", delitems));
        }
        GuiUtils.showPopupMenu(items, near);
    }

    /**
     * Save the meta data
     */
    public void saveMetaDataMap() {
        Hashtable pointMetaDataMap = getMetaDataMap();
        Vector    items            = new Vector();
        items.add("");
        for (Enumeration keys = pointMetaDataMap.keys();
                keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            items.add(key);
        }
        JComboBox box = new JComboBox(items);
        box.setEditable(true);
        if ( !GuiUtils.showOkCancelDialog(
                null, "Saved Meta Data",
                GuiUtils.inset(GuiUtils.hbox(new JLabel("Name: "), box), 5),
                null)) {
            return;
        }
        String prefname       = box.getSelectedItem().toString().trim();
        List   metaDataFields = new ArrayList();
        String delimiter      = getDelimiter();
        for (int i = 0; i < paramRows.size(); i++) {
            ParamRow paramRow = (ParamRow) paramRows.get(i);
            paramRow.addToMetaData(metaDataFields);
        }
        pointMetaDataMap.put(prefname, metaDataFields);
        getDataContext().getIdv().getStore().putEncodedFile(PREF_METADATAMAP,
                pointMetaDataMap);
    }


    /**
     * Show gui
     *
     * @param contents text point file contents
     *
     * @return ok
     *
     * @throws IOException On badness
     */
    private boolean showAttributeGui(String contents) throws IOException {
        //        Misc.printStack("meta");


        JComponent metaDataComp = getMetaDataComponent(contents);

        boolean ok = GuiUtils.showOkCancelDialog(null, "Point Data",
                         metaDataComp, null);
        if (metaDataPropertiesWrapper != null) {
            metaDataPropertiesWrapper.add(BorderLayout.CENTER, metaDataComp);
        }

        if ( !ok) {
            return false;
        }
        applyMetaDataFields();
        return true;
    }




    /**
     * apply properties
     *
     * @return ok
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        if (map != null) {
            applyMetaDataFields();
            flushCache();
        }
        return true;
    }

    /**
     * add to properties tab
     *
     * @param tabbedPane tab
     */
    public void addPropertiesTabs(JTabbedPane tabbedPane) {
        super.addPropertiesTabs(tabbedPane);
        if ( !metaDataOk && (map == null)) {
            try {
                DataChoice dataChoice = (DataChoice) getDataChoices().get(0);
                Data sample = makeObs(dataChoice, null, null, null, true,
                                      false);
            } catch (Exception exc) {}
        }

        if ( !metaDataOk || (map != null)) {
            try {
                JComponent comp = getMetaDataComponent(null);
                metaDataPropertiesWrapper = GuiUtils.center(comp);
                tabbedPane.add("Point Meta-data", metaDataPropertiesWrapper);
            } catch (IOException exc) {
                logException("Creating metadata properties", exc);
            }
        }
    }


    /**
     * Apply properties
     */
    private void applyMetaDataFields() {
        map    = "(index)->(";
        params = "";
        int cnt = 0;
        metaDataFields = new ArrayList();
        String delimiter = getDelimiter();
        int    skip      = 0;
        for (int i = 0; i < paramRows.size(); i++) {
            ParamRow paramRow = (ParamRow) paramRows.get(i);
            String   unit     = paramRow.getUnit();
            String   name     = paramRow.getCleanName();
            String   missing  = paramRow.getMissing();
            String   extra    = paramRow.getExtra();
            List     fields   = new ArrayList();
            if (name.length() > 0) {
                if (unit.length() == 0) {
                    unit = "Text";
                }
            }
            paramRow.addToMetaData(metaDataFields);

            if (skip > 0) {
                skip--;
                continue;
            }


            if (name.length() > 0) {
                if (unit.equals("Text")) {
                    name = name + "(Text)";
                    unit = "";
                }
                if (cnt > 0) {
                    map = map + delimiter;
                }
                map = map + name;
                cnt++;
            } else {
                name = "skip";
            }
            if (unit.equals("Text")) {
                unit = "";
            }
            if (params.length() > 0) {
                params = params + delimiter;
            }
            params = params + name + "[";
            if (name.equals("Time")) {
                if (unit.trim().length() > 0) {
                    params = params + " fmt=\"" + unit + "\" ";
                }

            } else {
                if (unit.trim().length() > 0) {
                    params = params + " unit=\"" + unit + "\" ";
                }
            }
            if (missing.length() > 0) {
                params = params + " missing=\"" + missing + "\" ";
            }
            if (extra.length() > 0) {
                params = params + " " + extra;
                String colspan = StringUtil.findPattern(extra,
                                     "colspan *= *\"([^\"]+)\"");
                if (colspan != null) {
                    skip = new Integer(colspan).intValue() - 1;
                }
            }
            params = params + "]";
        }
        map = map + ")";
        //        System.out.println (map);
        //        System.out.println (params);

    }



    /**
     * Init the widgets
     *
     * @param fieldList widgets
     */
    public void applySavedMetaData(List fieldList) {
        for (int tokIdx = 0;
                (tokIdx < paramRows.size()) && (tokIdx < fieldList.size());
                tokIdx++) {
            ParamRow paramRow = (ParamRow) paramRows.get(tokIdx);
            paramRow.applyMetaData((List) fieldList.get(tokIdx));
        }
    }

    /**
     * Check to see if this TextPointDataSource is equal to the object
     * in question.
     *
     * @param o  object in question
     *
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof TextPointDataSource)) {
            return false;
        }
        TextPointDataSource that = (TextPointDataSource) o;
        return (this == that);
    }

    /**
     * Get the hashcode for this object
     * @return  hash code
     */
    public int hashCode() {
        int hashCode = getName().hashCode();
        return hashCode;
    }

    /** _more_ */
    List varNames = new ArrayList();

    /**
     * _more_
     *
     * @param trackParamIndex _more_
     * @param latIndex _more_
     * @param lonIndex _more_
     * @param altIndex _more_
     * @param times _more_
     * @param tuples _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private FieldImpl makeTrack(int trackParamIndex, int latIndex,
                                int lonIndex, int altIndex, List times,
                                List tuples)
            throws VisADException, RemoteException {
        float[] lats = new float[times.size()];
        float[] lons = new float[times.size()];
        float[] alts = new float[times.size()];
        //                float[]alts = (altIndex>=0?new float[times.size()]:null);
        Unit timeUnit    = ((DateTime) times.get(0)).getUnit();
        
        Real paramSample;
        if(trackParamIndex>=0) {
            paramSample = (Real) ((Data[]) tuples.get(0))[trackParamIndex];
        } else {
            paramSample =    getDefaultValue();
        }
        RealType timeType =
            RealType.getRealType(DataUtil.cleanName("track_time" + "_"
                + timeUnit), timeUnit);
        RealTupleType rangeType = new RealTupleType(
                                      ucar.visad.Util.getRealType(
                                          paramSample.getUnit()), timeType);
        double[][] newRangeVals = new double[2][times.size()];
        int        numObs       = times.size();
        for (int i = 0; i < numObs; i++) {
            DateTime dateTime = (DateTime) times.get(i);
            Real     value = (trackParamIndex>=0?(Real) ((Data[]) tuples.get(i))[trackParamIndex]:getDefaultValue());
            newRangeVals[0][i] = value.getValue();
            newRangeVals[1][i] = dateTime.getValue();
            Data[] tupleData = (Data[]) tuples.get(i);
            //Clear the garbage
            tuples.set(i, null);
            lats[i] = (float) ((Real) tupleData[latIndex]).getValue();
            lons[i] = (float) ((Real) tupleData[lonIndex]).getValue();
            if (altIndex >= 0) {
                alts[i] = (float) ((Real) tupleData[altIndex]).getValue();
            } else {
                alts[i] = 0f;
            }
        }
        GriddedSet llaSet = ucar.visad.Util.makeEarthDomainSet(lats, lons,
                                alts);
        Set[] rangeSets = new Set[2];
        rangeSets[0] = new DoubleSet(new SetType(rangeType.getComponent(0)));
        rangeSets[1] = new DoubleSet(new SetType(rangeType.getComponent(1)));
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField timeTrack = new FlatField(newType, llaSet,
                                            (CoordinateSystem) null,
                                            rangeSets,
                                            new Unit[] {
                                                paramSample.getUnit(),
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);

        FunctionType fiType = new FunctionType(RealType.Time,
                                  timeTrack.getType());
        DateTime endTime = (DateTime) times.get(0);
        FieldImpl fi =
            new FieldImpl(fiType,
                          new SingletonSet(new RealTuple(new Real[] {
                              endTime })));
        fi.setSample(0, timeTrack, false);
        return fi;
        //        return timeTrack;
    }



    private Real getDefaultValue() throws VisADException {
        if(dfltReal == null) {
            RealType  dfltRealType = RealType.getRealType("Default");
            dfltReal      = new Real(dfltRealType,1);
        }
        return dfltReal;
    }


    /**
     * Take a field of data and turn it into a field of PointObs.
     * Text file must have lines with lat, lon and values...
     * We use the FieldImpl that has domain recNum, recnum or index.
     * (
     * @param input     raw VisAD data
     * @param trackParam _more_
     * @return field of PointObs
     *
     * @throws VisADException   couldn't make the observations
     */
    private FieldImpl makePointObs(Data input, String trackParam)
            throws VisADException {

        varNames = new ArrayList();
        long      millis   = System.currentTimeMillis();
        FieldImpl retField = null;
        try {
            // first check to see if we can make a location ob
            // input has to have a FieldImpl as one component of the 
            // form (index -> (parm1, parm2, parm3, ...., parmN))

            FieldImpl recNumObs = null;
            // check for index 
            MathType inputType   = input.getType();
            int      recNumIndex = -1;
            RealType recNum      = null;
            for (int i = 0; i < recNumVars.length; i++) {
                recNum = RealType.getRealType(recNumVars[i]);
                if (MathType.findScalarType(inputType, recNum)) {
                    break;
                }
            }
            if (recNum == null) {
                throw new IllegalArgumentException(
                    "unable to find index for observations");
            }

            if (input instanceof Tuple) {
                TupleType tt = (TupleType) input.getType();
                for (int i = 0; i < tt.getDimension(); i++) {
                    MathType compType = tt.getComponent(i);
                    if ((compType instanceof FunctionType)) {
                        RealTupleType domType =
                            ((FunctionType) compType).getDomain();
                        if ((domType.getDimension() == 1)
                                && recNum.equals(domType.getComponent(0))) {
                            recNumObs =
                                (FieldImpl) ((Tuple) input).getComponent(i);
                            break;
                        }
                    }
                }
            } else if ((inputType instanceof FunctionType) && (recNum.equals(
                    ((FunctionType) inputType).getDomain().getComponent(
                        0)))) {
                recNumObs = (FieldImpl) input;
            }
            if (recNumObs == null) {
                throw new IllegalArgumentException(
                    "don't know how to convert input to a point ob");
            }

            TupleType    type;
            Gridded1DSet indexSet = null;
            try {
                type = (TupleType) ((FunctionType) recNumObs.getType())
                    .getRange();
                indexSet = (Gridded1DSet) recNumObs.getDomainSet();
            } catch (ClassCastException ce) {
                throw new IllegalArgumentException(
                    "don't know how to convert input to a point ob");
            }
            //System.out.println("type = " + type);
            //System.out.println(indexSet.getLength() + " obs");
            long    mil2     = System.currentTimeMillis();
            boolean allReals = (type instanceof RealTupleType);

            // check for time 
            int timeIndex = -1;
            for (int i = 0; i < timeVars.length; i++) {
                timeIndex = type.getIndex(timeVars[i]);
                if (timeIndex > -1) {
                    break;
                }
            }

            if (timeIndex == -1) {
                throw new IllegalArgumentException(
                    "can't find DateTime components");
            }

            Real      dfltAlt       = new Real(RealType.Altitude, 1);
            Real dfltReal = getDefaultValue();


            TupleType finalTT       = null;
            TupleType dataTupleType = null;
            Unit[]    dataUnits     = null;


            // Check for LAT/LON/ALT
            int latIndex        = type.getIndex(RealType.Latitude);
            int lonIndex        = type.getIndex(RealType.Longitude);
            int altIndex        = type.getIndex(RealType.Altitude);

            if(altIndex>=0) {
                varNames.add("Altitude");
            }
            int trackParamIndex = -1;
            if (trackParam != null) {
                if(trackParam.equals("Altitude")) {
                    trackParamIndex = altIndex;
                } else  if(trackParam.equals("Default")) {
                } else {
                    System.err.println ("track:" + trackParam + " " + type);
                    trackParamIndex = type.getIndex(trackParam);
                    if (trackParamIndex == -1) {
                        throw new IllegalArgumentException(
                                                           "Can't find track param");
                    }
                }
            }



            //if (altIndex == -1) altIndex = type.getIndex("elev");
            if ((latIndex == -1) || (lonIndex == -1)) {
                throw new IllegalArgumentException("can't find lat/lon");
            }

            int numVars        = type.getDimension();
            int numNotRequired = numVars - ((altIndex != -1)
                                            ? 4
                                            : 3);
            //System.out.println("Of " + numVars + " vars, " + numNotRequired + 
            //                   " are not required");

            int[] notReqIndices = new int[numNotRequired];

            int   l             = 0;
            for (int i = 0; i < numVars; i++) {
                if ((i != timeIndex) && (i != latIndex) && (i != lonIndex)
                        && (i != altIndex)) {
                    notReqIndices[l++] = i;
                }
            }

            int       numObs = indexSet.getLength();
            PointOb[] obs    = new PointObTuple[numObs];
            List      times  = new ArrayList();
            List      tuples = new ArrayList();
            for (int i = 0; i < numObs; i++) {
                Tuple  ob        = (Tuple) recNumObs.getSample(i);
                Data[] tupleData = ob.getComponents();
                tuples.add(tupleData);
                // get DateTime.  Must have valid time unit.  If not assume
                // seconds since epoch.  Maybe we should throw an error?
                Real timeVal = (Real) tupleData[timeIndex];
                if (timeVal.getUnit() != null) {
                    times.add(new DateTime(timeVal));
                } else {  // assume seconds since epoch
                    times.add(new DateTime(timeVal.getValue()));
                }
            }


            if (trackParam != null) {
                if ((groupVarName != null) && (groupVarName.length() > 0)) {
                    int groupParamIndex = -1;
                    for (int typeIdx = 0; typeIdx < type.getDimension();
                            typeIdx++) {
                        String ts = type.getComponent(typeIdx).toString();
                        if (ts.equals(groupVarName)
                                || ts.equals(groupVarName + "(Text)")) {
                            groupParamIndex = typeIdx;
                            break;
                        }
                    }
                    if (groupParamIndex == -1) {
                        throw new IllegalArgumentException(
                            "Can't find group param: " + groupVarName);
                    }
                    List      names = new ArrayList();
                    Hashtable seen  = new Hashtable();
                    for (int i = 0; i < numObs; i++) {
                        Data[] tupleData = (Data[]) tuples.get(i);
                        String v = tupleData[groupParamIndex].toString();
                        List   dataList  = (List) seen.get(v);
                        List   timeList  = (List) seen.get(v + "_timelist");
                        if (dataList == null) {
                            names.add(v);
                            dataList = new ArrayList();
                            timeList = new ArrayList();
                            seen.put(v, dataList);
                            seen.put(v + "_timelist", timeList);
                        }
                        timeList.add(times.get(i));
                        dataList.add(tupleData);
                    }
                    List     tracks    = new ArrayList();
                    MathType trackType = null;
                    for (int nameIdx = 0; nameIdx < names.size(); nameIdx++) {
                        String name     = (String) names.get(nameIdx);
                        List   dataList = (List) seen.get(name);
                        List timeList   = (List) seen.get(name + "_timelist");
                        FieldImpl track = makeTrack(trackParamIndex,
                                              latIndex, lonIndex, altIndex,
                                              timeList, dataList);
                        if (trackType == null) {
                            trackType = track.getType();
                        }
                        tracks.add(track);
                    }
                    TextType textType = TextType.getTextType(groupVarName
                                            + "_type");
                    TupleType tt = new TupleType(new MathType[] { textType,
                            trackType });
                    Data[] tracksData = new Data[tracks.size()];
                    for (int i = 0; i < tracks.size(); i++) {
                        String name = (String) names.get(i);
                        Data   d    = (Data) tracks.get(i);
                        tracksData[i] = new Tuple(tt,
                                new Data[] { new Text(textType, name),
                                             d });
                    }

                    RealType indexType = RealType.getRealType("index");
                    Set domain = new Linear1DSet(indexType, 0,
                                     tracks.size() - 1, tracks.size());
                    FunctionType aggregateType = new FunctionType(indexType,
                                                     tt);
                    FieldImpl aggregateField = new FieldImpl(aggregateType,
                                                   domain);
                    aggregateField.setSamples(tracksData, false);
                    return aggregateField;
                }

                return makeTrack(trackParamIndex, latIndex, lonIndex,
                                 altIndex, times, tuples);
            }



            times = PointObFactory.binTimes(times, getBinRoundTo(),
                                            getBinWidth());


            for (int i = 0; i < numObs; i++) {
                DateTime dateTime  = (DateTime) times.get(i);
                Data[]   tupleData = (Data[]) tuples.get(i);
                //Clear the garbage
                tuples.set(i, null);
                // get location
                EarthLocation location =
                    new EarthLocationLite((Real) tupleData[latIndex],
                                          (Real) tupleData[lonIndex],
                                          (altIndex != -1)
                                          ? (Real) tupleData[altIndex]
                                          : dfltAlt);


                // now make data
                Data[] others;
                if (numNotRequired > 0) {
                    others = (allReals == true)
                             ? new Real[numNotRequired]
                             : new Data[numNotRequired];
                    for (int j = 0; j < numNotRequired; j++) {
                        others[j] = (allReals == true)
                                    ? (Real) tupleData[notReqIndices[j]]
                                    : (Data) tupleData[notReqIndices[j]];
                        //if (i == 0) {
                        //    System.err.println("name:" + others[j].getType());
                        //}
                    }
                } else {
                    others = new Real[] { dfltReal };
                }


                if (i == 0)  {
                    for(int otherIdx=0;otherIdx<others.length;otherIdx++) { 
                        if(others[otherIdx] instanceof Real) {
                            Real r = (Real) others[otherIdx];
                            varNames.add(((RealType) r.getType()).getName());
                        }
                    }
                }


                if (dataTupleType == null) {
                    Tuple tmp = (allReals == true)
                                ? new RealTuple((Real[]) others)
                                : new Tuple(others, false);
                    dataTupleType = (TupleType) tmp.getType();
                    if (allReals) {
                        dataUnits = ((RealTuple) tmp).getTupleUnits();
                    }
                }


                Data rest = (allReals == true)
                            ? new RealTuple((RealTupleType) dataTupleType,
                                            (Real[]) others, null, dataUnits,
                                            false)
                            : new Tuple(dataTupleType, others, false, false);

                if (finalTT == null) {
                    PointObTuple pot = new PointObTuple(location, dateTime,
                                           rest);
                    obs[i]  = pot;
                    finalTT = Tuple.buildTupleType(pot.getComponents());
                } else {
                    obs[i] = new PointObTuple(location, dateTime, rest,
                            finalTT, false);

                }
            }
            retField = new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    obs[0].getType()), indexSet);
            retField.setSamples(obs, false, false);
        } catch (RemoteException re) {
            throw new VisADException("got RemoteException " + re);
        }
        //System.out.println("Making point obs took " + (System.currentTimeMillis() - millis));
        return retField;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private boolean isTrajectoryEnabled() {
        return getProperty("dataistrajectory", false);
    }


    /**
     * _more_
     */
    public void doMakeDataChoices() {
        super.doMakeDataChoices();
        if (isTrajectoryEnabled()) {
            if (getDataChoices().size() == 0) {
                return;
            }
            try {
                DataChoice dataChoice = (DataChoice) getDataChoices().get(0);


                Data sample = makeObs(dataChoice, null, null, null, true,
                                      true);
                //                System.err.println ("sample:" + sample);

                List cats = DataCategory.parseCategories("Track" + ";trace",
                                true);
                for (int i = 0; i < varNames.size(); i++) {
                    String var = (String) varNames.get(i);
                    DataChoice choice = new DirectDataChoice(this,
                                            "track:" + var, var, var, cats,
                                            (Hashtable) null);
                    addDataChoice(choice);
                }
            } catch (Exception exc) {
                logException("Creating track choices", exc);
            }
        }
    }


    /**
     * _more_
     *
     * @param comps _more_
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        if (isTrajectoryEnabled()) {}
    }


    /**
     * _more_
     *
     * @param dataChoice _more_
     * @param category _more_
     * @param dataSelection _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        Object id = dataChoice.getId();
        if ((id instanceof String) && (id.toString().startsWith("track:"))) {
            try {
                return makeObs((DataChoice) dataChoice, dataSelection, null,
                               id.toString().substring(6), false, true);
            } catch (Exception exc) {
                logException("Creating obs", exc);
                return null;
            }
        }
        return super.getDataInner(dataChoice, category, dataSelection,
                                  requestProperties);
    }


    /**
     * test
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        try {
            for (int i = 0; i < 1; i++) {
                TextPointDataSource tpds = new TextPointDataSource();
                //            long m1 =  Misc.gc();
                Data data = tpds.test(args[0]);

                //            long m2 =   Misc.gc();
                //            System.err.println ("memory:" + (m2-m1));
            }
        } catch (Exception exc) {
            System.err.println("err:" + exc);
            exc.printStackTrace();
        }
        if (true) {
            return;
        }


        long total = 0;
        for (int i = 0; i < 5; i++) {
            java.net.URL url = IOUtil.getURL(args[0],
                                             TextPointDataSource.class);
            long        t1 = System.currentTimeMillis();
            TextAdapter ta = new TextAdapter(url);
            long        t2 = System.currentTimeMillis();
            System.err.println(ta.getData());
            if (true) {
                break;
            }
            //            System.err.println("Time:" + (t2 - t1));
            if (i != 0) {
                total += (t2 - t1);
            }
        }
        System.err.println("avg:" + (total / 4));
        //putCache (source, obs);
    }


    /**
     * Set the Map property.
     *
     * @param value The new value for Map
     */
    public void setMap(String value) {
        map = value;
    }

    /**
     * Get the Map property.
     *
     * @return The Map
     */
    public String getMap() {
        return map;
    }

    /**
     * Set the Params property.
     *
     * @param value The new value for Params
     */
    public void setParams(String value) {
        params = value;
    }

    /**
     * Get the Params property.
     *
     * @return The Params
     */
    public String getParams() {
        return params;
    }


    /**
     * Set the MetaDataFields property.
     *
     * @param value The new value for MetaDataFields
     */
    public void setMetaDataFields(List value) {
        metaDataFields = value;
    }

    /**
     * Get the MetaDataFields property.
     *
     * @return The MetaDataFields
     */
    public List getMetaDataFields() {
        return metaDataFields;
    }


    /**
     * Set the GroupVarName property.
     *
     * @param value The new value for GroupVarName
     */
    public void setGroupVarName(String value) {
        groupVarName = value;
    }

    /**
     * Get the GroupVarName property.
     *
     * @return The GroupVarName
     */
    public String getGroupVarName() {
        return groupVarName;
    }

    /**
     * Set the SkipRows property.
     *
     * @param value The new value for SkipRows
     */
    public void setSkipRows(int value) {
        skipRows = value;
    }

    /**
     * Get the SkipRows property.
     *
     * @return The SkipRows
     */
    public int getSkipRows() {
        return skipRows;
    }

    /**
     * Class ParamRow _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class ParamRow {

        /** _more_          */
        JComboBox nameBox;

        /** _more_          */
        JTextField extraFld;

        /** _more_          */
        JTextField missingFld;

        /** _more_          */
        JTextField unitFld;

        /** _more_          */
        JButton popupBtn;

        /** _more_          */
        JLabel sample;

        /** _more_          */
        static Vector boxNames;

        /** _more_          */
        static Vector unitNames;

        /**
         * _more_
         */
        public ParamRow() {
            if (boxNames == null) {
                boxNames = new Vector(Misc.newList("", "Time", "Latitude",
                        "Longitude", "Altitude"));
                String unitStr =
                    ";celsius;kelvin;fahrenheit;deg;degrees west;feet;km;meters;m;miles;kts;yyyy-MM-dd HH:mm:ss";
                unitNames = new Vector(StringUtil.split(unitStr, ";", false,
                        false));
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getCleanName() {
            return ucar.visad.Util.cleanName(getName());
        }


        /**
         * _more_
         *
         * @param name _more_
         */
        public void setName(String name) {
            nameBox.setSelectedItem(name);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getName() {
            return nameBox.getSelectedItem().toString().trim();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getExtra() {
            return extraFld.getText().trim();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getMissing() {
            return missingFld.getText().trim();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getUnit() {
            return unitFld.getText().trim();
        }

        /**
         * _more_
         *
         * @param tokIdx _more_
         * @param toks _more_
         * @param comps _more_
         */
        public void init(int tokIdx, List toks, List comps) {
            if (nameBox == null) {
                nameBox = new JComboBox(boxNames);
                nameBox.setEditable(true);
                nameBox.setPreferredSize(new Dimension(40, 10));
                extraFld = new JTextField("", 5);
                extraFld.setToolTipText(
                    "<html>Extra attributes, e.g.:<br>colspan=&quot;some column span&quot;<br>Note:Values must be quoted.</html>");
                missingFld = new JTextField("", 5);
                unitFld    = new JTextField("", 10);
                popupBtn =
                    GuiUtils.getImageButton("/auxdata/ui/icons/Down.gif",
                                            getClass());
                popupBtn.setToolTipText("Set unit");
                popupBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        GuiUtils.popupUnitMenu(unitFld, popupBtn);
                    }
                });
                sample = new JLabel("");
            }
            sample.setText(StringUtil.shorten(toks.get(tokIdx).toString(),
                    25));
            comps.add(sample);
            comps.add(nameBox);
            comps.add(GuiUtils.centerRight(unitFld, popupBtn));
            comps.add(missingFld);
            comps.add(extraFld);
        }

        /**
         * _more_
         *
         * @param fields _more_
         */
        public void applyMetaData(List fields) {
            nameBox.setSelectedItem((String) fields.get(0));
            unitFld.setText((String) fields.get(1));
            missingFld.setText((String) fields.get(2));
            if (fields.size() >= 4) {
                extraFld.setText((String) fields.get(3));
            } else {
                extraFld.setText("");
            }
        }

        /**
         * _more_
         *
         * @param metaDataFields _more_
         */
        public void addToMetaData(List metaDataFields) {
            metaDataFields.add(Misc.newList(getCleanName(), getUnit(),
                                            getMissing(), getExtra()));

        }


    }
}


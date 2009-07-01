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
import ucar.unidata.util.FileManager;
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

import java.text.SimpleDateFormat;


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

    /** property id for the header map */
    public static final String PROP_HEADER_MAP = "data.textpoint.map";

    /** Property identifier for the hashtable of properties */
    public static final String PROP_DATAPROPERTIES =
        "data.textpoint.dataproperties";

    /** Property identifier for the */
    public static final String PROP_HEADER_EXTRA = "data.textpoint.extra";

    /** property id for the header params */
    public static final String PROP_HEADER_PARAMS = "data.textpoint.params";

    /** property id for how many rows to skip */
    public static final String PROP_HEADER_SKIP = "data.textpoint.skip";

    /** property id for the whole header blob, map and params */
    public static final String PROP_HEADER_BLOB = "data.textpoint.blob";

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

    /** the default real */
    private Real dfltReal;

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(TextPointDataSource.class.getName());

    /** variables for time */
    private String[] timeVars = {
        "time_nominal", "time_Nominal", "timeNominal", "timeObs","obtime",
        "reportTime", "time", "nominal_time", "Time", "observation_time","Observation_Time","datetime","dttm"
    };


    /** variables for latitude */
    private String[] latVars = { "Latitude", "latitude", "lat" };

    /** variables for longitude */
    private String[] lonVars = { "Longitude", "longitude", "lon" };


    private String[] altVars = { "altitude", "Altitude", "elevation","Elevation" };


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

    /** last type */
    private String lastType = "";

    /** last label */
    private String lastLabel = "";

    /** Can pass in properties to the parser with this */
    private Hashtable dataProperties;

    /** for the metadata gui */
    JLabel lineLbl;

    /** for the metadata gui */
    List lines;

    /**
     * Default constructor
     *
     * @throws VisADException  problem creating the object
     */
    public TextPointDataSource() throws VisADException {
        init();
    }

    public TextPointDataSource(String source)
        throws VisADException {
        this(new DataSourceDescriptor(), source, new Hashtable());
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
        if (properties != null) {
            this.dataProperties =
                (Hashtable) properties.get(PROP_DATAPROPERTIES);
        }
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
    public FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {
        //        System.err.println("MAKE OBS");
        return makeObs(dataChoice, subset, bbox, null, false, true);
    }

    /**
     * the data choice
     *
     * @param dataChoice the data choice
     *
     * @return the file or url this data choice refers to
     */
    protected String getSource(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        if ((id instanceof String) && (id.toString().startsWith("track:"))) {
            return (String) sources.get(0);
        }
        return super.getSource(dataChoice);
    }

    /**
     * get the input stream for the given file or url
     *
     * @param contents the header contents
     *
     * @return the input stream
     *
     * @throws Exception On badness
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

        String extra = getProperty(PROP_HEADER_EXTRA, (String) null);
        if (extra != null) {}
        return is;

    }


    /**
     * make the observations from the given datachoice
     *
     * @param dataChoice the data choice
     * @param subset data selection to subset with
     * @param bbox bounding box to subset
     * @param trackParam the parameter to use for thetrack
     * @param sampleIt do we just sample or do we read the full set of obs
     * @param showAttributeGuiIfNeeded popup the gui if we have a problem
     *
     * @return the field
     *
     * @throws Exception On badness
     */
    public FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
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

        return makeObs(contents, delimiter, subset, bbox, trackParam,
                       sampleIt, showAttributeGuiIfNeeded);
    }



    /**
     * make the observations from the given datachoice
     *
     * @param contents The text contents
     * @param delimiter The delimiter
     * @param subset data selection to subset with
     * @param bbox bounding box to subset
     * @param trackParam the parameter to use for thetrack
     * @param sampleIt do we just sample or do we read the full set of obs
     * @param showAttributeGuiIfNeeded popup the gui if we have a problem
     *
     * @return the field
     *
     * @throws Exception On badness
     */
    public FieldImpl makeObs(String contents, String delimiter,
                                DataSelection subset, LatLonRect bbox,
                                String trackParam, boolean sampleIt,
                                boolean showAttributeGuiIfNeeded)
            throws Exception {



        FieldImpl obs = null;
        //        FieldImpl obs = (FieldImpl) getCache (source);
        if (obs == null) {
            TextAdapter ta = null;
            try {
                String extra = getProperty(PROP_HEADER_EXTRA, (String) null);
                if ((params == null) || (params.length() == 0)) {
                    params = getProperty(PROP_HEADER_PARAMS, (String) null);
                }
                if ((map == null) || (map.length() == 0)) {
                    map = getProperty(PROP_HEADER_MAP, (String) null);
                }
                if (skipRows == 0) {
                    skipRows = getProperty(PROP_HEADER_SKIP, skipRows);
                }
                String blob = getProperty(PROP_HEADER_BLOB, (String) null);
                if ((blob != null) && (metaDataFields.size() == 0)) {
                    Object object =
                        getDataContext().getIdv().decodeObject(blob);
                    if (object instanceof List) {
                        object = new Metadata(-1, (List) object);
                    }
                    Metadata metadata = (Metadata) object;
                    metaDataFields = metadata.getItems();
                    applySavedMetaData(metadata);
                }

                ta = new TextAdapter(getInputStream(contents), delimiter,
                                     map, params, dataProperties, sampleIt);
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
                    //If user hits Cancel then we are in error and this datasource should get removed
                    setInError(true, false, "");
                    return null;
                }
                ta = new TextAdapter(getInputStream(contents), delimiter,
                                     map, params, dataProperties, sampleIt);
            }
            try {
                Data d = ta.getData();
                if (d == null) {
                    throw new IllegalArgumentException(
                        "Could not create point data");
                }
                obs = makePointObs(d, trackParam);
                if ((fieldsDescription == null) && (obs != null)) {
                    makeFieldDescription(obs);
                }
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
     * test
     *
     * @param source test source
     *
     * @return the data
     *
     * @throws Exception On badness
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
                map, params, dataProperties, false);
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
     * update the attribute gui
     *
     * @param lbl the label
     * @param index index
     * @param lines the lines to show
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


        applySavedMetaData(new Metadata(skipRows, metaDataFields));
        GuiUtils.tmpInsets = new Insets(5, 5, 0, 0);
        double[]   stretch = { 0.0, 1.0, 0.5, 0.0, 0.5 };
        JComponent panel = GuiUtils.doLayout(comps, 5, stretch,
                                             GuiUtils.WT_N);
        widgetPanel.removeAll();
        JScrollPane widgetSP =
            GuiUtils.makeScrollPane(GuiUtils.top(GuiUtils.inset(panel, 5)),
                                    200, 200);
        widgetSP.setPreferredSize(new Dimension(200, 200));
        widgetPanel.add(BorderLayout.CENTER, widgetSP);
    }

    /**
     * update the gui
     *
     * @param line the sampled text line
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
            lines = new ArrayList();
            for (int i = 0; i < 100; i++) {
                String line = bis.readLine();
                if (line == null) {
                    break;
                }
                List toks = StringUtil.split(line, getDelimiter(), false,
                                             false);
                lines.add(line);
            }


            lineLbl = new JLabel(" ");
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
    private Hashtable<String, Metadata> getMetaDataMap() {
        Hashtable tmp =
            (Hashtable) getDataContext().getIdv().getStore().getEncodedFile(
                PREF_METADATAMAP);
        if (tmp == null) {
            tmp = new Hashtable();
        }
        Hashtable<String, Metadata> pointMetaDataMap = new Hashtable<String,
                                                           Metadata>();
        for (Enumeration keys = tmp.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            Object value = tmp.get(key);
            if (value instanceof List) {
                value = new Metadata(-1, (List) value);
            }
            pointMetaDataMap.put(key, (Metadata) value);
        }
        return pointMetaDataMap;
    }


    /**
     * Remove the given key from the saved mapping. Write out the file
     *
     * @param key key
     */
    public void deleteMetaData(String key) {
        Hashtable<String, Metadata> pointMetaDataMap = getMetaDataMap();
        pointMetaDataMap.remove(key);
        getDataContext().getIdv().getStore().putEncodedFile(PREF_METADATAMAP,
                pointMetaDataMap);
    }



    /**
     * write the plugin
     */
    public void writePlugin() {
        try {
            JTextField typeFld  = new JTextField(lastType, 30);
            JTextField labelFld = new JTextField(lastLabel, 30);
            JCheckBox trajectoryCbx = new JCheckBox("Is trajectory data",
                                          false);
            JComponent contents = GuiUtils.formLayout(new Object[] {
                "Type:", typeFld, "Label:", labelFld, new JLabel(""),
                trajectoryCbx
            });
            if ( !GuiUtils.showOkCancelDialog(null,
                    "Data Source Type Plugin", contents, null)) {
                return;
            }
            lastType  = typeFld.getText().trim();
            lastLabel = labelFld.getText().trim();
            String[] tmp = makeMetadataHeader();
            if (tmp == null) {
                return;
            }
            String trajectory = (trajectoryCbx.isSelected()
                                 ? "true"
                                 : "false");
            String xml = DataManager.getDatasourceXml(lastType, lastLabel,
                             getClass(), Misc.newHashtable(new Object[] {
                PROP_HEADER_MAP, tmp[0], PROP_HEADER_PARAMS, tmp[1],
                PROP_HEADER_SKIP, "" + skipRows, "dataistrajectory",
                trajectory, PROP_HEADER_BLOB,
                getDataContext().getIdv().encodeObject(new Metadata(skipRows,
                    metaDataFields), false)
            }));
            getDataContext().getIdv().getPluginManager().addText(xml,
                    lastType + "datasource.xml");
        } catch (Exception exc) {
            logException("Writing data source type", exc);
        }
    }


    /**
     * write the header text to a file
     */
    public void writeHeader() {
        try {
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_TXT, null);
            if (filename == null) {
                return;
            }
            String[]     tmp = makeMetadataHeader();
            StringBuffer sb  = new StringBuffer(tmp[0]);
            sb.append("\n");
            sb.append(tmp[1]);
            IOUtil.writeFile(filename, sb.toString());
        } catch (IOException exc) {
            logException("Writing header", exc);
        }
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
        items.add(GuiUtils.makeMenuItem("Write Header", this, "writeHeader"));
        items.add(GuiUtils.makeMenuItem("Write Data Source Plugin", this,
                                        "writePlugin"));
        Hashtable<String, Metadata> pointMetaDataMap = getMetaDataMap();
        if (pointMetaDataMap.size() > 0) {
            List delitems = new ArrayList();
            items.add(GuiUtils.MENU_SEPARATOR);
            for (Enumeration keys = pointMetaDataMap.keys();
                    keys.hasMoreElements(); ) {
                String   key      = (String) keys.nextElement();
                Metadata metadata = (Metadata) pointMetaDataMap.get(key);
                items.add(GuiUtils.makeMenuItem(key, this,
                        "applySavedMetaDataFromUI", metadata));
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
        Hashtable<String, Metadata> pointMetaDataMap = getMetaDataMap();
        Vector                      items            = new Vector();
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
        pointMetaDataMap.put(prefname,
                             new Metadata(skipRows, metaDataFields));
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
                if ( !getMakeGridFields()) {
                    //If we are making grid fields then this has already been called
                    //If not then call it here so we check on the header
                    Data sample = makeObs(dataChoice, null, null, null, true,
                                          false);
                }
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
        String[] tmp = makeMetadataHeader();
        map    = tmp[0];
        params = tmp[1];
    }


    /**
     * make the metadata header from the gui
     *
     * @return the metadata header
     */
    private String[] makeMetadataHeader() {
        String map    = "(index)->(";
        String params = "";
        int    cnt    = 0;
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
        return new String[] { map, params };
        //        System.out.println (map);
        //        System.out.println (params);

    }


    /**
     * THis gets called from the Preferences menu and sets the 
     * metadata and also updates the skipRows
     *
     * @param metadata The metadata
     */
    public void applySavedMetaDataFromUI(Metadata metadata) {
        Misc.run(this, "applySavedMetaDataFromUIInner", metadata);
    }



    /**
     *  This gets called in a thread from the applySavedMetaDataFromUI method
     * 
     *  @param metadata The metadata
     */
    public void applySavedMetaDataFromUIInner(Metadata metadata) {
        if (metadata.getSkipRows() >= 0) {
            skipRows = metadata.getSkipRows();
        }
        setLineText(lineLbl, skipRows, lines);
        applySavedMetaData(metadata);
    }

    /**
     * Init the widgets
     *
     *
     * @param metadata The metadata
     */
    public void applySavedMetaData(Metadata metadata) {
        List fieldList = metadata.getItems();
        if (metadata.getSkipRows() >= 0) {
            skipRows = metadata.getSkipRows();
        }

        for (int tokIdx = 0;
                (tokIdx < paramRows.size()) && (tokIdx < fieldList.size());
                tokIdx++) {
            ParamRow paramRow = (ParamRow) paramRows.get(tokIdx);
            paramRow.applyMetaData((List) fieldList.get(tokIdx));
        }
        metaDataComp.validate();
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

    /** var names */
    List varNames = new ArrayList();

    /**
     * make a trajectory form the obs data
     *
     * @param trackParamIndex which parameter to use
     * @param latIndex where is the lat
     * @param lonIndex where is the lion
     * @param altIndex where is the alt
     * @param times the data tuples
     * @param tuples the trajectory
     *
     * @return the trajectory
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FieldImpl makeTrack(int trackParamIndex, int latIndex,
                                int lonIndex, int altIndex, List times,
                                List tuples)
            throws VisADException, RemoteException {
        float[] lats = new float[times.size()];
        float[] lons = new float[times.size()];
        float[] alts = new float[times.size()];
        //                float[]alts = (altIndex>=0?new float[times.size()]:null);
        Unit timeUnit = ((DateTime) times.get(0)).getUnit();

        Real paramSample;
        if (trackParamIndex >= 0) {
            paramSample = (Real) ((Data[]) tuples.get(0))[trackParamIndex];
        } else {
            paramSample = getDefaultValue();
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
            Real     value    = ((trackParamIndex >= 0)
                                 ? (Real) ((Data[]) tuples.get(
                                     i))[trackParamIndex]
                                 : getDefaultValue());
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



    /**
     * get the default real value to use
     *
     * @return dflt value
     *
     * @throws VisADException On badness
     */
    private Real getDefaultValue() throws VisADException {
        if (dfltReal == null) {
            RealType dfltRealType = RealType.getRealType("Default");
            dfltReal = new Real(dfltRealType, 1);
        }
        return dfltReal;
    }


    /**
     * Take a field of data and turn it into a field of PointObs.
     * Text file must have lines with lat, lon and values...
     * We use the FieldImpl that has domain recNum, recnum or index.
     * (
     * @param input     raw VisAD data
     * @param trackParam the track parameter
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
            Real      dfltReal      = getDefaultValue();


            TupleType finalTT       = null;
            TupleType dataTupleType = null;
            Unit[]    dataUnits     = null;


            // Check for LAT/LON/ALT
            int latIndex = type.getIndex(RealType.Latitude);
            int lonIndex = type.getIndex(RealType.Longitude);
            int altIndex = type.getIndex(RealType.Altitude);

            if (latIndex < 0) {
                for (int i = 0; i < latVars.length; i++) {
                    latIndex = type.getIndex(latVars[i]);
                    if (latIndex > -1) {
                        break;
                    }
                }
            }



            if (lonIndex < 0) {
                for (int i = 0; i < lonVars.length; i++) {
                    lonIndex = type.getIndex(lonVars[i]);
                    if (lonIndex > -1) {
                        break;
                    }
                }
            }


            if (altIndex < 0) {
                for (int i = 0; i < altVars.length; i++) {
                    altIndex = type.getIndex(altVars[i]);
                    if (altIndex > -1) {
                        break;
                    }
                }
            }




            if (altIndex >= 0) {
                varNames.add("Altitude");
            }
            int trackParamIndex = -1;
            if (trackParam != null) {
                if (trackParam.equals("Altitude")) {
                    trackParamIndex = altIndex;
                } else if (trackParam.equals("Default")) {}
                else {
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


                if (i == 0) {
                    for (int otherIdx = 0; otherIdx < others.length;
                            otherIdx++) {
                        if (others[otherIdx] instanceof Real) {
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
     * should we make trajectories out of the point obs
     *
     * @return make trajectories
     */
    private boolean isTrajectoryEnabled() {
        return getProperty("dataistrajectory", false);
    }


    /**
     * Read a sample of the data. e.g., just the first ob
     *
     * @param dataChoice The data choice
     *
     * @return The first ob
     *
     * @throws Exception On badness
     */
    protected FieldImpl getSample(DataChoice dataChoice) throws Exception {
        return (FieldImpl) makeObs(dataChoice, null, null, null, true, true);

    }

    /**
     * Make the data choices
     */
    public void doMakeDataChoices() {
        super.doMakeDataChoices();

        try {
            if (getDataChoices().size() == 0) {
                return;
            }
            DataChoice dataChoice = (DataChoice) getDataChoices().get(0);
            //Sample the data to see if we need to show the metadata gui
            Data sample = makeObs(dataChoice, null, null, null, true, true);

            if (isTrajectoryEnabled()) {

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
            }
        } catch (Exception exc) {
            logException("Creating track choices", exc);
        }

    }


    /**
     * Add to properties gui
     *
     * @param comps properties comps
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        if (isTrajectoryEnabled()) {}
    }


    /**
     * Get the data represented by this class.  Calls makeObs, real work
     * needs to be implemented there.
     *
     * @param dataChoice         choice for data
     * @param category           category of data
     * @param dataSelection      subselection properties
     * @param requestProperties  additional selection properties (not used here)
     * @return  Data object representative of the choice
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
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
    public static void main2(String[] args) throws Exception {
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
     * usage message
     */
    private static void usage() {
        System.err.println(
            "Usage: java PointObFactory -header headerfile <-skip skiplines> <-skiptonumber> <-xlsdateformat dateformat> <-Dproperty=value> <.csv files>");
        System.err.println(
            "e.g.: java PointObFactory test.hdr -skip 14 -DLatitude.value=41.5 -DLongitude.value=-101.2 -xlsdateformat MM/dd/yyyy foo.csv bar.xls");
    }


    /**
     * main
     *
     * @param args args
     *
     * @throws Exception on badness
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            usage();
            return;
        }
        String    header         = null;
        Hashtable dataProperties = new Hashtable();
        Hashtable properties     = new Hashtable();
        properties.put(PROP_DATAPROPERTIES, dataProperties);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        boolean          readToFirstNumeric = false;

        int              argIdx             = 1;
        for (argIdx = 0; argIdx < args.length; argIdx++) {
            if (args[argIdx].equals("-skip")) {
                if (argIdx == args.length - 1) {
                    usage();
                    return;
                }
                properties.put(TextPointDataSource.PROP_HEADER_SKIP,
                               new Integer(args[argIdx + 1]));
                argIdx++;
            } else if (args[argIdx].equals("-header")) {
                if (argIdx == args.length - 1) {
                    usage();
                    return;
                }
                header = IOUtil.readContents(args[argIdx + 1],
                                             TextPointDataSource.class);
                argIdx++;
            } else if (args[argIdx].equals("-skiptonumber")) {
                readToFirstNumeric = true;
            } else if (args[argIdx].equals("-xlsdateformat")) {
                if (argIdx == args.length - 1) {
                    usage();
                    return;
                }
                sdf = new SimpleDateFormat(args[argIdx + 1]);
                argIdx++;
            } else if (args[argIdx].startsWith("-D")) {
                List l = StringUtil.split(args[argIdx].substring(2), "=");
                if (l.size() != 2) {
                    System.err.println("Invalid property:" + args[argIdx]);
                    usage();
                    return;
                }
                dataProperties.put(l.get(0), l.get(1));
            } else if (args[argIdx].startsWith("-")) {
                System.err.println("Unknown argument:" + args[argIdx]);
                usage();
                return;
            } else {
                break;
            }
        }

        if (header == null) {
            //            System.err.println("No -header specified");
            //            usage();
            //            return;
        }

        if(header!=null) {
            List toks = StringUtil.split(header, "\n", true, true);
            if (toks.size() != 2) {
                System.err.println("Bad header");
                return;

            }

            properties.put(TextPointDataSource.PROP_HEADER_MAP, toks.get(0));
            properties.put(TextPointDataSource.PROP_HEADER_PARAMS, toks.get(1));

        }

        for (int i = argIdx; i < args.length; i++) {
            String csvFile = args[i];
            //            if(csvFile.endsWith(".xls")) {
            //                contents  = DataUtil.xlsToCsv(csvFile);
            //            }
            System.err.println("File:" + csvFile);
            TextPointDataSource dataSource =
                new TextPointDataSource(new DataSourceDescriptor(), csvFile,
                                        properties);

            String contents;
            if (args[i].endsWith(".xls")) {
                contents = DataUtil.xlsToCsv(args[i], readToFirstNumeric,
                                             sdf);
            } else {
                contents = IOUtil.readContents(args[i],
                        TextPointDataSource.class);
            }


            FieldImpl field = dataSource.makeObs(contents, ",", null, null,
                                  null, false, false);


            String ncFile = IOUtil.stripExtension(args[i]) + ".nc";
            System.err.println("Writing nc file:" + ncFile);
            PointObFactory.writeToNetcdf(new File(ncFile), field);

        }

        System.exit(0);

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
     * Class ParamRow data structure for gui
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class ParamRow {

        /** name widget */
        JComboBox nameBox;

        /** extra field */
        JTextField extraFld;

        /** missing field */
        JTextField missingFld;

        /** unit field */
        JTextField unitFld;

        /** button */
        JButton popupBtn;

        /** shows the sample from the csv file */
        JLabel sample;

        /** names */
        static Vector boxNames;

        /** units */
        static Vector unitNames;

        /**
         * ctor
         */
        public ParamRow() {
            if (boxNames == null) {
                boxNames = new Vector(Misc.toList(new Object[] {
                    "", "Time", "Latitude", "Longitude", "Altitude", "IDN"
                }));
                String unitStr =
                    ";celsius;kelvin;fahrenheit;deg;degrees west;feet;km;meters;m;miles;kts;yyyy-MM-dd HH:mm:ss";
                unitNames = new Vector(StringUtil.split(unitStr, ";", false,
                        false));
            }
        }

        /**
         * clean up the name
         *
         * @return cleaned up name
         */
        public String getCleanName() {
            return ucar.visad.Util.cleanName(getName());
        }


        /**
         * set the name
         *
         * @param name the name
         */
        public void setName(String name) {
            nameBox.setSelectedItem(name);
        }

        /**
         * get the name entered by the user
         *
         * @return the name
         */
        public String getName() {
            return nameBox.getSelectedItem().toString().trim();
        }

        /**
         * get the extra entered by the user
         *
         * @return extra
         */
        public String getExtra() {
            return extraFld.getText().trim();
        }

        /**
         * get the missing value entered by the user
         *
         * @return missing vlaue
         */
        public String getMissing() {
            return missingFld.getText().trim();
        }

        /**
         * get the unit entered by the user
         *
         * @return unit
         */
        public String getUnit() {
            return unitFld.getText().trim();
        }

        /**
         * init the widget
         *
         * @param tokIdx token idx
         * @param toks samples
         * @param comps comps
         */
        public void init(int tokIdx, List toks, List comps) {
            if (nameBox == null) {
                nameBox = new JComboBox(boxNames);
                nameBox.setEditable(true);
                nameBox.setPreferredSize(new Dimension(40, 10));
                //Listen for changes and set the unitFld to be degrees if the name is lat or long
                nameBox.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent event) {
                        String name = nameBox.getSelectedItem() + "";
                        /*
                        if(name.equals("latitude")) {
                            nameBox.setSelectedItem("Latitude");
                            return;
                        }

                        if(name.equals("longitude")) {
                            nameBox.setSelectedItem("Longitude");
                            return;
                            }*/

                        if (name.toLowerCase().equals("latitude")
                                || name.toLowerCase().equals("longitude")) {
                            if (unitFld.getText().trim().length() == 0) {
                                unitFld.setText("degrees");
                            }
                        }
                    }
                });
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
            JButton applyNameBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/HorArrow16.gif",
                                        getClass());
            applyNameBtn.setToolTipText("Use column value as the field name");
            applyNameBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    nameBox.setSelectedItem(sample.getText());
                }
            });


            comps.add(GuiUtils.leftRight(sample, applyNameBtn));
            //            comps.add(sample);
            comps.add(nameBox);
            comps.add(GuiUtils.centerRight(unitFld, popupBtn));
            comps.add(missingFld);
            comps.add(extraFld);
        }

        /**
         * update the gui
         *
         * @param fields tje fields
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
         * add the gui state to the list
         *
         * @param metaDataFields the list
         */
        public void addToMetaData(List metaDataFields) {
            metaDataFields.add(Misc.newList(getCleanName(), getUnit(),
                                            getMissing(), getExtra()));

        }


    }


    /**
     * Class Metadata holds the skipRows and the list of metadata items
     *
     *
     * @author IDV Development Team
     */
    public static class Metadata {

        /** Number of rows to skip */
        private int skipRows = -1;

        /** metadata items */
        private List items;

        /**
         * ctor
         */
        public Metadata() {}

        /**
         * ctor
         *
         * @param rows rows to skip
         * @param items metadata items
         */
        public Metadata(int rows, List items) {
            this.skipRows = rows;
            this.items    = items;
        }

        /**
         *  Set the SkipRows property.
         *
         *  @param value The new value for SkipRows
         */
        public void setSkipRows(int value) {
            skipRows = value;
        }

        /**
         *  Get the SkipRows property.
         *
         *  @return The SkipRows
         */
        public int getSkipRows() {
            return skipRows;
        }

        /**
         *  Set the Items property.
         *
         *  @param value The new value for Items
         */
        public void setItems(List value) {
            items = value;
        }

        /**
         *  Get the Items property.
         *
         *  @return The Items
         */
        public List getItems() {
            return items;
        }




    }


}


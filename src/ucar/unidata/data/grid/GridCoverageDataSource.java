/*
 * Copyright 1997-2020 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.grid;


import org.w3c.dom.Document;
import org.w3c.dom.Element;


import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.adapter.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.NamedAnything;

import ucar.nc2.util.NamedObject;
import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataOperand;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.VerticalPerspectiveView;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.ui.DataTreeDialog;
import ucar.unidata.ui.TextSearcher;
import ucar.unidata.util.*;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;
import ucar.visad.data.CalendarDateTime;

import visad.Data;
import visad.DateTime;
import visad.FieldImpl;
import visad.Real;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;


/**
 * Handles gridded files
 *
 * @author IDV Development Team
 */

public class GridCoverageDataSource extends GridDataSource {


    /** Preference */
    public static final String PREF_VERTICALCS = IdvConstants.PREF_VERTICALCS;
    /** Preference - warn users for large remote data requests */
    public static final String PREF_LARGE_REMOTE_DATA_WARN =
            IdvConstants.PREF_LARGE_REMOTE_DATA_WARN;
    /** grid size */
    public static final String PROP_GRIDSIZE = "prop.gridsize";
    /** property timesize */
    public static final String PROP_TIMESIZE = "prop.timesize";
    /** property time variable */
    public static final String PROP_TIMEVAR = "timeVariable";
    /** Throw an error when loading a grid bigger than this in megabytes */
    //private static final int SIZE_THRESHOLD = 500000000;
    //Note that 60 MB is twice the limit of image data from ADDE
    private static final int SIZE_THRESHOLD = 120;
    /** The prefix we hack onto the u and v  variables */
    private static final String PREFIX_GRIDRELATIVE = "GridRelative_";
    /** for test */
    public static boolean testMode = false;
    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
            ucar.unidata.util.LogUtil.getLogInstance(
                    GeoGridDataSource.class.getName());
    /** category attributes */
    private static String[] categoryAttributes = { "GRIB_param_category",
            "Grib2_Parameter_Category" };
    /** for test */
    private static boolean forceSubset = false;
    /** Used to synchronize the geogridadapter */
    protected final Object DOMAIN_SET_MUTEX = new Object();
    /** This is used to synchronize geogrid read access */
    protected final Object readLock = new Object();
    /** the dataset */
    private DtCoverageDataset dataset;
    /** list of times for this dataset */
    private List myTimes = new ArrayList();
    /** list of levels for this dataset */
    private List myLevels = new ArrayList();
    /** hashtable of coordinate systems and times */
    private Hashtable gcsVsTime = new Hashtable();
    /** Keep track of each data choices times */
    private Hashtable timeMap = new Hashtable();
    /** The first projection we find */
    private ProjectionImpl sampleProjection;
    /** Keep track of the spatial dimensions */
    private String dimensionsLabel;
    /** Keep track of the spatial dimensions */
    private String threeDDimensionsLabel = null;
    /** Keep track of the spatial dimensions */
    private String twoDDimensionsLabel = null;
    /** Keep track of the max grid size */
    private int max3DX;
    /** Keep track of the max grid size */
    private int max3DY;
    /** Keep track of the max grid size */
    private int max3DZ;
    /** Keep track of the max grid size */
    private int max3D;
    /** Do we really reverse the time indices */
    private boolean reverseTimes = false;
    /** for properties */
    private JCheckBox reverseTimesCheckbox;
    /** for zidv */
    private CalendarDateRange dateRange;
    /** old resolver URL */
    private String oldResolverUrl;


    /**
     * Default constructor
     */
    public GridCoverageDataSource() {}



    /**
     * Construct a GeoGridDataSource
     *
     * @param descriptor   the data source descriptor
     * @param gds          The GridDataset
     * @param name         A name
     * @param filename     the filename
     */
    public GridCoverageDataSource(DataSourceDescriptor descriptor,
                                  DtCoverageDataset gds, String name, String filename) {
        super(descriptor, filename, name, (Hashtable) null);
        dataset = gds;
    }


    /**
     * Create a GeoGridDataSource from the GridDataset
     *
     * @param gds  the GridDataset
     */
    public GridCoverageDataSource(DtCoverageDataset gds) {
        dataset = gds;
    }


    /**
     * Create a GridCoverageDataSource from a File.
     *
     * @param descriptor   Describes this data source, has a label etc.
     * @param file         This is the file that points to the actual
     *                     data source.
     * @param properties   General properties used in the base class
     *
     * @throws IOException  problem opening file
     */
    public GridCoverageDataSource(DataSourceDescriptor descriptor, File file,
                                  Hashtable properties)
            throws IOException {
        this(descriptor, file.getPath(), properties);
    }


    /**
     * Create a GeoGridDataSource from the filename.
     *
     * @param descriptor   Describes this data source, has a label etc.
     * @param filename     This is the filename (or url) that points
     *                     to the actual data source.
     * @param properties   General properties used in the base class
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public GridCoverageDataSource(DataSourceDescriptor descriptor,
                             String filename, Hashtable properties)
            throws IOException {
        //Make sure we pass filename up here - as opposed to calling
        //this (new File (filename)) because the new File (filename).getPath () != filename
        super(descriptor, filename, "Geogrid data source", properties);
    }


    /**
     * Create a GeoGridDataSource from the filename.
     *
     * @param descriptor   Describes this data source, has a label etc.
     * @param files List of files or urls
     * @param properties   General properties used in the base class
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public GridCoverageDataSource(DataSourceDescriptor descriptor, List files,
                             Hashtable properties)
            throws IOException {
        //Make sure we pass filename up here - as opposed to calling
        //this (new File (filename)) because the new File (filename).getPath () != filename
        super(descriptor, files, "Geogrid data source", properties);
    }

    /**
     * check if a input is only numeric number
     *
     * @param str  is this numeric
     *
     * @return true if numeric
     */
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Test this class by running
     * "java ucar.unidata.data.grid.GeoGridDataSource <filename>"
     *
     * @param args  filename
     *
     * @throws Exception  some error occurred
     */
    public static void main(String[] args) throws Exception {

        // dead dataset...
        String leadUrl =
                "dods://lead.unidata.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20071002_1200.grib1";

        // dead dataset...
        String mlodeUrl =
                "dods://motherlode.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20071002_1200.grib1";
        String   url  = ((args.length == 0)
                ? leadUrl
                : mlodeUrl);

        String[] urls = { url };
        testMode = true;

        for (int i = 0; i < 10000; i++) {
            for (int urlIdx = 0; urlIdx < urls.length; urlIdx++) {
                System.err.println("Reading data:" + i + " " + urls[urlIdx]);
                GridCoverageDataSource ggds = new GridCoverageDataSource(null,
                        urls[urlIdx], null);
                ggds.doMakeDataChoices();
                DataChoice dataChoice = ggds.findDataChoice("Temperature");
                if (dataChoice == null) {
                    dataChoice = ggds.findDataChoice("T");
                }
                //                System.err.println ("" + dataChoice.getProperties());
                ggds.makeFieldImpl(dataChoice, ggds.getDataSelection(), null);
            }
        }



    }

    /**
     * for test
     *
     * @param s string to format
     *
     * @return formatted string
     */
    private static String preText(String s) {
        s = StringUtil.replace(s, "<", "&lt;");
        s = StringUtil.replace(s, ">", "&gt;");
        s = StringUtil.replace(s, "\n", "<br>\n");
        s = StringUtil.replace(s, "\t", "    ");
        s = StringUtil.replace(s, " ", "&nbsp;");
        return "<div style=\"margin-left:20;border:solid 1px\">" + s
                + "</div>";
    }

    /**
     * get max/min
     *
     * @return double value of min/max
     */
    static private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
        double midpoint = (lon1 + lon2) / 2;
        lon1 = LatLonPointImpl.lonNormal(lon1, midpoint);
        lon2 = LatLonPointImpl.lonNormal(lon2, midpoint);

        return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
    }

    /**
     * make coverage
     *
     * @return coverage dataset
     */
    private static Coverage makeCoverage(DtCoverage dt, DtCoverageAdapter reader) {
        return new Coverage(dt.getName(), dt.getDataType(), dt.getAttributes(), dt.getCoordinateSystem().getName(),
                dt.getUnitsString(), dt.getDescription(), reader, dt);
    }

    /**
     * Set the default selection bounds
     *
     * @param rect rectangle
     */
    public void setDefaultSelectionBounds(Rectangle2D.Float rect) {
        getDataSelection().getGeoSelection(true).setLatLonRect(rect);
    }

    /**
     * Load any subset info in field mask xml
     *
     * @param root xml root
     */
    protected void applyFieldMask(Element root) {
        super.applyFieldMask(root);
        GeoSelection geoSubset = getDataSelection().getGeoSelection();
        if (geoSubset == null) {
            geoSubset = new GeoSelection();
            getDataSelection().setGeoSelection(geoSubset);
        }
        Element stride = XmlUtil.getElement(root, "stride");
        if (stride != null) {
            geoSubset.setXStride(XmlUtil.getAttribute(stride, ATTR_X,
                    geoSubset.getXStride()));
            geoSubset.setYStride(XmlUtil.getAttribute(stride, ATTR_Y,
                    geoSubset.getYStride()));
            geoSubset.setZStride(XmlUtil.getAttribute(stride, ATTR_Z,
                    geoSubset.getZStride()));
        }
        Element subset = XmlUtil.getElement(root, "subset");
        if (subset != null) {
            geoSubset.setBoundingBox(
                    new GeoLocationInfo(
                            XmlUtil.getAttribute(subset, ATTR_NORTH, 0.0),
                            XmlUtil.getAttribute(subset, ATTR_WEST, 0.0),
                            XmlUtil.getAttribute(subset, ATTR_SOUTH, 0.0),
                            XmlUtil.getAttribute(subset, ATTR_EAST, 0.0)));
        }

    }

    /**
     * Can we mask the data?
     *
     * @return  true if we can
     */
    protected boolean canDoFieldMask() {
        return true;
    }

    /**
     * Write out the field mask file
     *
     * @param doc   doc to write to
     * @param root  root node
     */
    protected void writeFieldMaskFile(Document doc, Element root) {
        GeoSelection geoSubset = getDataSelection().getGeoSelection();
        if (geoSubset != null) {
            Element stride = doc.createElement("stride");
            root.appendChild(stride);
            if (geoSubset.getXStride() > 1) {
                stride.setAttribute(ATTR_X, geoSubset.getXStride() + "");
            }
            if (geoSubset.getYStride() > 1) {
                stride.setAttribute(ATTR_Y, geoSubset.getYStride() + "");
            }
            if (geoSubset.getZStride() > 1) {
                stride.setAttribute(ATTR_Z, geoSubset.getZStride() + "");
            }
            GeoLocationInfo bbox = geoSubset.getBoundingBox();
            if (bbox != null) {
                Element subset = doc.createElement("subset");
                subset.setAttribute(ATTR_NORTH, bbox.getMaxLat() + "");
                subset.setAttribute(ATTR_SOUTH, bbox.getMinLat() + "");
                subset.setAttribute(ATTR_WEST, bbox.getMinLon() + "");
                subset.setAttribute(ATTR_EAST, bbox.getMaxLon() + "");
                root.appendChild(subset);
            }

        }

    }

    /**
     * Initialize if being unpersisted.
     */
    public void initAfterUnpersistence() {
        //Support legacy bundles
        if ( !haveSources() && (oldSourceFromBundles == null)
                && !hasPollingInfo()) {
            oldSourceFromBundles = getName();
        }
        super.initAfterUnpersistence();
        resolvePath();
        //Call getDataset to see if we have a valid file
        getDataset();
    }

    /**
     * Add in the spatial dimensions label
     *
     * @return The subset properties component
     */
    protected JComponent doMakeGeoSubsetPropertiesComponent() {
        JComponent comp = super.doMakeGeoSubsetPropertiesComponent();
        if ((dataset == null) || (dimensionsLabel == null)) {
            return comp;
        }

        JLabel label = new JLabel(dimensionsLabel);
        return GuiUtils.topCenter(GuiUtils.left(GuiUtils.inset(label, 5)),
                comp);
    }

    /**
     * Get the extra label that is shown in the geo-subset panel
     *
     * @return Extra label for geosubset panel
     */
    protected JComponent getExtraGeoSelectionComponent() {
        if (dimensionsLabel == null) {
            return super.getExtraGeoSelectionComponent();
        }
        String tmp = dimensionsLabel;
        int    idx = tmp.indexOf("#");
        if (idx > 0) {
            tmp = tmp.substring(0, idx);
        }
        return new JLabel(tmp);
    }

    /**
     * Add any extra tabs into the properties tab
     *
     * @param tabbedPane The properties tab
     */
    public void addPropertiesTabs(JTabbedPane tabbedPane) {
        super.addPropertiesTabs(tabbedPane);
        if (dataset == null) {
            return;
        }
        int height = 300;
        int width  = 400;

        /*
        JTextArea infoText = new JTextArea();
        infoText.setText(dataset.getInfo());
        infoText.setFont(Font.decode("monospaced"));

        JScrollPane infoScroller = GuiUtils.makeScrollPane(infoText, width, height);
        infoScroller.setPreferredSize(new Dimension(width, height));
        infoScroller.setMinimumSize(new Dimension(width, height));
        tabbedPane.add("Info", GuiUtils.inset(infoScroller, 5));
        */


        JTextArea    dumpText = new JTextArea();
        TextSearcher searcher = new TextSearcher(dumpText);
        dumpText.setFont(Font.decode("monospaced"));
        //ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StringWriter bos = new StringWriter();
        try {
            ucar.nc2.NCdumpW.print(dataset.getNetcdfFile(), "", bos, null);
        } catch (IOException ioe) {
            logException("Dumping netcdf file", ioe);
        }
        dumpText.setText(bos.toString());
        JScrollPane scroller = GuiUtils.makeScrollPane(dumpText, width,
                height);
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));
        tabbedPane.add("Metadata",
                GuiUtils.inset(GuiUtils.centerBottom(scroller,
                        searcher), 5));
    }

    /**
     * Initialize after we have been created.
     */
    public void initAfterCreation() {
        super.initAfterCreation();
        resolvePath();
        //Call getDataset to see if we have a valid file
        getDataset();
    }

    /**
     * What should be changed by the user when in data relative mode
     *
     * @return paths to changed
     */
    public List getDataPaths() {
        String resolverUrl = (String) getProperty(PROP_RESOLVERURL);
        if ((resolverUrl != null) && (resolverUrl.length() > 0)) {
            return Misc.newList(resolverUrl);
        }
        return super.getDataPaths();
    }

    /**
     * Update the state
     *
     * @param newObject new object
     * @param newProperties  properties
     */
    public void updateState(Object newObject, Hashtable newProperties) {
        removeProperty(PROP_RESOLVERURL);
        super.updateState(newObject, newProperties);
    }

    /**
     * Set what the user has changed
     *
     * @param paths new paths
     */
    public void setTmpPaths(List paths) {
        //TODO: Figure out what to do here
        String resolverUrl = (String) getProperty(PROP_RESOLVERURL);
        oldResolverUrl = resolverUrl;
        if (((paths != null) && (paths.size() > 0)) && (resolverUrl != null)
                && (resolverUrl.length() > 0)) {
            Hashtable properties = getProperties();
            if (properties == null) {
                properties = new Hashtable();
            }
            String firstone = paths.get(0).toString();
            //If we are being saved as a zidv then we remove the resolverurl
            if (firstone
                    .indexOf(ucar.unidata.idv.IdvPersistenceManager
                            .PROP_ZIDVPATH) >= 0) {
                getProperties().remove(PROP_RESOLVERURL);
            } else {
                String resolvedUrl = CatalogUtil.resolveUrl(firstone,
                        properties);
                if (resolvedUrl != null) {
                    setProperty(PROP_RESOLVERURL, firstone);
                }
            }
        }
        super.setTmpPaths(paths);
    }

    /**
     * Resolve the url if we have to
     */
    protected void resolvePath() {
        //Do we have a resolver
        String resolverUrl = (String) getProperty(PROP_RESOLVERURL);
        if ((resolverUrl != null) && (resolverUrl.length() > 0)) {
            Hashtable properties = getProperties();
            if (properties == null) {
                properties = new Hashtable();
            }
            String resolvedUrl = CatalogUtil.resolveUrl(resolverUrl,
                                     properties);
            if (resolvedUrl == null) {
                setInError(true);
                return;
            }
            //            System.err.println("    got resolved path:" + resolvedUrl);
            sources = Misc.newList(resolvedUrl);
        }
    }

    /**
     * The source has changed
     */
    protected void sourcesChanged() {
        dataset   = null;
        gcsVsTime = new Hashtable();
        super.sourcesChanged();
    }

    /**
     * Clear out the data set
     */
    public void reloadData() {
        myTimes   = null;
        dataset   = null;
        gcsVsTime = new Hashtable();
        resolvePath();
        dataChoices = null;
        //        doMakeDataChoices();
        getDataChoices();
        super.reloadData();

        /**
         *  not sure if we want to do this since we might have
         *  cachedflatfields out there that are pointing at the old
         *  directory
         *  clearFileCache();
         */

    }

    /**
     * Called when Datasource is removed.
     */
    public void doRemove() {
        super.doRemove();
        try {
            if (dataset != null) {
                dataset.close();
            }
        } catch (IOException ioe) {}
        dataset   = null;
        gcsVsTime = null;
    }

    /**
     * Can this DataSource save data to local disk?
     *
     * @return true if this DataSource can save data to local disk?
     */
    public boolean canSaveDataToLocalDisk() {
        if (isFileBased()) {
            //            return false;
        }
        return true;
    }

    /**
     * Get the label for the save data file option
     *
     * @return label
     */
    protected String getSaveDataFileLabel() {
        return (isFileBased()
                ? "Writing grid file"
                : getSaveDataFileLabel());
    }

    /**
     * Make savel local actions
     *
     * @param actions list of actions
     */
    protected void makeSaveLocalActions(List actions) {
        String         lbl = (isFileBased()
                ? "Subset and Write Grid"
                : "Write Local Grid");
        AbstractAction a   = new AbstractAction(lbl) {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(new Runnable() {
                    public void run() {
                        try {
                            saveDataToLocalDisk();
                        } catch (Exception exc) {
                            logException("Writing data to local disk", exc);
                        }
                    }
                });
            }
        };
        actions.add(a);
    }

    /**
     * Overwrite this method so we don't show the loading dialog
     *
     * @param msg The msg to show in the dialog
     *
     * @return The jobmanager loadid
     */
    protected Object beginWritingDataToLocalDisk(String msg) {
        final Object loadId = JobManager.getManager().startLoad(msg, false,
                                  false);
        return loadId;
    }

    /**
     * Save the data to local disk.
     *
     *
     * @param prefix  the prefix for the local file name
     * @param loadId  the load id (for cancelling)
     * @param changeLinks true to change the links
     *
     * @return The list of files
     *
     * @throws Exception  problem saving data.
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {


        List                  choices            = getDataChoices();
        final List<JCheckBox> checkboxes         = new ArrayList<JCheckBox>();
        List                  categories         = new ArrayList();
        Hashtable             catMap             = new Hashtable();
        Hashtable             currentDataChoices = new Hashtable();


        List                  displays = getIdv().getDisplayControls();
        for (int i = 0; i < displays.size(); i++) {
            List dataChoices =
                    ((DisplayControl) displays.get(i)).getDataChoices();
            if (dataChoices == null) {
                continue;
            }
            List finalOnes = new ArrayList();
            for (int j = 0; j < dataChoices.size(); j++) {
                ((DataChoice) dataChoices.get(j)).getFinalDataChoices(
                        finalOnes);
            }
            for (int dcIdx = 0; dcIdx < finalOnes.size(); dcIdx++) {
                DataChoice dc = (DataChoice) finalOnes.get(dcIdx);
                if ( !(dc instanceof DirectDataChoice)) {
                    continue;
                }
                DirectDataChoice ddc = (DirectDataChoice) dc;
                if (ddc.getDataSource() != this) {
                    continue;
                }
                currentDataChoices.put(ddc.getName(), "");
            }
        }

        if (getDefaultSave()) {
            List varNames = new ArrayList();

            for (int i = 0; i < dataChoices.size(); i++) {
                DataChoice dataChoice = (DataChoice) dataChoices.get(i);
                if ( !(dataChoice instanceof DirectDataChoice)) {
                    continue;
                }
                String name = dataChoice.getName();
                //hack, hack, hack,
                if (name.startsWith(PREFIX_GRIDRELATIVE)) {
                    name = name.substring(PREFIX_GRIDRELATIVE.length());
                }
                if (currentDataChoices.get(name) != null) {
                    varNames.add(name);
                }
            }
            return (currentDataChoices.size() > 0)
                    ? writeNc(prefix, changeLinks, varNames)
                    : null;
        }


        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if ( !(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            String label = dataChoice.getDescription();

            JCheckBox cbx =
                    new JCheckBox(label,
                            currentDataChoices.get(dataChoice.getName())
                                    != null);
            ThreeDSize size =
                    (ThreeDSize) dataChoice.getProperty(PROP_GRIDSIZE);
            cbx.setToolTipText(dataChoice.getName());
            checkboxes.add(cbx);
            DataCategory dc    = dataChoice.getDisplayCategory();
            List         comps = (List) catMap.get(dc);
            if (comps == null) {
                comps = new ArrayList();
                catMap.put(dc, comps);
                categories.add(dc);
            }
            comps.add(cbx);
            comps.add(GuiUtils.filler());
            if (size != null) {
                JLabel sizeLabel = GuiUtils.rLabel(size.getSize() + "  ");
                sizeLabel.setToolTipText(size.getLabel());
                comps.add(sizeLabel);
            } else {
                comps.add(new JLabel(""));
            }
        }

        final JCheckBox allCbx = new JCheckBox("Select All");
        allCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (JCheckBox cbx : checkboxes) {
                    cbx.setSelected(allCbx.isSelected());
                }
            }
        });
        List        catComps = new ArrayList();
        JTabbedPane tab      = new JTabbedPane(JTabbedPane.LEFT);

        for (int i = 0; i < categories.size(); i++) {
            List comps = (List) catMap.get(categories.get(i));
            JPanel labelPanel = new JPanel(new BorderLayout());
            JPanel leftLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            leftLabelPanel.add(new JLabel("Field Name"));
            JPanel rightLabelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            rightLabelPanel.add(new JLabel("Grid Size (Points)"));
            labelPanel.add(leftLabelPanel, BorderLayout.WEST);
            labelPanel.add(rightLabelPanel, BorderLayout.EAST);

            JPanel innerPanel = GuiUtils.doLayout(comps, 3, GuiUtils.WT_NYN,
                                    GuiUtils.WT_N);

            JScrollPane sp = new JScrollPane(GuiUtils.top(innerPanel));
            sp.setPreferredSize(new Dimension(500, 400));

            // TJJ Nov 2015 - keep scrollpane and label panel separate so
            // labels are always visible
            JPanel spAndLabels = new JPanel(new BorderLayout());
            spAndLabels.add(labelPanel, BorderLayout.NORTH);
            spAndLabels.add(sp, BorderLayout.CENTER);
            JComponent inner = GuiUtils.inset(GuiUtils.center(spAndLabels), 5);
            tab.addTab(categories.get(i).toString(), inner);
        }

        //        JComponent contents = GuiUtils.hbox(catComps);
        JComponent contents = tab;
        contents = GuiUtils.topCenter(
                GuiUtils.inset(
                        GuiUtils.leftRight(
                                new JLabel("Select the fields to download"),
                                allCbx), 5), contents);
        JLabel label = new JLabel(getNameForDataSource(this, 50, true));
        contents = GuiUtils.topCenter(label, contents);
        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "", contents, null)) {
            return null;
        }


        List varNames = new ArrayList();
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if ( !(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            JCheckBox cbx = (JCheckBox) checkboxes.get(i);
            if ( !cbx.isSelected()) {
                continue;
            }
            String name = dataChoice.getName();
            //hack, hack, hack,
            if (name.startsWith(PREFIX_GRIDRELATIVE)) {
                name = name.substring(PREFIX_GRIDRELATIVE.length());
            }
            varNames.add(name);
        }
        if (varNames.size() == 0) {
            return null;
        }

        return writeNc(prefix, changeLinks, varNames);
    }

    /**
     * Write netCDF file.
     *
     * @param prefix  the prefix for the local file name
     * @param changeLinks true to change the links
     * @param varNames the var names to write
     *
     * @return The list of files
     */
    private List writeNc(String prefix, boolean changeLinks, List varNames) {
        Object       loadId;
        LatLonRect   llr           = null;
        int          hStride       = 1;
        int          zStride       = 1;
        int          timeStride    = 1;
        GeoSelection geoSubset     = getDataSelection().getGeoSelection();
        boolean      includeLatLon = false;

        if (geoSubset != null) {
            if (geoSubset.getBoundingBox() != null) {
                llr = geoSubset.getBoundingBox().getLatLonRect();
            }
            if (geoSubset.hasXStride()) {
                hStride = geoSubset.getXStride();
            }
            if (geoSubset.hasZStride()) {
                zStride = geoSubset.getZStride();
            }
        }

        if(getProperties().get(PROP_TIMESTRIDE) != null){
            Object tstrike = getProperties().get(PROP_TIMESTRIDE);
            timeStride = ((Integer)tstrike).intValue();
        }

        if(dateRange == null) {
            List times = getTimesFromDataSelection(getDataSelection(),
                    (DataChoice) getDataChoices().get(0));
            try {
                if ((getDataSelection() != null) && !times.isEmpty()) {
                    CalendarDateTime t0 =
                            new CalendarDateTime((DateTime) times.get(0));
                    CalendarDate dt0 = t0.getCalendarDate();
                    CalendarDateTime t1 =
                            new CalendarDateTime((DateTime) times.get(times.size() - 1));
                    CalendarDate dt1 = t1.getCalendarDate();
                    dateRange = CalendarDateRange.of(dt0, dt1);
                }
            } catch (Exception e) {}
        }
        // if geoSubset is null or no bbx
        //if (llr == null) {
        //  llr = dataset.getBoundingBox();
        //}

        String           path         = prefix;
        CFGridCoverageWriter2    writer       = new CFGridCoverageWriter2();
        NetcdfFileWriter ncFileWriter = null;
        try {
            ncFileWriter = NetcdfFileWriter.createNew(Version.netcdf3, path);
            ncFileWriter.setLargeFile(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Start the load, showing the dialog
        loadId = JobManager.getManager().startLoad("Copying data", true,
                true);
        SubsetParams subsetParams = new SubsetParams();
        subsetParams.setLatLonBoundingBox(llr);
        try {
            // ucar.nc2.dt.GridDataset gds,
            // List<String> gridList,
            // LatLonRect llbb,
            // ProjectionRect projRect,
            // int horizStride,
            // Range zRange,
            // CalendarDateRange dateRange,
            // int stride_time,
            // boolean addLatLon,
            // NetcdfFileWriter writer

            FeatureDatasetCoverage cc = DtCoverageAdapter.factory(dataset, null);
            CFGridCoverageWriter2.write(cc.getSingleCoverageCollection() , varNames, subsetParams, false, ncFileWriter);
        } catch (Exception exc) {
            logException("Error writing local netcdf file.\nData:"
                    + getFilePath() + "\nVariables:" + varNames, exc);
            return null;
        } finally {
            JobManager.getManager().stopLoad(loadId);
        }


        if (geoSubset != null && changeLinks) {
            geoSubset.clearStride();
            geoSubset.setBoundingBox(null);
            if (geoSelectionPanel != null) {
                geoSelectionPanel.initWith(doMakeGeoSelectionPanel());
            }
        }

        List newFiles = Misc.newList(path);
        if (changeLinks) {
            //Get rid of the resolver URL
            getProperties().remove(PROP_RESOLVERURL);
            setNewFiles(newFiles);
        }
        return newFiles;
    }

    /**
     * Are we a local file
     *
     * @return is a local file
     */
    public boolean isLocalFile() {
        return new File(getFilePath()).exists();
    }

    /**
     *  Overwrite setNewFiles so we clear out the resolverurl
     *
     * @param files The list of new files to use
     */
    public void setNewFiles(List files) {
        getProperties().remove(PROP_RESOLVERURL);
        super.setNewFiles(files);
    }

    /**
     * Get the local directory
     *
     * @param label   a label
     * @param prefix  the prefix
     *
     * @return the path
     */
    protected String getLocalDirectory(String label, String prefix) {
        changeDataPathsCbx.setToolTipText(
                "Should this data source also be changed");
        return FileManager.getWriteFile(FileManager.FILTER_NETCDF, null,
                GuiUtils.top(changeDataPathsCbx));
    }

    /**
     * Get the full description of the grid
     *
     * @return  the description
     */
    public String getFullDescription() {
        String       desc        = super.getFullDescription();
        StringBuffer sb2d        = null;
        StringBuffer sb3d        = null;
        List         dataChoices = getDataChoices();
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if ( !(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            ThreeDSize size =
                    (ThreeDSize) dataChoice.getProperty(PROP_GRIDSIZE);
            Integer timeSize =
                    (Integer) dataChoice.getProperty(PROP_TIMESIZE);
            if (size != null) {
                long         total     = size.getSizeY() * size.getSizeX();
                StringBuffer theSb     = null;
                String       sizeEntry = null;
                if (size.getSizeZ() > 1) {
                    if (sb3d == null) {
                        sb3d = new StringBuffer();
                    }
                    total *= size.getSizeZ();
                    theSb = sb3d;
                    sizeEntry = size.getSizeX() + "x" + size.getSizeY() + "x"
                            + size.getSizeZ();
                } else {
                    if (sb2d == null) {
                        sb2d = new StringBuffer();
                    }
                    theSb     = sb2d;
                    sizeEntry = size.getSizeX() + "x" + size.getSizeY();
                }
                theSb.append("<tr><td>" + dataChoice.getName() + "</td><td>"
                        + dataChoice.getDescription() + "</td><td>"
                        + sizeEntry + "</td><td>");
                if (timeSize != null) {
                    int times = timeSize.intValue();
                    if (times > 0) {
                        total *= timeSize.intValue();
                        theSb.append("" + timeSize);
                    }
                }
                theSb.append("</td>");
                theSb.append("<td>" + total + "</td></tr>");
            }
        }
        StringBuffer sb = null;
        if ((sb2d != null) || (sb3d != null)) {
            sb = new StringBuffer(desc);
            String resolverUrl = (String) getProperty(PROP_RESOLVERURL);
            if (resolverUrl != null) {
                sb.append("<p>");
                sb.append("Resolver URL:" + resolverUrl);
            }
            sb.append(
                    "\n<p><table><tr><td><b>Field</b></td><td><b>Description</b></td><td><b>Dimensions</b></td><td><b>#Times</b></td><td><b>#Points</b></td></tr>\n");
        }
        if (sb2d != null) {
            sb.append(sb2d);
        }
        if (sb3d != null) {
            sb.append(sb3d);
        }
        if (sb != null) {
            sb.append("</table>\n");
        }

        if ((sb != null) && (myLevels != null) && (myLevels.size() > 0)) {
            sb.append("<h2>Levels</h2>");
            for (Object o : myLevels) {
                sb.append("" + o);
                sb.append("<br>");
            }
        }


        if (sb == null) {
            return desc;
        }
        return sb.toString();
    }

    /**
     * Reset the tmp state
     */
    public void resetTmpState() {
        super.resetTmpState();
        if (oldResolverUrl != null) {
            setProperty(PROP_RESOLVERURL, oldResolverUrl);
        }


    }

    /**
     * Create the dataset from the name of this DataSource.
     *
     * @return new GridDataset
     */
    protected DtCoverageDataset doMakeDataSet() {
        checkForInitAfterUnPersistence();
        String file = getFilePath();
        if (file == null) {
            if (haveBeenUnPersisted) {
                file = getName();
            }
        }
        if (file == null) {
            return null;
        }
        if (sources == null) {
            sources = new ArrayList();
            sources.add(file);
        }


        //Make sythetic data ncml file
        if (sources.size() > 1) {
            String       timeName = getProperty(PROP_TIMEVAR, "time");
            StringBuffer sb       = new StringBuffer();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append(
                    "<netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n");
            sb.append("<aggregation type=\"joinExisting\" dimName=\""
                    + timeName + "\" timeUnitsChange=\"true\">\n");
            for (int i = 0; i < sources.size(); i++) {
                String s = sources.get(i).toString();

                try {
                    if (s.startsWith("http") && s.endsWith("entry.das")) {  // opendap from ramadda
                        s = DODSNetcdfFile.canonicalURL(s);
                        sb.append(XmlUtil.tag("netcdf",
                                XmlUtil.attrs("location", s, "enhance",
                                        "true"), ""));
                    } else {
                        sb.append(XmlUtil.tag("netcdf",
                                XmlUtil.attrs("location",
                                        IOUtil.getURL(s, getClass()).toString(),
                                        "enhance", "true"), ""));
                    }
                } catch (IOException ioe) {
                    setInError(true);
                    throw new WrapperException(
                            "Grid data source failed aggregating resource: " + s,
                            ioe);
                }
            }
            sb.append("</aggregation>\n</netcdf>\n");
            file = getDataContext().getObjectStore().getUniqueTmpFile(
                    "multigrid_" + UUID.randomUUID().toString(), ".ncml");
            try {
                IOUtil.writeFile(file, sb.toString());
            } catch (IOException ioe) {
                logException("Unable to write file: " + file, ioe);
                return null;
            }
            log_.debug("" + sb);
        }

        try {
            file = convertSourceFile(file);
            Trace.msg("GeoGridDataSource: opening file " + file);
            if (file.startsWith("http") && file.endsWith("entry.das")) {  // opendap from ramadda
                file = DODSNetcdfFile.canonicalURL(file);
            } else if(file.startsWith("http") && file.contains("/dods/")){
                file = DODSNetcdfFile.canonicalURL(file);
            } else if(file.startsWith("dods:") && file.endsWith("ncml")){
                file = file.replace("dods:","https:");
            }
            if (file.contains(":443")) {
                file = file.replace(":443","");
            }
            DtCoverageDataset gds = DtCoverageDataset.open(file);
            return gds;
        } catch (java.io.FileNotFoundException fnfe) {
            setInError(true);
            LogUtil.consoleMessage("Original error:\n" + fnfe.toString()
                    + "\n" + LogUtil.getStackTrace(fnfe));
            throw new BadDataException("Unable to open grid:\n" + file);
        } catch (Exception exc) {
            setInError(true);
            throw new WrapperException(
                    "Grid data source failed making data set: " + file, exc);
        }
    }

    /**
     * Return the GridDataset associated with this DataSource.
     *
     * @return dataset
     */
    public DtCoverageDataset getDataset() {
        if (dataset == null) {
            Trace.call1("GeoGridDataSource.getDataSet", " name = " + sources);
            dataset = doMakeDataSet();
            Trace.call2("GeoGridDataSource.getDataSet");
        }
        return dataset;
    }

    /**
     * Return the sample projection
     *
     * @return the sample projection
     */
    protected ProjectionImpl getSampleDataProjection() {
        if(sampleProjection instanceof LatLonProjection){
            LatLonProjection ll = (LatLonProjection)sampleProjection;
            ((LatLonProjection)sampleProjection).setCenterLon(0.0);
            //   System.out.println("jjj");
        }
        return sampleProjection;
    }

    /**
     * This method is called at initialization time and
     * creates a set of {@link ucar.unidata.data.DirectDataChoice}-s
     * and adds them into the base class managed list of DataChoice-s
     * with the method addDataChoice.
     */
    protected void doMakeDataChoices() {

        DtCoverageDataset myDataset = getDataset();
        if (myDataset == null) {
            return;
        }
        max3DX = -1;
        max3DY = -1;
        max3DZ = -1;
        max3D  = -1;

        boolean       gridRelativeWind = false;


        Iterator iter = myDataset.getGrids().iterator();
        SortedSet uniqueTimes =
                Collections.synchronizedSortedSet(new TreeSet());


        while (iter.hasNext()) {
            DtCoverage cfield = (DtCoverage) iter.next();
            if (sampleProjection == null) {
                sampleProjection = cfield.getCoordinateSystem().getProjection();
                //                System.err.println ("The sample projection is:" + sampleProjection);
            }
            //      System.out.println("llr:" + cfield.getProjection().getDefaultMapAreaLL());
            DtCoverageCS gcs   = cfield.getCoordinateSystem();
            CoordinateAxis1D zaxis = gcs.getVerticalAxis() ;
            if (  zaxis == null) {
                continue;
            }
            CoordinateAxis1DTime tAxis    = (CoordinateAxis1DTime)gcs.getTimeAxis();
            if(tAxis == null){
                //CoordinateAxis t0Axis = gcs.getTimeAxis();
                CoordinateAxis1DTime trAxis = gcs.getRunTimeAxis();
                if(trAxis.getAxisType() == AxisType.RunTime){
                    tAxis = trAxis;
                }
            }

            List geoTimes = getGeoGridTimes(tAxis);
            uniqueTimes.addAll(geoTimes);

        }

        if ( !uniqueTimes.isEmpty()) {
            myTimes = new ArrayList(uniqueTimes);
        } else {
            myTimes = new ArrayList();
        }


        DataChoice choice;
        // for each GeoGridImpl in the dataset
        iter = myDataset.getGrids().iterator();
        Hashtable timeToIndex = new Hashtable();
        for (int i = 0; i < myTimes.size(); i++) {
            timeToIndex.put(myTimes.get(i), new Integer(i));
        }
        int cnt = 0;
        while (iter.hasNext()) {
            DtCoverage cfield = (DtCoverage) iter.next();
            choice = makeDataChoiceFromGeoGrid(cfield, myTimes, timeToIndex);
            if (choice != null) {
                cnt++;
                if (gridRelativeWind == true) {
                    if ((choice.getDescription().indexOf("u-component") >= 0)
                            || (choice.getDescription().indexOf(
                            "v-component") >= 0)) {
                        choice.setName(PREFIX_GRIDRELATIVE
                                + choice.getName());
                    }
                }
                addDataChoice(choice);
            }
        }

        //Check if we found any grids
        if (cnt == 0) {
            if (LogUtil.getInteractiveMode()
                    && GuiUtils.showOkCancelDialog(null, "No Gridded Data",
                    GuiUtils.inset(new JLabel("<html>No gridded data found for:<br><br>&nbsp;&nbsp;<i>"
                            + this
                            + "</i><br><br>Do you want to try to load this as another data type?</html>"), 5), null)) {
                getIdv().getDataManager().createDataSourceAndAskForType(
                        getFilePath(), getProperties());
                setInError(true, false, "");
            } else {
                //For now just bail out
                setInError(true, false, "");
            }
            return;
        }

        if (max3D > 0) {
            threeDDimensionsLabel = "Max grid size: x: " + max3DX + " y: "
                    + max3DY + " z: " + max3DZ
                    + "   #points: "
                    + (max3DX * max3DY * max3DZ);
        }


        if (threeDDimensionsLabel != null) {
            dimensionsLabel = threeDDimensionsLabel;
        } else {
            dimensionsLabel = twoDDimensionsLabel;
        }




    }

    /**
     * Get the Data object specified by the particular selection criteria.
     *
     * @param dataChoice          DataChoice to select.
     * @param category            DataCategory (unused at present)
     * @param givenDataSelection  DataSelection criteria for this request.
     * @param requestProperties   extra request selection properties (not used
     *                            in this class)
     *
     * @return  Data object corresponding to the data choice
     *
     * @throws VisADException  couldn't create Data object
     * @throws RemoteException  couldn't create remote Data object
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection givenDataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        //        synchronized (readLock) {
        //        System.err.println("getData:" + getFilePath() +" field="+dataChoice);
        boolean isPR = givenDataSelection.getProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION, false);
        boolean fromBundle = getIdv().getStateManager().getProperty(
                IdvConstants.PROP_LOADINGXML, false);
        if (isPR && fromBundle) {
            // ucar.unidata.geoloc.LatLonPoint[] llp0 =  givenDataSelection.getGeoSelection().getRubberBandBoxPoints();
            GeoLocationInfo gInfo =
                    givenDataSelection.getGeoSelection().getBoundingBox();
            if (gInfo != null) {
                //GeoLocationInfo gInfo1 = new GeoLocationInfo(
                //        llp0[0].getLatitude(), llp0[0].getLongitude(),
                //        llp0[1].getLatitude(), llp0[1].getLongitude());
                givenDataSelection.getGeoSelection().setBoundingBox(gInfo);
            }
        }
        Data data = makeFieldImpl(dataChoice, givenDataSelection,
                requestProperties);

        return data;
        //        }
    }

    /**
     * Get the list of parameters
     *
     * @return  the list
     */
    public List<String> listParameters() {
        List<String> params = new ArrayList<String>();
        for (DataChoice dc : (List<DataChoice>) getDataChoices()) {
            params.add(dc.getName());
        }
        return params;
    }

    /**
     * Get the data for a particular parameter
     *
     * @param parameter  the parameter name
     *
     * @return  the Data or null
     *
     * @throws RemoteException Java RMI Error
     * @throws VisADException  VisAD Error
     */
    public Data getData(String parameter)
            throws VisADException, RemoteException {
        DataChoice dataChoice = findDataChoice(parameter);
        if (dataChoice == null) {
            return null;
        }
        DataSelection dataSelection = new DataSelection();
        //      dataSelection.setTimes(Misc.newList(new Integer(0)));
        Data data = makeFieldImpl(dataChoice, dataSelection, new Hashtable());
        return data;
        //        }
    }

    /**
     * Return the list of DateTime-s associated with this DataSource.
     *
     * @return  List of DateTime-s.
     */
    protected List doMakeDateTimes() {
        return myTimes;
    }

    /**
     * Get the list of all levels available from this DataSource
     *
     *
     * @param dataChoice The data choice we are getting levels for
     * @param dataSelection  the data selection
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice,
                             DataSelection dataSelection) {
        try {
            dataSelection = DataSelection.merge(dataSelection,
                    getDataSelection());
            //            System.err.println("levels:" + dataSelection.getFromLevel());
            Object fromLevel      = dataSelection.getFromLevel();
            Object toLevel        = dataSelection.getToLevel();
            int    fromLevelIndex = -1;
            int    toLevelIndex   = -1;
            if ((fromLevel != null) && (toLevel != null)) {
                long t1 = System.currentTimeMillis();
                List allLevels =
                        getAllLevels(dataChoice,
                                new DataSelection(GeoSelection.STRIDE_BASE));


                long t2 = System.currentTimeMillis();
                //                System.err.println("time 1:" + (t2-t1));
                fromLevelIndex = indexOf(fromLevel, allLevels);
                toLevelIndex   = indexOf(toLevel, allLevels);
            }


            long t1 = System.currentTimeMillis();
            GridCoverageAdapter geoGridAdapter = makeGeoGridAdapter(dataChoice,
                    dataSelection, null,
                    fromLevelIndex, toLevelIndex,
                    true);
            long t2 = System.currentTimeMillis();
            //            System.err.println("time 2:" + (t2-t1));
            if (geoGridAdapter != null) {
                List tmpLevels = geoGridAdapter.getLevels();
                myLevels = tmpLevels;
                return tmpLevels;
            }
            return myLevels;
        } catch (VisADException vae) {
            throw new ucar.unidata.util.WrapperException(vae);
        } catch (HugeSizeException hse) {
            return null;
        }
    }

    /**
     * We can do geo selection in the properties gui
     *
     * @return can do geo selection
     */
    public boolean canDoGeoSelection() {
        return true;
    }

    /**
     * Can this data source cache its
     *
     * @return can cache data to disk
     */
    public boolean canCacheDataToDisk() {
        return true;
    }

    /**
     * Utility to create a new GeoGridAdapter for the given choice and data selection and
     * level indices
     *
     * @param dataChoice The data choice
     * @param givenDataSelection Data selection
     * @param requestProperties request properties
     * @param fromLevelIndex First level index. -1 if it is undefined
     * @param toLevelIndex Second level index. -1 if it is undefined
     * @param forMetaData   true if we are using this to get metadata instead of
     *                      reading data.
     *
     * @return The GeoGridAdapter
     *
     *
     * @throws HugeSizeException _more_
     * @throws VisADException On badness
     */
    private GridCoverageAdapter makeGeoGridAdapter(DataChoice dataChoice,
                                              DataSelection givenDataSelection, Hashtable requestProperties,
                                              int fromLevelIndex, int toLevelIndex, boolean forMetaData)
            throws VisADException, HugeSizeException {

        boolean readingFullGrid = !forMetaData;
        int     numLevels       = -1;
        if ((fromLevelIndex >= 0) && (toLevelIndex >= 0)) {
            numLevels = Math.abs(toLevelIndex - fromLevelIndex) + 1;
        }


        DtCoverageDataset myDataset = getDataset();
        if (myDataset == null) {
            return null;
        }
        Object  extraCacheKey = null;
        DtCoverage geoGrid       = findGridForDataChoice(myDataset, dataChoice);
        FeatureDatasetCoverage dddd = DtCoverageAdapter.factory(myDataset, null);
        CoverageCollection cc = dddd.getCoverageCollections().get(0);
        Coverage geoCoverage = cc.findCoverage(geoGrid.getName());
        SubsetParams geiGridSubsetParams = new SubsetParams();
        geiGridSubsetParams.setLatLonBoundingBox(geoGrid.getCoordinateSystem().getLatLonBoundingBox());

        String  paramName     = dataChoice.getStringId();
        if (geoGrid == null) {
            return null;
        }
        ucar.nc2.Dimension ensDim       = geoGrid.getEnsembleDimension() ;
        GeoSelection       geoSelection = ((givenDataSelection != null)
                ? givenDataSelection
                .getGeoSelection()
                : null);
        List transf = geoGrid.getCoordinateSystem().getCoordTransforms();
        CoordinateAxis1D vertAxis = geoGrid.getCoordinateSystem().getVerticalAxis();

        boolean needVolume =
                ((transf.size() == 2 && transf.get(1) != null)
                        && ((requestProperties != null)
                        && (requestProperties.get(
                        DerivedDataChoice.PROP_FROMDERIVED) != null)));
        //        System.out.println("need volume = " + needVolume + " " + geoGrid.getCoordinateSystem().getVerticalTransform());

        StringBuffer filename     = new StringBuffer("grid_" + paramName);

        String       regionOption = null;

        regionOption =
                givenDataSelection.getProperty(DataSelection.PROP_REGIONOPTION,
                        DataSelection.PROP_USEDEFAULTAREA);
        boolean isProgressiveResolution =
                givenDataSelection.getProperty(
                        DataSelection.PROP_PROGRESSIVERESOLUTION, false);
        boolean matchDisplayRegion = ((geoSelection != null)
                ? geoSelection.getUseViewBounds()
                : false);

        if ( !isProgressiveResolution
                && (dataChoice.getDataSelection() != null)) {
            isProgressiveResolution =
                    dataChoice.getDataSelection().getProperty(
                            DataSelection.PROP_PROGRESSIVERESOLUTION, false);
        }

        try {
            Range ensRange   = makeRange(ensDim, null, 1);
            Range timeRange  = null;
            Range levelRange = null;
            Range xRange     = null;
            Range yRange     = null;
            if ((fromLevelIndex >= 0) && (toLevelIndex >= 0) && !needVolume) {
                levelRange = new Range(fromLevelIndex, toLevelIndex);
                filename.append("_r_" + fromLevelIndex + "_" + toLevelIndex);
            }

            /*    if(geoSelection != null){
                    LatLonPoint[] llp0 = geoSelection.getRubberBandBoxPoints();
                    if(llp0 != null){
                        if(isReload || (this.haveBeenUnPersisted)) {
                            GeoLocationInfo gInfo = new GeoLocationInfo(llp0[0].getLatitude(), llp0[0].getLongitude(),
                                    llp0[1].getLatitude(), llp0[1].getLongitude());
                            geoSelection.setBoundingBox(gInfo);
                        }
                    }
                }
                */

            /** if we are doing PR, then we adjust the stride */
            if (isProgressiveResolution && (geoSelection != null)
                    && !geoSelection.hasStride()) {
                int xLength = geoGrid.getXDimension().getLength();
                int yLength = geoGrid.getYDimension().getLength();


                if (geoSelection.getLatLonRect() != null) {
                    // spatial subset or usedisplayarea
                    LatLonRect gsbox = geoSelection.getLatLonRect();
                    LatLonRect grbox =
                            geoGrid.getCoordinateSystem().getLatLonBoundingBox();
                    LatLonRect bbox;
                    //if(regionOption.equals(DataSelection.PROP_USESELECTEDAREA))
                    if ( !matchDisplayRegion) {
                        bbox = gsbox;
                    } else {
                        bbox = grbox.intersect(gsbox);
                        if (bbox == null) {
                            bbox = grbox;
                        }
                    }
                    List yx_ranges = getRangesFromLatLonRect(geoGrid.getCoordinateSystem(), bbox);

                    yRange = makeRange(geoGrid.getYDimension(),
                            (Range) yx_ranges.get(0), 1);

                    xRange = makeRange(geoGrid.getXDimension(),
                            (Range) yx_ranges.get(1), 1);

                    yLength = yRange.length();
                    xLength = xRange.length();
                    geiGridSubsetParams.setLatLonBoundingBox(bbox);
                }


                Rectangle2D rect = geoSelection.getScreenBound();
                if (rect == null) {
                    rect = dataChoice.getDataSelection().getGeoSelection()
                            .getScreenBound();
                }

                int xstride = calculateStrideFactor(xLength,
                        (int) rect.getWidth());
                int ystride = calculateStrideFactor(yLength,
                        (int) rect.getHeight());

                if (xstride == 1) {
                    xstride = 0;
                }
                if (ystride == 1) {
                    ystride = 0;
                }
                geoSelection.setXStride(xstride);
                geoSelection.setYStride(ystride);
            }
            //System.out.println("new x y strides: " + geoSelection.getXStride() + " "
            //        + geoSelection.getYStride());
            int xStride = (geoSelection != null)
                    ? geoSelection.getXStride()
                    : 1;
            int yStride = (geoSelection != null)
                    ? geoSelection.getYStride()
                    : 1;
            // Set 0 or -1 to be 1
            if (xStride < 1) {
                xStride = 1;
            }
            if (yStride < 1) {
                yStride = 1;
            }

            //geiGridSubsetParams.setHorizStride(xStride);
            String magValue = DataUtil.makeSamplingLabel(xStride, yStride,
                    "grid point");
            dataChoice.setProperty("MAG", magValue);
            if ((geoSelection != null)
                    && (geoSelection.hasSpatialSubset()
                    || geoSelection.getHasNonOneStride())) {
                //TODO: We should determine the size of the subset grid and use that.
                //readingFullGrid = false;
                //System.err.println("subsetting using:" + geoSelection.getLatLonRect());
                extraCacheKey = geoSelection;
                if (levelRange != null) {
                    extraCacheKey = Misc.newList(extraCacheKey, levelRange);
                }
                filename.append("_x_" + geoSelection.getXStrideToUse());
                filename.append("_y_" + geoSelection.getYStrideToUse());
                filename.append("_z_" + geoSelection.getZStrideToUse());

                if (geoSelection.getLatLonRect() != null) {
                    LatLonRect gsbox = geoSelection.getLatLonRect();
                    LatLonRect grbox =
                            geoGrid.getCoordinateSystem().getLatLonBoundingBox();
                    LatLonRect bbox;
                    if ( !matchDisplayRegion) {
                        bbox = gsbox;
                    } else {
                        bbox = grbox.intersect(gsbox);
                        if (bbox == null) {
                            bbox = grbox;
                        }
                    }
                    geiGridSubsetParams.setLatLonBoundingBox(bbox);
                    filename.append("_rect_" + cleanBBoxName(bbox));
                    List yx_ranges = getRangesFromLatLonRect(geoGrid.getCoordinateSystem(), bbox);

                    yRange = makeRange(geoGrid.getYDimension(),
                            (Range) yx_ranges.get(0),
                            geoSelection.getYStrideToUse());
                    xRange = makeRange(geoGrid.getXDimension(),
                            (Range) yx_ranges.get(1),
                            geoSelection.getYStrideToUse());
                } else if (geoSelection.getHasNonOneStride()) {
                    geiGridSubsetParams.setLatLonBoundingBox(geoGrid.getCoordinateSystem().getLatLonBoundingBox());
                    geiGridSubsetParams.setHorizStride(geoSelection.getYStrideToUse());
                    yRange = makeRange(geoGrid.getYDimension(), yRange,
                            geoSelection.getYStrideToUse());
                    xRange = makeRange(geoGrid.getXDimension(), xRange,
                            geoSelection.getYStrideToUse());
                }
                // Z stride is ignored if
                if ((levelRange != null) && geoSelection.hasZStride()
                        && (geoSelection.getZStrideToUse() > 1)) {
                    levelRange = new Range(fromLevelIndex, toLevelIndex,
                            geoSelection.getZStrideToUse());
                }

                //                System.out.println("level range(1):  " + levelRange);
                if(levelRange != null) {
                    double startL = vertAxis.getCoordValue(fromLevelIndex);
                    double endL = vertAxis.getCoordValue(toLevelIndex);
                    geiGridSubsetParams.setVertCoordIntv(new double[]{startL, endL});
                }
                //geoGrid = (Coverage) geoGrid.makeSubset(null, ensRange, null,
                 //       levelRange, yRange, xRange);
            } else if (levelRange != null) {
                extraCacheKey = levelRange;
                //                System.out.println("level range(2):  " + levelRange);
                //geoGrid = geoGrid.subset(null, levelRange, null, null);
                double startL = vertAxis.getCoordValue(fromLevelIndex);
                double endL = vertAxis.getCoordValue(toLevelIndex);
                geiGridSubsetParams.setVertCoordIntv(new double[]{startL, endL});
                //geoGrid = (Coverage) geoGrid.makeSubset(null, ensRange, null,
                //        levelRange, yRange, xRange);
            }
        } catch (InvalidRangeException ire) {
            throw new IllegalArgumentException("Invalid range:" + ire);
        }

        // check to see if user wants to be warned about download size
        boolean warn = getIdv().getStore().get(PREF_LARGE_REMOTE_DATA_WARN,
                false);
        boolean fromBundle = this.haveBeenUnPersisted;
        // just prior to loading data
        if ((readingFullGrid) && ( !fromBundle) && (warn)) {
            // check if interactive, if restoring from a bundle, and if file being loaded is remote
            if (getIdv().getInteractiveMode() && ( !isLocalFile())) {
                long total = 1;
                // get dimensions (note that the time dimension returned does not take into
                // account subsetting!
                List dims = geoGrid.getDimensions();
                // grab spatial dimension indices.
                List<Integer> geoDims =
                        Arrays.asList(geoGrid.getXDimensionIndex(),
                                geoGrid.getYDimensionIndex(),
                                geoGrid.getZDimensionIndex());
                for (int d = 0; d < geoDims.size(); d++) {
                    if (geoDims.get(d) != -1) {
                        ucar.nc2.Dimension dim =
                                (ucar.nc2.Dimension) dims.get(geoDims.get(d));
                        total *= dim.getLength();
                    }
                }
                // check if there is a time dimension, and if so, take into account for number of points
                if (geoGrid.getTimeDimension() != null) {
                    try {
                        total *= givenDataSelection.getTimes().size();
                    } catch (NullPointerException npe) {
                        // if use default is selected in field selector for time, then
                        // the getTimes() on the given data source throws and NPE and we
                        // need to go to geoGrid to get the number of times. Note that
                        // that getTimes() on geoGrid does not reflect any temporal subsetting
                        // which is why we check the givenDataSelection first...
                        CoordinateAxis1DTime ca = (CoordinateAxis1DTime)geoGrid.getCoordinateSystem().getTimeAxis();
                        total *= getTimes(ca.getCalendarDates()).size();
                    }
                }
                // compute size in megabytes of request (minus overhead of network protocol)
                double mb = (total * geoGrid.getDataType().getSize());
                mb = (mb / 1048576.);
                if (mb > SIZE_THRESHOLD) {
                    JCheckBox askCbx = new JCheckBox("Don't show this again",
                            !warn);
                    JComponent msgContents =
                            GuiUtils
                                    .vbox(GuiUtils
                                            .inset(new JLabel("<html>You are about to load "
                                                    + ((int) mb)
                                                    + " MB of data.<br>Are you sure you want to do this?<p><hr>"
                                                    + "<br>Consider subsetting for better performance!<p></html>"), 5), GuiUtils
                                            .inset(askCbx,
                                                    new Insets(5, 0, 0, 0)));

                    /**
                     * JComponent msgContents =
                     * new JLabel(
                     * "<html>You are about to load " + ((int) mb)
                     * + " MB of data.<br>Are you sure you want to do this?<p><hr>"
                     * + "<br>Consider subsetting for better performance!<p></html>");
                     */
                    if (askCbx.isSelected()) {
                        getIdv().getStore().put(PREF_LARGE_REMOTE_DATA_WARN,
                                false);
                    }
                    if ( !GuiUtils.askOkCancel(
                            "Large Remote Data Request Warning",
                            msgContents)) {
                        throw new HugeSizeException();
                    }
                }
            }
        }


        GridCoverageAdapter adapter = new GridCoverageAdapter(this, geoCoverage,
                geiGridSubsetParams, dataChoice.getName(),
                dataset.getNetcdfDataset(),
                extraCacheKey);

        adapter.cacheFile = filename.toString();
        return adapter;
    }

    /**
     * _more_
     *
     * @param dataPoints _more_
     * @param displayPoints _more_
     *
     * @return _more_
     */
    public int calculateStrideFactor(int dataPoints, int displayPoints) {
        if (dataPoints <= displayPoints) {
            return 0;
        } else {
            int factor = (int) Math.floor((1.0 * dataPoints)
                    / (1.0 * displayPoints) + 0.8);
            return factor;
        }
    }

    /**
     * Make a range for the given parameters
     *
     * @param dim   The dimension to subset
     * @param range an existing subset
     * @param stride  the stride
     *
     * @return the corresponding Range
     *
     * @throws InvalidRangeException not a valid range
     */
    private Range makeRange(ucar.nc2.Dimension dim, Range range, int stride)
            throws InvalidRangeException {
        if (dim == null) {
            return null;
        }
        if (range == null) {
            range = new Range(0, dim.getLength() - 1, stride);
        } else {
            range = new Range(range.first(), range.last(), stride);
        }
        return range;
    }

    /**
     * Clean up the bounding box name so it can be used in a file name.
     * change : and + and any other strange chars to _
     *
     * @param bbox  bounding box
     *
     * @return  cleaned up name
     */
    private String cleanBBoxName(LatLonRect bbox) {
        String name = Util.cleanName(bbox.toString());
        name = name.replaceAll(":", "_");
        name = name.replaceAll("\\+", "_");
        return name;
    }

    /**
     * Find the index of the given object in the list of levels. If its
     *  a Real then check values
     *
     * @param o Object
     * @param levels levels
     *
     * @return index
     *
     * @throws VisADException On badness
     */
    public int indexOf(Object o, List levels) throws VisADException {
        if (o instanceof TwoFacedObject) {
            o = ((TwoFacedObject) o).getId();
        }

        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }

        if (o instanceof String) {
            try {
                o = ucar.visad.Util.toReal(o.toString());
            } catch (Exception ignoreThis) {}
        }


        if (o instanceof String) {
            String s = (String) o;
            if (s.startsWith("#")) {
                int index = new Integer(s.substring(1).trim()).intValue();
                return index;
            }
            o = new Real(new Double(s).doubleValue());
        }

        if ((o instanceof Real) && (levels.size() > 0)
                && (levels.get(0) instanceof Real)) {
            Real r = (Real) o;
            for (int i = 0; i < levels.size(); i++) {
                //TODO: Check if the units are convertible
                Real lr = (Real) levels.get(i);
                if (r.getValue(lr.getUnit()) == lr.getValue()) {
                    return i;
                }
            }
            return -1;
        }
        return levels.indexOf(o);
    }

    /**
     * Make the FieldImpl corresponding to the DataChoice and
     * specified DataSelection (times).
     *
     * @param dataChoice   DataChoice.
     * @param givenDataSelection  specified selection criteria.
     * @param requestProperties request properties
     *
     * @return  the grid of data corresponding to the choice
     *
     * @throws VisADException  couldn't create Data object
     * @throws RemoteException  couldn't create remote Data object
     */
    private FieldImpl makeFieldImpl(DataChoice dataChoice,
                                    DataSelection givenDataSelection,
                                    Hashtable requestProperties)
            throws VisADException, RemoteException {


        long millis = System.currentTimeMillis();
        List allLevels =
                getAllLevels(dataChoice,
                        new DataSelection(GeoSelection.STRIDE_BASE));

        Trace.call1("GeoGridDataSource.makeField");

        Object fromLevel      = givenDataSelection.getFromLevel();
        Object toLevel        = givenDataSelection.getToLevel();
        int    fromLevelIndex = -1;
        int    toLevelIndex   = -1;

        if ((fromLevel != null) && (toLevel != null)) {
            fromLevelIndex = indexOf(fromLevel, allLevels);
            //            System.err.println ("fromLevel index:" + fromLevelIndex);
            toLevelIndex = indexOf(toLevel, allLevels);
            if ((toLevelIndex < 0) || (fromLevelIndex < 0)) {
                System.err.println("Did not find level indices:   fromLevel:"
                        + fromLevel + " index:" + fromLevelIndex
                        + " toLevel:" + toLevel + " index:"
                        + toLevelIndex + "\nLevels:" + allLevels);
                if ((allLevels != null) && !allLevels.isEmpty()) {
                    System.err.println("fromLevel is a "
                            + fromLevel.getClass().getName()
                            + ", toLevel is a "
                            + toLevel.getClass().getName());
                    System.err.println(
                            "levels are "
                                    + allLevels.get(0).getClass().getName());
                }
            }
        }




        long      starttime = System.currentTimeMillis();
        FieldImpl fieldImpl = null;
        //GridDataset myDataset = getDataset();
        //if (myDataset == null) {
        //    return null;
        //}
        //GeoGrid geoGrid   = findGridForDataChoice(myDataset, dataChoice);
        //String  paramName = dataChoice.getStringId();

        Trace.call1("GeoGridDataSource.make GeoGridAdapter");
        //      System.err.println("levels:" + fromLevelIndex +" " + toLevelIndex);
        GridCoverageAdapter adapter = null;
        try {
            adapter = makeGeoGridAdapter(dataChoice, givenDataSelection,
                    requestProperties, fromLevelIndex,
                    toLevelIndex, false);
        } catch (HugeSizeException hse) {
            return null;
        }

        if (adapter == null) {
            throw new BadDataException("Could not find field:"
                    + dataChoice.getStringId());
        }
        Trace.call2("GeoGridDataSource.make GeoGridAdapter");
        Coverage geoGrid   = adapter.getGeoGrid();

        String  paramName = dataChoice.getStringId();

        Trace.call1("GeoGridDataSource.make times");
        List times = getTimesFromDataSelection(givenDataSelection,
                dataChoice);

        // Datasource overrides data selection
        List members = getEnsembleSelection();
        if (members == null) {
            members =
                    (List) givenDataSelection.getProperty(PROP_ENSEMBLEMEMBERS);
        }
        int[] membersIndices = null;
        if (members != null) {
            int msize = members.size();
            membersIndices = new int[msize];
            for (int i = 0; i < msize; i++) {
                membersIndices[i] = ((Integer) members.get(i)).intValue();
            }
        }
        int[] timeIndices = null;
        List  allTimes    = null;
        if (times != null) {
            timeIndices = new int[times.size()];
            if(geoGrid.getCoordSys().getTimeAxis() == null &&
                    geoGrid.getCoordSys().getAxis(AxisType.RunTime) != null)
                allTimes = getGeoGridTimes((CoverageCoordAxis1D) geoGrid
                        .getCoordSys().getAxis(AxisType.RunTime));
            else
                allTimes = getGeoGridTimes((CoverageCoordAxis1D) geoGrid
                        .getCoordSys().getTimeAxis());
            int numTimes = allTimes.size();
            if (holdsIndices(times)) {
                for (int i = 0; i < times.size(); i++) {
                    int index = ((Integer) times.get(i)).intValue();
                    if (getReverseTimes()) {
                        index = numTimes - index - 1;
                    }

                    timeIndices[i] = index;
                }
            } else {
                for (int i = 0; i < times.size(); i++) {
                    int index = allTimes.indexOf(times.get(i));
                    if (getReverseTimes()) {
                        index = numTimes - index - 1;
                    }
                    timeIndices[i] = index;
                }
            }
        }
        Trace.call2("GeoGridDataSource.make times");
        /*
        System.err.print("times:");
        for(int i=0;i<timeIndices.length;i++) {
            System.err.print(" " + timeIndices[i]);
        }
        System.err.println("");
        */
        /* forecast hour */
        CoverageCoordAxis dd =
                geoGrid.getCoordSys().getAxis(AxisType.RunTime) ;
        CoverageCoordAxis dt =
                geoGrid.getCoordSys().getTimeAxis();
        StringBuilder buf = new StringBuilder();
        if ((dd != null) && (dt != null)) {
            for (int i = 0; i < timeIndices.length; i++) {
                CalendarDate cd   = null;
                int dsize = ((CoverageCoordAxis1D) dd).getCoordValueNames().size();
                if(i < dsize)
                    cd  = dd.makeDate(timeIndices[i]);
                else
                    cd  = dd.makeDate(timeIndices[dsize - 1]);
                CalendarDate ct   = dt.makeDate(timeIndices[i]);
                long         diff = ct.getDifferenceInMsecs(cd);
                float        fh   = diff / (1000.0f * 3600.0f);
                buf.append(fh + ",");
            }

        } else if (dt != null) {
            CalendarDate ct0 = dt.makeDate(timeIndices[0]);
            if (allTimes.size() >= timeIndices.length) {
                //for speed contour over topo, topo is likely has
                //its time dimension lenght 1 or zero.
                for (int i = 0; i < timeIndices.length; i++) {
                    CalendarDate ct   = dt.makeDate(timeIndices[i]);
                    long         diff = ct.getDifferenceInMsecs(ct0);
                    float        fh   = diff / (1000.0f * 3600.0f);
                    buf.append(fh + ",");
                }
            }
        }
        if (buf.length() > 1) {
            buf.deleteCharAt(buf.length() - 1);
            dataChoice.setProperty("RUNTIME", buf.toString());
        }
        Trace.call1("GeoGridDataSource.getSequence");
        Object loadId = JobManager.getManager().startLoad("GeoGrid");
        if (adapter != null) {
            if ((membersIndices != null) && (membersIndices.length > 0)) {
                fieldImpl = adapter.getSequence(timeIndices, membersIndices,
                        loadId);
            } else {
                fieldImpl = adapter.getSequence(timeIndices, loadId);
            }
        }

        if (fieldImpl == null) {
            //            System.err.println ("data selection:" + givenDataSelection);
            //            System.err.println ("data selection times:" + times);
            //            System.err.println ("allTimes:" + allTimes);
            //            Misc.printArray("timeIndices", timeIndices);
        }
        boolean useDriverTime = false;
        if (givenDataSelection != null) {
            useDriverTime = givenDataSelection.getProperty(
                    DataSelection.PROP_USESTIMEDRIVER, false);
        }
        if ((givenDataSelection != null) && !times.isEmpty()) {
            CalendarDateTime t0 =
                    new CalendarDateTime((DateTime) times.get(0));
            CalendarDate dt0 = t0.getCalendarDate();
            CalendarDateTime t1 =
                    new CalendarDateTime((DateTime) times.get(times.size() - 1));
            CalendarDate dt1 = t1.getCalendarDate();
            dateRange = CalendarDateRange.of(dt0, dt1);
        } else {
            dateRange = null;
        }

        Trace.call2("GeoGridDataSource.getSequence");

        // if made a non-null FlatField, this is displayable as 3D data;
        // if not, quit.
        if (fieldImpl == null) {
            if ( !JobManager.getManager().canContinue(loadId)) {
                return null;
            }
            LogUtil.userMessage(log_,
                    "Unable to load field: " + paramName
                            + " from:" + getFilePath(), true);
            return null;
        }

        JobManager.getManager().stopLoad(loadId);
        Trace.call2("GeoGridDataSource.makeField");
        LogUtil.message("");
        log_.debug("Read grid in " + (System.currentTimeMillis() - millis));
        return fieldImpl;
    }  // end makeField

    /**
     * Find the grid in the dataset from the DataChoice
     *
     * @param ds  the grid dataset
     * @param dc  the data choice
     * @return the GeoGrid or null dataset doesn't exist or if variable not found
     */
    public DtCoverage findGridForDataChoice(DtCoverageDataset ds, DataChoice dc) {
        if (ds == null) {
            return null;
        }
        String name = dc.getStringId();
        DtCoverage geoGrid = ds.findGridByName(name);
        return geoGrid;
    }

    /**
     * Utility to check if we should ignore the given z axis
     *
     * @param zaxis given z axis
     *
     * @return Is ok
     */
    protected boolean isZAxisOk(CoordinateAxis1D zaxis) {
        return GeoGridAdapter.isZAxisOk(zaxis);
    }

    /**
     * Override the base class method to return the times for the data choice
     *
     * @param dataChoice  DataChoice in question
     * @return  List of all times for that choice
     */
    public List getAllDateTimes(DataChoice dataChoice) {
        return (List) timeMap.get(dataChoice.getId());
    }

    /**
     * Create a DataChoice corresponding to the GeoGrid.  This is the
     * workhorse of the initialization.
     *
     * @param cfield   GeoGrid
     * @param allTimes set of times to use
     * @param timeToIndex a mapping of time to index
     *
     * @return corresponding DataChoice
     */
    private DataChoice makeDataChoiceFromGeoGrid(DtCoverage cfield,
                                                 List allTimes, Hashtable timeToIndex) {

        DtCoverageCS gcs    = cfield.getCoordinateSystem();
        LatLonRect      llr    = gcs.getLatLonBoundingBox();
        LatLonPointImpl lleft  = llr.getLowerLeftPoint();
        LatLonPointImpl uright = llr.getUpperRightPoint();
        double centerLat = lleft.getLatitude()
                + (uright.getLatitude() - lleft.getLatitude())
                / 2.0;

        EarthLocation elt = null;
        Hashtable     ht  = new Hashtable();
        try {
            elt = new EarthLocationTuple(centerLat, llr.getCenterLon(), 0);
            ht.put(IdvConstants.INITIAL_PROBE_EARTHLOCATION, elt);
        } catch (Exception e) {}
        //int             zIndex = gcs.getZdim();
        //int             yIndex = gcs.getYdim();
        //int             xIndex = gcs.getXdim();
        CoordinateAxis  xaxis = gcs.getXHorizAxis();
        CoordinateAxis  yaxis = gcs.getYHorizAxis();
        CoordinateAxis1D  zaxis = gcs.getVerticalAxis();

        // get dimensions of coordinate axes.
        long sizeZ = 0;
        if (zaxis != null) {
            sizeZ = (int) zaxis.getSize();
        }



        Hashtable threeDProps = Misc.newHashtable(DataChoice.PROP_ICON,
                "/auxdata/ui/icons/3D.gif");
        Hashtable twoDProps = Misc.newHashtable(DataChoice.PROP_ICON,
                "/auxdata/ui/icons/2D.gif");
        if (ht != null) {
            threeDProps.putAll(ht);
            twoDProps.putAll(ht);
        }



        DirectDataChoice choice = null;
        if (sizeZ < 0) {
            log_.error("    weird Geogrid -- it claims size Z<0; parm "
                    + cfield.getName());
            return null;
        } else if ( !isZAxisOk(zaxis)) {
            // do not use grid with "Hybrid", potential temp or
            // boundary layer vertical axis coordinate
        } else {  // might know how to handle this.
            String parmName    = cfield.getName();
            String pseudoName  = parmName;
            String description = cfield.getDescription();
            if ((description == null) || description.equals("")) {
                description = parmName;
            }

            CoordinateAxis1DTime tAxis    = (CoordinateAxis1DTime)gcs.getTimeAxis();
            if(tAxis == null && gcs.getRunTimeAxis() != null)
                tAxis = gcs.getRunTimeAxis();
            List                 geoTimes = getGeoGridTimes(tAxis);

            timeMap.put(parmName, geoTimes);

            //            List indexList = Misc.getIndexList(geoTimes, allTimes);
            List indexList = new ArrayList();
            if ((geoTimes != null) && (allTimes != null)) {
                for (int i = 0; i < geoTimes.size(); i++) {
                    Integer timeIndex =
                            (Integer) timeToIndex.get(geoTimes.get(i));
                    indexList.add(timeIndex);
                }
            }

            // none or only one level, we'll call it a 2D grid
            DataSelection dataSelection = DataSelection.NULL;
            if (false && !indexList.isEmpty()) {
                dataSelection = new DataSelection(indexList,
                        DataSelection.TIMESMODE_USETHIS);
            }

            List      categories = null;
            Hashtable props      = null;
            if ((sizeZ == 0) || (sizeZ == 1)) {
                //if (sizeZ == 0) {
                int xLength               =
                        cfield.getXDimension().getLength();
                int yLength               =
                        cfield.getYDimension().getLength();
                //ucar.nc2.Dimension ensDim = cfield.getEnsembleDimension();
                if (twoDDimensionsLabel == null) {
                    twoDDimensionsLabel = "Total grid size:  x: " + xLength
                            + "  y: " + yLength
                            + "    #points: "
                            + (xLength * yLength);
                }
                props = new Hashtable(twoDProps);
                props.put(PROP_GRIDSIZE, new ThreeDSize(xLength, yLength));
                if (geoTimes != null) {
                    props.put(PROP_TIMESIZE, new Integer(geoTimes.size()));
                }
              /*  if ((ensDim != null) && (ensDim.getLength() > 1)) {
                    //List             ensMembers = null;
                    //CoordinateAxis1D eAxis      = gcs.getEnsembleAxis();
                    int              numEns     = ensDim.getLength();
                   // if ((ensMembers == null) && (eAxis != null)) {
                    //    ensMembers = eAxis.getNames();
                    //}
                    int[]    ids    = new int[numEns];
                    String[] enames = new String[numEns];
                    for (int i = 0; i < numEns; i++) {
                        ids[i] = i;
                        NamedAnything na = (NamedAnything) ensMembers.get(i);
                        if (isNumeric(na.toString())) {
                            enames[i] = "Member " + na.toString();
                        } else {
                            enames[i] = na.toString();
                        }

                    }
                    List ensSet = TwoFacedObject.createList(ids, enames);
                    props.put(PROP_ENSEMBLEMEMBERS, ensSet);


                } */

               /* if ((ensDim != null) && (ensDim.getLength() > 1)) {
                    categories = (tAxis == null)
                            ? getTwoDCategories()
                            : getTwoDEnsTimeSeriesCategories();
                } else { */
                    categories = (tAxis == null)
                            ? getTwoDCategories()
                            : getTwoDTimeSeriesCategories();
                //}
                /*
                choice = new DirectDataChoice(this, parmName, pseudoName,
                        description, (taxis == null)
                                     ? getTwoDCategories()
                                     : getTwoDTimeSeriesCategories(), dataSelection,
                                     props);
                */

            } else {  // if (sizeZ > 1)
                // Have 3D field (we expect); usually sizeZ > 1:
                int xLength               =
                        cfield.getXDimension().getLength();
                int yLength               =
                        cfield.getYDimension().getLength();
                int zLength               =
                        cfield.getZDimension().getLength();
                //ucar.nc2.Dimension ensDim = cfield.getEnsembleDimension();
                if (xLength * yLength * zLength > max3D) {
                    max3D  = xLength * yLength * zLength;
                    max3DX = xLength;
                    max3DY = yLength;
                    max3DZ = zLength;
                }
                ThreeDSize size = new ThreeDSize(xLength, yLength, zLength);
                props = new Hashtable(threeDProps);
                props.put(PROP_GRIDSIZE, size);
                if (geoTimes != null) {
                    props.put(PROP_TIMESIZE, new Integer(geoTimes.size()));
                }
                /* if ((ensDim != null) && (ensDim.getLength() > 1)) {
                    List             ensMembers = null;
                    CoordinateAxis1D eAxis      = gcs.getEnsembleAxis();
                    int              numEns     = ensDim.getLength();
                    if ((ensMembers == null) && (eAxis != null)) {
                        ensMembers = eAxis.getNames();
                    }
                    int[]    ids    = new int[numEns];
                    String[] enames = new String[numEns];
                    for (int i = 0; i < numEns; i++) {
                        ids[i] = i;
                        NamedAnything na = (NamedAnything) ensMembers.get(i);
                        enames[i] = na.toString();
                    }
                    List ensSet = TwoFacedObject.createList(ids, enames);
                    props.put(PROP_ENSEMBLEMEMBERS, ensSet);


                } */
                /*
                choice = new DirectDataChoice(this, parmName, pseudoName,
                        description, (tAxis == null)
                                     ? getThreeDCategories()
                                     : getThreeDTimeSeriesCategories(), dataSelection,
                                     props);
                */

              /*  if ((ensDim != null) && (ensDim.getLength() > 1)) {
                    categories = (tAxis == null)
                            ? getThreeDCategories()
                            : getThreeDEnsTimeSeriesCategories();

                } else { */
                    categories = (tAxis == null)
                            ? getThreeDCategories()
                            : getThreeDTimeSeriesCategories();
              //  }
            }
            // see if we have any categorization
            Attribute attr = null;
            for (int i = 0; (i < categoryAttributes.length) && (attr == null);
                 i++) {
                attr = cfield.findAttributeIgnoreCase(categoryAttributes[i]);
            }
            if (attr != null) {
                String append = attr.getStringValue();
                if (append != null) {
                    append = append.replaceAll(DataCategory.DIVIDER, "_");
                }
                DataCategory cat = (DataCategory) categories.get(0);
                cat = cat.copyAndAppend(append);
                List newCategories = new ArrayList();
                newCategories.add(cat);
                for (int i = 1; i < categories.size(); i++) {
                    newCategories.add(categories.get(i));
                }
                categories = newCategories;
            }

            // see if we have any categorization
            Group            group    = null;
          /*  VariableDS variable = cfield.getVariable();
            if (variable != null) {
                group = variable.getParentGroup();
                if ((group != null) && !group.equals("")) {
                    String append = group.getName();
                    if (append != null) {
                        append = append.replaceAll("/", "");
                        append = append.replaceAll(DataCategory.DIVIDER, "_");
                    }
                    DataCategory cat = (DataCategory) categories.get(0);
                    cat = cat.copyAndAppend(append);
                    List newCategories = new ArrayList();
                    newCategories.add(cat);
                    for (int i = 1; i < categories.size(); i++) {
                        newCategories.add(categories.get(i));
                    }
                    categories = newCategories;
                }
            } */


            choice = new DirectDataChoice(this, parmName, pseudoName,
                    description, categories,
                    dataSelection, props);
        }
        return choice;
    }

    /**
     * make a list of DateTime-s from a GeoGrid timeAxis
     *
     * @param timeAxis  - Coverage time CoordinateAxis
     * @return corresponding List of DateTime-s.
     */
    private List getGeoGridTimes(CoverageCoordAxis1D  timeAxis) {
        if ((timeAxis == null) || (timeAxis.getNcoords() == 0)) {
            return new ArrayList(0);
        }
        List times = (List) gcsVsTime.get(timeAxis);
        if (times != null) {
            return times;
        }
        try {
            times = new ArrayList();
         /*   Object [] ttt = timeAxis.getCoordValueNames().toArray();
            for(Object oj: ttt) {
                NamedAnything anything = (NamedAnything)oj;
                CalendarDate cdate = (CalendarDate)anything.getValue();
                times.add(DataUtil.makeDateTime(cdate));
            } */
            times = DataUtil.makeDateTimes(timeAxis);
            gcsVsTime.put(timeAxis, times);
        } catch (Exception e) {
            System.out.println("getGeoGridTimes() " + e);
        }
        return times;
    }

    /**
     *  make a list of DateTime-s from a GeoGrid timeAxis
     *
     *  @param timeAxis  - GeoGrid time CoordinateAxis
     * @return corresponding List of DateTime-s.
     */
    private List getGeoGridTimes(CoordinateAxis1DTime  timeAxis) {
        if ((timeAxis == null) || (timeAxis.getSize() == 0)) {
            return new ArrayList(0);
        }
        List times = (List) gcsVsTime.get(timeAxis);
        if (times != null) {
            return times;
        }
        try {
            times = DataUtil.makeDateTimes(timeAxis);
            gcsVsTime.put(timeAxis, times);
        } catch (Exception e) {
            System.out.println("getGeoGridTimes() " + e);
        }
        return times;
    }

    /**
     * Set the FileNameOrUrl property.
     *
     * @param value The new value for FileNameOrUrl
     */
    public void setFileNameOrUrl(String value) {
        oldSourceFromBundles = value;
    }

    /**
     * Apply the properties
     *
     * @return everything ok
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        if (reverseTimesCheckbox != null) {
            reverseTimes = reverseTimesCheckbox.isSelected();
        }
        return true;
    }

    /**
     * Add the reverse times checkbox
     *
     * @return extra comp
     */
    protected JComponent getExtraTimesComponent() {
        reverseTimesCheckbox = new JCheckBox("Reverse Times", reverseTimes);
        reverseTimesCheckbox.setToolTipText(
                "If you have selected the first time then really use the last time");
        return GuiUtils.right(reverseTimesCheckbox);
    }

    /**
     * Get the ReverseTimes property.
     *
     * @return The ReverseTimes
     */
    public boolean getReverseTimes() {
        return reverseTimes;
    }

    /**
     * Set the ReverseTimes property.
     *
     * @param value The new value for ReverseTimes
     */
    public void setReverseTimes(boolean value) {
        reverseTimes = value;
    }

    public List<Range> getRangesFromLatLonRect(DtCoverageCS dtCoverageCS, LatLonRect rect) throws InvalidRangeException {
        double minx, maxx, miny, maxy;

        ProjectionImpl proj = dtCoverageCS.getProjection();
        if (proj != null && !(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation)
                && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
            // first clip the request rectangle to the bounding box of the grid
            LatLonRect bb = dtCoverageCS.getLatLonBoundingBox();
            LatLonRect rect2 = bb.intersect(rect);
            if (null == rect2)
                throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
            rect = rect2;
        }

        CoordinateAxis xaxis = dtCoverageCS.getXHorizAxis();
        CoordinateAxis yaxis = dtCoverageCS.getYHorizAxis();


        LatLonPointImpl llpt = rect.getLowerLeftPoint();
        LatLonPointImpl urpt = rect.getUpperRightPoint();
        LatLonPointImpl lrpt = rect.getLowerRightPoint();
        LatLonPointImpl ulpt = rect.getUpperLeftPoint();

        minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
        miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
        maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
        maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

        // normalize to [minLon,minLon+360]
        double minLon = xaxis.getMinValue();
        minx = LatLonPointImpl.lonNormalFrom( minx, minLon);
        maxx = LatLonPointImpl.lonNormalFrom( maxx, minLon);




        if ((xaxis instanceof CoordinateAxis1D) && (yaxis instanceof CoordinateAxis1D)) {
            CoordinateAxis1D xaxis1 = (CoordinateAxis1D) xaxis;
            CoordinateAxis1D yaxis1 = (CoordinateAxis1D) yaxis;

            int minxIndex = xaxis1.findCoordElementBounded(minx);
            int minyIndex = yaxis1.findCoordElementBounded(miny);

            int maxxIndex = xaxis1.findCoordElementBounded(maxx);
            int maxyIndex = yaxis1.findCoordElementBounded(maxy);

            List<Range> list = new ArrayList<>();
            list.add(new Range(Math.min(minyIndex, maxyIndex), Math.max(minyIndex, maxyIndex)));
            list.add(new Range(Math.min(minxIndex, maxxIndex), Math.max(minxIndex, maxxIndex)));
            return list;

        } else if ((xaxis instanceof CoordinateAxis2D) && (yaxis instanceof CoordinateAxis2D)) {
            CoordinateAxis2D lon_axis = (CoordinateAxis2D) xaxis;
            CoordinateAxis2D lat_axis = (CoordinateAxis2D) yaxis;
            int shape[] = lon_axis.getShape();
            int nj = shape[0];
            int ni = shape[1];

            int mini = Integer.MAX_VALUE, minj = Integer.MAX_VALUE;
            int maxi = -1, maxj = -1;

            // margolis 2/18/2010
            //minx = LatLonPointImpl.lonNormal( minx ); // <-- THIS IS NEW
            //maxx = LatLonPointImpl.lonNormal( maxx ); // <-- THIS IS NEW

            // brute force, examine every point LOOK BAD
            for (int j = 0; j < nj; j++) {
                for (int i = 0; i < ni; i++) {
                    double lat = lat_axis.getCoordValue(j, i);
                    double lon = lon_axis.getCoordValue(j, i);
                    //lon = LatLonPointImpl.lonNormal( lon ); // <-- THIS IS NEW

                    if ((lat >= miny) && (lat <= maxy) && (lon >= minx) && (lon <= maxx)) {
                        if (i > maxi) maxi = i;
                        if (i < mini) mini = i;
                        if (j > maxj) maxj = j;
                        if (j < minj) minj = j;
                        //System.out.println(j+" "+i+" lat="+lat+" lon="+lon);
                    }
                }
            }

            // this is the case where no points are included
            if ((mini > maxi) || (minj > maxj)) {
                mini = 0;
                minj = 0;
                maxi = -1;
                maxj = -1;
            }

            ArrayList<Range> list = new ArrayList<>();
            list.add(new Range(minj, maxj));
            list.add(new Range(mini, maxi));
            return list;

        } else {
            throw new IllegalArgumentException("must be 1D or 2D/LatLon ");
        }

    }

    /**
     *
     *
     * @return nameObject list
     */
    public List<NamedObject> getTimes(List<CalendarDate> cdates) {
        List<NamedObject> times = new ArrayList<>( cdates.size());
        for (CalendarDate cd: cdates) {
            times.add(new ucar.nc2.util.NamedAnything(cd.toString(), "calendar date"));
        }
        return times;
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Wed, Nov 28, '12
     * @author         Enter your name here...
     */
    public static class HugeSizeException extends Exception {}



}

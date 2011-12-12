/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.*;
import ucar.nc2.util.NamedAnything;



import ucar.unidata.data.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.ui.TextSearcher;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.*;

import ucar.visad.Util;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import visad.util.DataUtility;

import java.awt.Dimension;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import javax.swing.*;



/**
 * Handles gridded files
 *
 * @author IDV Development Team
 */

public class GeoGridDataSource extends GridDataSource {


    /** Used to synchronize the geogridadapter */
    protected final Object DOMAIN_SET_MUTEX = new Object();

    /** Throw an error when loading a grid bigger than this */
    private static final int SIZE_THRESHOLD = 500000000;

    /** The prefix we hack onto the u and v  variables */
    private static final String PREFIX_GRIDRELATIVE = "GridRelative_";


    /** Preference */
    public static final String PREF_VERTICALCS = IdvConstants.PREF_VERTICALCS;

    /** grid size */
    public static final String PROP_GRIDSIZE = "prop.gridsize";

    /** property timesize */
    public static final String PROP_TIMESIZE = "prop.timesize";

    /** property time variable */
    public static final String PROP_TIMEVAR = "timeVariable";

    /** This is used to synchronize geogrid read access */
    protected final Object readLock = new Object();

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            GeoGridDataSource.class.getName());

    /** the dataset */
    private GridDataset dataset;

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

    /** category attributes */
    private static String[] categoryAttributes = { "GRIB_param_category" };


    /** Do we really reverse the time indices */
    private boolean reverseTimes = false;

    /** for properties_ */
    private JCheckBox reverseTimesCheckbox;



    /**
     * Default constructor
     */
    public GeoGridDataSource() {}


    /**
     * Construct a GeoGridDataSource
     *
     * @param descriptor   the data source descriptor
     * @param gds          The GridDataset
     * @param name         A name
     * @param filename     the filename
     */
    public GeoGridDataSource(DataSourceDescriptor descriptor,
                             GridDataset gds, String name, String filename) {
        super(descriptor, filename, name, (Hashtable) null);
        dataset = gds;
    }


    /**
     * Create a GeoGridDataSource from the GridDataset
     *
     * @param gds  the GridDataset
     */
    public GeoGridDataSource(GridDataset gds) {
        dataset = gds;
    }


    /**
     * Create a GeoGridDataSource from a File.
     *
     * @param descriptor   Describes this data source, has a label etc.
     * @param file         This is the file that points to the actual
     *                     data source.
     * @param properties   General properties used in the base class
     *
     * @throws IOException  problem opening file
     */
    public GeoGridDataSource(DataSourceDescriptor descriptor, File file,
                             Hashtable properties)
            throws IOException {
        this(descriptor, file.getPath(), properties);
    }



    /**
     * Create a GeoGridDataSource from the filename.
     * @param descriptor   Describes this data source, has a label etc.
     * @param filename     This is the filename (or url) that points
     *                     to the actual data source.
     * @param properties   General properties used in the base class
     *
     * @throws IOException
     */
    public GeoGridDataSource(DataSourceDescriptor descriptor,
                             String filename, Hashtable properties)
            throws IOException {
        //Make sure we pass filename up here - as opposed to calling
        //this (new File (filename)) because the new File (filename).getPath () != filename
        super(descriptor, filename, "Geogrid data source", properties);
    }


    /**
     * Create a GeoGridDataSource from the filename.
     * @param descriptor   Describes this data source, has a label etc.
     * @param files List of files or urls
     * @param properties   General properties used in the base class
     *
     * @throws IOException
     */
    public GeoGridDataSource(DataSourceDescriptor descriptor, List files,
                             Hashtable properties)
            throws IOException {
        //Make sure we pass filename up here - as opposed to calling
        //this (new File (filename)) because the new File (filename).getPath () != filename
        super(descriptor, files, "Geogrid data source", properties);
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
            ucar.nc2.NCdumpW.print(dataset.getNetcdfDataset(), "", bos, null);
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


        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if ( !(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            String label = dataChoice.getDescription();
            if (label.length() > 30) {
                label = label.substring(0, 29) + "...";
            }
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
            JPanel innerPanel = GuiUtils.doLayout(comps, 3, GuiUtils.WT_NYN,
                                    GuiUtils.WT_N);
            JScrollPane sp = new JScrollPane(GuiUtils.top(innerPanel));
            sp.setPreferredSize(new Dimension(500, 400));
            JPanel top =
                GuiUtils.right(GuiUtils.rLabel("Grid Size (Points)  "));
            JComponent inner = GuiUtils.inset(GuiUtils.topCenter(top, sp), 5);
            tab.addTab(categories.get(i).toString(), inner);
            //            catComps.add();
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

        String         path   = prefix;
        NetcdfCFWriter writer = new NetcdfCFWriter();

        //Start the load, showing the dialog
        loadId = JobManager.getManager().startLoad("Copying data", true,
                true);
        try {
            writer.makeFile(path, dataset, varNames, llr, /*dateRange*/ null,
                            includeLatLon, hStride, zStride, timeStride);
        } catch (Exception exc) {
            logException("Error writing local netcdf file.\nData:"
                         + getFilePath() + "\nVariables:" + varNames, exc);
            return null;
        } finally {
            JobManager.getManager().stopLoad(loadId);
        }


        if (geoSubset != null) {
            geoSubset.clearStride();
            geoSubset.setBoundingBox(null);
            if (geoSelectionPanel != null) {
                geoSelectionPanel.initWith(doMakeGeoSelectionPanel());
            }
        }

        //                       LatLonRect llbb, 
        //                       boolean addLatLon,
        //                       int horizStride, int stride_z, int stride_time) throws IOException, InvalidRangeException {


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


    /** old resolver URL */
    private String oldResolverUrl;

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
    protected GridDataset doMakeDataSet() {
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
                "<netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n");
            sb.append("<aggregation type=\"joinExisting\" dimName=\""
                      + timeName + "\" timeUnitsChange=\"true\">\n");
            for (int i = 0; i < sources.size(); i++) {
                String s = sources.get(i).toString();
                try {
                    sb.append(
                        XmlUtil.tag(
                            "netcdf",
                            XmlUtil.attrs(
                                "location",
                                IOUtil.getURL(s, getClass()).toString(),
                                "enhance", "true"), ""));
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
            GridDataset gds = GridDataset.open(file);
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
    public GridDataset getDataset() {
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
        return sampleProjection;
    }



    /**
     * This method is called at initialization time and
     * creates a set of {@link ucar.unidata.data.DirectDataChoice}-s
     * and adds them into the base class managed list of DataChoice-s
     * with the method addDataChoice.
     */
    protected void doMakeDataChoices() {

        GridDataset myDataset = getDataset();
        if (myDataset == null) {
            return;
        }
        max3DX = -1;
        max3DY = -1;
        max3DZ = -1;
        max3D  = -1;

        boolean       gridRelativeWind = false;
        NetcdfDataset ncFile           = myDataset.getNetcdfDataset();
        Variable      windFlag         = ncFile.findVariable("ResCompFlag");
        if (windFlag != null) {  // found it
            try {
                Array array = windFlag.read();
                gridRelativeWind = !((array.getInt(array.getIndex())
                                      & 1 << 3) == 0);
            } catch (IOException ioe) {
                LogUtil.printException(log_, "Couldn't read variable ", ioe);
            }
        }

        Iterator iter = myDataset.getGrids().iterator();
        SortedSet uniqueTimes =
            Collections.synchronizedSortedSet(new TreeSet());


        while (iter.hasNext()) {
            GeoGrid cfield = (GeoGrid) iter.next();
            if (sampleProjection == null) {
                sampleProjection = cfield.getProjection();
                //                System.err.println ("The sample projection is:" + sampleProjection);
            }
            //      System.out.println("llr:" + cfield.getProjection().getDefaultMapAreaLL());
            GridCoordSystem  gcs   = cfield.getCoordinateSystem();
            CoordinateAxis1D zaxis = gcs.getVerticalAxis();
            if ( !isZAxisOk(zaxis)) {
                continue;
            }
            CoordinateAxis1DTime tAxis    = gcs.getTimeAxis1D();
            List                 geoTimes = getGeoGridTimes(tAxis);
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
            GeoGrid cfield = (GeoGrid) iter.next();
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
                }  /*else {  // check for GRIB definition
                     String canonical = DataAlias.aliasToCanonical(choice.getName());
                     if (Misc.equals(canonical, "U") ||
                         Misc.equals(canonical, "V")) {
                          Attribute vecFlag =
                              cfield.findAttributeIgnoreCase("GRIB_VectorComponentFlag");
                          if (vecFlag != null) {
                              String vecFlagVal = vecFlag.getStringValue();
                              if (vecFlagVal.equals("gridRelative")) {
                                   choice.setName(PREFIX_GRIDRELATIVE
                                                  + choice.getName());
                              }
                          }
                     }
                 }
                 */
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
            GeoGridAdapter geoGridAdapter = makeGeoGridAdapter(dataChoice,
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
     * @throws VisADException On badness
     */
    private GeoGridAdapter makeGeoGridAdapter(DataChoice dataChoice,
            DataSelection givenDataSelection, Hashtable requestProperties,
            int fromLevelIndex, int toLevelIndex, boolean forMetaData)
            throws VisADException {

        boolean readingFullGrid = !forMetaData;
        int     numLevels       = -1;
        if ((fromLevelIndex >= 0) && (toLevelIndex >= 0)) {
            numLevels = Math.abs(toLevelIndex - fromLevelIndex) + 1;
        }


        GridDataset myDataset = getDataset();
        if (myDataset == null) {
            return null;
        }
        Object  extraCacheKey = null;
        String  paramName     = dataChoice.getStringId();
        GeoGrid geoGrid       = myDataset.findGridByName(paramName);
        if (geoGrid == null) {
            return null;
        }
        ucar.nc2.Dimension ensDim       = geoGrid.getEnsembleDimension();
        GeoSelection       geoSelection = ((givenDataSelection != null)
                                           ? givenDataSelection
                                               .getGeoSelection()
                                           : null);
        boolean needVolume =
            ((geoGrid.getCoordinateSystem().getVerticalTransform() != null)
             && ((requestProperties != null)
                 && (requestProperties.get(
                     DerivedDataChoice.PROP_FROMDERIVED) != null)));
        //        System.out.println("need volume = " + needVolume + " " + geoGrid.getCoordinateSystem().getVerticalTransform());

        StringBuffer filename = new StringBuffer("grid_" + paramName);

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

            if ((geoSelection != null)
                    && (geoSelection.hasSpatialSubset()
                        || geoSelection.getHasNonOneStride())) {
                //TODO: We should determine the size of the subset grid and use that.
                readingFullGrid = false;
                //System.err.println("subsetting using:" + geoSelection.getLatLonRect());
                extraCacheKey = geoSelection;
                if (levelRange != null) {
                    extraCacheKey = Misc.newList(extraCacheKey, levelRange);
                }
                filename.append("_x_" + geoSelection.getXStrideToUse());
                filename.append("_y_" + geoSelection.getYStrideToUse());
                filename.append("_z_" + geoSelection.getZStrideToUse());

                if (geoSelection.getLatLonRect() != null) {
                    LatLonRect bbox = geoSelection.getLatLonRect();
                    filename.append("_rect_" + cleanBBoxName(bbox));
                    List yx_ranges =
                        geoGrid.getCoordinateSystem().getRangesFromLatLonRect(
                            bbox);
                    yRange = makeRange(geoGrid.getYDimension(),
                                       (Range) yx_ranges.get(0),
                                       geoSelection.getYStrideToUse());
                    xRange = makeRange(geoGrid.getXDimension(),
                                       (Range) yx_ranges.get(1),
                                       geoSelection.getYStrideToUse());
                } else if (geoSelection.getHasNonOneStride()) {
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
                geoGrid = (GeoGrid) geoGrid.makeSubset(null, ensRange, null,
                        levelRange, yRange, xRange);
                /*
                geoGrid = geoGrid.subset(null, levelRange,
                                         geoSelection.getLatLonRect(),
                                         geoSelection.getZStrideToUse(),
                                         geoSelection.getYStrideToUse(),
                                         geoSelection.getXStrideToUse());
                */
            } else if (levelRange != null) {
                extraCacheKey = levelRange;
                //                System.out.println("level range(2):  " + levelRange);
                //geoGrid = geoGrid.subset(null, levelRange, null, null);
                geoGrid = (GeoGrid) geoGrid.makeSubset(null, ensRange, null,
                        levelRange, yRange, xRange);
            }
        } catch (InvalidRangeException ire) {
            throw new IllegalArgumentException("Invalid range:" + ire);
        }


        if (readingFullGrid) {
            ThreeDSize size =
                (ThreeDSize) dataChoice.getProperty(PROP_GRIDSIZE);
            if (size != null) {
                long total = size.getSizeY() * size.getSizeX();
                if (size.getSizeZ() > 1) {
                    if (numLevels > 0) {
                        total *= numLevels;
                    } else {
                        total *= size.getSizeZ();
                    }
                }
                if (total > SIZE_THRESHOLD) {
                    double mb = (total * 4);
                    mb = (mb / 1000000.0);
                    throw new BadDataException(
                        "You are requesting a grid with " + total
                        + " points which is " + Misc.format(mb)
                        + " (MB) of data.\nPlease subset the grid");
                }
            }
        }


        GeoGridAdapter adapter = new GeoGridAdapter(this, geoGrid,
                                     dataChoice.getName(),
                                     dataset.getNetcdfDataset(),
                                     extraCacheKey);

        adapter.cacheFile = filename.toString();
        return adapter;
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
    private int indexOf(Object o, List levels) throws VisADException {
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




        String      paramName = dataChoice.getStringId();
        long        starttime = System.currentTimeMillis();
        FieldImpl   fieldImpl = null;
        GridDataset myDataset = getDataset();
        if (myDataset == null) {
            return null;
        }
        GeoGrid geoGrid = myDataset.findGridByName(paramName);


        Trace.call1("GeoGridDataSource.make GeoGridAdapter");
        //      System.err.println("levels:" + fromLevelIndex +" " + toLevelIndex);
        GeoGridAdapter adapter = makeGeoGridAdapter(dataChoice,
                                     givenDataSelection, requestProperties,
                                     fromLevelIndex, toLevelIndex, false);
        if (adapter == null) {
            throw new BadDataException("Could not find field:"
                                       + dataChoice.getStringId());
        }
        Trace.call2("GeoGridDataSource.make GeoGridAdapter");

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
            allTimes =
                getGeoGridTimes((CoordinateAxis1DTime) geoGrid
                    .getCoordinateSystem().getTimeAxis1D());
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
    private DataChoice makeDataChoiceFromGeoGrid(GeoGrid cfield,
            List allTimes, Hashtable timeToIndex) {

        GridCoordSystem gcs    = cfield.getCoordinateSystem();
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
        CoordinateAxis   xaxis = gcs.getXHorizAxis();
        CoordinateAxis   yaxis = gcs.getYHorizAxis();
        CoordinateAxis1D zaxis = gcs.getVerticalAxis();

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

            CoordinateAxis1DTime tAxis    = gcs.getTimeAxis1D();
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
                ucar.nc2.Dimension ensDim = cfield.getEnsembleDimension();
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
                if ((ensDim != null) && (ensDim.getLength() > 1)) {
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
                        if (isNumeric(na.toString())) {
                            enames[i] = "Member " + na.toString();
                        } else {
                            enames[i] = na.toString();
                        }

                    }
                    List ensSet = TwoFacedObject.createList(ids, enames);
                    props.put(PROP_ENSEMBLEMEMBERS, ensSet);


                }

                if ((ensDim != null) && (ensDim.getLength() > 1)) {
                    categories = (tAxis == null)
                                 ? getTwoDCategories()
                                 : getTwoDEnsTimeSeriesCategories();
                } else {
                    categories = (tAxis == null)
                                 ? getTwoDCategories()
                                 : getTwoDTimeSeriesCategories();
                }
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
                ucar.nc2.Dimension ensDim = cfield.getEnsembleDimension();
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
                if ((ensDim != null) && (ensDim.getLength() > 1)) {
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


                }
                /*
                choice = new DirectDataChoice(this, parmName, pseudoName,
                        description, (tAxis == null)
                                     ? getThreeDCategories()
                                     : getThreeDTimeSeriesCategories(), dataSelection,
                                     props);
                */

                if ((ensDim != null) && (ensDim.getLength() > 1)) {
                    categories = (tAxis == null)
                                 ? getThreeDCategories()
                                 : getThreeDEnsTimeSeriesCategories();

                } else {
                    categories = (tAxis == null)
                                 ? getThreeDCategories()
                                 : getThreeDTimeSeriesCategories();
                }
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
            VariableEnhanced variable = cfield.getVariable();
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
            }


            choice = new DirectDataChoice(this, parmName, pseudoName,
                                          description, categories,
                                          dataSelection, props);
        }
        return choice;
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
     * make a list of DateTime-s from a GeoGrid timeAxis
     *
     * @param timeAxis  - GeoGrid time CoordinateAxis
     * @return corresponding List of DateTime-s.
     */
    private List getGeoGridTimes(CoordinateAxis1DTime timeAxis) {
        if ((timeAxis == null) || (timeAxis.getSize() == 0)) {
            return new ArrayList(0);
        }
        List times = (List) gcsVsTime.get(timeAxis);
        if (times != null) {
            return times;
        }
        java.util.Date[] dates = timeAxis.getTimeDates();
        times = new ArrayList(dates.length);
        try {
            for (int i = 0; i < dates.length; i++) {
                times.add(new DateTime(dates[i]));
            }
            gcsVsTime.put(timeAxis, times);
        } catch (Exception e) {
            System.out.println("getGeoGridTimes() " + e);
        }
        return times;
    }


    /** for test */
    private static boolean forceSubset = false;

    /** for test */
    public static boolean testMode = false;


    /*
    public void putCache(Object key, Object value) {
        if(testMode) return;
        super.putCache(key,value);
        }*/


    /**
     * Test this class by running
     * "java ucar.unidata.data.grid.GeoGridDataSource <filename>"
     *
     * @param args  filename
     *
     * @throws Exception  some error occurred
     */
    public static void main(String[] args) throws Exception {

        /*

        if (true) {
            int j=0;
            //            int []bufferSizes = {100,250,500,750,1000,5000,8092};
            int []bufferSizes = {500,500,500,500};
            for(int i=0;i<100;i++) {
                //                for(j=0;j<2;j++) {
                    ucar.grib.grib2.Grib2BitMapSection.SKIPIT = (j==0);
                    for(String arg: args) {
                        //                        ucar.unidata.io.RandomAccessFile.BUFFERSIZE = bufferSizes[i];
                        ucar.unidata.io.RandomAccessFile.BUFFERSIZE = 500;
                        GridDataset gds = GridDataset.open(arg);
                        gds.close();
                        File gbxFile = new File(arg+".gbx");
                        gbxFile.delete();
                    }
                    //                }
            }
            return;
        }

        */


        String leadUrl =
            "dods://lead.unidata.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20071002_1200.grib1";

        String mlodeUrl =
            "dods://motherlode.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20071002_1200.grib1";
        String atmUrl =
            "dods://thredds.cise-nsf.gov:8080/thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20071002_1200.grib1";
        String   url  = ((args.length == 0)
                         ? leadUrl
                         : ((args.length == 1)
                            ? mlodeUrl
                            : atmUrl));

        String[] urls = { url };
        testMode = true;

        for (int i = 0; i < 10000; i++) {
            for (int urlIdx = 0; urlIdx < urls.length; urlIdx++) {
                System.err.println("Reading data:" + i + " " + urls[urlIdx]);
                GeoGridDataSource ggds = new GeoGridDataSource(null,
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


        /*
    GridDataset dataset    = GridDataset.open("elev.nc");
    GeoGrid     geoGrid    = dataset.findGridByName("foo");
    GeoGrid     geoGrid50  = geoGrid.subset(null, null, null, 0, 50, 50);
    GeoGrid     geoGrid100 = geoGrid.subset(null, null, null, 0, 100,
                                 100);


    System.exit(0);
        */


        /**
         *
         * testMode    = true;
         * forceSubset = false;
         * LogUtil.setTestMode(true);
         * LogUtil.startOutputBuffer();
         * CacheManager.setDoCache(false);
         *
         * if (args.length == 0) {
         *   System.out.println("Must supply a file name");
         *   System.exit(1);
         * }
         * boolean      verbose      = false;
         * StringBuffer titleBuffer  = new StringBuffer();
         * StringBuffer errors       = new StringBuffer();
         * StringBuffer buffer       = new StringBuffer();
         * boolean      doAll        = false;
         * boolean      nextOneTitle = false;
         * List         nots         = new ArrayList();
         * for (int i = 0; i < args.length; i++) {
         *   if (nextOneTitle) {
         *       titleBuffer.append(args[i] + "<br>");
         *       nextOneTitle = false;
         *       continue;
         *   }
         *
         *   if (args[i].startsWith("-not:")) {
         *       nots.add(args[i].substring(5));
         *   }
         *
         *   if (args[i].equals("-verbose")) {
         *       verbose = true;
         *       continue;
         *   }
         *   if (args[i].equals("-title")) {
         *       nextOneTitle = true;
         *       continue;
         *   }
         *   if (args[i].equals("-doall")) {
         *       doAll = true;
         *       continue;
         *   }
         *   if (args[i].equals("-subset")) {
         *       forceSubset = true;
         *       buffer.append("Doing subsetting<p>\n");
         *       continue;
         *   }
         *   if (args[i].equals("-nosubset")) {
         *       forceSubset = false;
         *       buffer.append("Not doing subsetting<p>\n");
         *       continue;
         *   }
         *   boolean shouldProcess = true;
         *   for (int notIdx = 0; notIdx < nots.size(); notIdx++) {
         *       if (args[i].indexOf(nots.get(notIdx).toString()) >= 0) {
         *           shouldProcess = false;
         *       }
         *   }
         *   if ( !shouldProcess) {
         *       continue;
         *   }
         *
         *   String       name       = "file" + i;
         *   boolean      fileOk     = true;
         *   StringBuffer fileBuffer = new StringBuffer();
         *   fileBuffer.append("<hr><a name=\"" + name + "\">\n<b>File: "
         *                     + args[i]
         *                     + "</b><br><div style=\"margin-left:30\">\n");
         *   DataChoice        dataChoice = null;
         *   GeoGridDataSource ggds       = null;
         *   try {
         *       ggds = new GeoGridDataSource(null, args[i], null);
         *   } catch (Throwable exc) {
         *       errors.append("<a href=\"#" + name + "\">" + args[i]
         *                     + "</a><br>");
         *       fileBuffer.append("Failed to open file:");
         *       fileBuffer.append("<pre>");
         *       fileBuffer.append(LogUtil.getStackTrace(exc));
         *       fileBuffer.append("</pre>");
         *       fileBuffer.append("</div>\n");
         *       buffer.append(fileBuffer.toString());
         *       continue;
         *   }
         *   List dataChoices = ggds.getDataChoices();
         *   if (dataChoices.size() == 0) {
         *       errors.append("<a href=\"#" + name + "\">" + args[i]
         *                     + "</a><br>");
         *       fileBuffer.append("No data choices\n");
         *       fileBuffer.append("</div>\n");
         *       buffer.append(fileBuffer.toString());
         *       continue;
         *   }
         *   StringBuffer okBuffer    = new StringBuffer();
         *   StringBuffer notokBuffer = new StringBuffer();
         *   boolean      fieldOk     = true;
         *   LogUtil.println(args[i]);
         *   for (int dcIdx = 0; fieldOk && (dcIdx < dataChoices.size());
         *           dcIdx++) {
         *       if ( !doAll && (dcIdx > 0)) {
         *           break;
         *       }
         *       //                LogUtil.println("\tLoop:" + dcIdx);
         *
         *       dataChoice = (DataChoice) dataChoices.get(dcIdx);
         *       DataSelection dataSelection = ggds.getDataSelection();
         *       Data          testData      = null;
         *       try {
         *           testData = ggds.makeFieldImpl(dataChoice, dataSelection);
         *       } catch (Throwable exc) {
         *           fileOk  = false;
         *           fieldOk = false;
         *           notokBuffer.append("<b>Exception reading field:"
         *                              + dataChoice + "</b>");
         *           notokBuffer.append(preText(LogUtil.getStackTrace(exc)));
         *       }
         *       String s = LogUtil.getOutputBuffer(true).trim();
         *       if (s.length() > 0) {
         *           boolean showError = true;
         *           if ( !verbose) {
         *               List lines = StringUtil.split(s, "\n", true, true);
         *               if (lines.size() == 1) {
         *                   if (s.indexOf("Unable to load field") >= 0) {
         *                       showError = false;
         *                   }
         *                   if (s.indexOf("Unknown unit") >= 0) {
         *                       showError = false;
         *                   }
         *               }
         *           }
         *
         *           if (showError) {
         *               fileOk = false;
         *               notokBuffer.append("<b>Reading: "
         *                                  + dataChoice.getId() + "</b>"
         *                                  + preText(s));
         *           }
         *       } else {
         *           okBuffer.append(dataChoice.getId() + " ");
         *       }
         *       if (notokBuffer.indexOf("Exception") >= 0) {
         *           notokBuffer.append("*** Stopping here ***");
         *           break;
         *       }
         *
         *   }
         *   if ( !fileOk) {
         *       errors.append("<a href=\"#" + name + "\">" + args[i]
         *                     + "</a><br>");
         *   }
         *   if (okBuffer.toString().length() > 0) {
         *       //                fileBuffer.append("OK: " + okBuffer.toString() + "<p>");
         *   }
         *   if (notokBuffer.toString().length() > 0) {
         *       fileBuffer.append(notokBuffer.toString());
         *   }
         *   fileBuffer.append("</div>\n");
         *   if ((notokBuffer.toString().length() > 0) || !fileOk) {
         *       buffer.append(fileBuffer.toString());
         *   }
         * }
         * LogUtil.stopOutputBuffer();
         * System.out.println("<html><body><h2>Geogrid test</h2>\n");
         * System.out.println(titleBuffer.toString());
         * if (errors.toString().length() > 0) {
         *   System.out.println("Errors:<br>");
         *   System.out.println(errors.toString());
         * }
         *
         * System.out.println(buffer.toString());
         *
         * System.exit(0);
         */

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
     * Set the ReverseTimes property.
     *
     * @param value The new value for ReverseTimes
     */
    public void setReverseTimes(boolean value) {
        reverseTimes = value;
    }

    /**
     * Get the ReverseTimes property.
     *
     * @return The ReverseTimes
     */
    public boolean getReverseTimes() {
        return reverseTimes;
    }

}

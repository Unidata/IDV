/*
 * $Id: GeoGridDataSource.java,v 1.179 2007/06/18 22:28:35 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


import org.w3c.dom.Element;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.*;


import ucar.unidata.data.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;

import ucar.unidata.idv.chooser.ThreddsHandler;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.*;

import ucar.visad.CachedFlatField;

import ucar.visad.Util;

import visad.Data;
import visad.DataReference;
import visad.DateTime;
import visad.Field;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded3DSet;
import visad.MathType;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import visad.util.DataUtility;

import java.awt.*;

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

import javax.swing.*;



/**
 * Handles gridded files
 *
 * @author IDV Development Team
 * @version $Revision: 1.179 $
 */

public class GeoGridDataSource extends GridDataSource {

    /** Preference */
    public static final String PREF_VERTICALCS = IdvConstants.PREF_VERTICALCS;

    /** grid size */
    public static final String PROP_GRIDSIZE = "prop.gridsize";

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


    /**
     * Default constructor
     */
    public GeoGridDataSource() {}

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


        JTextArea dumpText = new JTextArea();
        dumpText.setFont(Font.decode("monospaced"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ucar.nc2.NCdump.print(dataset.getNetcdfDataset(), "", bos, null);
        } catch (IOException ioe) {
            logException("Dumping netcdf file", ioe);
        }
        dumpText.setText(bos.toString());
        JScrollPane scroller = GuiUtils.makeScrollPane(dumpText, width,
                                   height);
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));
        tabbedPane.add("Metadata", GuiUtils.inset(scroller, 5));
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
     * Set what the user has changed
     *
     * @param paths new paths
     */
    public void xxxxxsetTmpPaths(List paths) {
        //TODO: Figure out what to do here
        String resolverUrl = (String) getProperty(PROP_RESOLVERURL);
        if ((paths.size() > 0) && (resolverUrl != null)
                && (resolverUrl.length() > 0)) {
            setProperty(PROP_RESOLVERURL, paths.get(0).toString());
            return;
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
            String resolvedUrl = ThreddsHandler.resolveUrl(resolverUrl,
                                     properties);
            if (resolvedUrl == null) {
                setInError(true);
                return;
            }
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
        dataset   = null;
        gcsVsTime = null;
    }


    /**
     * Can this DataSource save data to local disk?
     *
     * @return true if this DataSource can save data to local disk?
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased() && (getProperty(PROP_SERVICE_NCSERVER) != null);
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

        if ( !canSaveDataToLocalDisk()) {
            return null;
        }

        List      choices            = getDataChoices();
        List      checkboxes         = new ArrayList();
        List      categories         = new ArrayList();
        Hashtable catMap             = new Hashtable();
        Hashtable currentDataChoices = new Hashtable();

        List      displays = getDataContext().getIdv().getDisplayControls();
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

        List catComps = new ArrayList();
        for (int i = 0; i < categories.size(); i++) {
            List comps = (List) catMap.get(categories.get(i));
            JPanel innerPanel = GuiUtils.doLayout(comps, 3, GuiUtils.WT_NYN,
                                    GuiUtils.WT_N);
            JScrollPane sp = new JScrollPane(GuiUtils.top(innerPanel));
            sp.setPreferredSize(new Dimension(300, 400));
            JPanel top =
                GuiUtils.leftRight(new JLabel(categories.get(i).toString()),
                                   GuiUtils.rLabel("Grid Size (Points)  "));
            catComps.add(GuiUtils.inset(GuiUtils.topCenter(top, sp), 5));


        }


        JComponent contents = GuiUtils.hbox(catComps);
        contents = GuiUtils.topCenter(
            GuiUtils.inset(new JLabel("Select the fields to download"), 5),
            contents);
        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "", contents, null)) {
            return null;
        }

        String ncsAttrs = null;
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dataChoice = (DataChoice) dataChoices.get(i);
            if ( !(dataChoice instanceof DirectDataChoice)) {
                continue;
            }
            JCheckBox cbx = (JCheckBox) checkboxes.get(i);
            if ( !cbx.isSelected()) {
                continue;
            }
            if (ncsAttrs == null) {
                ncsAttrs = "";
            } else {
                ncsAttrs = ncsAttrs + "&";
            }
            ncsAttrs = ncsAttrs + "grid=" + dataChoice.getName();
        }
        if (ncsAttrs == null) {
            return null;
        }

        GeoSelection geoSubset = getDataSelection().getGeoSelection();
        if ((geoSubset != null) && (geoSubset.getBoundingBox() != null)) {
            GeoLocationInfo bbox = geoSubset.getBoundingBox();
            ncsAttrs = ncsAttrs + "&north=" + bbox.getMaxLat() + "&south="
                       + bbox.getMinLat() + "&west=" + bbox.getMinLon()
                       + "&east=" + bbox.getMaxLon();
        }



        String ncsUrl = (String) getProperty(PROP_SERVICE_NCSERVER);
        String wcsUrl = (String) getProperty(PROP_SERVICE_WCS);
        wcsUrl =
            wcsUrl
            + "?service=WCS&version=1.1.0&request=GetCoverage&identifier=grid&format=image/netcdf";
        //&BoundingBox=-71,47,-66,51,urn:ogc:def:crs:OGC:2:84
        System.out.println(wcsUrl);
        ncsUrl = ncsUrl + "?";
        ncsUrl = ncsUrl + ncsAttrs;

        URL url = new URL(ncsUrl);
        List newFiles = IOUtil.writeTo(
                            Misc.newList(url), prefix,
                            IOUtil.getFileExtension(
                                sources.get(0).toString()), loadId);

        if (newFiles != null) {
            if (changeLinks) {
                //Get rid of the resolver URL
                getProperties().remove(PROP_RESOLVERURL);
                getProperties().remove(PROP_SERVICE_NCSERVER);
                getProperties().remove(PROP_SERVICE_WCS);
                setNewFiles(newFiles);
            }
        }
        return newFiles;

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
            if (size != null) {
                int total = size.getSizeY() * size.getSizeX();
                if (size.getSizeZ() > 1) {
                    if (sb3d == null) {
                        sb3d = new StringBuffer();
                    }
                    total *= size.getSizeZ();
                    sb3d.append("<tr><td>" + dataChoice.getName()
                                + "</td><td>" + dataChoice.getDescription()
                                + "</td><td>" + size.getSizeX() + "x"
                                + size.getSizeY() + "x" + size.getSizeZ()
                                + " (" + total + ")");
                } else {
                    if (sb2d == null) {
                        sb2d = new StringBuffer();
                    }
                    sb2d.append("<tr><td>" + dataChoice.getName()
                                + "</td><td>" + dataChoice.getDescription()
                                + "</td><td>" + size.getSizeX() + "x"
                                + size.getSizeY() + " (" + total + ")");
                }

            }
        }
        StringBuffer sb = null;
        if ((sb2d != null) || (sb3d != null)) {
            sb = new StringBuffer(desc);
            sb.append(
                "<p><table><tr><td><b>Field</b></td><td><b>Description</b></td><td><b>Dimensions</b></td></tr>\n");
        }
        if (sb2d != null) {
            sb.append(sb2d);
        }
        if (sb3d != null) {
            sb.append(sb3d);
        }

        if (sb == null) {
            return desc;
        }
        return sb.toString();
    }


    /**
     * Create the dataset from the name of this DataSource.
     *
     * @return new GridDataset
     */
    protected GridDataset doMakeDataSet() {
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
            StringBuffer sb = new StringBuffer();
            sb.append(
                "<netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n");
            sb.append(
                "<aggregation type=\"joinExisting\" dimName=\"time\">\n");
            for (int i = 0; i < sources.size(); i++) {
                String s = sources.get(i).toString();
                try {
                    sb.append("<netcdf location=\""
                              + IOUtil.getURL(s, getClass()) + "\" />\n");
                } catch (IOException ioe) {
                    setInError(true);
                    throw new WrapperException(
                        "Grid data source failed aggregating resource: " + s,
                        ioe);
                }
            }
            sb.append("</aggregation>\n</netcdf>\n");
            file = getDataContext().getObjectStore().getUniqueTmpFile(
                "multigrid", ".ncml");
            try {
                IOUtil.writeFile(file, sb.toString());
            } catch (IOException ioe) {
                logException("Unable to write file: " + file, ioe);
                return null;
            }
            // System.out.println("" + sb);
        }



        try {
            //            System.err.println ("file:" + file);
            GridDataset gds = GridDataset.open(file);
            return gds;
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
                gridRelativeWind = (array.getInt(array.getIndex()) == 8);
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
                        choice.setName("GridRelative_" + choice.getName());
                    }
                }
                addDataChoice(choice);
            }
        }

        //Check if we found any grids
        if (cnt == 0) {
            if (GuiUtils.showOkCancelDialog(
                    null, "No Gridded Data",
                    GuiUtils.inset(
                        new JLabel(
                            "<html>No gridded data found for:<br><br>&nbsp;&nbsp;<i>"
                            + this
                            + "</i><br><br>Do you want to try to load this as another data type?</html>"), 5), null)) {
                getDataContext().getIdv().getDataManager()
                    .createDataSourceAndAskForType(getFilePath(),
                        getProperties());
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
        synchronized (readLock) {
            return makeFieldImpl(dataChoice, givenDataSelection,
                                 requestProperties);
        }
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
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice) {
        try {
            GeoGridAdapter geoGridAdapter = makeGeoGridAdapter(dataChoice,
                                                getDataSelection(), null, -1,
                                                -1);
            if (geoGridAdapter != null) {
                return geoGridAdapter.getLevels();
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
     *
     * @return The GeoGridAdapter
     *
     * @throws VisADException On badness
     */
    private GeoGridAdapter makeGeoGridAdapter(DataChoice dataChoice,
            DataSelection givenDataSelection, Hashtable requestProperties,
            int fromLevelIndex, int toLevelIndex)
            throws VisADException {

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
        GeoSelection geoSelection = ((givenDataSelection != null)
                                     ? givenDataSelection.getGeoSelection()
                                     : null);
        boolean needVolume =
            ((geoGrid.getCoordinateSystem().getVerticalTransform() != null)
             && ((requestProperties != null)
                 && (requestProperties.get(
                     DerivedDataChoice.PROP_FROMDERIVED) != null)));
        // System.out.println("need volume = " + needVolume);


        StringBuffer filename = new StringBuffer("grid_" + paramName);

        try {
            ucar.ma2.Range levelRange = null;
            if ((fromLevelIndex >= 0) && (toLevelIndex >= 0) && !needVolume) {
                levelRange = new ucar.ma2.Range(fromLevelIndex, toLevelIndex);
                filename.append("_r_" + fromLevelIndex + "_" + toLevelIndex);
            }

            if ((geoSelection != null) && geoSelection.getHasValidState()) {
                //System.err.println("subsetting using:" + geoSelection.getLatLonRect());
                extraCacheKey = geoSelection;
                if (levelRange != null) {
                    extraCacheKey = Misc.newList(extraCacheKey, levelRange);
                }
                filename.append("_x_" + geoSelection.getXStrideToUse());
                filename.append("_y_" + geoSelection.getYStrideToUse());
                filename.append("_z_" + geoSelection.getZStrideToUse());
                if (geoSelection.getLatLonRect() != null) {
                    filename.append("_rect_" + geoSelection.getLatLonRect());
                }
                geoGrid = geoGrid.subset(null, levelRange,
                                         geoSelection.getLatLonRect(),
                                         geoSelection.getZStrideToUse(),
                                         geoSelection.getYStrideToUse(),
                                         geoSelection.getXStrideToUse());
            } else if (levelRange != null) {
                extraCacheKey = levelRange;
                geoGrid       = geoGrid.subset(null, levelRange, null, null);
            }
        } catch (InvalidRangeException ire) {
            throw new IllegalArgumentException("Invalid range:" + ire);
        }



        GeoGridAdapter adapter = new GeoGridAdapter(this, geoGrid,
                                     dataChoice.getName(),
                                     dataset.getNetcdfDataset(),
                                     extraCacheKey);

        adapter.cacheFile = filename.toString();
        return adapter;
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


        long millis    = System.currentTimeMillis();
        List allLevels = getAllLevels(dataChoice);

        Trace.call1("GeoGridDataSource.makeField");


        Object fromLevel      = givenDataSelection.getFromLevel();
        Object toLevel        = givenDataSelection.getToLevel();
        int    fromLevelIndex = -1;
        int    toLevelIndex   = -1;
        if ((fromLevel != null) && (toLevel != null)) {
            fromLevelIndex = indexOf(fromLevel, allLevels);
            toLevelIndex   = indexOf(toLevel, allLevels);
        }

        //        System.err.println ("fromLevel:" + fromLevel + " index:" + fromLevelIndex);
        //        Misc.printStack ("index:" + fromLevelIndex + " " +toLevelIndex, 15,null);



        String      paramName = dataChoice.getStringId();
        long        starttime = System.currentTimeMillis();
        FieldImpl   fieldImpl;
        GridDataset myDataset = getDataset();
        if (myDataset == null) {
            return null;
        }
        GeoGrid geoGrid = myDataset.findGridByName(paramName);



        Trace.call1("GeoGridDataSource.make GeoGridAdapter");
        GeoGridAdapter adapter = makeGeoGridAdapter(dataChoice,
                                     givenDataSelection, requestProperties,
                                     fromLevelIndex, toLevelIndex);
        Trace.call2("GeoGridDataSource.make GeoGridAdapter");


        Trace.call1("GeoGridDataSource.make times");
        List times = getTimesFromDataSelection(givenDataSelection,
                         dataChoice);

        int[] timeIndices = null;
        List  allTimes    = null;
        if (times != null) {
            timeIndices = new int[times.size()];
            allTimes =
                getGeoGridTimes((CoordinateAxis1DTime) geoGrid
                    .getCoordinateSystem().getTimeAxis1D());
            if (holdsIndices(times)) {
                for (int i = 0; i < times.size(); i++) {
                    timeIndices[i] = ((Integer) times.get(i)).intValue();
                }
            } else {
                for (int i = 0; i < times.size(); i++) {
                    timeIndices[i] = allTimes.indexOf(times.get(i));
                }
            }
        }
        Trace.call2("GeoGridDataSource.make times");

        Trace.call1("GeoGridDataSource.getSequence");
        Object loadId = JobManager.getManager().startLoad("GeoGrid");
        fieldImpl = adapter.getSequence(timeIndices, loadId);

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
            LogUtil.userMessage(log_, "Unable to load field: " + paramName,
                                true);
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
            if ((sizeZ == 0) || (sizeZ == 1)) {
                //if (sizeZ == 0) {
                int xLength = cfield.getXDimension().getLength();
                int yLength = cfield.getYDimension().getLength();
                if (twoDDimensionsLabel == null) {
                    twoDDimensionsLabel = "Total grid size:  x: " + xLength
                                          + "  y: " + yLength
                                          + "    #points: "
                                          + (xLength * yLength);
                }
                Hashtable props = new Hashtable(twoDProps);
                props.put(PROP_GRIDSIZE, new ThreeDSize(xLength, yLength));
                choice = new DirectDataChoice(this, parmName, pseudoName,
                        description, (tAxis == null)
                                     ? getTwoDCategories()
                                     : getTwoDTimeSeriesCategories(), dataSelection,
                                     props);

            } else {  // if (sizeZ > 1)
                // Have 3D field (we expect); usually sizeZ > 1:
                int xLength = cfield.getXDimension().getLength();
                int yLength = cfield.getYDimension().getLength();
                int zLength = cfield.getZDimension().getLength();
                if (xLength * yLength * zLength > max3D) {
                    max3D  = xLength * yLength * zLength;
                    max3DX = xLength;
                    max3DY = yLength;
                    max3DZ = zLength;
                }
                ThreeDSize size  = new ThreeDSize(xLength, yLength, zLength);
                Hashtable  props = new Hashtable(threeDProps);
                props.put(PROP_GRIDSIZE, size);
                choice = new DirectDataChoice(this, parmName, pseudoName,
                        description, (tAxis == null)
                                     ? getThreeDCategories()
                                     : getThreeDTimeSeriesCategories(), dataSelection,
                                     props);

            }
        }
        return choice;
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
    protected static boolean testMode = false;

    /**
     * Test this class by running
     * "java ucar.unidata.data.grid.GeoGridDataSource <filename>"
     *
     * @param args  filename
     *
     * @throws Exception  some error occurred
     */
    public static void main(String[] args) throws Exception {

        GridDataset dataset    = GridDataset.open("elev.nc");
        GeoGrid     geoGrid    = dataset.findGridByName("foo");
        GeoGrid     geoGrid50  = geoGrid.subset(null, null, null, 0, 50, 50);
        GeoGrid     geoGrid100 = geoGrid.subset(null, null, null, 0, 100,
                                     100);


        System.exit(0);



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



}


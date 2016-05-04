/*
 * Copyright 1997-2015 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.imagery;


import edu.wisc.ssec.mcidas.*;

import edu.wisc.ssec.mcidas.adde.AddeImageURL;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;

import ucar.unidata.data.*;


import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;


import ucar.unidata.util.TwoFacedObject;

import ucar.visad.data.AreaImageFlatField;

import visad.*;

import visad.data.DataRange;
import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;

import visad.meteorology.SingleBandedImage;

import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.util.*;


import javax.swing.*;


/**
 * A data source for ADDE images AREA files. This is a thin wrapper (derived class) around the ImageDataSource
 * which does all of the work.
 *
 * @author Don Murray
 * @version $Revision: 1.26 $ $Date: 2006/12/01 20:42:05 $
 */
public class McIDASImageDataSource extends ImageDataSource {

    /** _more_ */
    DataChoice dataChoice;

    /** _more_ */
    AddeImageDescriptor descriptor;

    /** _more_ */
    DataSourceDescriptor dataSourceDescriptor;

    /** _more_ */
    private AreaDirectory[][] currentDirs;

    /** _more_ */
    private Hashtable timeMap = new Hashtable();

    /** _more_ */
    protected Hashtable bandDirs;

    /** _more_ */
    private boolean useSatBandInfo = true;

    /** _more_ */
    private List<BandInfo> bandInfos;

    /** _more_ */
    private AddeSatBands satBandInfo;

    /** _more_ */
    int[] calblc;

    /** _more_ */
    Calibrator cali;

    /** _more_ */
    String[] calUnitDesp = {
        " ", "Raw", "Radiance", "Albedo", "Temperature", "Brightness",
        "Reflectivity"
    };

    /** _more_ */
    AddeImageInfo imageInfo;

    /** _more_ */
    boolean isPreCalibrated = false;

    /** _more_ */
    AddeImageInfo previewImageInfo;

    /** _more_ */
    private String source;

    /** _more_ */
    AreaAdapter areaAdapter = null;

    /** _more_ */
    AREAnav baseAnav = null;

    /** _more_ */
    private AREACoordinateSystem acs;

    /** _more_ */
    AreaFile af;

    /** _more_ */
    protected AddeImageDataSelection addeImageDataSelection;

    /** _more_ */
    BandInfo bandId;

    /** _more_ */
    Hashtable allBandDirs;

    /** _more_ */
    int elem0 = 0;

    /** _more_ */
    int line0 = 0;

    /** _more_ */
    int elems = 0;

    /** _more_ */
    int lines = 0;

    /** _more_ */
    int eMag = 1;  // the one to be used for adapter and

    /** _more_ */
    int lMag = 1;  // it is equal to eMag0 if non AR

    /** _more_ */
    int eMag0 = 1;  // this value is hard coded in the VisAD

    /** _more_ */
    int lMag0 = 1;

    /** _more_ */
    private String choiceName;

    /**
     *  The parameterless ctor unpersisting.
     */
    public McIDASImageDataSource() {}


    /**
     *  Create a new McIDASImageDataSource with  a single AREA file.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  image AREA file
     *  @param properties The properties for this data source.
     */


    /** _more_ */
    AreaDirectoryList adl1;

    /**
     * Construct a McIDASImageDataSource
     *
     * @param descriptor _more_
     * @param image _more_
     * @param properties _more_
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 String image, Hashtable properties) {
        super(descriptor, new String[] { image }, properties);

        try {
            this.areaAdapter = new AreaAdapter(image, false);
            this.af          = this.areaAdapter.getAreaFile();
            this.baseAnav    = this.af.getNavigation();
            this.acs =
                (AREACoordinateSystem) this.areaAdapter.getCoordinateSystem();
            af = AreaFileFactory.getAreaFileInstance(image);
            AreaDirectoryList adl = new AreaDirectoryList(image);
            currentDirs = adl.getSortedDirs();
        } catch (Exception ee) {}
        ;
        this.descriptor = new AddeImageDescriptor(af.getAreaDirectory(),
                image);
        this.dataSourceDescriptor = descriptor;

        init();
    }

    /**
     *  Create a new McIDASImageDataSource with list of  AREA files.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images List of AREA files
     *  @param properties The properties for this data source.
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 ArrayList images, Hashtable properties) {
        super(descriptor, StringUtil.listToStringArray(images), properties);

        String image = (String) images.get(0);
        try {
            this.areaAdapter = new AreaAdapter(image, false);
            af               = AreaFileFactory.getAreaFileInstance(image);
            this.baseAnav    = this.af.getNavigation();
            AreaDirectoryList adl = new AreaDirectoryList(image);
            currentDirs = adl.getSortedDirs();
        } catch (Exception ee) {}
        ;
        this.descriptor = new AddeImageDescriptor(af.getAreaDirectory(),
                image);
        this.dataSourceDescriptor = descriptor;

        init();
    }


    /**
     *  Create a new McIDASImageDataSource with array of  AREA files.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of AREA files
     *  @param properties The properties for this data source.
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 String[] images, Hashtable properties) {
        super(descriptor, images, properties);
    }

    /**
     *  Create a new McIDASImageDataSource with a {@link ImageDataset}
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  ids The dataset.
     *  @param properties The properties for this data source.
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 ImageDataset ids, Hashtable properties) {
        super(descriptor, ids, properties);
    }

    /**
     * _more_
     */
    public void init() {
        bandDirs    = new Hashtable(1);
        allBandDirs = new Hashtable();
        bandInfos   = new ArrayList<BandInfo>();
        //satBandInfo = this.descriptor.getDirectory().
        AreaDirectory dir   = this.descriptor.getDirectory();
        int[]         bands = dir.getBands();
        dir.getCalibrationType();
        calblc = af.getCal();
        int    lines       = dir.getLines();
        int    elems       = dir.getElements();
        int    cline       = lines / 2;
        int    celem       = elems / 2;

        String locateValue = cline + " " + celem;
        this.previewImageInfo = getPreviewImageInfo(dir,
                AddeImageURL.KEY_LINEELE);
        lines = lines / previewImageInfo.getLineMag();
        elems = elems / previewImageInfo.getElementMag();
        this.imageInfo = new AddeImageInfo(AddeImageURL.KEY_LINEELE,
                                           locateValue, "CENTER", lines,
                                           elems,
                                           previewImageInfo.getLineMag(),
                                           previewImageInfo.getElementMag());
        //String locateKey,  String locateValue, String placeValue, int lines, int elements, int lmag, int emag)
        // this.source = getPreviewSource(aid.getSource(), aid.getDirectory());
        try {
            if (calblc == null) {
                isPreCalibrated = true;
                cali = CalibratorFactory.getCalibrator(dir.getSensorID(),
                        calblc);
                bandInfos = makeBandInfos(dir, bands, cali);
                Hashtable pt = getProperties();
                pt.put(ImageDataSource.PROP_BANDINFO, bandInfos);
            } else {
                cali = CalibratorFactory.getCalibrator(dir.getSensorID(),
                        calblc);
                bandInfos = makeBandInfos(dir, bands, cali);
                Hashtable pt = getProperties();
                pt.put(ImageDataSource.PROP_BANDINFO, bandInfos);
            }
        } catch (Exception e) {}
        //Hashtable pt = getProperties();
        //pt.put(ImageDataSource.PROP_BANDINFO, bandInfos);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getChoiceName() {
        return this.choiceName;
    }

    /**
     * _more_
     *
     * @param choiceName _more_
     */
    public void setChoiceName(String choiceName) {
        this.choiceName = choiceName;
    }


    /**
     * _more_
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        if (getTmpPaths() != null) {  // zidv bundle
            return;
        }
        //String ver = IdvPersistenceManager.getBundleIdvVersion();
        // ver == null is quicklinks history

        if ((this.source == null) && (imageList != null)
                && (imageList.size() > 0)) {
            List descriptors =
                super.getDescriptors(super.findDataChoice(this.choiceName),
                                     null);
            AddeImageDescriptor desc1 =
                (AddeImageDescriptor) descriptors.get(0);
            this.source = desc1.getSource();
            try {
                this.areaAdapter = new AreaAdapter(this.source, false);
                this.af          = this.areaAdapter.getAreaFile();
                this.baseAnav    = this.af.getNavigation();
                this.acs =
                    (AREACoordinateSystem) this.areaAdapter
                        .getCoordinateSystem();
                af = AreaFileFactory.getAreaFileInstance(this.source);
                AreaDirectoryList adl = new AreaDirectoryList(this.source);
                currentDirs = adl.getSortedDirs();
            } catch (Exception ew) {}
            allBandDirs = (Hashtable) getProperties().get("allBands");
            ArrayList oj = (ArrayList) getProperties().get("bandinfo");
            if (oj != null) {
                this.bandId = (BandInfo) oj.get(0);
            }

            AreaDirectory thisDir = desc1.getDirectory();
            this.descriptor = new AddeImageDescriptor(thisDir, null);
            init();
        }
    }


    /**
     * _more_
     *
     * @param ad _more_
     * @param bands _more_
     * @param cb _more_
     *
     * @return _more_
     *
     * @throws CalibratorException _more_
     */
    private List<BandInfo> makeBandInfos(AreaDirectory ad, int[] bands,
                                         Calibrator cb)
            throws CalibratorException {
        List<BandInfo> l = new ArrayList<BandInfo>();
        if ((ad != null) && (cb != null)) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int band = bands[i];
                    //Calibrator cb = CalibratorFactory.getCalibrator(ad.getSensorID(),calblc);
                    BandInfo bi = new BandInfo(ad.getSensorID(), band);
                    bi.setBandDescription(getBandName(ad, band));
                    bi.setCalibrationUnits(
                        getAvailableUnits(
                            cb.calibratedList(band, isPreCalibrated)));
                    bi.setPreferredUnit("BRIT");
                    l.add(bi);
                }
            }
        } else if (ad != null) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int      band = bands[i];
                    BandInfo bi   = new BandInfo(ad.getSensorID(), band);
                    bi.setBandDescription(getBandName(ad, band));
                    bi.setCalibrationUnits(null);
                    bi.setPreferredUnit("BRIT");
                    l.add(bi);
                }
            }
        }
        return l;
    }

    /**
     * _more_
     *
     * @param ulist _more_
     *
     * @return _more_
     */
    private List<TwoFacedObject> getAvailableUnits(int[] ulist) {
        // get Vector array of Calibration types.   Layout is
        // v[i] = band[i] and for each band, it is a vector of
        // strings of calibration names and descriptions
        // n = name, n+1 = desc.
        // for radar, we only have one band
        Vector<TwoFacedObject> l = new Vector<TwoFacedObject>();

        if (ulist == null) {
            return new ArrayList<TwoFacedObject>();
        }

        TwoFacedObject tfo                = null;
        int            preferredUnitIndex = 0;
        String         preferredUnit      = "BRIT";

        for (int i = 0; i < ulist.length; i++) {
            String name = (String) AreaFileFactory.calIntToStr(ulist[i]);
            String desc = (String) calUnitDesp[ulist[i]];
            //dAreaFileFactory.calIntToStr(ulist[i]);
            //desc = desc.substring(0, 1).toUpperCase()
            //        + desc.substring(1).toLowerCase();
            tfo = new TwoFacedObject(desc, name);
            l.add(tfo);
            if (name.equalsIgnoreCase(preferredUnit)) {
                preferredUnitIndex = i;
            }
        }

        return l;
    }

    /**
     * _more_
     *
     * @param ad _more_
     * @param band _more_
     *
     * @return _more_
     */
    private String getBandName(AreaDirectory ad, int band) {
        // if (band== 0) return ALLBANDS.toString();


        if (satBandInfo == null) {
            return "Band: " + band;
        }
        String[] descrs = satBandInfo.getBandDescr(ad.getSensorID(),
                              ad.getSourceType());
        if (descrs != null) {
            if ((band >= 0) && (band < descrs.length)) {
                return descrs[band];
            }
        }
        return "Band: " + band;


    }

    /**
     *  Overwrite base class  method to return the name of this class.
     *
     *  @return The name.
     */
    public String getImageDataSourceName() {
        return "McIDAS Image dataset";
    }


    /**
     * Get the name for the main data object
     *
     * @return name of main data object
     */
    public String getDataName() {
        /*  TODO: Flesh this out
        List images = getImages();
        if (images != null) {
            Object o = images.get(0);
            if (o instanceof AddeImageDescriptor) {
                AreaDirectory ad = ((AddeImageDescriptor) o).getDirectory();
                return "Band " + ad.getBands()[0];
            }
        }
        */
        return "All Images";
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

        this.dataChoice = dataChoice;
        this.choiceName = dataChoice.getName();
        GeoSelection geoSelection = dataSelection.getGeoSelection();

        boolean isProgressiveResolution =
            dataSelection.getProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION, true);
        String t1 =
            dataSelection.getProperty(DataSelection.PROP_REGIONOPTION,
                                      DataSelection.PROP_USEDEFAULTAREA);
        int dlMag = 0;
        int deMag = 0;
        if ( !isProgressiveResolution) {
            dlMag =
                addeImageDataSelection.getAdvancedPanel().getLineMagValue();
            // .lineMagLbl.getText().trim());
            deMag =
                addeImageDataSelection.getAdvancedPanel()
                    .getElementMagValue();
        }

        if (geoSelection != null) {
            Rectangle2D rect = geoSelection.getScreenBound();
            ImageDataSelectionInfo adSource =
                (ImageDataSelectionInfo) dataSelection.getProperty(
                    "advancedURL");

            if ((geoSelection.getBoundingBox() == null)
                    && (adSource != null)) {
                String locationKey = adSource.getLocateKey();
                deMag = adSource.getElementMag();
                dlMag = adSource.getLineMag();

                String placeValue = adSource.getPlaceValue();
                // first set the ULEFT point elem0 and line0
                if (locationKey.equals("LATLON")) {

                    String     locValue = adSource.getLocateValue();
                    String[]   segs     = locValue.split(" ");
                    String     seg0     = segs[0];
                    String     seg1     = segs[1];

                    double[][] ll       = new double[2][1];
                    ll[0][0] = Float.parseFloat(seg0);
                    ll[1][0] = Float.parseFloat(seg1);
                    double[][] el = baseAnav.toLinEle(ll);

                    elem0 = (int) Math.floor(el[0][0] + 0.5)
                            * Math.abs(eMag0);
                    line0 = (int) Math.floor(el[1][0] + 0.5)
                            * Math.abs(lMag0);
                } else {
                    elem0 = adSource.getLocationElem();
                    line0 = adSource.getLocationLine();
                }

                int lines0 = adSource.getLines() * Math.abs(dlMag);
                int elems0 = adSource.getElements() * Math.abs(deMag);

                if (placeValue.equals("CENTER")) {
                    elem0 = elem0 - elems0 / 2;
                    line0 = line0 - lines0 / 2;
                }

                if (isProgressiveResolution) {
                    lMag = calculateMagFactor(lines0, (int) rect.getHeight());
                    eMag = lMag * elFactor;
                } else {
                    eMag = Math.abs(deMag);
                    lMag = Math.abs(dlMag);
                }

                lines = (int) Math.floor(lines0 / lMag + 0.5);
                elems = elems0 / eMag;

                if (lMag == 1) {
                    lines = lines0;
                } else {
                    lines = (int) Math.floor(lines0 / lMag + 0.5);
                }

                if (eMag == 1) {
                    elems = elems0;
                } else {
                    elems = (int) Math.floor(elems0 / eMag + 0.5);
                }

            } else if (t1.equals(DataSelection.PROP_USEDISPLAYAREA)
                       || (geoSelection.getBoundingBox() != null)
                       || geoSelection.getUseViewBounds()) {

                double maxLat = geoSelection.getBoundingBox().getMaxLat();
                double minLat = geoSelection.getBoundingBox().getMinLat();
                double maxLon = geoSelection.getBoundingBox().getMaxLon();
                double minLon = geoSelection.getBoundingBox().getMinLon();
                maxLat = (maxLat > 90)
                         ? 90
                         : maxLat;
                minLat = (minLat < -90)
                         ? -90
                         : minLat;
                maxLon = (maxLon >= 180)
                         ? 180
                         : maxLon;
                minLon = (minLon < -180)
                         ? -180
                         : minLon;

                GeoLocationInfo mapInfo = new GeoLocationInfo(maxLat, minLon,
                                              minLat, maxLon);
                LatLonPointImpl llp =
                    (LatLonPointImpl) dataSelection.getProperty(
                        "centerPosition");

                if (llp != null) {
                    BandInfo      id      = (BandInfo) dataChoice.getId();
                    int[]         dir     = null;
                    AreaDirectory thisDir = null;
                    if ((allBandDirs != null) && (allBandDirs.size() > 0)) {
                        thisDir = (AreaDirectory) allBandDirs.get(
                            id.getBandNumber());

                    } else {
                        thisDir = this.descriptor.getDirectory();
                    }
                    dir = thisDir.getDirectoryBlock();
                    // boolean inside = insideImageBoundingBox(llp.getLatitude(), llp.getLongitude()) ;
                    GeoLocationInfo ginfo = getImageBoundingBox(1, 1, dir[8],
                                                dir[9]);

                    if (ginfo.getLatLonRect().containedIn(
                            mapInfo.getLatLonRect())) {

                        //this is when the map area is large and totally covered the image area
                        elem0 = 0;
                        line0 = 0;

                        int lines0 = dir[8];  //2726
                        int elems0 = dir[9];  //1732

                        if (isProgressiveResolution) {
                            //&& locationKey.equals("LALO")) {
                            //eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                            lMag = calculateMagFactor(lines0,
                                    (int) rect.getHeight());
                            lMag = (lMag >= 2)
                                   ? lMag / 2
                                   : 1;
                            eMag = lMag * elFactor;
                        } else {
                            eMag = Math.abs(deMag);
                            lMag = Math.abs(dlMag);
                        }

                        if (lMag == 1) {
                            lines = lines0;
                        } else {
                            lines = (int) Math.floor(lines0 / lMag + 0.5);
                        }

                        if (eMag == 1) {
                            elems = elems0;
                        } else {
                            elems = (int) Math.floor(elems0 / eMag + 0.5);
                        }
                        //System.out.println("newLine X newElement : "
                        //                   + newLines + " " + newelems);

                    } else {
                        LatLonRect bbox = mapInfo.getLatLonRect().intersect(
                                              ginfo.getLatLonRect());
                        if (mapInfo.getLatLonRect().containedIn(
                                ginfo.getLatLonRect())) {
                            bbox = mapInfo.getLatLonRect();
                        }

                        if (bbox == null) {
                            bbox = mapInfo.getLatLonRect();
                        } else {
                            boolean con = ginfo.getLatLonRect().contains(llp);
                            if ( !con) {
                                llp = new LatLonPointImpl(
                                    thisDir.getCenterLatitude(),
                                    thisDir.getCenterLongitude());

                            }
                        }
                        maxLat = bbox.getLatMax();
                        minLat = bbox.getLatMin();
                        maxLon = bbox.getLonMax();
                        minLon = bbox.getLonMin();

                        // ULEFT conner
                        double[][] ll = new double[2][1];
                        ll[0][0] = maxLat;
                        ll[1][0] = minLon;
                        double[][] el = baseAnav.toLinEle(ll);

                        elem0 = (int) Math.floor(el[0][0] + 0.5)
                                * Math.abs(eMag0);
                        line0 = (int) Math.floor(el[1][0] + 0.5)
                                * Math.abs(lMag0);

                        //lines
                        float[][] latlon = new float[2][1];
                        float[][] lrLinEle;
                        float[][] ulLinEle;
                        latlon[1][0] = (float) llp.getLongitude();
                        latlon[0][0] = (float) maxLat;
                        ulLinEle     = baseAnav.toLinEle(latlon);
                        if ((ulLinEle[1][0] < 0)
                                || (ulLinEle[1][0] != ulLinEle[1][0])) {
                            ulLinEle[1][0] = 0;
                        }

                        latlon[1][0] = (float) llp.getLongitude();
                        latlon[0][0] = (float) minLat;
                        lrLinEle     = baseAnav.toLinEle(latlon);
                        if ((lrLinEle[1][0] < 0)
                                || (lrLinEle[1][0] != lrLinEle[1][0])) {
                            int ln = dir[8];
                            lrLinEle[1][0] = ln / Math.abs(lMag0);
                        }

                        int lines0 = (int) (lrLinEle[1][0] - ulLinEle[1][0])
                                     * Math.abs(lMag0);

                        //elems
                        latlon[1][0] = (float) minLon;
                        latlon[0][0] = (float) llp.getLatitude();
                        ulLinEle = this.acs.fromReference(latlon);  //baseAnav.toLinEle(latlon);
                        if ((ulLinEle[0][0] < 0)
                                || (ulLinEle[0][0] != ulLinEle[0][0])) {
                            ulLinEle[0][0] = 0;
                        }

                        latlon[1][0] = (float) maxLon;
                        latlon[0][0] = (float) llp.getLatitude();
                        lrLinEle = this.acs.fromReference(latlon);  //baseAnav.toLinEle(latlon);
                        if ((lrLinEle[0][0] < 0)
                                || (lrLinEle[0][0] != lrLinEle[0][0])) {
                            int en = dir[9];
                            lrLinEle[0][0] = en / Math.abs(eMag0);
                        }

                        int elems0 = (int) Math.abs(lrLinEle[0][0]
                                         - ulLinEle[0][0]) * Math.abs(eMag0);

                        //lMag & eMag
                        if (isProgressiveResolution) {
                            //&& locationKey.equals("LALO")) {
                            //eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                            lMag = calculateMagFactor(lines0,
                                    (int) rect.getHeight());
                            lMag = (lMag >= 2)
                                   ? lMag / 2
                                   : 1;
                            eMag = lMag * elFactor;
                        } else {
                            eMag = Math.abs(deMag);
                            lMag = Math.abs(dlMag);
                        }

                        //
                        if (lMag == 1) {
                            lines = lines0;
                        } else {
                            lines = (int) Math.floor(lines0 / lMag + 0.5);
                        }

                        if (eMag == 1) {
                            elems = elems0;
                        } else {
                            elems = (int) Math.floor(elems0 / eMag + 0.5);
                        }

                        // check
                        if (lines > dir[8]) {
                            lines = dir[8];
                        }
                        if (elems > dir[9]) {
                            elems = dir[9];
                        }

                    }

                }

            }
        }

        Data imageData = super.getDataInner(dataChoice, category,
                                            dataSelection, requestProperties);


        return imageData;
    }


    /**
     * _more_
     *
     * @param eMag _more_
     * @param lMag _more_
     * @param lines _more_
     * @param elems _more_
     *
     * @return _more_
     */
    public GeoLocationInfo getImageBoundingBox(int eMag, int lMag, int lines,
            int elems) {
        double     maxLat = 0;
        double     minLat = 0;
        double     maxLon = 0;
        double     minLon = 0;


        double[][] ll;

        double[][] el    = new double[2][1];
        int        eSize = elems / eMag;
        int        lSize = lines / lMag;
        try {
            // minlon  using upper left corner
            int i = 0;
            int j = 0;
            el[0][0] = i;
            el[1][0] = j;
            ll       = baseAnav.toLatLon(el);
            while ((ll[0][0] != ll[0][0]) && (i < eSize) && (j < lSize)) {
                i++;
                j++;
                el[0][0] = i;
                el[1][0] = j;
                ll       = baseAnav.toLatLon(el);
            }
            if (ll[0][0] != ll[0][0]) {
                minLon = -180;
            } else {
                minLon = ll[1][0];
            }

            //maxlon using lower right corner
            i        = eSize;
            j        = lSize;
            el[0][0] = i;
            el[1][0] = j;
            ll       = baseAnav.toLatLon(el);
            while ((ll[0][0] != ll[0][0]) && (i > 0) && (j > 0)) {
                i--;
                j--;
                el[0][0] = i;
                el[1][0] = j;
                ll       = baseAnav.toLatLon(el);
            }
            if (ll[0][0] != ll[0][0]) {
                maxLon = 180;
            } else {
                maxLon = ll[1][0];
            }

            //maxlat   using middle line
            i        = eSize / 2;
            j        = 0;
            el[0][0] = i;
            el[1][0] = j;
            ll       = baseAnav.toLatLon(el);
            while ((ll[0][0] != ll[0][0]) && (j < lSize)) {
                j++;
                el[0][0] = i;
                el[1][0] = j;
                ll       = baseAnav.toLatLon(el);
            }
            if (ll[0][0] != ll[0][0]) {
                maxLat = 90.0;
            } else {
                maxLat = ll[0][0];
            }

            //minlat   using middle line
            i        = eSize / 2;
            j        = lSize;
            el[0][0] = i;
            el[1][0] = j;
            ll       = baseAnav.toLatLon(el);
            while ((ll[0][0] != ll[0][0]) && (j > 0)) {
                j--;
                el[0][0] = i;
                el[1][0] = j;
                ll       = baseAnav.toLatLon(el);
            }
            if (ll[0][0] != ll[0][0]) {
                minLat = -90.0;

            } else {
                minLat = ll[0][0];
            }
        } catch (Exception e) {
            System.out.println("convertToLatLon e=" + e);
        }

        return new GeoLocationInfo(maxLat, minLon, minLat, maxLon);
    }


    /**
     * _more_
     *
     * @param dataPoints _more_
     * @param displayPoints _more_
     *
     * @return _more_
     */
    public int calculateMagFactor(int dataPoints, int displayPoints) {
        if (dataPoints <= displayPoints) {
            return 1;
        } else {
            int factor = (int) Math.floor((1.0 * dataPoints)
                                          / (1.0 * displayPoints) + 0.8);
            return factor;
        }
    }

    /**
     * _more_
     *
     * @param aid _more_
     * @param rangeType _more_
     * @param fromSequence _more_
     * @param readLabel _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected SingleBandedImage makeImage(AddeImageDescriptor aid,
                                          MathType rangeType,
                                          boolean fromSequence,
                                          String readLabel)
            throws VisADException, RemoteException {

        String   desc       = dataChoice.getDescription();
        int      calOutType = Arrays.asList(calUnitDesp).indexOf(desc);
        BandInfo binfo;
        Object   id   = dataChoice.getId();
        int      band = 0;

        if ((cali != null)) {
            binfo = (BandInfo) id;
            band  = binfo.getBandNumber();
        }


        if (aid == null) {
            return null;
        }

        //if (dataChoice instanceof CompositeDataChoice) {
        // binfo = ((CompositeDataChoice) dataChoice).getDataChoices().get(0).g
        //}
        String source = aid.getSource() + " band: " + band + " calOutType: "
                        + calOutType + " line0 " + line0 + " elem0 " + elem0
                        + " lines " + lines + " elems " + elems;

        SingleBandedImage result = (SingleBandedImage) getCache(source);
        if (result != null) {
            return result;
        }

        try {
            AddeImageInfo aii     = aid.getImageInfo();
            AreaDirectory areaDir = null;


            if (areaDir == null) {
                areaDir = aid.getDirectory();
            }

            if ( !getCacheDataToDisk()) {
                areaDir = null;
            }

            if ((areaDir != null) && (cali != null)) {
                String unitStr = cali.calibratedUnit(calOutType);
                Unit   unit    = null;
                if (unitStr != null) {
                    unit = DataUtil.parseUnit(unitStr);
                }
                if (line0 < 0) {
                    line0 = 1;
                }
                if (elem0 < 0) {
                    elem0 = 1;
                }
                int elemTotal = areaDir.getElements();
                int lineTotal = areaDir.getLines();
                while ((lMag * lines + line0) > lineTotal) {
                    lines--;
                }
                while ((eMag * elems + elem0) > elemTotal) {
                    elems--;
                }
                result = AreaImageFlatField.createImmediateWithUnit(aid,
                        readLabel, unit, line0, elem0, lines, elems, lMag,
                        eMag, band);
                timeMap.put(aid.getSource(), result.getStartTime());

            }

            float[][] data0;
            if ( !isPreCalibrated && (cali != null)) {
                data0 = result.getFloats();
                float[]   data1 = cali.calibrate(data0[0], band, calOutType);
                float[][] data2 = new float[1][data1.length];
                data2[0] = data1;
                result.setSamples(data2);
            } else if (isPreCalibrated
                       && (calOutType == Calibrator.CAL_TEMP)) {
                data0 = result.getFloats();
                float[]   data1 = convertBritToTemp(data0[0]);
                float[][] data2 = new float[1][data1.length];
                data2[0] = data1;
                result.setSamples(data2);
            } else if ((areaDir == null) || (cali == null)) {
                AreaAdapter aa = new AreaAdapter(aid.getSource(), false);
                timeMap.put(aid.getSource(), aa.getImageStartTime());
                result = aa.getImage();
            }

            putCache(source, result);
            return result;

        } catch (java.io.IOException ioe) {
            throw new VisADException("Creating AreaAdapter - " + ioe);
        }

    }


    /**
     * _more_
     *
     * @param components _more_
     * @param dataChoice _more_
     */
    protected void initDataSelectionComponents(List components,
            DataChoice dataChoice) {

        try {

            BandInfo id = null;
            if ((dataChoice.getId() instanceof BandInfo)
                    && (allBandDirs != null)) {
                id = (BandInfo) dataChoice.getId();
                if (this.bandId == null) {
                    this.bandId = id;
                }
            } else {
                return;
            }

            /*  if ((id != null) && !id.equals(this.bandId)) {
                  // now different band selected, and the preview and advanced need to be recreated

                  AreaDirectory thisDir =
                      (AreaDirectory) allBandDirs.get(id.getBandNumber());
                  //this.source = getPreviewSource(this.source, thisDir);

                  this.descriptor = new AddeImageDescriptor(thisDir, null);

              } */

            if ((baseAnav == null) || !id.equals(this.bandId)) {

                try {
                    areaAdapter = new AreaAdapter(this.source, false);
                    AreaFile areaFile = areaAdapter.getAreaFile();
                    baseAnav = areaFile.getNavigation();
                    acs      = new AREACoordinateSystem(areaFile);
                } catch (Exception e) {
                    // LogUtil.userErrorMessage(
                    //     "Error in initDataSelectionComponents  e=" + e);
                }

                this.bandId = id;
            }

            //if (areaAdapter != null) {
            addeImageDataSelection = new AddeImageDataSelection(this,
                    dataChoice, source, baseAnav, this.imageInfo, acs,
                    areaAdapter, cali, this.bandId.getBandNumber());
            //}



        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                LogUtil.getCurrentWindow(), ex.getMessage(),
                "Error in initDataSelectionComponents 2", 0);
            getDataContext().getIdv().showNormalCursor();
            return;
        }
        if (areaAdapter != null) {
            components.add(addeImageDataSelection.getRegionPanel());
            components.add(addeImageDataSelection.getAdvancedPanel());
        }
    }

    /** _more_ */
    int elFactor = 1;

    /**
     * _more_
     *
     * @param navType _more_
     * @param aDir _more_
     *
     * @return _more_
     */
    public AddeImageInfo getPreviewImageInfo(AreaDirectory aDir,
                                             String navType) {

        AddeImageInfo adinfo;
        int[]         dir    = aDir.getDirectoryBlock();

        int           inLine = dir[8];  //Integer.parseInt(lineStr);
        int           inElem = dir[9];  //Integer.parseInt(elemStr);
        int inLineMag        = 1;  //  = Integer.parseInt(strTok1.nextToken());
        int inElemMag        = 1;  //  = Integer.parseInt(strTok1.nextToken());
        if (aDir.getCenterLatitudeResolution()
                == aDir.getCenterLatitudeResolution()) {
            elFactor =
                (int) Math.ceil(aDir.getCenterLatitudeResolution()
                                / aDir.getCenterLongitudeResolution() - 0.5);
        }

        // System.out.println("Line and element ratio = " + elFactor);
        int lineFactor = 1;
        int elemFactor = 1;

        int outElem    = inElem;
        int outLine    = inLine;

        if (navType.equals("LALO")) {
            while (outLine > 450) {
                lineFactor += 1;
                outLine    = inLine / lineFactor;
            }
            inLineMag *= lineFactor;
            inElemMag = inLineMag / elFactor;
            if (inElemMag == 0) {
                inElemMag = 1;
            }
            outElem = inElem / inElemMag;
        } else {
            while (outElem > 450) {
                elemFactor += 1;
                outElem    = inElem / elemFactor;
            }
            inElemMag *= elemFactor;
            inLineMag = inElemMag / elFactor;
            if (inLineMag == 0) {
                inLineMag = 1;
            }
            outLine = inLine / inLineMag;
        }
        // alway in the center of the image and this is why it is divided by 2
        int    cline       = inLine / 2;
        int    celem       = inElem / 2;
        int    eMag        = inElemMag;
        int    lMag        = inLineMag;
        String locateValue = cline + " " + celem;

        adinfo = new AddeImageInfo(AddeImageURL.KEY_LINEELE, locateValue,
                                   "CENTER", outLine, outElem, lMag, eMag);
        //String locateKey,  String locateValue, String placeValue, int lines, int elements, int lmag, int emag)
        return adinfo;

    }

    /**
     * _more_
     *
     * @param inVal _more_
     *
     * @return _more_
     */
    public float convertBritToTemp(int inVal) {

        int   con1 = 418;
        int   con2 = 660;
        int   ilim = 176;

        float outVal;
        if (inVal > ilim) {
            outVal = con1 - inVal;
        } else {
            outVal = (con2 - inVal) / 2;
        }

        return (outVal);
    }

    /**
     *
     * convert a gray scale value to brightness temperature
     *
     * @param inputData   input data array
     *
     *
     * @return _more_
     */
    public float[] convertBritToTemp(float[] inputData) {

        // create the output data buffer
        float[] outputData = new float[inputData.length];

        // just call the other calibrate routine for each data point
        for (int i = 0; i < inputData.length; i++) {
            outputData[i] = convertBritToTemp((int) inputData[i]);
        }

        // return the calibrated buffer
        return outputData;

    }

}

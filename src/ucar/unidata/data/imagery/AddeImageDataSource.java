/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import visad.Data;
import visad.VisADException;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;


import java.awt.geom.Rectangle2D;

import java.io.RandomAccessFile;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JOptionPane;


/**
 * A data source for ADDE images. This is a thin wrapper (derived class) around the ImageDataSource
 * which does all of the work.
 *
 * @author Don Murray
 * @version $Revision: 1.56 $ $Date: 2007/07/05 18:46:09 $
 */

public class AddeImageDataSource extends ImageDataSource {

    /* ADDE request string */

    /** _more_ */
    private String source;

    /** _more_ */
    AddeImageDescriptor descriptor;

    /** _more_ */
    protected List<DataChoice> stashedChoices = null;

    /** _more_ */
    private String choiceName;

    /** _more_ */
    AREAnav baseAnav = null;

    /** _more_ */
    AreaAdapter areaAdapter = null;

    /** _more_ */
    int eMag;

    /** _more_ */
    int lMag;

    /** _more_ */
    int eleMag = 0;

    /** _more_ */
    int lineMag = 0;

    /** _more_ */
    int elFactor = 1;

    /** _more_ */
    public final static String SPACING_BRIT = "1";

    /** _more_ */
    public final static String SPACING_NON_BRIT = "4";

    /** _more_ */
    private Boolean showPreview = Boolean.FALSE;

    /** _more_ */
    private AREACoordinateSystem acs;

    /** _more_ */
    BandInfo bandId;

    /** _more_ */
    Hashtable allBandDirs;

    /**
     *  The parameterless ctor unpersisting.
     */
    public AddeImageDataSource() {}

    /**
     *  Create a new AddeImageDataSource with an a single image ADDE url.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  image ADDE Url
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public AddeImageDataSource(DataSourceDescriptor descriptor, String image,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, new String[] { image }, properties);
    }

    /**
     *  Create a new AddeImageDataSource with an array (String) image ADDE urls.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of  ADDE urls.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */

    public AddeImageDataSource(DataSourceDescriptor descriptor,
                               String[] images, Hashtable properties)
            throws VisADException {
        super(descriptor, images, properties);
    }

    /**
     *  Create a new AddeImageDataSource with an array (String) image ADDE urls.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of  ADDE urls.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */

    public AddeImageDataSource(DataSourceDescriptor descriptor, List images,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, images, properties);
    }


    /**
     *  Create a new AddeImageDataSource with the given dataset.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  ids The dataset.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public AddeImageDataSource(DataSourceDescriptor descriptor,
                               ImageDataset ids, Hashtable properties)
            throws VisADException {
        super(descriptor, ids, properties);

        List                descs = ids.getImageDescriptors();
        AddeImageDescriptor aid   = (AddeImageDescriptor) descs.get(0);
        this.source = getPreviewSource(aid.getSource(), aid.getDirectory());

        allBandDirs     = (Hashtable) properties.get("allBands");
        this.descriptor = aid;
        ArrayList oj = (ArrayList) properties.get("bandinfo");
        if (oj != null) {
            this.bandId = (BandInfo) oj.get(0);
        }
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
            List    descriptors = super.getDescriptors(super.findDataChoice(this.choiceName), null);
            AddeImageDescriptor desc1 =
                (AddeImageDescriptor) descriptors.get(0);
            this.source = desc1.getSource();
            allBandDirs = (Hashtable) getProperties().get("allBands");
            ArrayList oj = (ArrayList) getProperties().get("bandinfo");
            if (oj != null) {
                this.bandId = (BandInfo) oj.get(0);
            }

            AreaDirectory thisDir = desc1.getDirectory();

            this.source = getPreviewSource(this.source, thisDir);
            if (oj != null) {
                this.source =
                    replaceKey(this.source, "BAND",
                               Integer.toString(this.bandId.getBandNumber()));
            }
            this.descriptor = new AddeImageDescriptor(thisDir, null);
            List<DataSelectionComponent> dataSelectionComponents =
                    new ArrayList<DataSelectionComponent>();
            initDataSelectionComponents(dataSelectionComponents, super.findDataChoice(this.choiceName) );
        }
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

        this.choiceName = dataChoice.getName();
        if (this.choiceName != null) {
            //     setProperty(PROP_DATACHOICENAME, this.choiceName);
        }

        return super.getDataInner(dataChoice, category, dataSelection,
                                  requestProperties);
    }



    /**
     * _more_
     *
     * @param description _more_
     *
     * @return _more_
     */
    protected String getUnitString(String description) {
        if (description.contains("Brightness")) {
            return "BRIT";
        } else if (description.contains("Raw")) {
            return "RAW";
        } else if (description.contains("Albedo")) {
            return "ALB";
        } else if (description.contains("Temperature")) {
            return "TEMP";
        } else if (description.contains("Prod")) {
            return "PROD";
        } else if (description.contains("Radiance")) {
            return "RAD";
        //} else if (description.contains("Reflectivity")) {
        //    return "ECHO";
        } else if (description.contains("Vip level")) {
            return "VIP";
        } else if (description.contains("Vertical h2o")) {
            return "H2O";
        } else if (description.contains("Echo")) {
            return "TOPS";
        } else if (description.contains("Hydrometeor")) {
            return "CLAS";
        } else if (description.contains("Rsp1")) {
            return "RSP1";
        } else if (description.contains("Precipitation")) {
            return "RAIN";
        } else {
            return null;
        }
    }

    /**
     * _more_
     *
     * @param dataChoice _more_
     * @param subset _more_
     *
     * @return _more_
     */
    protected List getDescriptors(DataChoice dataChoice,
                                  DataSelection subset) {

        List<AddeImageDescriptor> descriptors =
            super.getDescriptors(dataChoice, subset);
        GeoSelection geoSelection            = subset.getGeoSelection();
        boolean      isProgressiveResolution = true;
        boolean fromBundle = getIdv().getStateManager().getProperty(
                                 IdvConstants.PROP_LOADINGXML, false);

        String t1 = subset.getProperty(DataSelection.PROP_REGIONOPTION,
                DataSelection.PROP_USEDEFAULTAREA);
        String navType = subset.getProperty("navType", "X");

        String unitStr = getUnitString(dataChoice.getDescription());

        // when geoSelection is null, it is from the old bundle and return the desscriptors.
        //boolean isOldBundle = false;
        //if (allBandDirs == null) {  //geoSelection == null)
        //return descriptors;
        //}
        if (fromBundle) {
            String ver = IdvPersistenceManager.getBundleIdvVersion();

            if ((ver == null)
                    || (Character.getNumericValue(ver.charAt(0)) < 5)) {
                this.source = descriptors.get(0).getSource();
                //isOldBundle = true;
                initOldBundle(dataChoice, descriptors, this.source);

            } else {
                AddeImageDescriptor desc1 =
                        (AddeImageDescriptor) descriptors.get(0);
                this.source = desc1.getSource();
                allBandDirs = (Hashtable) getProperties().get("allBands");
                ArrayList oj = (ArrayList) getProperties().get("bandinfo");
                if (oj != null) {
                    this.bandId = (BandInfo) oj.get(0);
                }
                String zpath1 = (String) getIdv().getStateManager().getProperty(
                        IdvPersistenceManager.PROP_ZIDVPATH);
                AreaDirectory thisDir;
                if ((zpath1 != null) && (zpath1.length() > 0)) {
                    // thisDir = desc1.getDirectory();
                    return descriptors;
                } else {
                    //thisDir = (AreaDirectory) allBandDirs.get(this.bandId.getBandNumber());
                    thisDir = desc1.getDirectory();
                }
                // (AreaDirectory) allBandDirs.get(this.bandId.getBandNumber());
                this.source = getPreviewSource(this.source, thisDir);
                if (oj != null) {
                    this.source =
                            replaceKey(this.source, "BAND",
                                    Integer.toString(this.bandId.getBandNumber()));
                }
                this.descriptor = new AddeImageDescriptor(thisDir, null);
                List<DataSelectionComponent> dataSelectionComponents =
                        new ArrayList<DataSelectionComponent>();
                initDataSelectionComponents(dataSelectionComponents, super.findDataChoice(this.choiceName) );
            }

            if (baseAnav == null) {
                try {
                    areaAdapter = new AreaAdapter(this.source, false);
                    AreaFile areaFile = areaAdapter.getAreaFile();
                    baseAnav = areaFile.getNavigation();
                    acs      = new AREACoordinateSystem(areaFile);
                } catch (Exception e) {
                    LogUtil.userErrorMessage(
                        "Error in getDescriptors  e=" + e);
                }
            }

            if ((ver == null)
                    || (Character.getNumericValue(ver.charAt(0)) < 5)) {
                return descriptors;    //old bundle
            }
            //BandInfo id = (BandInfo) dataChoice.getId();
            //AreaDirectory thisDir =
            //   (AreaDirectory) allBandDirs.get(id.getBandNumber());
            //List dsList = new ArrayList();
            //dataChoice.getDataSources(dsList);
            //AddeImageDataSource ds = (AddeImageDataSource) dsList.get(0);
            //this.source = getPreviewSource(ds.source, thisDir);
            //boolean mdr = dataChoice.getProperty("MatchDisplayRegion", false);
            //if(!matchRegion || !mdr)
            //    return descriptors;
        }


        int         dlMag = 0;
        int         deMag = 0;

        isProgressiveResolution =
            subset.getProperty(DataSelection.PROP_PROGRESSIVERESOLUTION,
                               true);

        if( fromBundle && !isProgressiveResolution){
            dlMag = getLineMag();
            deMag = getEleMag();
            addeImageDataSelection.getAdvancedPanel().setLineMagSlider(dlMag);
            addeImageDataSelection.getAdvancedPanel().setElementMagSlider(deMag);
        } else if ( !isProgressiveResolution) {
            dlMag = addeImageDataSelection.getAdvancedPanel().getLineMagValue();
            deMag = addeImageDataSelection.getAdvancedPanel().getElementMagValue();
        }


        if (geoSelection != null) {
            Rectangle2D rect  = geoSelection.getScreenBound();
            ImageDataSelectionInfo adSource =
                    (ImageDataSelectionInfo) subset.getProperty(
                            "advancedURL");
            if (geoSelection.getBoundingBox() == null && adSource != null) {

                //ImageDataSelectionInfo adSource =
                 //   (ImageDataSelectionInfo) subset.getProperty(
                 //       "advancedURL");

                int lines = adSource.getLines()
                            * Math.abs(adSource.getLineMag());;
                int elems = adSource.getElements()
                            * Math.abs(adSource.getElementMag());;;

                String locationKey   = adSource.getLocateKey();
                String locationValue = adSource.getLocateValue();
                String place         = adSource.getPlaceValue();

                if (isProgressiveResolution) {
                    // eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                    lineMag = calculateMagFactor(lines,
                            (int) rect.getHeight());
                    if (isProgressiveResolution && navType.equals("LALO"))
                        lineMag = (lineMag >= 2) ? lineMag/2 : 1;
                    // lineMag = calculateMagFactor(lines,
                    //         (int) rect.getHeight()) - 1;
                    //lineMag = eleMag / elFactor;
                    eleMag = lineMag * elFactor;
                } else {
                    eleMag  = Math.abs(deMag);
                    lineMag = Math.abs(dlMag);
                }

                //System.out.println(
                //    "Magnification factor of line X element : " + lineMag
                 //   + " " + eleMag);
                int newLines;
                int newelems;

                if (lineMag == 1) {
                    newLines = lines;
                } else {
                    newLines = (int) Math.floor(lines / lineMag + 0.5);
                }

                if (eleMag == 1) {
                    newelems = elems;
                } else {
                    newelems = (int) Math.floor(elems / eleMag + 0.5);
                }
                //System.out.println("newLine X newElement : " + newLines + " "
                //                   + newelems);
                try {
                    descriptors = reSetImageDataDescriptor(descriptors,
                            locationKey, locationValue, place, newLines,
                            newelems, lineMag, eleMag, unitStr, navType);
                } catch (Exception e) {}

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
                    (LatLonPointImpl) subset.getProperty("centerPosition");

                if (llp != null) {
                    BandInfo      id      = (BandInfo) dataChoice.getId();
                    int[]         dir     = null;
                    AreaDirectory thisDir = null;
                    if (allBandDirs != null) {
                        thisDir = (AreaDirectory) allBandDirs.get(
                            id.getBandNumber());

                    } else {
                        thisDir = this.descriptor.getDirectory();
                    }
                    dir = thisDir.getDirectoryBlock();
                    // boolean inside = insideImageBoundingBox(llp.getLatitude(), llp.getLongitude()) ;
                    GeoLocationInfo ginfo = getImageBoundingBox(eMag, lMag,
                                                dir[8], dir[9]);
                    if (ginfo.getLatLonRect().containedIn(
                            mapInfo.getLatLonRect())) {
                        //this is when the map area is large and totally covered the image area
                        int    lines       = dir[8];  //2726
                        int    elems       = dir[9];  //1732

                        int    cline       = lines / 2;
                        int    celem       = elems / 2;

                        String locateValue = cline + " " + celem;


                        if (isProgressiveResolution && navType.equals("LALO")) {
                            //eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                            lineMag = calculateMagFactor(lines,
                                    (int) rect.getHeight());
                            lineMag = (lineMag >= 2) ? lineMag/2 : 1;
                            eleMag = lineMag * elFactor;
                        } else if (isProgressiveResolution) {
                            eleMag = calculateMagFactor(elems,
                                    (int) rect.getWidth());
                            // lineMag = calculateMagFactor(lines,
                            //         (int) rect.getHeight()) - 1;
                            lineMag = eleMag / elFactor;
                        } else {
                            eleMag  = Math.abs(deMag);
                            lineMag = Math.abs(dlMag);
                        }

                        //System.out.println(
                        //    "Magnification factor of line X element : "
                        //    + lineMag + " " + eleMag);
                        int newLines;
                        int newelems;

                        if (lineMag == 1) {
                            newLines = lines;
                        } else {
                            newLines = (int) Math.floor(lines / lineMag
                                    + 0.5);
                        }

                        if (eleMag == 1) {
                            newelems = elems;
                        } else {
                            newelems = (int) Math.floor(elems / eleMag + 0.5);
                        }
                        //System.out.println("newLine X newElement : "
                        //                   + newLines + " " + newelems);
                        try {
                            descriptors =
                                reSetImageDataDescriptor(descriptors,
                                    AddeImageURL.KEY_LINEELE, locateValue,
                                    "CENTER", newLines, newelems, lineMag,
                                    eleMag, unitStr, navType);
                        } catch (Exception e) {}
                    } else {
                        LatLonRect bbox = mapInfo.getLatLonRect().intersect(
                                ginfo.getLatLonRect());
                        if(mapInfo.getLatLonRect().containedIn(ginfo.getLatLonRect()))
                            bbox = mapInfo.getLatLonRect();

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

                        descriptors =
                            geoSpaceSubsetD(geoSelection.getScreenBound(),
                                            unitStr, eMag, lMag, baseAnav,
                                            descriptors, maxLat, minLat,
                                            maxLon, minLon, elFactor, dlMag,
                                            deMag, "CENTER",
                                            isProgressiveResolution, llp, navType);
                    }


                }
            }
        }
        String magValue = "";
       // if (isProgressiveResolution) {
        if (eleMag >= 1 || lineMag >= 1) {
            magValue = DataUtil.makeSamplingLabel(eleMag, lineMag, "pixel");
            //System.out.println("newLine X newElement : " + lineMag + " "+ eleMag);
            //magValue = "Resolution: " + "-" + Integer.toString(lineMag)
            //           + " " + "-" + Integer.toString(eleMag);
         }
       /* } else if (dlMag < -1 || deMag < -1) {
            magValue = DataUtil.makeSamplingLabel(deMag, dlMag, "pixel");
            //magValue = "Resolution: " + dlMag + " " + deMag;
        } */
        //if ( magValue != null ) {
        dataChoice.setProperty("MAG", magValue);
        //}

        if(fromBundle)
            return descriptors;

        Hashtable dp = new Hashtable();
        dp.put("descriptorsToSave", descriptors);
        dataChoice.setProperties(dp);
        return descriptors;


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
     * @return _more_
     */
    public int getEleMag() {
        return eleMag;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLineMag() {
        return lineMag;
    }

    /**
     * _more_
     *
     * @param mag _more_
     */
    public void setEleMag(int mag) {
        eleMag = mag;
    }

    /**
     * _more_
     *
     * @param mag _more_
     */
    public void setLineMag(int mag) {
        lineMag = mag;
    }




    /**
     * _more_
     *
     * @param inSource _more_
     * @param aDir _more_
     *
     * @return _more_
     */
    public String getPreviewSource(String inSource, AreaDirectory aDir) {
        String outSource = inSource;
        int[]  dir       = aDir.getDirectoryBlock();

        int    inLine    = dir[8];  //Integer.parseInt(lineStr);
        int    inElem    = dir[9];  //Integer.parseInt(elemStr);
        int    inLineMag = 1;  //  = Integer.parseInt(strTok1.nextToken());
        int    inElemMag = 1;  //  = Integer.parseInt(strTok1.nextToken());
        if (aDir.getCenterLatitudeResolution()
                == aDir.getCenterLatitudeResolution()) {
            elFactor =
                (int) Math.ceil(aDir.getCenterLatitudeResolution()
                                / aDir.getCenterLongitudeResolution() - 0.5);
        }
        String navType = getKey(inSource, AddeImageURL.KEY_NAV);
        // System.out.println("Line and element ratio = " + elFactor);
        int lineFactor = 1;
        int elemFactor = 1;

        int outElem    = inElem;
        int outLine    = inLine;

        if(navType.equals("LALO")){
            while (outLine > 450) {
                lineFactor += 1;
                outLine    = inLine / lineFactor;
            }
            inLineMag *= lineFactor;
            inElemMag = inLineMag / elFactor;
            if(inElemMag == 0)
                inElemMag = 1;
            outElem   = inElem / inElemMag;
        } else {
            while (outElem > 450) {
                elemFactor += 1;
                outElem    = inElem / elemFactor;
            }
            inElemMag *= elemFactor;
            inLineMag = inElemMag / elFactor;
            if(inLineMag == 0)
                inLineMag = 1;
            outLine   = inLine / inLineMag;
        }
        // alway in the center of the image and this is why it is divided by 2
        int cline = inLine / 2;
        int celem = inElem / 2;
        eMag = inElemMag;
        lMag = inLineMag;
        String locateValue = cline + " " + celem;

        String magStr1 = "-" + String.valueOf(inLineMag) + " " + "-"
                         + String.valueOf(inElemMag);
        String sizeStr1 = String.valueOf(outLine) + " "
                          + String.valueOf(outElem);
        outSource = replaceKey(outSource, AddeImageURL.KEY_UNIT, "BRIT");
        outSource = replaceKey(outSource, AddeImageURL.KEY_MAG, magStr1);
        outSource = replaceKey(outSource, AddeImageURL.KEY_SIZE, sizeStr1);
        String key0 = getKey(outSource, AddeImageURL.KEY_LATLON);
        if ((key0 != null) && (key0.length() > 2)) {
            outSource = replaceKey(outSource, AddeImageURL.KEY_LATLON,
                                   AddeImageURL.KEY_LINEELE, locateValue);
        } else {
            outSource = replaceKey(outSource, AddeImageURL.KEY_LINEELE,
                                   locateValue);
        }
        return outSource;

    }

    /**
     *  Overwrite base class  method to return the name of this class.
     *
     *  @return The name.
     */
    public String getImageDataSourceName() {
        return "Adde Image Data Source";
    }

    /**
     * Get the name for this data.  Override base class for more info.
     *
     * @return  name for the main data object
     */
    public String getDataName() {
        String dataName =
            (String) getProperty(
                ucar.unidata.idv.chooser.adde.AddeChooser.DATA_NAME_KEY,
                (String) null);
        if (dataName == null) {
            dataName = (String) getProperty(
                ucar.unidata.idv.chooser.adde.AddeChooser.PROP_DATANAME,
                (String) null);
        }

        if ((dataName == null) || dataName.trim().equals("")) {
            dataName = super.getDataName();
        }
        return dataName;

    }

    /**
     * Save files to local disk
     *
     * @param prefix destination dir and file prefix
     * @param loadId For JobManager
     * @param changeLinks Change internal file references
     *
     * @return Files copied
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {
        List newFiles = super.saveDataToLocalDisk(prefix, loadId,
                            changeLinks);
        if (newFiles == null) {
            return newFiles;
        }
        // write 0 as the first word
        for (int i = 0; i < newFiles.size(); i++) {
            try {
                RandomAccessFile to =
                    new RandomAccessFile((String) newFiles.get(i), "rw");
                to.seek(0);
                to.writeInt(0);
                to.close();
            } catch (Exception e) {
                System.out.println("unable to set first word to 0");
            }
        }
        return newFiles;
    }


    /**
     * _more_
     *
     * @param choice _more_
     */
    protected void addDataChoice(DataChoice choice) {
        // logger.trace("choice={}", choice);
        super.addDataChoice(choice);
        if (stashedChoices == null) {
            stashedChoices = new ArrayList();
        }
        stashedChoices.add(choice);
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
     *
     * @param bi _more_
     *
     * @return _more_
     */
    private static String makeBandParam(BandInfo bi) {
        return new StringBuilder().append(bi.getSensor()).append(
            "_Band").append(bi.getBandNumber()).append('_').append(
            bi.getPreferredUnit()).toString();
    }


    /**
     * _more_
     */
    public void reloadData() {
        //super.reloadData();
        if( addeImageDataSelection != null && addeImageDataSelection.leMagPanel != null) {
            addeImageDataSelection.advancedPanel.updateMagPanel();
            addeImageDataSelection.advancedPanel.setLineMagSlider(lineMag);
            addeImageDataSelection.advancedPanel.setElementMagSlider(eleMag);
        }
        super.reloadData();
    }

    /**
     * _more_
     *
     *
     * @param despList _more_
     * @param locateKey _more_
     * @param locateValue _more_
     * @param PlaceValue _more_
     * @param lines _more_
     * @param elems _more_
     * @param lineMag _more_
     * @param eleMag _more_
     * @param unit _more_
     *
     *
     * @return _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    static public List reSetImageDataDescriptor(List despList,
            String locateKey, String locateValue, String PlaceValue,
            int lines, int elems, int lineMag, int eleMag, String unit, String nav)
            throws RemoteException, VisADException {

        List descriptorList = new ArrayList();
        for (int i = 0; i < despList.size(); i++) {
            AddeImageDescriptor imageDescriptor =
                (AddeImageDescriptor) despList.get(i);
            AddeImageInfo info = imageDescriptor.getImageInfo();

            info.setElementMag(-1*eleMag);
            info.setLineMag(-1*lineMag);

            if (locateKey != null) {
                info.setLocateKey(locateKey);
                info.setLocateValue(locateValue);
                info.setPlaceValue(PlaceValue);
            } else {
                //set center
                info.setLocateValue(locateValue);
            }
       /*     if(lines == 0 && elems == 0){
                AreaDirectory ad = imageDescriptor.getDirectory();
                lines = ad.getLines()/lineMag;
                elems = ad.getElements()/lineMag;
            }  */
            info.setLines(lines);
            info.setElements(elems);
            String sizeValue = Integer.toString(lines) + " "
                               + Integer.toString(elems);
            String magValue = "-" + Integer.toString(lineMag) + " " + "-"
                              + Integer.toString(eleMag);
            String source = imageDescriptor.getSource();

            if(getKey(source, locateKey) != null && getKey(source, locateKey).length() > 0){
                source = replaceKey(source, locateKey, locateValue);
            } else if (locateKey.equals(AddeImageURL.KEY_LATLON)){
                source = replaceKey(source, AddeImageURL.KEY_LINEELE,
                        AddeImageURL.KEY_LATLON, locateValue);
            } else {
                source = replaceKey(source, AddeImageURL.KEY_LATLON,
                        AddeImageURL.KEY_LINEELE, locateValue);
            }

            source = replaceKey(source, AddeImageURL.KEY_PLACE, PlaceValue);
            source = replaceKey(source, AddeImageURL.KEY_SIZE, sizeValue);
            source = replaceKey(source, AddeImageURL.KEY_MAG, magValue);
            //source = replaceKey(source, AddeImageURL.KEY_SPAC, 1);
            if (unit != null) {
                source = replaceKey(source, AddeImageURL.KEY_UNIT, unit);
            }

            if(nav != null) {
                source = replaceKey(source, AddeImageURL.KEY_NAV, nav);
            }
            imageDescriptor.setSource(source);
            descriptorList.add(imageDescriptor);
        }
        return descriptorList;
    }

    /**
     * _more_
     *
     * @param latLonPoints _more_
     *
     * @return _more_
     */
    public double[] getLatLonPoints(LatLonPoint[] latLonPoints) {

        double maxLat = latLonPoints[0].getLatitude();
        double minLat = latLonPoints[0].getLatitude();
        double minLon = latLonPoints[0].getLongitude();
        double maxLon = latLonPoints[0].getLongitude();

        try {
            for (int i = 1; i < latLonPoints.length; i++) {
                LatLonPoint llp = latLonPoints[i];

                if (llp.getLatitude() > maxLat) {
                    maxLat = llp.getLatitude();
                }
                if (llp.getLatitude() < minLat) {
                    minLat = llp.getLatitude();
                }
                if (llp.getLongitude() > maxLon) {
                    maxLon = llp.getLongitude();
                }
                if (llp.getLongitude() < minLon) {
                    minLon = llp.getLongitude();
                }
            }


        } catch (Exception e) {}

        return new double[] { maxLat, minLat, maxLon, minLon };

    }

    /**
     * _more_
     *
     * @param rect _more_
     * @param unit _more_
     * @param eMag _more_
     * @param lMag _more_
     * @param baseAnav _more_
     * @param despList _more_
     * @param maxLat _more_
     * @param minLat _more_
     * @param maxLon _more_
     * @param minLon _more_
     * @param factor _more_
     * @param dlMag _more_
     * @param deMag _more_
     * @param placeValue _more_
     * @param isProgressiveResolution _more_
     * @param centerLLP _more_
     *
     * @return _more_
     */

    public List geoSpaceSubsetD(Rectangle2D rect, String unit, int eMag,
                                int lMag, AREAnav baseAnav, List despList,
                                double maxLat, double minLat, double maxLon,
                                double minLon, int factor, int dlMag,
                                int deMag, String placeValue,
                                boolean isProgressiveResolution,
                                LatLonPointImpl centerLLP, String nav) {

        // check if this is rubber band event, if not do nothing


        //now the rubberband is changed and the IDV is going to do sth.
        try {
            AddeImageDescriptor dsep   =
                (AddeImageDescriptor) despList.get(0);
            int                 lines  = 0;
            int                 elems  = 0;
            float[][]           latlon = new float[2][1];
            float[][]           lrLinEle;
            float[][]           ulLinEle;

            //lines
            latlon[1][0] = (float) centerLLP.getLongitude();
            latlon[0][0] = (float) maxLat;
            ulLinEle     = baseAnav.toLinEle(latlon);
            if ((ulLinEle[1][0] < 0) || (ulLinEle[1][0] != ulLinEle[1][0])) {
                ulLinEle[1][0] = 0;
            }

            latlon[1][0] = (float) centerLLP.getLongitude();
            latlon[0][0] = (float) minLat;
            lrLinEle     = baseAnav.toLinEle(latlon);
            if ((lrLinEle[1][0] < 0) || (lrLinEle[1][0] != lrLinEle[1][0])) {
                int ln = dsep.getDirectory().getDirectoryBlock()[8];
                lrLinEle[1][0] = ln / Math.abs(lMag);
            }

            lines = (int) (lrLinEle[1][0] - ulLinEle[1][0]) * Math.abs(lMag);

            //elems
            latlon[1][0] = (float) minLon;
            latlon[0][0] = (float) centerLLP.getLatitude();
            ulLinEle = this.acs.fromReference(latlon);  //baseAnav.toLinEle(latlon);
            if ((ulLinEle[0][0] < 0) || (ulLinEle[0][0] != ulLinEle[0][0])) {
                ulLinEle[0][0] = 0;
            }

            latlon[1][0] = (float) maxLon;
            latlon[0][0] = (float) centerLLP.getLatitude();
            lrLinEle = this.acs.fromReference(latlon);  //baseAnav.toLinEle(latlon);
            if ((lrLinEle[0][0] < 0) || (lrLinEle[0][0] != lrLinEle[0][0])) {
                int en = dsep.getDirectory().getDirectoryBlock()[9];
                lrLinEle[0][0] = en / Math.abs(eMag);
            }

            elems = (int) Math.abs(lrLinEle[0][0] - ulLinEle[0][0])
                    * Math.abs(eMag);


            // check
            if (lines > dsep.getDirectory().getDirectoryBlock()[8]) {
                lines = dsep.getDirectory().getDirectoryBlock()[8];
            }
            if (elems > dsep.getDirectory().getDirectoryBlock()[9]) {
                elems = dsep.getDirectory().getDirectoryBlock()[9];
            }
            if (isProgressiveResolution && nav.equals("LALO")) {
                //eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                lineMag = calculateMagFactor(lines,
                        (int) rect.getHeight());
                lineMag = (lineMag >= 2) ? lineMag/2 : 1;
                eleMag = lineMag * factor;
            } else if (isProgressiveResolution) {
                eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                // int lineMag = calculateMagFactor(lines, (int) rect.getHeight());
                lineMag = eleMag / factor;
                lineMag = (lineMag > 0)
                          ? lineMag
                          : 1;
            } else {
                eleMag  = Math.abs(deMag);
                lineMag = Math.abs(dlMag);
            }
            //System.out.println("Magnification factor of line X element : "
            //                   + lineMag + " " + eleMag);
            int newLines;
            int newelems;

            if (lineMag == 1) {
                newLines = (int) (lines * 1.0);
            } else {
                newLines = (int) (Math.floor(lines / lineMag + 0.5) * 1.);
            }

            if (eleMag == 1) {
                newelems = (int) (elems * 1.0);
            } else {
                newelems = (int) (Math.floor(elems / eleMag + 0.5) * 1.);
            }

            //System.out.println("Line: lines " + lines + " lineMag " + lineMag
            //                   + " newLines " + newLines + " displayH "
             //                  + (int) rect.getHeight());


            String locateValue = null;
            if (placeValue.equals("ULEFT")) {
                locateValue = Misc.format(maxLat) + " " + Misc.format(minLon);
            } else {
                double cLat = centerLLP.getLatitude();
                double cLon = centerLLP.getLongitude();

                locateValue = Misc.format(cLat) + " " + Misc.format(cLon);
            }


            return reSetImageDataDescriptor(despList,
                                            AddeImageURL.KEY_LATLON,
                                            locateValue, placeValue,
                                            newLines, newelems, lineMag,
                                            eleMag, unit, nav);
        } catch (Exception e) {}

        return null;

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
     * @param source _more_
     *
     * @return _more_
     */
    public String getBaseSource(String source) {
        String addeCmdBuff = source;
        if (addeCmdBuff.contains("BAND=")) {
            String bandStr = getKey(addeCmdBuff, "BAND");
            if (bandStr.length() == 0) {
                addeCmdBuff = replaceKey(addeCmdBuff, "BAND", "1");
            }
        }
        if (addeCmdBuff.contains("MAG=")) {
            String[] segs = addeCmdBuff.split("MAG=");
            String   seg0 = segs[0];
            String   seg1 = segs[1];
            int      indx = seg1.indexOf("&");
            seg1 = seg1.substring(indx);
            String magString = "-2" + " " + "-2";
            addeCmdBuff = seg0 + "MAG=" + magString + seg1;
        }
        //addeCmdBuff = addeCmdBuff.replace("imagedata", "imagedir");


        return addeCmdBuff;
    }

    /**
     * _more_
     *
     * @param src _more_
     * @param key _more_
     * @param val _more_
     *
     * @return _more_
     */
    static public String replaceKey(String src, String key, Object val) {
        String returnString = src;
        // make sure we got valid key/val pair
        if ((key == null) || (val == null)) {
            return returnString;
        }
        key = key.toUpperCase() + '=';
        if (returnString.contains(key)) {
            String[] segs = returnString.split(key);
            String   seg0 = segs[0];
            String   seg1 = segs[1];
            int      indx = seg1.indexOf("&");
            if (indx < 0) {
                seg1 = "";
            } else if (indx > 0) {
                seg1 = seg1.substring(indx);
            }
            returnString = seg0 + key + val + seg1;
        } else {
            returnString = returnString + '&' + key + val;
        }
        // if key is for cal units, and it was changed to BRIT,
        // must change the spacing key too
        if ((key.equals(AddeImageURL.KEY_UNIT + "="))
                && ("BRIT".equals(val))) {
            returnString = replaceKey(returnString, AddeImageURL.KEY_SPAC,
                                      AddeImageURL.KEY_SPAC, SPACING_BRIT);
        } /*else {
            returnString = replaceKey(returnString, AddeImageURL.KEY_SPAC,
                                      AddeImageURL.KEY_SPAC,
                                      SPACING_NON_BRIT);
        }   */
        return returnString;
    }


    /**
     * _more_
     *
     * @param src _more_
     * @param oldKey _more_
     * @param newKey _more_
     * @param val _more_
     *
     * @return _more_
     */
    static public String replaceKey(String src, String oldKey, String newKey,
                                    Object val) {
        String returnString = src;
        oldKey = oldKey.toUpperCase() + '=';
        newKey = newKey.toUpperCase() + '=';
        if (returnString.contains(oldKey)) {
            String[] segs = returnString.split(oldKey);
            String   seg0 = segs[0];
            String   seg1 = segs[1];
            int      indx = seg1.indexOf("&");
            if (indx < 0) {
                seg1 = "";
            } else if (indx > 0) {
                seg1 = seg1.substring(indx);
            }
            returnString = seg0 + newKey + val + seg1;
        } else {
            returnString = returnString + '&' + newKey + val;
        }
        return returnString;
    }



    /**
     * _more_
     *
     * @param src _more_
     * @param key _more_
     *
     * @return _more_
     */
    static public String getKey(String src, String key) {
        String returnString = "";
        key = key.toUpperCase() + '=';
        if (src.contains(key)) {
            String[] segs = src.split(key);
            segs         = segs[1].split("&");
            returnString = segs[0];
        }
        return returnString;
    }

    /**
     * _more_
     *
     * @param src _more_
     * @param key _more_
     *
     * @return _more_
     */
    static public String removeKey(String src, String key) {
        String returnString = src;
        key = key.toUpperCase() + '=';
        if (returnString.contains(key)) {
            String[] segs = returnString.split(key);
            String   seg0 = segs[0];
            String   seg1 = segs[1];
            int      indx = seg1.indexOf("&");
            if (indx >= 0) {
                seg1 = seg1.substring(indx + 1);
            }
            returnString = seg0 + seg1;
        }
        return returnString;
    }

    /**
     * _more_
     *
     * @param dataChoice _more_
     * @param descriptors _more_
     * @param sourceStr _more_
     */
    protected void initOldBundle(DataChoice dataChoice,
                                 List<AddeImageDescriptor> descriptors,
                                 String sourceStr) {
        allBandDirs = new Hashtable(1);
        AreaDirectory dir0   = descriptors.get(0).getDirectory();
        BandInfo      binfo  = (BandInfo) dataChoice.getId();
        int           bindex = binfo.getBandNumber();

        allBandDirs.put(bindex, dir0);
        String   magVal  = getKey(sourceStr, AddeImageURL.KEY_MAG);
        if(magVal.length() == 0)
            return;
        String[] magVals = magVal.split(" ");
        eMag = new Integer(magVals[1]).intValue();
        lMag = new Integer(magVals[0]).intValue();
    }

    /**
     * _more_
     *
     * @param flag _more_
     * @param excp _more_
     */
    protected void handlePreviewImageError(int flag, Exception excp) {
        getDataContext().getIdv().showNormalCursor();
        LogUtil.userErrorMessage("Error in makePreviewImage  e=" + flag + " "
                                 + excp);
    }



    /**
     * Class description
     *
     *
     * @version        Enter version here..., Thu, Jul 11, '13
     * @author         Enter your name here...
     */


    /** _more_ */
    protected AddeImageDataSelection addeImageDataSelection;

    /**
     * _more_
     *
     * @return _more_
     */
    public AddeImageDataSelection getAddeImageDataSelection() {
        return this.addeImageDataSelection;
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
            /* String zpath = (String)getIdv().getStateManager().getProperty(
                     IdvPersistenceManager.PROP_ZIDVPATH);
             if(zpath != null && zpath.length() > 0) // is zidv
                 return; */

            //AreaAdapter   aa = new AreaAdapter(this.source, false);
            BandInfo id = null;
            if ((dataChoice.getId() instanceof BandInfo)
                    && (allBandDirs != null)) {
                id = (BandInfo) dataChoice.getId();
            } else {
                return;
            }

            if ((id != null) && !id.equals(this.bandId)) {
                // now different band selected, and the preview and advanced need to be recreated

                AreaDirectory thisDir =
                    (AreaDirectory) allBandDirs.get(id.getBandNumber());
                this.source = getPreviewSource(this.source, thisDir);
                this.source =
                    replaceKey(this.source, "BAND",
                               Integer.toString(id.getBandNumber()));
                this.descriptor = new AddeImageDescriptor(thisDir, null);

            }

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

            addeImageDataSelection = new AddeImageDataSelection(this,
                        dataChoice, source, baseAnav, this.descriptor, acs,
                        areaAdapter);



        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                LogUtil.getCurrentWindow(), ex.getMessage(),
                "Error in initDataSelectionComponents 2", 0);
            getDataContext().getIdv().showNormalCursor();
            return;
        }
        components.add(addeImageDataSelection.getRegionPanel());
        components.add(addeImageDataSelection.getAdvancedPanel());
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public int getEMag() {
        return eMag;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLMag() {
        return lMag;
    }


}

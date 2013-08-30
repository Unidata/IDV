/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
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

import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import ucar.nc2.*;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.chooser.TimesChooser;
import ucar.unidata.util.*;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.view.geoloc.NavigatedPanel;

import ucar.visad.MapProjectionProjection;
import ucar.visad.UtcDate;
import ucar.visad.data.AreaImageFlatField;
import ucar.visad.display.RubberBandBox;

import visad.*;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;

import visad.georef.EarthLocation;

import visad.georef.MapProjection;

import visad.meteorology.SingleBandedImage;


import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;

import java.awt.*;

import java.awt.Dimension;
import java.awt.event.*;

import java.awt.image.BufferedImage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.IOException;
import java.io.RandomAccessFile;


import java.rmi.RemoteException;

import java.security.PublicKey;

import java.text.ParseException;

import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


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
    boolean isReload = false;

    /** _more_ */
    List rbbDescriptors = null;

    /** _more_ */
    String baseSource = null;

    /** _more_ */
    AREAnav baseAnav = null;

    /** _more_ */
    AddeImageDescriptor baseImageDescriptor = null;

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

        this.descriptor = aid;
        // sourceProps     = properties;
        if (baseSource == null) {
            baseImageDescriptor = (AddeImageDescriptor) imageList.get(0);
            baseSource          = baseImageDescriptor.getSource();
            int[]         dir   = aid.getDirectory().getDirectoryBlock();
            int           lines = dir[8];  //2726
            int           elems = dir[9];  //1732

            AddeImageInfo iInfo = aid.getImageInfo();
            eMag = baseImageDescriptor.getImageInfo().getElementMag();
            lMag = baseImageDescriptor.getImageInfo().getLineMag();

            iInfo.setLines(lines / Math.abs(lMag));
            iInfo.setElements(elems / Math.abs(eMag));
            String sizeValue = Integer.toString(lines / Math.abs(lMag)) + " "
                               + Integer.toString(elems / Math.abs(eMag));
            baseSource = replaceKey(baseSource, AddeImageURL.KEY_SIZE,
                                    sizeValue);
            baseSource = replaceKey(baseSource, AddeImageURL.KEY_LINEELE,
                                    sizeValue);
            try {
                AreaFile areaFile = new AreaFile(baseSource);
                baseAnav = areaFile.getNavigation();
                acs      = new AREACoordinateSystem(areaFile);
            } catch (Exception e) {}
            elFactor =
                (int) Math
                    .ceil(baseImageDescriptor.getDirectory()
                        .getCenterLatitudeResolution() / baseImageDescriptor
                        .getDirectory().getCenterLongitudeResolution() - 0.5);
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
            setProperty(PROP_DATACHOICENAME, this.choiceName);
        }

        return super.getDataInner(dataChoice, category, dataSelection,
                                  requestProperties);
    }

    /**
     * _more_
     *
     * @param data _more_
     *
     * @return _more_
     */
    protected boolean shouldCache(Data data) {
        return false;
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
        } else {
            return "X";
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

        List         descriptors = super.getDescriptors(dataChoice, subset);
        GeoSelection geoSelection            = subset.getGeoSelection();
        boolean      isProgressiveResolution = true;

        //if (isReload) {
        // try {
        //    descriptors = reloadDescriptors(descriptors, subset);
        //   } catch (Exception ee) {}
        // }

        Rectangle rect    = geoSelection.getScreenBound();
        String    unitStr = getUnitString(dataChoice.getDescription());


        int       dlMag   = 0;
        int       deMag   = 0;

        Object t =
            subset.getProperty(DataSelection.PROP_PROGRESSIVERESOLUTION);
        if (t instanceof Boolean) {
            isProgressiveResolution = ((Boolean) t).booleanValue();
            if ( !isProgressiveResolution) {
                dlMag = advancedSelection.getLineMag();
                deMag = advancedSelection.getElementMag();
            }
        }
        boolean useDisplayArea = false;
        boolean hasConner      = false;
        Object  t1 = subset.getProperty(DataSelection.PROP_REGIONOPTION);
        if (t1 != null) {
            String areaOpt = (String) t1;
            if (t1.equals("Use Display Area")) {
                useDisplayArea = true;
            }
        }

        Object t2 = subset.getProperty(DataSelection.PROP_HASSCONNER);
        if (t2 != null) {
            hasConner = (Boolean) t2;
        }
        if (geoSelection != null) {  //&& !isReload) {
            // applies the rubberbandbox geosubset here
            //GeoSelection gs = subset.getGeoSelection();
            if ((geoSelection.getRubberBandBoxPoints() != null) && isReload) {
                double[] latLons =
                    getLatLonPoints(geoSelection.getRubberBandBoxPoints());
                descriptors = geoSpaceSubset(geoSelection.getScreenBound(),
                                             null, eMag, lMag, baseAnav,
                                             descriptors, latLons[0],
                                             latLons[1], latLons[2],
                                             latLons[3], elFactor, dlMag,
                                             deMag, "ULEFT",
                                             isProgressiveResolution);
            } else if (geoSelection.getBoundingBox() != null) {
                //use selected
                double maxLat = geoSelection.getBoundingBox().getMaxLat();
                double minLat = geoSelection.getBoundingBox().getMinLat();
                double maxLon = geoSelection.getBoundingBox().getMaxLon();
                double minLon = geoSelection.getBoundingBox().getMinLon();
                // double maxLat, double minLat, double maxLon, double minLon
                if (useDisplayArea) {
                    AddeImageDescriptor desc =
                        (AddeImageDescriptor) descriptors.get(0);
                    int[] dir = desc.getDirectory().getDirectoryBlock();
                    descriptors =
                        geoSpaceSubsetA(geoSelection.getScreenBound(),
                                        unitStr, eMag, lMag, baseAnav,
                                        descriptors, maxLat, minLat, maxLon,
                                        minLon, elFactor, dlMag, deMag,
                                        "CENTER", isProgressiveResolution,
                                        dir);
                } else if (hasConner) {
                    descriptors =
                        geoSpaceSubset(geoSelection.getScreenBound(),
                                       unitStr, eMag, lMag, baseAnav,
                                       descriptors, maxLat, minLat, maxLon,
                                       minLon, elFactor, dlMag, deMag,
                                       "CENTER", isProgressiveResolution);
                } else {
                    descriptors =
                        geoSpaceSubset(geoSelection.getScreenBound(),
                                       unitStr, eMag, lMag, baseAnav,
                                       descriptors, maxLat, minLat, maxLon,
                                       minLon, elFactor, dlMag, deMag,
                                       "ULEFT", isProgressiveResolution);
                }

            } else if (useDisplayArea) {

                LatLonRect llrect = geoSelection.getScreenLatLonRect();
                double     maxLat = llrect.getLatMax();
                double     minLat = llrect.getLatMin();
                double     maxLon = llrect.getLonMax();
                double     minLon = llrect.getLonMin();

                descriptors = geoSpaceSubset(geoSelection.getScreenBound(),
                                             unitStr, eMag, lMag, baseAnav,
                                             descriptors, maxLat, minLat,
                                             maxLon, minLon, elFactor, dlMag,
                                             deMag, "CENTER",
                                             isProgressiveResolution);
            } else {  // use default
                AddeImageDescriptor desc =
                    (AddeImageDescriptor) descriptors.get(0);
                int[]  dir         = desc.getDirectory().getDirectoryBlock();
                int    lines       = dir[8];  //2726
                int    elems       = dir[9];  //1732

                int    cline       = lines / 2;
                int    celem       = elems / 2;

                String locateValue = cline + " " + celem;



                if (isProgressiveResolution) {
                    eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                    // lineMag = calculateMagFactor(lines,
                    //         (int) rect.getHeight()) - 1;
                    lineMag = eleMag / elFactor;
                } else {
                    eleMag  = Math.abs(eMag);
                    lineMag = Math.abs(lMag);
                }

                System.out.println(
                    "Magnification factor of line X element : " + lineMag
                    + " " + eleMag);
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

                try {
                    descriptors = reSetImageDataDescriptor(descriptors,
                            AddeImageURL.KEY_LINEELE, locateValue, "CENTER",
                            newLines, newelems, lineMag, eleMag, unitStr);
                } catch (Exception e) {}
            }
        }

        // geoSelection.setXStride(eleMag);
        // geoSelection.setYStride(lineMag);

        isReload = false;
        return descriptors;


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
        elFactor =
            (int) Math.ceil(aDir.getCenterLatitudeResolution()
                            / aDir.getCenterLongitudeResolution() - 0.5);
        int lineFactor = 1;
        int elemFactor = 1;

        int outElem    = inElem;
        int outLine    = inLine;

        while (outElem > 450) {
            elemFactor += 1;
            outElem    = inElem / elemFactor;
        }
        inElemMag *= elemFactor;
        inLineMag = inElemMag / elFactor;

        outLine   = inLine / inLineMag;
        int    cline       = inLine / inLineMag;
        int    celem       = inElem / inElemMag;

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

        isReload = true;
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
            int lines, int elems, int lineMag, int eleMag, String unit)
            throws RemoteException, VisADException {

        List descriptorList = new ArrayList();
        for (int i = 0; i < despList.size(); i++) {
            AddeImageDescriptor imageDescriptor =
                (AddeImageDescriptor) despList.get(i);
            AddeImageInfo info = imageDescriptor.getImageInfo();

            info.setElementMag(eleMag);
            info.setLineMag(lineMag);

            if (locateKey != null) {
                info.setLocateKey(locateKey);
                info.setLocateValue(locateValue);
                info.setPlaceValue(PlaceValue);
            } else {
                //set center
                info.setLocateValue(locateValue);
            }

            info.setLines(lines);
            info.setElements(elems);
            String sizeValue = Integer.toString(lines) + " "
                               + Integer.toString(elems);
            String magValue = "-" + Integer.toString(lineMag) + " " + "-"
                              + Integer.toString(eleMag);
            String source = imageDescriptor.getSource();

            if (locateKey != AddeImageURL.KEY_LINEELE) {
                source = replaceKey(source, AddeImageURL.KEY_LINEELE,
                                    AddeImageURL.KEY_LATLON, locateValue);
                source = replaceKey(source, AddeImageURL.KEY_PLACE,
                                    PlaceValue);
            } else {
                source = replaceKey(source, AddeImageURL.KEY_LINEELE,
                                    locateValue);
            }
            source = replaceKey(source, AddeImageURL.KEY_SIZE, sizeValue);
            source = replaceKey(source, AddeImageURL.KEY_MAG, magValue);
            source = replaceKey(source, AddeImageURL.KEY_SPAC, 1);
            if (unit != null) {
                source = replaceKey(source, AddeImageURL.KEY_UNIT, unit);
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
     *
     * @return _more_
     */

    public List geoSpaceSubset(Rectangle rect, String unit, int eMag,
                               int lMag, AREAnav baseAnav, List despList,
                               double maxLat, double minLat, double maxLon,
                               double minLon, int factor, int dlMag,
                               int deMag, String placeValue,
                               boolean isProgressiveResolution) {
        // check if this is rubber band event, if not do nothing


        //now the rubberband is changed and the IDV is going to do sth.
        try {

            float[][] latlon = new float[2][1];
            latlon[1][0] = (float) minLon;
            latlon[0][0] = (float) maxLat;
            float[][] ulLinEle = baseAnav.toLinEle(latlon);

            latlon[1][0] = (float) maxLon;
            latlon[0][0] = (float) minLat;
            float[][] lrLinEle   = baseAnav.toLinEle(latlon);
            int       displayNum = (int) rect.getWidth();
            int lines = (int) (lrLinEle[1][0] - ulLinEle[1][0])
                        * Math.abs(lMag);
            int elems = (int) (lrLinEle[0][0] - ulLinEle[0][0])
                        * Math.abs(eMag);


            if (isProgressiveResolution) {
                eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                // int lineMag = calculateMagFactor(lines, (int) rect.getHeight());
                lineMag = eleMag / factor;
            } else {
                eleMag  = Math.abs(deMag);
                lineMag = Math.abs(dlMag);
            }
            System.out.println("Magnification factor of line X element : "
                               + lineMag + " " + eleMag);
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

            System.out.println("Line: lines " + lines + " lineMag " + lineMag
                               + " newLines " + newLines + " displayH "
                               + (int) rect.getHeight());


            String locateValue = null;
            if (placeValue.equals("ULEFT")) {
                locateValue = Misc.format(maxLat) + " " + Misc.format(minLon);
            } else {
                double cLat = (maxLat + minLat) / 2;
                double cLon = (maxLon + minLon) / 2;

                locateValue = Misc.format(cLat) + " " + Misc.format(cLon);
            }

            return reSetImageDataDescriptor(despList,
                                            AddeImageURL.KEY_LATLON,
                                            locateValue, placeValue,
                                            newLines, newelems, lineMag,
                                            eleMag, unit);
        } catch (Exception e) {}

        return null;
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
     * @param dir _more_
     *
     * @return _more_
     */
    public List geoSpaceSubsetA(Rectangle rect, String unit, int eMag,
                                int lMag, AREAnav baseAnav, List despList,
                                double maxLat, double minLat, double maxLon,
                                double minLon, int factor, int dlMag,
                                int deMag, String placeValue,
                                boolean isProgressiveResolution, int[] dir) {

        // check if this is rubber band event, if not do nothing


        //now the rubberband is changed and the IDV is going to do sth.
        try {

            float[][] latlon = new float[2][1];
            latlon[1][0] = (float) minLon;
            latlon[0][0] = (float) (maxLat + minLat) / 2;
            float[][] clLinEle = baseAnav.toLinEle(latlon);

            latlon[1][0] = (float) maxLon;
            latlon[0][0] = (float) (minLat + maxLat) / 2;
            float[][] crLinEle = baseAnav.toLinEle(latlon);

            latlon[1][0] = (float) (maxLon + minLon) / 2;
            latlon[0][0] = (float) (minLat + maxLat) / 2;
            float[][] cLinEle = baseAnav.toLinEle(latlon);

            latlon[1][0] = (float) (maxLon + minLon) / 2;
            latlon[0][0] = (float) maxLat;
            float[][] ctLinEle = baseAnav.toLinEle(latlon);

            latlon[1][0] = (float) (maxLon + minLon) / 2;
            latlon[0][0] = (float) minLat;
            float[][] cbLinEle   = baseAnav.toLinEle(latlon);

            int       displayNum = (int) rect.getWidth();
            int       lines;
            int       elems;

            if ((ctLinEle[1][0] != ctLinEle[1][0])
                    && (cbLinEle[1][0] != cbLinEle[1][0])) {
                //both points outside image
                lines = dir[8];
            } else if (ctLinEle[1][0] != ctLinEle[1][0]) {
                // top is outside
                lines = (int) (cbLinEle[1][0] - cLinEle[1][0])
                        * Math.abs(lMag) * 2;
            } else if (cbLinEle[1][0] != cbLinEle[1][0]) {
                // bottom is outside
                lines = (int) (cLinEle[1][0] - ctLinEle[1][0])
                        * Math.abs(lMag) * 2;
            } else {
                // both inside
                lines = (int) (cbLinEle[1][0] - ctLinEle[1][0])
                        * Math.abs(lMag);
            }

            if ((clLinEle[0][0] != clLinEle[0][0])
                    && (crLinEle[0][0] != crLinEle[0][0])) {
                //both points outside image
                elems = dir[9];
            } else if (clLinEle[0][0] != clLinEle[0][0]) {
                // left is outside
                elems = (int) (crLinEle[0][0] - cLinEle[0][0])
                        * Math.abs(eMag) * 2;
            } else if (crLinEle[0][0] != crLinEle[0][0]) {
                // right is outside
                elems = (int) (cLinEle[0][0] - clLinEle[0][0])
                        * Math.abs(eMag) * 2;
            } else {
                //both inside
                elems = (int) (crLinEle[0][0] - clLinEle[0][0])
                        * Math.abs(eMag);
            }



            if (isProgressiveResolution) {
                eleMag = calculateMagFactor(elems, (int) rect.getWidth());
                // int lineMag = calculateMagFactor(lines, (int) rect.getHeight());
                lineMag = eleMag / factor;
            } else {
                eleMag  = Math.abs(deMag);
                lineMag = Math.abs(dlMag);
            }
            System.out.println("Magnification factor of line X element : "
                               + lineMag + " " + eleMag);
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

            System.out.println("Line: lines " + lines + " lineMag " + lineMag
                               + " newLines " + newLines + " displayH "
                               + (int) rect.getHeight());


            String locateValue = null;
            if (placeValue.equals("ULEFT")) {
                locateValue = Misc.format(maxLat) + " " + Misc.format(minLon);
            } else {
                double cLat = (maxLat + minLat) / 2;
                double cLon = (maxLon + minLon) / 2;

                locateValue = Misc.format(cLat) + " " + Misc.format(cLon);
            }

            return reSetImageDataDescriptor(despList,
                                            AddeImageURL.KEY_LATLON,
                                            locateValue, placeValue,
                                            newLines, newelems, lineMag,
                                            eleMag, unit);
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
        } else {
            returnString = replaceKey(returnString, AddeImageURL.KEY_SPAC,
                                      AddeImageURL.KEY_SPAC,
                                      SPACING_NON_BRIT);
        }
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
    public class ImagePreviewSelection extends DataSelectionComponent {

        /** _more_ */
        public NavigatedMapPanel display;

        /** _more_ */
        private AddeImagePreview image_preview;

        /** _more_ */
        GeoSelection geoSelection;

        /** _more_ */
        private java.awt.geom.Rectangle2D.Float new_bb;

        /** _more_ */
        private JPanel MasterPanel;

        /** _more_ */
        private String source;

        /** _more_ */
        private AreaAdapter aAdapter;

        /** _more_ */
        AddeImageDescriptor descriptor;

        /** _more_ */
        private JCheckBox chkUseFull;
        //final AddeImageDataSource this;

        /** _more_ */
        private JComboBox regionOptionLabelBox;

        /** _more_ */
        public String USE_DEFAULTREGION = "Use Default";

        /** _more_ */
        public String USE_SELECTEDREGION = "Use Selected";

        /** _more_ */
        public String USE_DISPLAYREGION = "Use Display Area";

        /** _more_ */
        private String[] regionSubsetOptionLabels = new String[] {
                                                        USE_DEFAULTREGION,
                USE_SELECTEDREGION, USE_DISPLAYREGION };

        /** _more_ */
        private JComponent regionsListInfo;

        /** _more_ */
        private String regionOption = USE_DEFAULTREGION;


        /** _more_ */
        AddeImageDataSource imageDataSource;

        /** _more_ */
        int preNumLines = 0;

        /** _more_ */
        int preNumEles = 0;

        /**
         * Construct a ImagePreviewSelection
         *
         *
         *
         * @param imageDataSource _more_
         * @param adapter _more_
         * @param source _more_
         * @param descriptor _more_
         *
         * @throws IOException _more_
         * @throws ParseException _more_
         * @throws VisADException _more_
         */
        ImagePreviewSelection(AddeImageDataSource imageDataSource,
                              AreaAdapter adapter, String source,
                              AddeImageDescriptor descriptor)
                throws IOException, ParseException, VisADException {
            super("Region");
            this.imageDataSource = imageDataSource;
            this.aAdapter        = adapter;
            this.source          = source;
            this.descriptor      = descriptor;

            createImagePreview(source);
            display = new NavigatedMapPanel(null, true, true,
                                            image_preview.getPreviewImage(),
                                            this.source);


            chkUseFull = new JCheckBox("Use Default");

            chkUseFull.setSelected(true);
            getRegionsList();
            JScrollPane jsp = new JScrollPane();
            jsp.getViewport().setView(display);
            //  jsp.add(GuiUtils.topCenter(regionsListInfo, display));
            JPanel labelsPanel = null;
            labelsPanel = new JPanel();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, 1));
            labelsPanel.add(regionsListInfo);

            MasterPanel = new JPanel(new java.awt.BorderLayout());
            MasterPanel.add(labelsPanel, "North");
            MasterPanel.add(jsp, "Center");

            display.getNavigatedPanel().addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent focusEvent) {
                    System.err.println("Gain");
                }

                @Override
                public void focusLost(FocusEvent focusEvent) {
                    update();
                }
            });


        }

        /**
         * _more_
         *
         * @return _more_
         */
        public JComponent getRegionsList() {
            return getRegionsList(USE_DEFAULTREGION);

        }

        /**
         * _more_
         *
         * @param cbxLabel _more_
         *
         * @return _more_
         */
        public JComponent getRegionsList(String cbxLabel) {
            if (regionsListInfo == null) {
                regionsListInfo = makeRegionsListAndPanel(cbxLabel, null);
            }

            return regionsListInfo;
        }

        /**
         * _more_
         *
         * @param cbxLabel _more_
         * @param extra _more_
         *
         * @return _more_
         */
        private JComponent makeRegionsListAndPanel(String cbxLabel,
                JComponent extra) {
            final JComboBox regionOptionLabelBox = new JComboBox();

            //added
            regionOptionLabelBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    String selectedObj =
                        (String) regionOptionLabelBox.getSelectedItem();
                    setRegionOptions(selectedObj);

                }

            });

            //timeDeclutterFld = new JTextField("" + getTimeDeclutterMinutes(), 5);
            GuiUtils.enableTree(regionOptionLabelBox, true);

            List regionOptionNames = Misc.toList(regionSubsetOptionLabels);

            GuiUtils.setListData(regionOptionLabelBox, regionOptionNames);
            //        JComponent top = GuiUtils.leftRight(new JLabel("Times"),
            //                                            allTimesButton);
            JComponent top;


            if (extra != null) {
                top = GuiUtils.leftRight(extra, regionOptionLabelBox);
            } else {
                top = GuiUtils.right(regionOptionLabelBox);
            }


            return top;

        }

        /**
         * _more_
         *
         * @param selectedObject _more_
         */
        public void setRegionOptions(String selectedObject) {

            regionOption = selectedObject.toString();
            if (selectedObject.equals(USE_DEFAULTREGION)) {
                display.getNavigatedPanel().setSelectedRegion(
                    (LatLonRect) null);
                GeoSelection gs = getDataSelection().getGeoSelection();
                if (gs != null) {
                    gs.setBoundingBox(null);
                }
                display.getNavigatedPanel().setSelectRegionMode(false);
                display.getNavigatedPanel().repaint();
            } else if (selectedObject.equals(USE_SELECTEDREGION)) {
                display.getNavigatedPanel().setSelectRegionMode(true);
            } else if (selectedObject.equals(USE_DISPLAYREGION)) {
                display.getNavigatedPanel().setSelectedRegion(
                    (LatLonRect) null);
                display.getNavigatedPanel().setSelectRegionMode(false);
                display.getNavigatedPanel().repaint();
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getRegionOption() {
            return regionOption;
        }

        /**
         * _more_
         *
         * @param option _more_
         */
        public void setRegionOption(String option) {
            regionOption = option;
        }

        /**
         * _more_
         *
         * @param dataChoice _more_
         */
        public void setDataChoice(DataChoice dataChoice) {


            // display.updateImage(image_preview.getPreviewImage());
        }

        /**
         * _more_
         *
         * @param source _more_
         *
         * @throws IOException _more_
         */
        private void createImagePreview(String source) throws IOException {

            int selIndex = -1;

            //LastBandNames = SelectedBandNames;
            //LastCalInfo = CalString;
            getDataContext().getIdv().showWaitCursor();
            image_preview = new AddeImagePreview(this.aAdapter,
                    this.descriptor);
            getDataContext().getIdv().showNormalCursor();
            //String bandInfo = "test";
            // lblBandInfo = new JLabel(bandInfo);
        }





        /**
         * _more_
         *
         * @return _more_
         */
        public String getFileName() {
            return this.source;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public AddeImagePreview getAddeImagePreview() {
            return image_preview;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected JComponent doMakeContents() {
            return MasterPanel;
        }

        /**
         * _more_
         *
         * @param dataSelection _more_
         */
        public void applyToDataSelection(DataSelection dataSelection) {
            ProjectionRect rect =
                display.getNavigatedPanel().getSelectedRegion();
            if (rect == null) {
                // no region subset, full image
            } else {
                rect.getBounds();
                GeoLocationInfo bbox = GeoSelection.getDefaultBoundingBox();
                if (bbox != null) {
                    this.geoSelection = new GeoSelection(bbox);
                }
            }


        }

        /**
         * _more_
         */
        public void update() {

            ProjectionRect rect =
                display.getNavigatedPanel().getSelectedRegion();
            if (rect == null) {
                // no region subset, full image
            } else {
                ProjectionImpl projectionImpl = display.getProjectionImpl();
                LatLonRect latLonRect =
                    projectionImpl.getLatLonBoundingBox(rect);
                GeoLocationInfo gInfo;
                if (latLonRect.getHeight() != latLonRect.getHeight()) {
                    //conner point outside the earth

                    LatLonPointImpl cImpl =
                        projectionImpl.projToLatLon(rect.x
                            + rect.getWidth() / 2, rect.y
                                + rect.getHeight() / 2);
                    LatLonPointImpl urImpl =
                        projectionImpl.projToLatLon(rect.x + rect.getWidth(),
                            rect.y + rect.getHeight());
                    LatLonPointImpl ulImpl =
                        projectionImpl.projToLatLon(rect.x,
                            rect.y + rect.getHeight());
                    LatLonPointImpl lrImpl =
                        projectionImpl.projToLatLon(rect.x + rect.getWidth(),
                            rect.y);
                    LatLonPointImpl llImpl =
                        projectionImpl.projToLatLon(rect.x, rect.y);

                    double maxLat = Double.NaN;
                    double minLat = Double.NaN;
                    double maxLon = Double.NaN;
                    double minLon = Double.NaN;
                    if (cImpl.getLatitude() != cImpl.getLatitude()) {
                        //do nothing
                    } else if (ulImpl.getLatitude() != ulImpl.getLatitude()) {
                        //upper left conner
                        maxLat = cImpl.getLatitude()
                                 + (cImpl.getLatitude()
                                    - lrImpl.getLatitude());
                        minLat = lrImpl.getLatitude();
                        maxLon = lrImpl.getLongitude();
                        minLon = cImpl.getLongitude()
                                 - (lrImpl.getLongitude()
                                    - cImpl.getLongitude());
                    } else if (urImpl.getLatitude() != urImpl.getLatitude()) {
                        //upper right conner
                        maxLat = cImpl.getLatitude()
                                 + (cImpl.getLatitude()
                                    - llImpl.getLatitude());
                        minLat = llImpl.getLatitude();
                        maxLon = cImpl.getLongitude()
                                 + (cImpl.getLongitude()
                                    - lrImpl.getLongitude());
                        minLon = lrImpl.getLongitude();
                    } else if (llImpl.getLatitude() != llImpl.getLatitude()) {
                        // lower left conner
                        maxLat = urImpl.getLatitude();
                        minLat = cImpl.getLatitude()
                                 - (urImpl.getLatitude()
                                    - cImpl.getLatitude());
                        maxLon = urImpl.getLongitude();
                        minLon = cImpl.getLongitude()
                                 - (urImpl.getLongitude()
                                    - cImpl.getLongitude());
                    } else if (lrImpl.getLatitude() != lrImpl.getLatitude()) {
                        // lower right conner
                        maxLat = ulImpl.getLatitude();
                        minLat = cImpl.getLatitude()
                                 - (ulImpl.getLatitude()
                                    - cImpl.getLatitude());
                        maxLon = cImpl.getLongitude()
                                 + (cImpl.getLongitude()
                                    - ulImpl.getLongitude());
                        minLon = ulImpl.getLongitude();
                    }

                    gInfo = new GeoLocationInfo(maxLat,
                            LatLonPointImpl.lonNormal(minLon), minLat,
                            LatLonPointImpl.lonNormal(maxLon));

                } else {
                    gInfo = new GeoLocationInfo(latLonRect);
                }
                // update the advanced
                float[][] latlon = new float[2][1];
                latlon[1][0] = (float) gInfo.getMinLon();
                latlon[0][0] = (float) gInfo.getMaxLat();
                float[][] ulLinEle = baseAnav.toLinEle(latlon);

                latlon[1][0] = (float) gInfo.getMaxLon();
                latlon[0][0] = (float) gInfo.getMinLat();
                float[][] lrLinEle   = baseAnav.toLinEle(latlon);
                int       displayNum = (int) rect.getWidth();
                int lines = (int) (lrLinEle[1][0] - ulLinEle[1][0])
                            * Math.abs(lMag);
                int elems = (int) (lrLinEle[0][0] - ulLinEle[0][0])
                            * Math.abs(eMag);
                // set latlon coord
                imageDataSource.advancedSelection.setIsFromRegionUpdate(true);
                imageDataSource.advancedSelection.coordinateTypeComboBox
                    .setSelectedIndex(0);
                // set lat lon values   locateValue = Misc.format(maxLat) + " " + Misc.format(minLon);
                imageDataSource.advancedSelection.setPlace("ULEFT");
                imageDataSource.advancedSelection.setLatitude(
                    gInfo.getMaxLat());
                imageDataSource.advancedSelection.setLongitude(
                    gInfo.getMinLon());
                imageDataSource.advancedSelection.convertToLineEle();

                // update the size
                imageDataSource.advancedSelection.setNumLines(lines);
                imageDataSource.advancedSelection.setNumEles(elems);
                imageDataSource.advancedSelection.setIsFromRegionUpdate(
                    false);
                preNumEles  = elems;
                preNumLines = lines;
            }

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getPreNumLines() {
            return preNumLines;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getPreNumEles() {
            return preNumEles;
        }

        /**
         * _more_
         *
         * @param num _more_
         */
        public void setPreNumLines(int num) {
            preNumLines = num;
        }

        /**
         * _more_
         *
         * @param num _more_
         */
        public void setPreNumEles(int num) {
            preNumEles = num;
        }
    }

    /** _more_ */
    public ImagePreviewSelection previewSelection;

    /** _more_ */
    public AddeImageSelectionPanel advancedSelection;

    /**
     * _more_
     *
     * @param components _more_
     * @param dataChoice _more_
     */
    protected void initDataSelectionComponents(List components,
            DataChoice dataChoice) {

        try {

            AreaAdapter   aa = new AreaAdapter(this.baseSource, false);

            MapProjection mProjection = (MapProjection) acs;

            if (null == previewSelection) {

                previewSelection = new ImagePreviewSelection(this, aa,
                        baseSource, this.descriptor);

                advancedSelection = new AddeImageSelectionPanel(this,
                        dataChoice, this.baseSource, baseAnav,
                        this.descriptor, mProjection, previewSelection);

            } else {
                previewSelection.setDataChoice(dataChoice);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(LogUtil.getCurrentWindow(),
                                          ex.getMessage(), "Exception", 0);
            getDataContext().getIdv().showNormalCursor();
            return;
        }
        components.add(previewSelection);
        components.add(advancedSelection);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsReload() {
        return isReload;
    }

    /**
     * _more_
     *
     * @param isReload _more_
     */
    public void setIsReload(boolean isReload) {
        this.isReload = isReload;
    }
}

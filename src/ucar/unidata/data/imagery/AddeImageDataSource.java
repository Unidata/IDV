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


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.data.AreaImageFlatField;
import ucar.visad.display.RubberBandBox;

import visad.*;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;

import visad.georef.EarthLocation;

import visad.meteorology.SingleBandedImage;

import java.awt.*;

import java.io.RandomAccessFile;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data source for ADDE images. This is a thin wrapper (derived class) around the ImageDataSource
 * which does all of the work.
 *
 * @author Don Murray
 * @version $Revision: 1.56 $ $Date: 2007/07/05 18:46:09 $
 */

public class AddeImageDataSource extends ImageDataSource {


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
     */
    public void reloadData() {
        // reset url based on rbb
        if (rubberBoxChanged()) {
            rubberBoxToImageDescriptor();
        }
        super.reloadData();


    }

    /** _more_ */
    Gridded2DSet last2DSet = null;

    /** _more_ */
    String baseSource = null;

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean rubberBoxChanged() {
        // check if this is rubber band event, if not do nothing
        ViewManager      vm         = getIdv().getViewManager();
        NavigatedDisplay navDisplay = null;
        if (vm.getMaster() instanceof NavigatedDisplay) {
            navDisplay = (NavigatedDisplay) vm.getMaster();
        } else {
            return false;
        }
        RubberBandBox rubberBandBox = navDisplay.getRubberBandBox();
        // get the displayCS here:

        Gridded2DSet new2DSet = rubberBandBox.getBounds();
        if ((rubberBandBox != null) && !new2DSet.equals(last2DSet)) {
            last2DSet = new2DSet;
            return true;
        } else {
            return false;
        }

    }

    /**
     * _more_
     *
     * @param locateKey _more_
     * @param locateValue _more_
     * @param PlaceValue _more_
     * @param lines _more_
     * @param elems _more_
     * @param lineMag _more_
     * @param eleMag _more_
     * @param llr _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void reSetImageDataDescriptor(String locateKey,
                                            String locateValue,
                                            String PlaceValue, int lines,
                                            int elems, int lineMag,
                                            int eleMag, LatLonRect llr)
            throws RemoteException, VisADException {

        //
        List dsList = new ArrayList();

        for (int i = 0; i < imageList.size(); i++) {
            AddeImageDescriptor imageDescriptor =
                (AddeImageDescriptor) imageList.get(0);
            AddeImageInfo info = imageDescriptor.getImageInfo();

            AreaDirectory dir  = imageDescriptor.getDirectory();

            info.setElementMag(eleMag);
            info.setLineMag(lineMag);
            info.setLocateKey(locateKey);
            info.setLocateValue(locateValue);
            info.setPlaceValue(PlaceValue);
            info.setLines(lines);
            info.setElements(elems);

            AddeImageURL newURL = new AddeImageURL(info.getHost(),
                                      info.getRequestType(), info.getGroup(),
                                      info.getDescriptor(),
                                      info.getLocateKey(),
                                      info.getLocateValue(),
                                      info.getPlaceValue(), info.getLines(),
                                      info.getElements(), info.getLineMag(),
                                      info.getElementMag(), info.getBand(),
                                      info.getUnit(), info.getSpacing());
            imageDescriptor.setSource(newURL.toString());
        }

    }



    /**
     * _more_
     */
    public void rubberBoxToImageDescriptor() {
        // check if this is rubber band event, if not do nothing
        ViewManager      vm            = getIdv().getViewManager();
        NavigatedDisplay navDisplay    = (NavigatedDisplay) vm.getMaster();

        Rectangle        rect          = navDisplay.getScreenBounds();
        RubberBandBox    rubberBandBox = navDisplay.getRubberBandBox();

        //now the rubberband is changed and the IDV is going to do sth.
        try {
            AddeImageDescriptor imageDescriptor =
                (AddeImageDescriptor) imageList.get(0);
            //String baseSource0 = getBaseSource(imageDescriptor.getSource()) ;
            if (baseSource == null) {
                baseSource = imageDescriptor.getSource();
            }
            AreaFile   areaFile = new AreaFile(baseSource);

            double[][] dd       = rubberBandBox.getBounds().getDoubles();

            EarthLocation ulpoint = navDisplay.getEarthLocation(dd[0][0],
                                        dd[1][0], 0);
            EarthLocation lrpoint = navDisplay.getEarthLocation(dd[0][1],
                                        dd[1][1], 0);
            EarthLocation elcenter = navDisplay.getEarthLocation((dd[0][0]
                                         + dd[0][1]) / 2, (dd[1][0]
                                             + dd[1][1]) / 2, 0);
            AREAnav   anav   = areaFile.getNavigation();

            float[][] latlon = new float[2][1];
            latlon[1][0] = (float) ulpoint.getLongitude().getValue();
            latlon[0][0] = (float) ulpoint.getLatitude().getValue();
            float[][] ulLinEle = anav.toLinEle(latlon);

            latlon[1][0] = (float) lrpoint.getLongitude().getValue();
            latlon[0][0] = (float) lrpoint.getLatitude().getValue();
            float[][] lrLinEle   = anav.toLinEle(latlon);
            int       displayNum = (int) rect.getWidth();
            int       lines      = (int) (lrLinEle[1][0] - ulLinEle[1][0])
                                   * 2;
            int       elems      = (int) (lrLinEle[0][0] - ulLinEle[0][0])
                                   * 2;


            int eleMag = 1;  //calculateMagFactor(elems, (int)rect.getWidth());
            int lineMag = 1;  //calculateMagFactor(lines, (int)rect.getHeight());

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
            // call forceCoords to calculate line and ele

            //

            String locateValue =
                Misc.format(elcenter.getLatitude().getValue()) + " "
                + Misc.format(elcenter.getLongitude().getValue());
            LatLonRect llr = navDisplay.getLatLonRect();
            //Rectangle rect = mpd.getScreenBounds();
            reSetImageDataDescriptor("LATLON", locateValue, "CENTER",
                                     newLines, newelems, lineMag, eleMag,
                                     llr);
            //locateKey, locateValue, PlaceValue, lines, elems, lineMag, eleMag
        } catch (Exception e) {}
        System.out.println("hhhhh");


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
                                          / (1.0 * displayPoints) + 0.5);
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
    private String replaceKey(String src, String key, Object val) {
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
        if ((key.equals(UNIT_KEY + "=")) && ("BRIT".equals(val))) {
            returnString = replaceKey(returnString, SPAC_KEY, SPAC_KEY,
                                      SPACING_BRIT);
        } else {
            returnString = replaceKey(returnString, SPAC_KEY, SPAC_KEY,
                                      SPACING_NON_BRIT);
        }
        return returnString;
    }

    /** _more_ */
    String SPAC_KEY = "space";

    /** _more_ */
    String UNIT_KEY = "unit";

    /** _more_ */
    String SPACING_BRIT = "1";

    /** _more_ */
    String SPACING_NON_BRIT = "4";

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
    private String replaceKey(String src, String oldKey, String newKey,
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
    private String getKey(String src, String key) {
        String returnString = "";
        key = key.toUpperCase() + '=';
        if (src.contains(key)) {
            String[] segs = src.split(key);
            segs         = segs[1].split("&");
            returnString = segs[0];
        }
        return returnString;
    }

}

/*
 * Copyright 1997-2016 Unidata Program Center/University Corporation for
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

import edu.wisc.ssec.mcidas.adde.AddeSatBands;

import ucar.unidata.data.*;


import ucar.unidata.util.StringUtil;


import ucar.unidata.util.TwoFacedObject;

import ucar.visad.data.AreaImageFlatField;

import visad.*;

import visad.data.DataRange;
import visad.data.mcidas.AreaAdapter;

import visad.meteorology.SingleBandedImage;



import java.rmi.RemoteException;

import java.util.*;


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
        " ", "Raw", "Radiance", "Albedo", "Temperature", "Brightness"
    };

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
    AreaFile af;

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

        String image = (String)images.get(0);
        try {
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
        bandDirs = new Hashtable(1);
        //satBandInfo = this.descriptor.getDirectory().
        AreaDirectory dir   = this.descriptor.getDirectory();
        int[]         bands = dir.getBands();
        dir.getCalibrationType();
        calblc = af.getCal();

        try {
            cali = CalibratorFactory.getCalibrator(dir.getSensorID(), calblc);
            bandInfos = makeBandInfos(dir, bands, cali);
        } catch (Exception e) {}
        Hashtable pt = getProperties();
        pt.put(ImageDataSource.PROP_BANDINFO, bandInfos);
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
        if (ad != null) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int band = bands[i];
                    //Calibrator cb = CalibratorFactory.getCalibrator(ad.getSensorID(),calblc);
                    BandInfo bi = new BandInfo(ad.getSensorID(), band);
                    bi.setBandDescription(getBandName(ad, band));
                    bi.setCalibrationUnits(
                        getAvailableUnits(
                            cb.calibratedList(band, calblc == null)));
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

        Data imageData = super.getDataInner(dataChoice, category,
                                            dataSelection, requestProperties);


        return imageData;
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
        BandInfo binfo      = (BandInfo) dataChoice.getId();
        int      band       = binfo.getBandNumber();


        if (aid == null) {
            return null;
        }

        if(dataChoice instanceof CompositeDataChoice) {
           // binfo = ((CompositeDataChoice) dataChoice).getDataChoices().get(0).g
        }
        String source = aid.getSource() + " band: " + band + " calOutType: "
                        + calOutType;

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

            if (areaDir != null) {
                int hash = ((aii != null)
                            ? aii.makeAddeUrl().hashCode()
                            : areaDir.hashCode());
                //result = AreaImageFlatField.create(aid, areaDir,
                //      rangeType, readLabel);
                String unitStr = cali.calibratedUnit(calOutType);
                Unit   unit    = null;
                if (unitStr != null) {
                    unit = DataUtil.parseUnit(unitStr);
                }
                result = AreaImageFlatField.createImmediateWithUnit(aid, readLabel,
                        unit);
                timeMap.put(aid.getSource(), result.getStartTime());

            } else {
                AreaAdapter aa = new AreaAdapter(aid.getSource(), false);
                timeMap.put(aid.getSource(), aa.getImageStartTime());
                result = aa.getImage();
            }
            if (!cali.getIsPreCalibrated()) {
                float[][] data0 = result.getFloats();
                float[] data1 = cali.calibrate(data0[0], band, calOutType);
                float[][] data2 = new float[1][data1.length];
                data2[0] = data1;
                result.setSamples(data2);
            } else if (cali.getIsPreCalibrated()
                    && calOutType == Calibrator.CAL_TEMP){
                AreaImageFlatField aiff = (AreaImageFlatField) result;
                DataRange[] sampleRanges = aiff.getRanges(true);
                if(sampleRanges[0].getMin() >= 0 && sampleRanges[0].getMax() <= 255) {
                    float[][] data0 = result.getFloats();
                    float[] data1 = cali.convertBritToTemp(data0[0]);
                    float[][] data2 = new float[1][data1.length];
                    data2[0] = data1;
                    result.setSamples(data2);
                }
            }

            // cali.calibrate(result0[0], 1, calblc);
            putCache(source, result);
            return result;

        } catch (java.io.IOException ioe) {
            throw new VisADException("Creating AreaAdapter - " + ioe);
        }

    }

}

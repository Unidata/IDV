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


import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;

import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileFactory;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;
import ucar.unidata.data.*;

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.util.TwoFacedObject;
import visad.Data;

import visad.DataReference;
import visad.DateTime;
import visad.VisADException;

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
    AddeImageDescriptor descriptor;
    DataSourceDescriptor dataSourceDescriptor;

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
    AreaFile af; AreaDirectoryList adl;
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 String image, Hashtable properties) {
        super(descriptor, new String[] { image }, properties);

         try {
             af = AreaFileFactory.getAreaFileInstance(image);
             adl = new AreaDirectoryList(image);
         }catch (Exception ee){};

        this.descriptor = new AddeImageDescriptor(image);
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
    protected Hashtable bandDirs;
    private boolean useSatBandInfo = true;
    private List<BandInfo> bandInfos;
    private AddeSatBands satBandInfo;

    public void init(){
        bandDirs = new Hashtable(1);
        //satBandInfo = this.descriptor.getDirectory().
        AreaDirectory dir = this.descriptor.getDirectory();
        int[] bands = dir.getBands();
        bandInfos = makeBandInfos(dir, bands);
Hashtable pt = getProperties();
pt.put(ImageDataSource.PROP_BANDINFO,bandInfos);
    }

    private List<BandInfo> makeBandInfos(AreaDirectory ad, int[] bands) {
        List<BandInfo> l = new ArrayList<BandInfo>();
        if (ad != null) {
            if (bands != null) {
                for (int i = 0; i < bands.length; i++) {
                    int      band = bands[i];
                    BandInfo bi   = new BandInfo(ad.getSensorID(), band);
                    bi.setBandDescription(getBandName(ad, band));
                    bi.setCalibrationUnits(getAvailableUnits(ad, band));
                    bi.setPreferredUnit("BRIT");
                    l.add(bi);
                }
            }
        }
        return l;
    }

    private List<TwoFacedObject> getAvailableUnits(AreaDirectory ad,
                                                   int band) {
        // get Vector array of Calibration types.   Layout is
        // v[i] = band[i] and for each band, it is a vector of
        // strings of calibration names and descriptions
        // n = name, n+1 = desc.
        // for radar, we only have one band
        if (ad == null) {
            return new ArrayList<TwoFacedObject>();
        }
        int[] bands = null;
        int   index = (bands == null)
                ? 0
                : Arrays.binarySearch(bands, band);
        if (index < 0) {
            index = 0;
        }
        Vector<TwoFacedObject> l = new Vector<TwoFacedObject>();
        Vector                 v      = new Vector<String>();
        String a = "RAW";
        String b =  "TEMP" ;
        String c =   "BRIT";

            v.add(a);v.add(a);
        v.add(b);v.add(b);
        v.add(c);v.add(c);

        TwoFacedObject         tfo                = null;
        int                    preferredUnitIndex = 0;
        String                 preferredUnit = "BRIT";
        if ((v != null) && (v.size() / 2 > 0)) {
            for (int i = 0; i < v.size() / 2; i++) {
                String name = (String) v.get(2 * i);
                String desc = (String) v.get(2 * i + 1);
                desc = desc.substring(0, 1).toUpperCase()
                        + desc.substring(1).toLowerCase();
                tfo = new TwoFacedObject(desc, name);
                l.add(tfo);
                if (name.equalsIgnoreCase(preferredUnit)) {
                    preferredUnitIndex = i;
                }
            }
        } else {
            l.add(new TwoFacedObject("Raw Value", "RAW"));
        }
        return l;
    }
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

}

/*
 * $Id: AddeFlatField.java,v 1.7 2007/08/08 17:14:56 jeffmc Exp $
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

package ucar.visad.data;


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileFactory;

import ucar.ma2.Array;
import ucar.ma2.Index;

//import ucar.nc2.dataset.grid.*;
import ucar.nc2.dt.grid.*;

import ucar.unidata.data.DataUtil;

import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.util.IOUtil;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;


import visad.*;

import visad.data.mcidas.AREACoordinateSystem;


import visad.meteorology.SingleBandedImage;

import visad.util.DataUtility;


import java.io.*;

import java.rmi.RemoteException;



/**
 * This is a FloatField that caches to disk its float array.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.7 $ $Date: 2007/08/08 17:14:56 $
 */
public class AddeImageFlatField extends CachedFlatField implements SingleBandedImage {




    /** _more_ */
    private String readLabel = "";

    /** _more_ */
    private GriddedSet domainSet;

    /** _more_ */
    private DateTime startTime;

    /**
     * copy ctor
     *
     * @param that What we clone from
     * @param copy copy the values
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSysArray  rangeCoordSysArray
     * @param rangeSets range sets
     * @param units units
     *
     * @throws VisADException On badness
     */
    public AddeImageFlatField(AddeImageFlatField that, boolean copy,
                              FunctionType type, Set domainSet,
                              CoordinateSystem rangeCoordSys,
                              CoordinateSystem[] rangeCoordSysArray,
                              Set[] rangeSets, Unit[] units)
            throws VisADException {
        super(that, copy, type, domainSet, rangeCoordSys, rangeCoordSysArray,
              rangeSets, units);
        this.domainSet = that.domainSet;
    }

    /**
     * Clone this object
     *
     * @param copy copy the values
     * @param type Function type
     * @param domainSet Domain set
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSysArray  rangeCoordSysArray
     * @param rangeSets range sets
     * @param units units
     *
     * @return New field
     *
     * @throws VisADException On badness
     */
    public CachedFlatField cloneMe(boolean copy, FunctionType type,
                                   Set domainSet,
                                   CoordinateSystem rangeCoordSys,
                                   CoordinateSystem[] rangeCoordSysArray,
                                   Set[] rangeSets, Unit[] units)
            throws VisADException {
        return new AddeImageFlatField(this, copy, type, domainSet,
                                      rangeCoordSys, rangeCoordSysArray,
                                      rangeSets, units);
    }



    /**
     * ctor
     *
     * @param floats The values
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeSets range sets
     * @param units units
     *
     * @throws VisADException On badness
     */
    public AddeImageFlatField(FunctionType type, Set domainSet,
                              CoordinateSystem rangeCoordSys,
                              Set[] rangeSets, Unit[] units, float[][] floats)
            throws VisADException {
        super(type, domainSet, rangeCoordSys, rangeSets, units, floats);
    }




    /**
     * _more_
     *
     * @param aii _more_
     * @param areaDirectory _more_
     * @param shouldCache _more_
     * @param cacheFile _more_
     * @param cacheClearDelay _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static AddeImageFlatField create(AddeImageInfo aii,
                                            AreaDirectory areaDirectory,
                                            boolean shouldCache,
                                            String cacheFile,
                                            long cacheClearDelay)
            throws VisADException, RemoteException {


        int nLines = aii.getLines();
        int nEles  = aii.getElements();

        // make the VisAD RealTypes for the dimension variables
        RealType line    = RealType.getRealType("ImageLine", null, null);
        RealType element = RealType.getRealType("ImageElement", null, null);


        // extract the number of bands (sensors) and make the VisAD type
        int band       = 0;
        int bandNums[] = areaDirectory.getBands();
        int numBands = areaDirectory.getNumberOfBands();  // this might be different

        // create indicies into the data structure for the bands
        int[] bandIndices = new int[numBands];
        if (band != 0) {                 // specific bands requested
            bandIndices[0] = -1;
            for (int i = 0; i < numBands; i++) {
                if (band == bandNums[i]) {
                    bandIndices[0] = i;
                    break;
                }
            }
            if (bandIndices[0] == -1) {  // not found
                throw new VisADException(
                    "requested band number not in image");
            }
            bandNums = new int[] { band };
            numBands = 1;
        } else {  // all bands
            for (int i = 0; i < numBands; i++) {
                bandIndices[i] = i;
            }
        }

        RealType[] bands = new RealType[numBands];


        // If we have calibration units, might as well use them.
        Unit  calUnit  = null;
        try {
            calUnit = visad.data.units.Parser.parse(
                visad.jmet.MetUnits.makeSymbol(
                    areaDirectory.getCalibrationUnitName()));
        } catch (Exception e) {
            calUnit = null;
        }
        String calType = areaDirectory.getCalibrationType();

        // first cut: the bands are named "Band##" where ## is the
        // band number from the AREA file bandmap
        for (int i = 0; i < numBands; i++) {
            bands[i] = (calUnit != null)
                       ? RealType.getRealType("Band" + bandNums[i] + "_"
                       + calType, calUnit)
                       : RealType.getRealType("Band" + bandNums[i]);
        }


        // the range of the FunctionType is the band(s)
        RealTupleType radiance = new RealTupleType(bands);

        // the domain is (element,line) since elements (X) vary fastest
        RealType[]             domain_components = { element, line };

        MyAREACoordinateSystem cs = new MyAREACoordinateSystem();
        RealTupleType image_domain = new RealTupleType(domain_components, cs,
                                         null);

        Linear2DSet domain_set = new Linear2DSet(image_domain, 0,
                                     (nEles - 1), nEles, (nLines - 1), 0,
                                     nLines);
        FunctionType image_type = new FunctionType(image_domain, radiance);

        // If calibrationType is brightnes (BRIT), then we can store
        // the values as shorts.  To do this, we crunch the values down
        // from 0-255 to 0-254 so we can have 255 left over for missing
        // values.
        Set[]   rangeSets = null;
        boolean pack      = false;
        pack = pack && calType.equalsIgnoreCase("BRIT");
        if (pack) {
            rangeSets = new Set[numBands];
            for (int i = 0; i < numBands; i++) {
                rangeSets[i] = new Integer1DSet(bands[i], 255);
            }
        }
        Unit[] rangeUnits = null;
        if (calUnit != null) {
            rangeUnits = new Unit[numBands];
            for (int i = 0; i < numBands; i++) {
                rangeUnits[i] = calUnit;
            }
        }


        AddeImageFlatField aiff = new AddeImageFlatField(image_type,
                                     domain_set, null, rangeSets, rangeUnits,
                                     null);
        aiff.bandIndices = bandIndices;
        aiff.aii         = aii;
        cs.aiff         = aiff;
        aiff.startTime   = new DateTime(areaDirectory.getStartTime());
        aiff.setCacheClearDelay(cacheClearDelay);
        aiff.setCacheFile(cacheFile);
        aiff.setShouldCache(shouldCache);
        return aiff;
    }



    /** _more_ */
    private AreaFile areaFile;

    /** _more_ */
    private     AddeImageInfo aii;

    /** _more_ */
    private     int[] bandIndices;




    /**
     * _more_
     *
     * @return _more_
     */
    protected AreaFile getAreaFile() {
        try {
            if (myParent != null) {
                return ((AddeImageFlatField) myParent).getAreaFile();
            }
            if (areaFile == null) {
                readData();
            }
            return areaFile;
        } catch (Exception exc) {
            throw new ucar.unidata.util.WrapperException(exc);
        }
    }


    /** _more_          */
    private int[][] dirNavAux;

    /** _more_          */
    private int[] nav;

    /** _more_          */
    private int[] aux;

    /** _more_          */
    private int[] dir;

    /**
     * _more_
     *
     * @return _more_
     */
    protected int[][] getDirNavAux() {
        if (myParent != null) {
            return ((AddeImageFlatField) myParent).getDirNavAux();
        }
        if (dirNavAux == null) {
            String file = getCacheFile();
            if (file != null) {
                file = file + ".dirnavaux";
                if (new File(file).exists()) {
                    try {
                        FileInputStream istream = new FileInputStream(file);
                        BufferedInputStream bis =
                            new BufferedInputStream(istream, 1000000);
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        dirNavAux = (int[][]) ois.readObject();
                        ois.close();
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }

            if (dirNavAux == null) {
                dirNavAux = new int[][] {
                    (int[]) getAreaFile().getAreaDirectory()
                        .getDirectoryBlock().clone(),
                    getAreaFile().getNav(), getAreaFile().getAux()
                };

                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos,
                                                   100000);
                    ObjectOutputStream p = new ObjectOutputStream(bos);
                    p.writeObject(dirNavAux);
                    p.flush();
                    fos.close();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
        return dirNavAux;
    }


    /**
     * Class MyAREACoordinateSystem _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class MyAREACoordinateSystem extends AREACoordinateSystem {

        /** _more_ */
        AddeImageFlatField aiff;

        /**
         * _more_
         *
         * @param dir _more_
         * @param nav _more_
         * @param aux _more_
         * @param useSpline _more_
         *
         * @throws VisADException _more_
         */
        public MyAREACoordinateSystem(int[] dir, int[] nav, int[] aux,
                                      boolean useSpline)
                throws VisADException {
            super(dir, nav, aux, useSpline);
        }

        /**
         * _more_
         *
         * @throws VisADException _more_
         */
        public MyAREACoordinateSystem() throws VisADException {}

        /**
         * _more_
         *
         * @return _more_
         */
        protected AREAnav getAreaNav() {
            AREAnav anav = super.getAreaNav();
            if (anav == null) {
                try {
                    int[][] dirNavAux = aiff.getDirNavAux();
                    init(dirNavAux[0], dirNavAux[1], dirNavAux[2], true);
                } catch (Exception exc) {
                    System.err.println("error making making areanav:" + exc);
                    exc.printStackTrace();
                }
            }
            return super.getAreaNav();
        }
    }


    /**
     * Used to provide a hook to derived classes to dynamically read in the data
     *
     * @return data
     */
    protected float[][] readData() {
        try {
            msg("Reading ADDE data  " + readLabel);
            LogUtil.message(readLabel);
            ucar.unidata.data.DataSourceImpl.incrOutstandingGetDataCalls();
            areaFile = AreaFileFactory.getAreaFileInstance(aii.makeAddeUrl());
            float[][][] flt_samples = areaFile.getFloatData();
            float[][] samples =
                new float[1][aii.getElements() * aii.getLines()];
            int nEles  = aii.getElements();
            int nLines = aii.getLines();

            float calScale = (1.0f / areaFile.getAreaDirectory().getCalibrationScaleFactor());
            for (int i = 0; i < nLines; i++) {
                for (int j = 0; j < nEles; j++) {
                    float v = calScale * flt_samples[bandIndices[0]][i][j];
                    samples[0][j + (nEles * i)] = v;
                }
            }
            return samples;
        } catch (Exception exc) {
            throw new ucar.unidata.util.WrapperException(exc);
        } finally {
            ucar.unidata.data.DataSourceImpl.decrOutstandingGetDataCalls();
            LogUtil.message("");
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public void setReadLabel(String s) {
        readLabel = s;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getStartTime() {
        return startTime;
    }

    /**
     * Return a descriptive string for this image.
     * @return description
     */
    public String getDescription() {
        return "image";
    }

    /**
     * Get the minimum possible value for this image
     * @return  a Real representing the minimum possible value.  Using a
     *          Real allows us to associate units and error estimates with
     *          the value
     */
    public Real getMinRangeValue() {
        System.err.println("getminrangevalue");
        return null;
    }

    /**
     * Get the maximum possible value for this image
     * @return  a Real representing the maximum possible value.  Using a
     *          Real allows us to associate units and error estimates with
     *          the value
     */
    public Real getMaxRangeValue() {
        System.err.println("getmaxrangevalue");
        return null;
    }

    /**
     * Check to see if this image has a domain that can map to Latitude
     * and Longitude.
     *
     * @return true if it has navigation, otherwise false
     */
    public boolean isNavigated() {
        System.err.println("isnavigated");
        return true;
    }



}


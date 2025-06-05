package edu.wisc.ssec.mcidasv.data.hydra;

import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;


import visad.CoordinateSystem;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.VisADException;

public class MultiSpectralData extends MultiDimensionAdapter {

    GeoSfcAdapter swathAdapter = null;
    SpectrumAdapter spectrumAdapter = null;
    CoordinateSystem cs = null;

    HashMap spectrumSelect = null;
    String sensorName = null;
    String platformName = null;
    String paramName = null;
    String inputParamName = null;
    String name = null;

    public float init_wavenumber = 919.50f;
    public String init_bandName = null;

    float[] dataRange = new float[]{180f, 320f};

    boolean hasBandNames = false;
    ArrayList<String> bandNameList = null;
    HashMap<String, Float> bandNameMap = null;

    private boolean needsReflCorr = false;
    private final Date date;
    private final FloatArrayCache reflCorrCache;

    protected int apod_offset = 0;

    public MultiSpectralData(GeoSfcAdapter swathAdapter, SpectrumAdapter spectrumAdapter,
                             String inputParamName, String paramName, String sensorName, String platformName, Date date) {
        this.swathAdapter = swathAdapter;
        this.spectrumAdapter = spectrumAdapter;
        this.paramName = paramName;
        this.inputParamName = inputParamName;
        this.name = swathAdapter.getArrayName();
        this.date = date;
        this.sensorName = sensorName;
        this.platformName = platformName;

        if (spectrumAdapter != null) {
            this.spectrumSelect = spectrumAdapter.getDefaultSubset();
            if (spectrumAdapter.hasBandNames()) {
                hasBandNames = true;
                bandNameList = spectrumAdapter.getBandNames();
                bandNameMap = spectrumAdapter.getBandNameMap();
            }
            try {
                setInitialWavenumber(getWavenumberFromChannelIndex(0));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("could not initialize initial wavenumber");
            }
        }

        needsReflCorr = (sensorName != null && sensorName.equals("MODIS"));
        reflCorrCache = new FloatArrayCache();

        setSpectrumAdapterProcessor();
    }

    public MultiSpectralData(GeoSfcAdapter swathAdapter, SpectrumAdapter spectrumAdapter,
                             String inputParamName, String paramName, String sensorName, String platformName) {
        this(swathAdapter, spectrumAdapter, inputParamName, paramName, sensorName, platformName, null);
    }

    public MultiSpectralData(GeoSfcAdapter swathAdapter, SpectrumAdapter spectrumAdapter,
                             String sensorName, String platformName) {
        this(swathAdapter, spectrumAdapter, "Radiance", "BrightnessTemp", sensorName, platformName);
    }

    public MultiSpectralData(GeoSfcAdapter swathAdapter, SpectrumAdapter spectrumAdapter) {
        this(swathAdapter, spectrumAdapter, null, null);
    }

    public MultiSpectralData() {
        this(null, null, null, null);
    }

    void setSpectrumAdapterProcessor() {
        if (swathAdapter != null) {
            if (spectrumAdapter != null) {
                spectrumAdapter.setRangeProcessor(swathAdapter.getRangeProcessor());
            }
        }
    }

    public FlatField getSpectrum(int[] coords)
            throws Exception, VisADException, RemoteException {
        if (coords == null) return null;
        if (spectrumAdapter == null) return null;
        spectrumSelect.put(SpectrumAdapter.x_dim_name, new double[]{(double) coords[0], (double) coords[0], 1.0});
        spectrumSelect.put(SpectrumAdapter.y_dim_name, new double[]{(double) coords[1], (double) coords[1], 1.0});

        return getSpectrum(spectrumSelect);
    }

    public FlatField getSpectrum(HashMap subset) throws Exception, VisADException, RemoteException {
        if (subset == null) return null;

        // Analyze the hashmap: only a spectrum at a single FOV is allowed
        double[] x_extent = ((double[]) subset.get(SpectrumAdapter.x_dim_name));
        double[] y_extent = ((double[]) subset.get(SpectrumAdapter.y_dim_name));
        if ((x_extent[0] != x_extent[1]) && (y_extent[0] != y_extent[1]) && (x_extent[2] != 1.0) && (y_extent[2] != 1.0)) {
            throw new Exception("Can only ask for a spectrum of a single FOV. Stride must be 1");
        }
        // convertSpectrum needs geolocation for the FOV:
        float x_coord = (float) x_extent[0];
        float y_coord = (float) y_extent[0];
        float[][] ll = new float[][]{{Float.NaN}, {Float.NaN}};
        if (cs != null) {
            ll = cs.toReference(new float[][]{{x_coord}, {y_coord}});
        } else {
            double[] loc = swathAdapter.getNavigation().getEarthLocOfDataCoord(new int[]{(int) x_coord, (int) y_coord});
            ll[0][0] = (float) loc[0];
            ll[1][0] = (float) loc[1];
        }

        FlatField spectrum = spectrumAdapter.getData(subset);
        return convertSpectrum(spectrum, paramName, ll[0][0], ll[1][0]);
    }

    public FlatField getSpectrum(MultiDimensionSubset subset) throws Exception, VisADException, RemoteException {
        return getSpectrum(subset.getSubset());
    }

    public FlatField getSpectrum(RealTuple location)
            throws Exception, VisADException, RemoteException {
        if (spectrumAdapter == null) return null;
        int[] coords = getSwathCoordinates(location, cs);
        if (coords == null) return null;
        float[][] ll = cs.toReference(new float[][]{{coords[0]}, {coords[1]}});
        spectrumSelect.put(SpectrumAdapter.x_dim_name, new double[]{(double) coords[0], (double) coords[0], 1.0});
        spectrumSelect.put(SpectrumAdapter.y_dim_name, new double[]{(double) coords[1], (double) coords[1], 1.0});

        FlatField spectrum = spectrumAdapter.getData(spectrumSelect);
        return convertSpectrum(spectrum, paramName, ll[0][0], ll[1][0]);
    }

    public FlatField getImage(HashMap subset)
            throws Exception, VisADException, RemoteException {

        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
        if (coords != null) {
            int channelIndex = (int) coords[0];
            new_subset.put(SpectrumAdapter.channelIndex_name,
                    new double[]{(double) channelIndex + apod_offset, (double) channelIndex + apod_offset, 1.0});
            FlatField image = swathAdapter.getData(new_subset);
            cs = ((RealTupleType) ((FunctionType) image.getType()).getDomain()).getCoordinateSystem();
            float channel = spectrumAdapter.getWavenumberFromChannelIndex(channelIndex);
            return convertImage(image, channel, paramName);
        } else {
            return swathAdapter.getData(subset);
        }
    }

    public FlatField getImage(float channel, HashMap subset)
            throws Exception, VisADException, RemoteException {
        if (spectrumAdapter == null) {
            return getImage(subset);
        }

        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channel);
        new_subset.put(SpectrumAdapter.channelIndex_name,
                new double[]{(double) channelIndex + apod_offset, (double) channelIndex + apod_offset, 1.0});
        FlatField image = swathAdapter.getData(new_subset);
        cs = ((RealTupleType) ((FunctionType) image.getType()).getDomain()).getCoordinateSystem();

        return convertImage(image, channel, paramName);
    }

    public FlatField getImage(float channel, HashMap subset, String outParam)
            throws Exception, VisADException, RemoteException {
        if (spectrumAdapter == null) {
            return getImage(subset);
        }

        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channel);
        new_subset.put(SpectrumAdapter.channelIndex_name,
                new double[]{(double) channelIndex + apod_offset, (double) channelIndex + apod_offset, 1.0});
        FlatField image = swathAdapter.getData(new_subset);
        cs = ((RealTupleType) ((FunctionType) image.getType()).getDomain()).getCoordinateSystem();

        return convertImage(image, channel, outParam);
    }

    public FlatField getImage(int channelIndex, HashMap subset, String outParam)
            throws Exception, VisADException, RemoteException {
        if (spectrumAdapter == null) {
            return getImage(subset);
        }

        HashMap new_subset = (HashMap) ((HashMap) subset).clone();
        new_subset.putAll((HashMap) subset);

        new_subset.put(SpectrumAdapter.channelIndex_name,
                new double[]{(double) channelIndex + apod_offset, (double) channelIndex + apod_offset, 1.0});
        FlatField image = swathAdapter.getData(new_subset);
        cs = ((RealTupleType) ((FunctionType) image.getType()).getDomain()).getCoordinateSystem();

        float channel = getWavenumberFromChannelIndex(channelIndex);
        return convertImage(image, channel, outParam);
    }

    public FlatField makeConvolvedRadiances(int[] channelIndexRange, HashMap subset, float[] wghts)
            throws Exception {
        subset.put(SpectrumAdapter.channelIndex_name, new double[]{(double) channelIndexRange[0], (double) channelIndexRange[1], 1.0});
        return swathAdapter.makeConvolvedRadiances(subset, wghts);
    }



    public FlatField getData(Object subset) throws Exception {
        return getImage((HashMap) subset);
    }

    public Set makeDomain(Object subset) throws Exception {
        throw new Exception("makeDomain unimplented");
    }

    public FlatField convertImage(FlatField image, float channel, String param)
            throws Exception {
        FlatField new_image = null;
        FunctionType f_type = (FunctionType) image.getType();
        if (param.equals("BrightnessTemp")) { //- convert radiance to BrightnessTemp
            FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));
            new_image = new FlatField(new_type, image.getDomainSet());
            float[][] values = image.getFloats(false);
            float[] bt_values = values[0];
            if (inputParamName.equals("Radiance")) {
                bt_values = radianceToBrightnessTemp(values[0], channel, platformName, sensorName);
            }
            new_image.setSamples(new float[][]{bt_values}, false);
        } else if (param.equals("Reflectance")) {
            FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("Reflectance"));
            Linear2DSet dSet = (Linear2DSet) image.getDomainSet();
            new_image = new FlatField(new_type, image.getDomainSet());
            if (sensorName != null && sensorName.equals("MODIS")) {
                CoordinateSystem cs = f_type.getDomain().getCoordinateSystem();
                Linear2DSet dset = (Linear2DSet) image.getDomainSet();
                float[][] samples = dset.getSamples();
                float[][] lonlat = cs.toReference(samples);

                float[] refls = (image.getFloats(true))[0];
                float[] reflCorr = reflCorrCache.get(dSet);
                if (reflCorr == null) {
                    reflCorr = MODIS_L1B_Utility.reflectanceCorrForSolzen(lonlat[0], lonlat[1], date);
                    reflCorrCache.put(dSet, reflCorr);
                }
                for (int k = 0; k < refls.length; k++) {
                    refls[k] *= reflCorr[k];
                }
                new_image.setSamples(new float[][]{refls}, false);
            } else {
                new_image.setSamples(image.getFloats(false), false);
            }

            if (inputParamName.equals("Reflectance100")) {
                new_image = (FlatField) new_image.divide(new Real(100));
            }
        } else {
            new_image = image;
        }

        return new_image;
    }


    FlatField convertSpectrum(FlatField spectrum, String param) throws Exception {
        return convertSpectrum(spectrum, param, Float.NaN, Float.NaN);
    }

    FlatField convertSpectrum(FlatField spectrum, String param, float lon, float lat) throws Exception {
        FlatField new_spectrum;
        FunctionType f_type = (FunctionType) spectrum.getType();

        if (param.equals("BrightnessTemp")) {
            FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));
            float[][] channels = ((SampledSet) spectrum.getDomainSet()).getSamples(false);
            float[][] values = spectrum.getFloats(false);
            float[] bt_values = values[0];
            if (inputParamName.equals("Radiance")) {
                bt_values = radianceToBrightnessTempSpectrum(values[0], channels[0], platformName, sensorName);
            }
            new_spectrum = new FlatField(new_type, spectrum.getDomainSet());
            new_spectrum.setSamples(new float[][]{bt_values}, true);
        } else if (param.equals("Reflectance")) {
            FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("Reflectance"));
            new_spectrum = new FlatField(new_type, spectrum.getDomainSet());
            if (sensorName != null && sensorName.equals("MODIS")) {
                float[] refls = (spectrum.getFloats(true))[0];
                refls = MODIS_L1B_Utility.reflectanceCorrForSolzen(refls, lon, lat, date);
                new_spectrum.setSamples(new float[][]{refls}, false);
            } else {
                new_spectrum.setSamples(spectrum.getFloats(false), false);
            }
            if (inputParamName.equals("Reflectance100")) {
                new_spectrum = (FlatField) new_spectrum.divide(new Real(100));
            }
        } else {
            new_spectrum = spectrum;
        }
        return new_spectrum;
    }

    public void setDataRange(float[] range) {
        dataRange = range;
    }

    public float[] getDataRange() {
        return dataRange;
    }

    public String getParameter() {
        return paramName;
    }

    public String getName() {
        return name;
    }

    public CoordinateSystem getCoordinateSystem() {
        return cs;
    }

    public void setCoordinateSystem(CoordinateSystem cs) {
        this.cs = cs;
    }

    public void setSwathDomainSet(Linear2DSet dset) {
        swathAdapter.setDomainSet(dset);
    }

    public SampledSet getSpectralDomain() throws Exception {
        return spectrumAdapter.getDomainSet();
    }

    public String getSensorName() {
        return sensorName;
    }

    public boolean hasBandName(String name) {
        return bandNameList.contains(name);
    }

    public int[] transformSwathCoords(int[] coords, String inName) {
        int[] outCoords = new int[]{coords[0], coords[1]};
        if (inName == null) {
            return coords;
        }
        if (inName.equals("VIIRS-M")) {
            if (sensorName.equals("VIIRS-I")) {
                outCoords[0] = 2 * coords[0];
                outCoords[1] = 2 * coords[1];
            }
        } else if (inName.equals("VIIRS-I")) {
            if (sensorName.equals("VIIRS-M")) {
                outCoords[0] = coords[0] / 2;
                outCoords[1] = coords[1] / 2;
            }
        } else if (inName.equals("AHI_2KM")) {
            if (sensorName.equals("AHI_1KM")) {
                outCoords[0] = coords[0] * 2;
                outCoords[1] = coords[1] * 2;
            } else if (sensorName.equals("AHI_HKM")) {
                outCoords[0] = coords[0] * 4;
                outCoords[1] = coords[1] * 4;
            }
        } else if (inName.equals("AHI_1KM")) {
            if (sensorName.equals("AHI_2KM")) {
                outCoords[0] = coords[0] / 2;
                outCoords[1] = coords[1] / 2;
            } else if (sensorName.equals("AHI_HKM")) {
                outCoords[0] = coords[0] * 2;
                outCoords[1] = coords[1] * 2;
            }
        } else if (inName.equals("AHI_HKM")) {
            if (sensorName.equals("AHI_1KM")) {
                outCoords[0] = coords[0] / 2;
                outCoords[1] = coords[1] / 2;
            } else if (sensorName.equals("AHI_2KM")) {
                outCoords[0] = coords[0] / 4;
                outCoords[1] = coords[1] / 4;
            }
        } else if (inName.equals("ABI_2KM")) {
            if (sensorName.equals("ABI_1KM")) {
                outCoords[0] = coords[0] * 2;
                outCoords[1] = coords[1] * 2;
            } else if (sensorName.equals("ABI_HKM")) {
                outCoords[0] = coords[0] * 4;
                outCoords[1] = coords[1] * 4;
            }
        } else if (inName.equals("ABI_1KM")) {
            if (sensorName.equals("ABI_2KM")) {
                outCoords[0] = coords[0] / 2;
                outCoords[1] = coords[1] / 2;
            } else if (sensorName.equals("ABI_HKM")) {
                outCoords[0] = coords[0] * 2;
                outCoords[1] = coords[1] * 2;
            }
        } else if (inName.equals("ABI_HKM")) {
            if (sensorName.equals("ABI_1KM")) {
                outCoords[0] = coords[0] / 2;
                outCoords[1] = coords[1] / 2;
            } else if (sensorName.equals("ABI_2KM")) {
                outCoords[0] = coords[0] / 4;
                outCoords[1] = coords[1] / 4;
            }
        }
        return outCoords;
    }


    void setReflectanceCorr(Linear2DSet domSet, float[] reflCorr) {
        reflCorrCache.put(domSet, reflCorr);
    }

    float[] getReflectanceCorr(Linear2DSet domSet) {
        return reflCorrCache.get(domSet);
    }

    public boolean hasBandNames() {
        return hasBandNames;
    }

    public ArrayList<String> getBandNames() {
        return bandNameList;
    }

    public HashMap<String, Float> getBandNameMap() {
        return bandNameMap;
    }

    public String getBandNameFromWaveNumber(float channel) {
        String bandName = null;
        Iterator iter = bandNameMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            float mapVal = ((Float) bandNameMap.get(key)).floatValue();
            if (channel == mapVal) {
                bandName = key;
                break;
            }
        }
        return bandName;
    }

    public float getWavenumberFromBandName(String bandName) {
        return bandNameMap.get(bandName);
    }

    public void setInitialWavenumber(float val) {
        init_wavenumber = val;
        if (hasBandNames) {
            init_bandName = getBandNameFromWaveNumber(init_wavenumber);
        }
    }

    public int[] getSwathCoordinates(RealTuple location, CoordinateSystem cs)
            throws VisADException, RemoteException {
        if (location == null) return null;
        if (cs == null) return null;

        RealTupleType rttype = (RealTupleType) location.getType();
        // trusted: latitude:0, longitude: 1
        int lat_idx = 0;
        int lon_idx = 1;
        if (rttype.equals(RealTupleType.SpatialEarth2DTuple)) {
            lon_idx = 0;
            lat_idx = 1;
        }
        Real[] comps = location.getRealComponents();
        float lon = (float) comps[lon_idx].getValue();
        float lat = (float) comps[lat_idx].getValue();
        if (lon < -180) lon += 360f;
        if (lon > 180) lon -= 360f;
        float[][] xy = cs.fromReference(new float[][]{{lon}, {lat}});
        if ((Float.isNaN(xy[0][0])) || Float.isNaN(xy[1][0])) return null;
        Set domain = swathAdapter.getDatasetDomain();
        int[] idx = domain.valueToIndex(xy);
        int[] lens = ((Linear2DSet) domain).getLengths();
        int lenX = lens[0];
        int lenY = lens[1];
        int[] coords = new int[2];
        coords[0] = idx[0] % lenX;
        coords[1] = idx[0] / lenX;

        if ((coords[0] < 0) || (coords[1] < 0)) return null;
        return coords;
    }

    public RealTuple getEarthCoordinates(float[] xy)
            throws VisADException, RemoteException {
        float[][] tup = cs.toReference(new float[][]{{xy[0]}, {xy[1]}});
        return new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[]{(double) tup[0][0], (double) tup[1][0]});
    }

    public int getChannelIndexFromWavenumber(float channel) throws Exception {
        return spectrumAdapter.getChannelIndexFromWavenumber(channel);
    }

    public float getWavenumberFromChannelIndex(int index) throws Exception {
        return spectrumAdapter.getWavenumberFromChannelIndex(index);
    }

    public Rectangle2D getLonLatBoundingBox(CoordinateSystem cs) {
        return null;
    }

    public Rectangle2D getLonLatBoundingBox(HashMap subset)
            throws Exception {
        Set domainSet = swathAdapter.makeDomain(subset);
        return getLonLatBoundingBox(domainSet);
    }

    public static Rectangle2D getLonLatBoundingBox(FlatField field) {
        Set domainSet = field.getDomainSet();
        return getLonLatBoundingBox(domainSet);
    }

    public static float[][] getLonLatBoundingCorners(Set domainSet) throws VisADException {
        CoordinateSystem cs =
                ((SetType) domainSet.getType()).getDomain().getCoordinateSystem();

        float start0, stop0, start1, stop1;
        int len0, len1;

        float[][] corners = null;

        if (domainSet instanceof Linear2DSet) {
            Linear1DSet lset = ((Linear2DSet) domainSet).getLinear1DComponent(0);
            start0 = (float) lset.getFirst();
            stop0 = (float) lset.getLast();
            len0 = lset.getLengthX();
            lset = ((Linear2DSet) domainSet).getLinear1DComponent(1);
            start1 = (float) lset.getFirst();
            stop1 = (float) lset.getLast();
            len1 = lset.getLengthX();

            float x, y, del_x, del_y;
            float lonA = Float.NaN;
            float lonB = Float.NaN;
            float lonC = Float.NaN;
            float lonD = Float.NaN;
            float latA = Float.NaN;
            float latB = Float.NaN;
            float latC = Float.NaN;
            float latD = Float.NaN;

            int nXpts = len0 / 1;
            int nYpts = len1 / 1;

            del_x = (stop0 - start0) / nXpts;
            del_y = (stop1 - start1) / nYpts;
            x = start0;
            y = start1;
            for (int j = 0; j < nYpts; j++) {
                y = start1 + j * del_y;
                for (int i = 0; i < nXpts; i++) {
                    x = start0 + i * del_x;
                    float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                    float lon = lonlat[0][0];
                    float lat = lonlat[1][0];
                    if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                        lonA = lon;
                        latA = lat;
                        break;
                    }
                }
                for (int i = 0; i < nXpts; i++) {
                    x = stop0 - i * del_x;
                    float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                    float lon = lonlat[0][0];
                    float lat = lonlat[1][0];
                    if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                        lonB = lon;
                        latB = lat;
                        break;
                    }
                }
                if (!Float.isNaN(lonA) && !Float.isNaN(lonB)) {
                    break;
                }
            }

            for (int j = 0; j < nYpts; j++) {
                y = stop1 - j * del_y;
                for (int i = 0; i < nXpts; i++) {
                    x = start0 + i * del_x;
                    float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                    float lon = lonlat[0][0];
                    float lat = lonlat[1][0];
                    if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                        lonC = lon;
                        latC = lat;
                        break;
                    }
                }
                for (int i = 0; i < nXpts; i++) {
                    x = stop0 - i * del_x;
                    float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                    float lon = lonlat[0][0];
                    float lat = lonlat[1][0];
                    if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                        lonD = lon;
                        latD = lat;
                        break;
                    }
                }
                if (!Float.isNaN(lonC) && !Float.isNaN(lonD)) {
                    break;
                }
            }
            corners = new float[][]{{lonA, lonB, lonC, lonD}, {latA, latB, latC, latD}};
        } else if (domainSet instanceof Gridded2DSet) {
            int[] lens = ((Gridded2DSet) domainSet).getLengths();
            start0 = 0f;
            start1 = 0f;
            stop0 = (float) lens[0];
            stop1 = (float) lens[1];

            float x, y, del_x, del_y;
            del_x = (stop0 - start0) / 10;
            del_y = (stop1 - start1) / 10;
            x = start0;
            y = start1;
            for (int j = 0; j < 11; j++) {
                y = start1 + j * del_y;
                for (int i = 0; i < 11; i++) {
                    x = start0 + i * del_x;
                    float[][] lonlat = ((Gridded2DSet) domainSet).gridToValue(new float[][]{{x}, {y}});
                    float lon = lonlat[0][0];
                    float lat = lonlat[1][0];
                    if ((lon > 180 || lon < -180) || (lat > 90 || lat < -90)) continue;
                }
            }
        }

        return corners;
    }

    public static Rectangle2D getLonLatBoundingBox(Set domainSet) {
        CoordinateSystem cs =
                ((SetType) domainSet.getType()).getDomain().getCoordinateSystem();

        float start0, stop0, start1, stop1;
        int len0, len1;
        float minLon = Float.MAX_VALUE;
        float minLat = Float.MAX_VALUE;
        float maxLon = -Float.MAX_VALUE;
        float maxLat = -Float.MAX_VALUE;


        if (domainSet instanceof Linear2DSet) {
            Linear1DSet lset = ((Linear2DSet) domainSet).getLinear1DComponent(0);
            start0 = (float) lset.getFirst();
            stop0 = (float) lset.getLast();
            len0 = lset.getLengthX();
            lset = ((Linear2DSet) domainSet).getLinear1DComponent(1);
            start1 = (float) lset.getFirst();
            stop1 = (float) lset.getLast();
            len1 = lset.getLengthX();

            float x, y, del_x, del_y;
            float lonA = Float.NaN;
            float lonB = Float.NaN;
            float lonC = Float.NaN;
            float lonD = Float.NaN;
            float latA = Float.NaN;
            float latB = Float.NaN;
            float latC = Float.NaN;
            float latD = Float.NaN;

            int nXpts = len0 / 8;
            int nYpts = len1 / 8;

            del_x = (stop0 - start0) / nXpts;
            del_y = (stop1 - start1) / nYpts;

            x = start0;
            y = start1;
            try {
                for (int j = 0; j < nYpts; j++) {
                    y = start1 + j * del_y;
                    for (int i = 0; i < nXpts; i++) {
                        x = start0 + i * del_x;
                        float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                        float lon = lonlat[0][0];
                        float lat = lonlat[1][0];
                        if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                            lonA = lon;
                            latA = lat;
                            break;
                        }
                    }
                    for (int i = 0; i < nXpts; i++) {
                        x = stop0 - i * del_x;
                        float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                        float lon = lonlat[0][0];
                        float lat = lonlat[1][0];
                        if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                            lonB = lon;
                            latB = lat;
                            break;
                        }
                    }
                    if (!Float.isNaN(lonA) && !Float.isNaN(lonB)) {
                        break;
                    }
                }

                for (int j = 0; j < nYpts; j++) {
                    y = stop1 - j * del_y;
                    for (int i = 0; i < nXpts; i++) {
                        x = start0 + i * del_x;
                        float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                        float lon = lonlat[0][0];
                        float lat = lonlat[1][0];
                        if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                            lonC = lon;
                            latC = lat;
                            break;
                        }
                    }
                    for (int i = 0; i < nXpts; i++) {
                        x = stop0 - i * del_x;
                        float[][] lonlat = cs.toReference(new float[][]{{x}, {y}});
                        float lon = lonlat[0][0];
                        float lat = lonlat[1][0];
                        if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
                            lonD = lon;
                            latD = lat;
                            break;
                        }
                    }
                    if (!Float.isNaN(lonC) && !Float.isNaN(lonD)) {
                        break;
                    }
                }
                float[][] corners = {{lonA, lonB, lonC, lonD}, {latA, latB, latC, latD}};
                for (int k = 0; k < corners[0].length; k++) {
                    float lon = corners[0][k];
                    float lat = corners[1][k];
                    if (lon < minLon) minLon = lon;
                    if (lat < minLat) minLat = lat;
                    if (lon > maxLon) maxLon = lon;
                    if (lat > maxLat) maxLat = lat;
                }
            } catch (Exception e) {
            }
        } else if (domainSet instanceof Gridded2DSet) {
            int[] lens = ((Gridded2DSet) domainSet).getLengths();
            start0 = 0f;
            start1 = 0f;
            stop0 = (float) lens[0];
            stop1 = (float) lens[1];

            float x, y, del_x, del_y;
            del_x = (stop0 - start0) / 10;
            del_y = (stop1 - start1) / 10;
            x = start0;
            y = start1;
            try {
                for (int j = 0; j < 11; j++) {
                    y = start1 + j * del_y;
                    for (int i = 0; i < 11; i++) {
                        x = start0 + i * del_x;
                        float[][] lonlat = ((Gridded2DSet) domainSet).gridToValue(new float[][]{{x}, {y}});
                        float lon = lonlat[0][0];
                        float lat = lonlat[1][0];
                        if ((lon > 180 || lon < -180) || (lat > 90 || lat < -90)) continue;
                        if (lon < minLon) minLon = lon;
                        if (lat < minLat) minLat = lat;
                        if (lon > maxLon) maxLon = lon;
                        if (lat > maxLat) maxLat = lat;
                    }
                }
            } catch (Exception e) {
            }
        }


        float del_lon = maxLon - minLon;
        float del_lat = maxLat - minLat;

        return new Rectangle2D.Float(minLon, minLat, del_lon, del_lat);
    }

    public float[] radianceToBrightnessTemp(float[] values, float channelValue) {
        float c1 = 1.191066E-5f;           //- mW/m2/ster/cm^-4
        float c2 = 1.438833f;              //- K*cm
        float nu = channelValue;         //- nu: wavenumber
        float B, K, BT;

        int n_values = values.length;
        float[] new_values = new float[n_values];
        for (int i = 0; i < n_values; i++) {
            B = values[i];
            K = (c1 * nu * nu * nu) / B;
            if (K == 0.0) {
                BT = B;
            } else {
                BT = c2 * nu / ((float) (Math.log((double) ((c1 * nu * nu * nu) / B) + 1.0f)));
            }
            if (BT < 0.01) BT = Float.NaN;
            new_values[i] = BT;
        }
        return new_values;
    }

    public float[] radianceToBrightnessTemp(float[] values, float channelValue, String platformName, String sensorName)
            throws Exception {
        float[] new_values = null;

        if (sensorName == null) {
            new_values = radianceToBrightnessTemp(values, channelValue);
        } else if (sensorName == "MODIS") {
            int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channelValue);
            int band_number = MODIS_L1B_Utility.emissive_indexToBandNumber(channelIndex);
            new_values = MODIS_L1B_Utility.modis_radiance_to_brightnessTemp(platformName, band_number, values);
        }
        return new_values;
    }

    public float[] radianceToBrightnessTempSpectrum(float[] values, float[] channelValues) {
        //- Converts radiances [mW/ster/m2/cm^-1] to BT [K]
        //-  Input: nu  array of wavenmbers [cm^-1]
        //-          B   radiances [mW/ster/m2/cm^-1]
        //-  Output: bt brightness temperature in [K]
        //-   Paolo Antonelli
        //-   Wed Feb 25 16:43:05 CST 1998

        float c1 = 1.191066E-5f;           //- mW/m2/ster/cm^-4
        float c2 = 1.438833f;              //- K*cm

        float nu;                        //- wavenumber
        float B, BT;

        int n_values = values.length;
        float[] new_values = new float[n_values];
        for (int i = 0; i < n_values; i++) {
            nu = channelValues[i];
            B = values[i];
            BT = c2 * nu / ((float) (Math.log(((c1 * nu * nu * nu) / B) + 1.0f)));
            new_values[i] = BT;
        }
        return new_values;
    }


    public float[] radianceToBrightnessTempSpectrum(float[] values, float[] channelValues,
                                                    String platformName, String sensorName)
            throws Exception {
        float[] new_values = null;

        if (sensorName == null) {
            new_values = radianceToBrightnessTempSpectrum(values, channelValues);
        } else if (sensorName == "MODIS") {
            new_values = new float[values.length];
            for (int k = 0; k < new_values.length; k++) {
                int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channelValues[k]);
                int band_number = MODIS_L1B_Utility.emissive_indexToBandNumber(channelIndex);
                float[] tmp = new float[1];
                tmp[0] = values[k];
                new_values[k] = (MODIS_L1B_Utility.modis_radiance_to_brightnessTemp(platformName, band_number, tmp))[0];
            }
        }

        return new_values;
    }

    public HashMap getDefaultSubset() {
        HashMap subset = swathAdapter.getDefaultSubset();
        double chanIdx = 0;

        try {
            chanIdx = spectrumAdapter.getChannelIndexFromWavenumber(init_wavenumber);
        } catch (Exception e) {
            System.out.println("couldn't get chanIdx, using zero");
        }

        subset.put(SpectrumAdapter.channelIndex_name, new double[]{chanIdx, chanIdx, 1});
        return subset;
    }


    public SpectrumAdapter getSpectrumAdapter() {
        return spectrumAdapter;
    }

    //public SwathAdapter getSwathAdapter() {
    public GeoSfcAdapter getSwathAdapter() {
        return swathAdapter;
    }

    public int getNumChannels() {
        return spectrumAdapter.getNumChannels();
    }

    public MultiDimensionSubset getDefaultSubsetImage() {
        return new MultiDimensionSubset(getDefaultSubset());
    }

    public MultiDimensionSubset getDefaultSubsetSpectrum() {
        return new MultiDimensionSubset(spectrumAdapter.getDefaultSubset());
    }

    public String toString() {
        return swathAdapter.toString();
    }
}

class FloatArrayCache {
    Linear2DSet domainSet0;
    float[] fltArray0;
    Linear2DSet domainSet1;
    float[] fltArray1;

    void put(Linear2DSet domSet, float[] fltArray) {
        if (domainSet0 == null || SwathAdapter.dimsEquals(domSet, domainSet0)) {
            domainSet0 = domSet;
            fltArray0 = fltArray;
        } else {
            domainSet1 = domSet;
            fltArray1 = fltArray;
        }
    }

    float[] get(Linear2DSet domSet) {
        if (domainSet0 != null && SwathAdapter.dimsEquals(domainSet0, domSet)) {
            return fltArray0;
        } else if (domainSet1 != null && SwathAdapter.dimsEquals(domainSet1, domSet)) {
            return fltArray1;
        } else {
            return null;
        }
    }
}
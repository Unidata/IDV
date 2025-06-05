package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.ArrayList;
import java.util.HashMap;

import visad.FunctionType;
import visad.Gridded1DSet;
import visad.QuickSort;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SingletonSet;
import visad.VisADException;

public class SpectrumAdapter extends MultiDimensionAdapter {

    public static String channels_name = "Channels";
    public static String channelIndex_name = "channelIndex";
    public static String FOVindex_name = "FOVindex";
    public static String channelUnit = "cm";
    public static String channelType = "wavenumber";
    public static String array_name = "array_name";
    public static String array_dimension_names = "array_dimension_names";
    public static String range_name = "range_name";
    public static String x_dim_name = "x_dim"; //- 2 spatial dimensions, x fastest varying
    public static String y_dim_name = "y_dim"; //-----------------------------------------
    public static String time_dim_name = "time_dim";
    public static String ancillary_file_name = "ancillary_file";
    public static String channelValues = "channelValues";
    public static String bandNames = "bandNames";
    public static String channelIndices_name = "channelIndices";


    public static HashMap getEmptyMetadataTable() {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(array_name, null);
        metadata.put(range_name, null);
        metadata.put(channelIndex_name, null);
        metadata.put(ancillary_file_name, null);
        metadata.put(x_dim_name, null);
        metadata.put(y_dim_name, null);
        metadata.put(time_dim_name, null);
        metadata.put(channelUnit, null);
        metadata.put(channelType, "wavenumber");
        metadata.put(channelValues, null);
        metadata.put(bandNames, null);
        metadata.put(channelIndices_name, null);

        return metadata;
    }

    public static HashMap<String, double[]> getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(x_dim_name, new double[3]);
        subset.put(y_dim_name, new double[3]);
        subset.put(channelIndex_name, new double[3]);
        return subset;
    }

    int numChannels;
    int channelIndex = -1;
    int[] channel_sort;
    SampledSet domainSet;
    RealType channelRealType;
    RealType spectrumRangeType;
    FunctionType spectrumType;

    ArrayList<String> bandNameList = new ArrayList<String>();
    String[] bandNameArray = null;
    HashMap<String, Float> bandNameMap = null;
    boolean hasBandNames = false;

    /* These should be monotonic increasing. Used to index a subset of channels from multispectral array.
       For example, AVHRR/3 has six channels, but 3a and 3b cannot operate at the same time so they
       may share the '2' index in the array.
    */
    int[] channelIndices = null;

    public SpectrumAdapter(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        this.init();
    }

    private void init() {
        for (int k = 0; k < array_rank; k++) {
            String name = (String) metadata.get(channelIndex_name);
            if (name != null) {
                if (name.equals(array_dim_names[k])) {
                    channelIndex = k;
                }
            }
        }

        channelIndices = (int[]) metadata.get(channelIndices_name);

        numChannels = computeNumChannels();

        String[] names = (String[]) metadata.get(bandNames);
        if (names != null) {
            hasBandNames = true;
            bandNameArray = new String[names.length];
            for (int k = 0; k < names.length; k++) {
                bandNameList.add(names[k]);
                bandNameArray[k] = names[k];
            }
        }

        try {
            domainSet = makeDomainSet();
            rangeType = makeSpectrumRangeType();
            spectrumType = new FunctionType(channelRealType, spectrumRangeType);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("cannot create spectrum domain");
        }

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

    public int computeNumChannels() {
        if (channelIndex == -1) {
            return 1;
        } else if (channelIndices != null) {
            return channelIndices.length;
        } else {
            return array_dim_lengths[channelIndex];
        }
    }

    public Set makeDomain(Object subset) throws Exception {
        return domainSet;
    }

    public SampledSet getDomainSet() throws Exception {
        return domainSet;
    }

    private SampledSet makeDomainSet() throws Exception {
        RealType domainType = makeSpectrumDomainType();
        float[] channels = getChannels();
        channel_sort = QuickSort.sort(channels);
        if (numChannels == 1) {
            domainSet = new SingletonSet(new RealTupleType(domainType), new double[]{(double) channels[0]}, null, null, null);
        } else {
            domainSet = new Gridded1DSet(domainType, new float[][]{channels}, numChannels);
        }
        return domainSet;
    }

    public float[] getChannels() throws Exception {
        float[] channels = null;
        if (metadata.get(channelValues) == null) {
            channels = reader.getFloatArray((String) metadata.get(channels_name),
                    new int[]{0}, new int[]{numChannels}, new int[]{1});
        } else {
            channels = (float[]) metadata.get(channelValues);
        }

        if (hasBandNames) {
            bandNameMap = new HashMap<String, Float>();
            for (int k = 0; k < numChannels; k++) {
                bandNameMap.put(bandNameArray[k], Float.valueOf(channels[k]));
            }
        }
        return channels;
    }

    public RealType makeSpectrumDomainType() throws Exception {
        /**
         if ( ((String)metadata.get(channelType)).equals("wavenumber") ) {
         ScaledUnit centimeter = new ScaledUnit(0.01, CommonUnit.meter, "cm");
         Unit tmp_unit = centimeter.pow(-1);
         ScaledUnit inv_centimeter = new ScaledUnit(1.0, tmp_unit, "cm^-1");
         channelRealType = RealType.getRealType("wavenumber", null);
         }
         **/
        channelRealType = RealType.getRealType((String) metadata.get(channelType), null);
        return channelRealType;
    }

    public RealType makeSpectrumRangeType() throws Exception {
        spectrumRangeType = RealType.getRealType("Radiance");
        return spectrumRangeType;
    }

    float[] sortRange(float[] range) {
        float[] sorted_range = new float[numChannels];
        for (int k = 0; k < numChannels; k++) sorted_range[k] = range[channel_sort[k]];
        return sorted_range;
    }

    double[] sortRange(double[] range) {
        double[] sorted_range = new double[numChannels];
        for (int k = 0; k < numChannels; k++) sorted_range[k] = range[channel_sort[k]];
        return sorted_range;
    }

    public HashMap getDefaultSubset() {
        HashMap<String, double[]> subset = SpectrumAdapter.getEmptySubset();

        double[] coords = (double[]) subset.get(y_dim_name);
        coords[0] = 1.0;
        coords[1] = 1.0;
        coords[2] = 1.0;
        subset.put(y_dim_name, coords);

        coords = (double[]) subset.get(x_dim_name);
        coords[0] = 1.0;
        coords[1] = 1.0;
        coords[2] = 1.0;
        subset.put(x_dim_name, coords);

        coords = (double[]) subset.get(channelIndex_name);
        if (channelIndices != null) {
            coords[0] = channelIndices[0];
        } else {
            coords[0] = 0.0;
        }
        coords[1] = (double) (numChannels - 1);
        coords[2] = 1.0;
        subset.put(channelIndex_name, coords);

        return subset;
    }

    public int getChannelIndexFromWavenumber(float wavenumber) throws VisADException {
        int idx = (domainSet.valueToIndex(new float[][]{{wavenumber}}))[0];
        idx = channel_sort[idx];
        if (channelIndices != null) {
            idx = channelIndices[idx];
        }
        return idx;
    }

    public float getWavenumberFromChannelIndex(int index) throws VisADException {
        int idx = index;
        if (channelIndices != null) {
            for (int i = 0; i < numChannels; i++) {
                if (index == channelIndices[i]) {
                    idx = i;
                    break;
                }
            }
        }
        idx = channel_sort[idx];
        return (domainSet.indexToValue(new int[]{idx}))[0][0];
    }

    public float getWavenumberFromBandName(String bandName) {
        return bandNameMap.get(bandName);
    }

    public int getNumChannels() {
        return numChannels;
    }
}
package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Set;
import visad.Gridded1DSet;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;
import visad.QuickSort;

import java.util.HashMap;

public abstract class ProfileAlongTrack extends MultiDimensionAdapter {

    private FunctionType mathtype;

    int TrackLen;
    int VertLen;

    private float[] vertLocs = null;
    private float[] trackTimes = null;
    private float[] trackLongitude = null;
    private float[] trackLatitude = null;

    public static String longitude_name = "Longitude";
    public static String latitude_name = "Latitude";
    public static String trackDim_name = "TrackDim";
    public static String vertDim_name = "VertDim";
    public static String array_name = "array_name";
    public static String profileTime_name = "ProfileTime";
    public static String profileTime_unit = "ProfileTime_unit";
    public static String altitude_unit = "altitude_unit";
    public static String sfcElev_name = "SurfaceElev";
    public static String valid_range = "valid_range";
    public static String ancillary_file_name = "ancillary_file";

    String[] rangeName_s = null;
    Class[] arrayType_s = null;
    Unit[] rangeUnit_s = new Unit[]{null};

    RealType track = RealType.getRealType(trackDim_name);
    RealType vert = RealType.getRealType(vertDim_name);
    RealType[] domainRealTypes = new RealType[2];

    RealType vertLocType;
    RealType trackTimeType;

    int track_idx = -1;
    int vert_idx = -1;
    int range_rank = -1;

    int track_tup_idx;
    int vert_tup_idx;

    boolean isVertDimAlt = true;

    CoordinateSystem cs = null;

    public static HashMap getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(trackDim_name, new double[3]);
        subset.put(vertDim_name, new double[3]);
        return subset;
    }

    public static HashMap getEmptyMetadataTable() {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(array_name, null);
        metadata.put(trackDim_name, null);
        metadata.put(vertDim_name, null);
        metadata.put(longitude_name, null);
        metadata.put(latitude_name, null);
        metadata.put(profileTime_name, null);
        metadata.put(profileTime_unit, null);
        metadata.put(altitude_unit, null);
        metadata.put(sfcElev_name, null);
        metadata.put(scale_name, null);
        metadata.put(offset_name, null);
        metadata.put(fill_value_name, null);
        /*
        metadata.put(range_name, null);
        metadata.put(range_unit, null);
        metadata.put(valid_range, null);
        */
        return metadata;
    }

    public ProfileAlongTrack() {
    }

    public ProfileAlongTrack(MultiDimensionReader reader, HashMap metadata) {
        this(reader, metadata, true);
    }

    public ProfileAlongTrack(MultiDimensionReader reader, HashMap metadata, boolean isVertDimAlt) {
        super(reader, metadata);
        this.isVertDimAlt = isVertDimAlt;
        this.init();
    }


    private void init() {
        for (int k = 0; k < array_rank; k++) {
            if (((String) metadata.get(trackDim_name)).equals(array_dim_names[k])) {
                track_idx = k;
            }
            if (((String) metadata.get(vertDim_name)).equals(array_dim_names[k])) {
                vert_idx = k;
            }
        }

        int[] lengths = new int[2];

        if (track_idx < vert_idx) {
            domainRealTypes[0] = vert;
            domainRealTypes[1] = track;
            track_tup_idx = 1;
            vert_tup_idx = 0;
            lengths[0] = array_dim_lengths[vert_idx];
            lengths[1] = array_dim_lengths[track_idx];
        } else {
            domainRealTypes[0] = track;
            domainRealTypes[1] = vert;
            track_tup_idx = 0;
            vert_tup_idx = 1;
            lengths[0] = array_dim_lengths[track_idx];
            lengths[1] = array_dim_lengths[vert_idx];
        }

        TrackLen = array_dim_lengths[track_idx];
        VertLen = array_dim_lengths[vert_idx];

        String rangeName = null;
        if (metadata.get("range_name") != null) {
            rangeName = (String) metadata.get("range_name");
        } else {
            rangeName = (String) metadata.get("array_name");
        }
        rangeType = RealType.getRealType(rangeName, rangeUnit_s[0]);

        try {
            rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
        } catch (Exception e) {
            System.out.println("RangeProcessor failed to create");
            e.printStackTrace();
        }

        try {
            if (isVertDimAlt) {
                vertLocs = getVertBinAltitude();
            }
            vertLocType = makeVertLocType();
            trackTimes = getTrackTimes();
            trackTimeType = makeTrackTimeType();
            trackLongitude = getTrackLongitude();
            trackLatitude = getTrackLatitude();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public int getTrackLength() {
        return TrackLen;
    }

    public int getVertLength() {
        return VertLen;
    }

    public int getVertIdx() {
        return vert_idx;
    }

    public int getTrackIdx() {
        return track_idx;
    }

    public int getVertTupIdx() {
        return vert_tup_idx;
    }

    public int getTrackTupIdx() {
        return track_tup_idx;
    }

    public Set makeDomain(Object subset) throws Exception {
        double[] first = new double[2];
        double[] last = new double[2];
        int[] length = new int[2];

        HashMap<String, double[]> domainSubset = new HashMap<String, double[]>();
        domainSubset.put(trackDim_name, (double[]) ((HashMap) subset).get(trackDim_name));
        domainSubset.put(vertDim_name, (double[]) ((HashMap) subset).get(vertDim_name));

        for (int kk = 0; kk < 2; kk++) {
            RealType rtype = domainRealTypes[kk];
            double[] coords = (double[]) ((HashMap) subset).get(rtype.getName());
            first[kk] = coords[0];
            last[kk] = coords[1];
            length[kk] = (int) ((last[kk] - first[kk]) / coords[2] + 1);
            last[kk] = first[kk] + coords[2] * (length[kk] - 1);
        }
        Linear2DSet domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
        final Linear1DSet[] lin1DSet_s = new Linear1DSet[]{domainSet.getLinear1DComponent(0),
                domainSet.getLinear1DComponent(1)};

        float[] new_altitudes = new float[length[vert_tup_idx]];
        float[] new_times = new float[length[track_tup_idx]];

        int track_start = (int) first[track_tup_idx];
        int vert_start = (int) first[vert_tup_idx];
        int vert_skip = (int) ((double[]) ((HashMap) subset).get(vertDim_name))[2];
        int track_skip = (int) ((double[]) ((HashMap) subset).get(trackDim_name))[2];
        for (int k = 0; k < new_altitudes.length; k++) {
            new_altitudes[k] = vertLocs[vert_start + (k * vert_skip)];
        }

        for (int k = 0; k < new_times.length; k++) {
            new_times[k] = trackTimes[track_start + (k * track_skip)];
        }

        final Gridded1DSet alt_set = new Gridded1DSet(vertLocType, new float[][]{new_altitudes}, new_altitudes.length);
        final Gridded1DSet time_set = new Gridded1DSet(trackTimeType, new float[][]{new_times}, new_times.length);
        final float vert_offset = (float) first[vert_tup_idx];
        final float track_offset = (float) first[track_tup_idx];

        RealTupleType reference = new RealTupleType(vertLocType, trackTimeType);

        CoordinateSystem cs = null;

        try {
            cs = new CoordinateSystem(reference, null) {
                public float[][] toReference(float[][] vals) throws VisADException {
                    int[] indexes = lin1DSet_s[0].valueToIndex(new float[][]{vals[0]});
                    for (int k = 0; k < vals[0].length; k++) {
                        //-indexes[k] = (int) (vals[vert_tup_idx][k] - vert_offset); ?
                    }
                    float[][] alts = alt_set.indexToValue(indexes);

                    indexes = lin1DSet_s[1].valueToIndex(new float[][]{vals[1]});
                    for (int k = 0; k < vals[0].length; k++) {
                        //-indexes[k] = (int) (vals[track_tup_idx][k] - track_offset); ?
                    }
                    float[][] times = time_set.indexToValue(indexes);

                    return new float[][]{alts[0], times[0]};
                }

                public float[][] fromReference(float[][] vals) throws VisADException {
                    int[] indexes = alt_set.valueToIndex(new float[][]{vals[vert_tup_idx]});
                    float[][] vert_coords = lin1DSet_s[vert_tup_idx].indexToValue(indexes);
                    indexes = time_set.valueToIndex(new float[][]{vals[track_tup_idx]});
                    float[][] track_coords = lin1DSet_s[track_tup_idx].indexToValue(indexes);
                    return new float[][]{vert_coords[0], track_coords[0]};
                }

                public double[][] toReference(double[][] vals) throws VisADException {
                    return Set.floatToDouble(toReference(Set.doubleToFloat(vals)));
                }

                public double[][] fromReference(double[][] vals) throws VisADException {
                    return Set.floatToDouble(fromReference(Set.doubleToFloat(vals)));
                }

                public boolean equals(Object obj) {
                    return true;
                }
            };
        } catch (Exception e) {
        }

        RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], cs, null);
        domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);

        return domainSet;
    }

    public FunctionType getMathType() {
        return null;
    }

    public RealType[] getDomainRealTypes() {
        return domainRealTypes;
    }

    public HashMap getDefaultSubset() {
        HashMap subset = ProfileAlongTrack.getEmptySubset();

        double[] coords = (double[]) subset.get("TrackDim");
        coords[0] = 20000.0;
        coords[1] = (TrackLen - 15000.0) - 1;
        //-coords[2] = 30.0;
        coords[2] = 5.0;
        subset.put("TrackDim", coords);

        coords = (double[]) subset.get("VertDim");
        coords[0] = 98.0;
        coords[1] = (VertLen) - 1;
        coords[2] = 2.0;
        subset.put("VertDim", coords);
        return subset;
    }

    public int[] getTrackRangeInsideLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
        int nn = 100;
        int skip = TrackLen / nn;
        double lon;
        double lat;

        int idx = 0;
        while (idx < TrackLen) {
            lon = (double) trackLongitude[idx];
            lat = (double) trackLatitude[idx];
            if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat) && (lat < maxLat))) break;
            idx += skip;
        }
        if (idx > TrackLen - 1) idx = TrackLen - 1;
        if (idx == TrackLen - 1) return new int[]{-1, -1};

        int low_idx = idx;
        while (low_idx > 0) {
            lon = (double) trackLongitude[low_idx];
            lat = (double) trackLatitude[low_idx];
            if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat) && (lat < maxLat))) {
                low_idx -= 1;
                continue;
            } else {
                break;
            }
        }

        int hi_idx = idx;
        while (hi_idx < TrackLen - 1) {
            lon = (double) trackLongitude[hi_idx];
            lat = (double) trackLatitude[hi_idx];
            if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat) && (lat < maxLat))) {
                hi_idx += 1;
                continue;
            } else {
                break;
            }
        }
        return new int[]{low_idx, hi_idx};
    }

    public HashMap getSubsetFromLonLatRect(HashMap subset, double minLat, double maxLat, double minLon, double maxLon) {
        double[] coords = (double[]) subset.get("TrackDim");
        int[] idxs = getTrackRangeInsideLonLatRect(minLat, maxLat, minLon, maxLon);
        coords[0] = (double) idxs[0];
        coords[1] = (double) idxs[1];
        if ((coords[0] == -1) || (coords[1] == -1)) return null;
        return subset;
    }

    public HashMap getSubsetFromLonLatRect(HashMap subset, double minLat, double maxLat, double minLon, double maxLon,
                                           int xStride, int yStride, int zStride) {

        double[] coords = (double[]) subset.get("TrackDim");
        int[] idxs = getTrackRangeInsideLonLatRect(minLat, maxLat, minLon, maxLon);
        coords[0] = (double) idxs[0];
        coords[1] = (double) idxs[1];
        if ((coords[0] == -1) || (coords[1] == -1)) return null;
        if (xStride > 0) {
            coords[2] = xStride;
        }

        coords = (double[]) subset.get("VertDim");
        if (yStride > 0) {
            coords[2] = yStride;
        }
        return subset;
    }

    public HashMap getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
        return getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon);
    }

    public HashMap getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon,
                                           int xStride, int yStride, int zStride) {

        return getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon, xStride, yStride, zStride);
    }

    public abstract float[] getVertBinAltitude() throws Exception;

    public abstract float[] getTrackTimes() throws Exception;

    public abstract RealType makeVertLocType() throws Exception;

    public abstract RealType makeTrackTimeType() throws Exception;

    public abstract float[] getTrackLongitude() throws Exception;

    public abstract float[] getTrackLatitude() throws Exception;

    public static float[] medianFilter(float[] A, int lenx, int leny, int window_lenx, int window_leny)
            throws VisADException {
        float[] result = new float[A.length];
        float[] window = new float[window_lenx * window_leny];
        float[] new_window = new float[window_lenx * window_leny];
        int[] sort_indexes = new int[window_lenx * window_leny];

        int a_idx;
        int w_idx;

        int w_lenx = window_lenx / 2;
        int w_leny = window_leny / 2;

        int lo;
        int hi;
        int ww_jj;
        int ww_ii;
        int cnt;

        for (int j = 0; j < leny; j++) {
            for (int i = 0; i < lenx; i++) {
                a_idx = j * lenx + i;
                cnt = 0;
                for (int w_j = -w_leny; w_j < w_leny; w_j++) {
                    for (int w_i = -w_lenx; w_i < w_lenx; w_i++) {
                        ww_jj = w_j + j;
                        ww_ii = w_i + i;
                        w_idx = (w_j + w_leny) * window_lenx + (w_i + w_lenx);
                        if ((ww_jj >= 0) && (ww_ii >= 0) && (ww_jj < leny) && (ww_ii < lenx)) {
                            window[cnt] = A[ww_jj * lenx + ww_ii];
                            cnt++;
                        }
                    }
                }
                System.arraycopy(window, 0, new_window, 0, cnt);
                //-sort_indexes = QuickSort.sort(new_window, sort_indexes);
                sort_indexes = QuickSort.sort(new_window);
                result[a_idx] = new_window[cnt / 2];
            }
        }
        return result;
    }

}
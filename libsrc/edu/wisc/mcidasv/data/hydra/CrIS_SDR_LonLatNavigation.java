package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Gridded2DSet;
import visad.RealTupleType;

public class CrIS_SDR_LonLatNavigation extends SwathNavigation  {

    private Gridded2DSet gset = null;

    public CrIS_SDR_LonLatNavigation(SwathAdapter swathAdapter) throws Exception {
        super(swathAdapter);
    }

    Gridded2DSet createInterpSet() throws Exception {

        int[] new_geo_start = new int[3];
        int[] new_geo_count = new int[3];
        int[] new_geo_stride = new int[3];

        new_geo_start[geo_xtrack_idx] = 0;
        new_geo_count[geo_xtrack_idx] = 30;
        new_geo_stride[geo_xtrack_idx] = 1;
        new_geo_start[geo_track_idx] = 0;
        new_geo_count[geo_track_idx] = geo_count[geo_track_idx];
        new_geo_stride[geo_track_idx] = 1;
        new_geo_start[2] = 0;
        new_geo_count[2] = 9;
        new_geo_stride[2] = 1;

        float[] lons = reader.getFloatArray(lon_array_name, new_geo_start, new_geo_count, new_geo_stride);
        float[] lats = reader.getFloatArray(lat_array_name, new_geo_start, new_geo_count, new_geo_stride);

        gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                new float[][] {lons, lats},
                geo_count[idx_order[0]], geo_count[idx_order[1]],
                null, null, null, false, false);
        return gset;
    }

    public Gridded2DSet getInterpSet() {
        return gset;
    }
}
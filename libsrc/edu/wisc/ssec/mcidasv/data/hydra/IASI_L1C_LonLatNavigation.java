package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Gridded2DSet;
import visad.RealTupleType;

public class IASI_L1C_LonLatNavigation extends SwathNavigation  {

    public IASI_L1C_LonLatNavigation(SwathAdapter swathAdapter) throws Exception {
        super(swathAdapter);
    }

    Gridded2DSet createInterpSet() throws Exception {
        float scale = 1E-06f;

        int[] new_geo_start = new int[2];
        int[] new_geo_count = new int[2];
        int[] new_geo_stride = new int[2];

        new_geo_start[geo_xtrack_idx] = 0;
        new_geo_count[geo_xtrack_idx] = 60*2;
        new_geo_stride[geo_xtrack_idx] = 1;
        new_geo_start[geo_track_idx] = 0;
        new_geo_count[geo_track_idx] = geo_count[geo_track_idx]/2;
        new_geo_stride[geo_track_idx] = 1;

        int[] lonValues = reader.getIntArray(lon_array_name, new_geo_start, new_geo_count, new_geo_stride);
        int[] latValues = reader.getIntArray(lat_array_name, new_geo_start, new_geo_count, new_geo_stride);

        float[] lons = new float[lonValues.length];
        float[] lats = new float[latValues.length];

        for (int k=0; k<lons.length; k++) {
            lons[k] = ((float)lonValues[k])*scale;
            lats[k] = ((float)latValues[k])*scale;
        }

        lons = IASI_L1C_Utility.psuedoScanReorder2(lons, 60, geo_count[0]);
        lats = IASI_L1C_Utility.psuedoScanReorder2(lats, 60, geo_count[0]);

        Gridded2DSet gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                new float[][] {lons, lats},
                geo_count[idx_order[0]], geo_count[idx_order[1]],
                null, null, null, false, false);
        return gset;
    }
}
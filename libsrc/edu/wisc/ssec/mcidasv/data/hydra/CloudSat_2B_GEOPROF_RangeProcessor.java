package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

class CloudSat_2B_GEOPROF_RangeProcessor extends RangeProcessor {

    public CloudSat_2B_GEOPROF_RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
        super(reader, metadata);
        if (scale == null) { // use implicit default value since E05, E06 has removed the scale/offset from the Radar Refl variable
            scale = new float[]{100f};
            offset = new float[]{0f};
        }
    }

    public float[] processRange(short[] values, HashMap subset) {
        float[] new_values = new float[values.length];
        for (int k = 0; k < values.length; k++) {
            float val = (float) values[k];
            if (val == missing[0]) {
                new_values[k] = Float.NaN;
            } else if ((val < valid_low) || (val > valid_high)) {
                new_values[k] = -40f;
            } else {
                new_values[k] = val / scale[0] + offset[0];
            }
        }
        return new_values;
    }

}
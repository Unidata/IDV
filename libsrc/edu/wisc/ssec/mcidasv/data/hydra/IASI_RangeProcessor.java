package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

class IASI_RangeProcessor extends RangeProcessor {

    public IASI_RangeProcessor() throws Exception {
        super();
    }

    public float[] processRange(short[] values, HashMap subset) {
        int channelIndex = (int) ((double[]) subset.get(SpectrumAdapter.channelIndex_name))[0];

        float[] new_values = IASI_L1C_Utility.getDecodedIASIImage(values, null, channelIndex);

        double[] track_coords = (double[]) subset.get(SwathAdapter.track_name);
        double[] xtrack_coords = (double[]) subset.get(SwathAdapter.xtrack_name);

        int numElems = ((int) (xtrack_coords[1] - xtrack_coords[0]) + 1);
        int numLines = ((int) (track_coords[1] - track_coords[0]) + 1);

        new_values = IASI_L1C_Utility.psuedoScanReorder2(new_values, numElems / 2, numLines * 2);

        //- subset here, if necessary

        return new_values;
    }

}
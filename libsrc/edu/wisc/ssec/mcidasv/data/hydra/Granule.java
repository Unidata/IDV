package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.ArrayList;
import java.util.HashMap;

import ucar.ma2.Range;

public class Granule {


    public int trackLen = 0;

    ArrayList<Granule> granules = null;


    public Granule(int trackLen) {
        this.trackLen = trackLen;
    }

    public Granule() {
        granules = new ArrayList<Granule>();
    }

    public void add(Granule granule) {
        trackLen += granule.trackLen;
        granules.add(granule);
    }


    public ArrayList<Segment> getGranulesSpanned(int start, int count, int stride) {

        ArrayList<Segment> segments = new ArrayList<Segment>();

        Segment segment = new Segment();

        // get start granule and initialize first segment;
        int granIdx = 0;

        segment.start = start;
        segment.granIdx = granIdx;
        int totLen = granules.get(granIdx).trackLen;
        int diff = start - (totLen - 1);
        while (diff > 0) {
            granIdx += 1;
            segment.granIdx = granIdx;
            segment.start = diff - 1; // back to zero-based after the diff
            totLen += granules.get(granIdx).trackLen;
            diff = start - (totLen - 1);
        }


        int segStart = 0;
        int segCnt = 0;

        for (int k = 0; k < count; k++) {
            int pos = start + k * stride;

            if ((pos - (totLen - 1)) <= 0) { // make sure matching zero-based index
                segCnt++;
                segment.count = segCnt;
            } else {
                segments.add(segment); // add middle segments


                granIdx += 1;
                segStart = (pos - 1) - (totLen - 1); //make sure zero-based index

                segment = new Segment();
                segment.granIdx = granIdx;
                segment.start = segStart;
                segCnt = 1;
                segment.count = segCnt;
                totLen += granules.get(granIdx).trackLen;
            }
        }

        // add last, or first if only one.
        segments.add(segment);

        return segments;
    }


    public ArrayList<SegmentRange> getGranulesRanges(int start, int count, int stride) throws Exception {
        ArrayList<SegmentRange> ranges = new ArrayList<SegmentRange>();

        ArrayList<Segment> segments = getGranulesSpanned(start, count, stride);

        for (int k = 0; k < segments.size(); k++) {
            Segment segment = segments.get(k);
            int first = segment.start;
            int last = (segment.count - 1) * stride + first;

            Range rng = new Range(first, last, stride);

            SegmentRange segRng = new SegmentRange();
            segRng.granIdx = segment.granIdx;
            segRng.range = rng;
            ranges.add(segRng);
        }

        return ranges;
    }

}

class Segment {
    int granIdx;
    int start;
    int count;
    int stride;
}
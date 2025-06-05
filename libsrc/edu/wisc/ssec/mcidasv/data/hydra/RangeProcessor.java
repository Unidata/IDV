package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;
import java.util.StringTokenizer;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import visad.util.Util;

public class RangeProcessor {

   // private static final Logger logger = LoggerFactory.getLogger(RangeProcessor.class);

    public static RangeProcessor createRangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
        if (reader instanceof GranuleAggregation) {
            AggregationRangeProcessor aggrRngProcessor = new AggregationRangeProcessor((GranuleAggregation) reader, metadata);
            ((GranuleAggregation) reader).addPreProcessor((String) metadata.get(SwathAdapter.array_name), aggrRngProcessor);
            return aggrRngProcessor;
        }

        String product_name = (String) metadata.get(MultiDimensionAdapter.product_name);
        if (product_name == null) {
            return new RangeProcessor(reader, metadata);
        }

        switch (product_name) {
            case "IASI_L1C_xxx":
                return new IASI_RangeProcessor();
            case "IASI_L1C_ncdf":
                return new IASI_NCDF_RangeProcessor(reader, metadata);
//					case "IASI_L1C_AAPP":
//						return new IASI_L1C_AAPP_RangeProcessor(new RangeProcessor(reader, metadata));
            case "GEOPROF":
                return new CloudSat_2B_GEOPROF_RangeProcessor(reader, metadata);
            case "ModisCloudMask":
                return new MODIS_CloudMask();
        }

        return new RangeProcessor(reader, metadata);
    }

    MultiDimensionReader reader;
    HashMap metadata;

    float[] scale = null;
    float[] offset = null;
    double[] missing = null;
    double[] valid_range = null;
    double valid_low = -Double.MAX_VALUE;
    double valid_high = Double.MAX_VALUE;

    boolean unpack = false;
    boolean unsigned = false;
    boolean rangeCheckBeforeScaling = true;
    boolean divideByScale = false;

    int scaleOffsetLen = 1;

    String multiScaleDimName = SpectrumAdapter.channelIndex_name;
    boolean hasMultiDimensionScale = false;

    int multiScaleDimensionIndex = 0;

    int soIndex = 0;

    public RangeProcessor() {
    }

    public RangeProcessor(float scale, float offset, float valid_low, float valid_high, float missing) {
        this.scale = new float[]{scale};
        this.offset = new float[]{offset};
        this.valid_low = valid_low;
        this.valid_high = valid_high;
        this.missing = new double[]{missing};
    }

    public RangeProcessor(MultiDimensionReader reader, HashMap metadata, double[] missing) {
        this.reader = reader;
        this.metadata = metadata;

        getFlags();

        try {
            getScaleOffset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.missing = java.util.Arrays.copyOf(missing, missing.length);
        if (unsigned) {
            for (int i = 0; i < this.missing.length; i++) {
                this.missing[i] = (double) Util.unsignedShortToInt((short) missing[i]);
            }
        }
    }

    public RangeProcessor(MultiDimensionReader reader, HashMap metadata, float scale, float offset, float valid_low, float valid_high, double[] missing) {
        this.reader = reader;
        this.metadata = metadata;

        getFlags();

        this.scale = new float[]{scale};
        if (divideByScale) {
            this.scale[0] = 1f / this.scale[0];
        }
        scaleOffsetLen = 1;

        this.offset = new float[]{offset};

        this.missing = java.util.Arrays.copyOf(missing, missing.length);
        if (unsigned) {
            for (int i = 0; i < this.missing.length; i++) {
                this.missing[i] = (double) Util.unsignedShortToInt((short) missing[i]);
            }
        }

        if (!Float.isNaN(valid_low) && !Float.isNaN(valid_high)) {
            this.valid_low = valid_low;
            this.valid_high = valid_high;
        }

        if (rangeCheckBeforeScaling) {
            if (unsigned) {
                if (this.valid_low != -Double.MAX_VALUE) {
                    this.valid_low = (double) Util.unsignedShortToInt((short) valid_low);
                }
                if (this.valid_high != Double.MAX_VALUE) {
                    this.valid_high = (double) Util.unsignedShortToInt((short) valid_high);
                }
            }
        }
    }


    public RangeProcessor(MultiDimensionReader reader, HashMap metadata, String multiScaleDimName) throws Exception {
        this(reader, metadata);
        this.multiScaleDimName = multiScaleDimName;
    }

    public RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
        this.reader = reader;
        this.metadata = metadata;

        String array_name = (String) metadata.get("array_name");

        if (metadata.get("unpack") != null) {
            unpack = true;
        }

        if (metadata.get("unsigned") != null) {
            unsigned = true;
        }

        if (metadata.get("divideByScale") != null) {
            divideByScale = true;
        }

        if (metadata.get("range_check_after_scaling") != null) {
            String s = (String) metadata.get("range_check_after_scaling");
            //logger.debug("range_check_after_scaling: " + s);
            rangeCheckBeforeScaling = false;
        }


        String metaStr = (String) metadata.get("scale_name");
        if (metaStr == null) {
            metaStr = "scale_factor";
        }
        scale = getAttributeAsFloatArray(array_name, metaStr);

        if (divideByScale && scale != null) {
            for (int k = 0; k < scale.length; k++) scale[k] = 1f / scale[k];
        }

        metaStr = (String) metadata.get("offset_name");
        if (metaStr == null) {
            metaStr = "add_offset";
        }
        offset = getAttributeAsFloatArray(array_name, metaStr);

        if (scale != null) {
            scaleOffsetLen = scale.length;

            if (offset != null) {
                if (scale.length != offset.length) {
                    throw new Exception("RangeProcessor: scale and offset array lengths must be equal");
                }
            } else {
                offset = new float[scaleOffsetLen];
                for (int i = 0; i < offset.length; i++) offset[i] = 0f;
            }

        }

        metaStr = (String) metadata.get("fill_value_name");
        if (metaStr == null) {
            metaStr = "_FillValue"; // Convention name
        }
        missing = getAttributeAsDoubleArray(array_name, metaStr);

        // if we are working with unsigned data, need to convert missing vals to unsigned too
        if (unsigned) {
            if (missing != null) {
                for (int i = 0; i < missing.length; i++) {
                    missing[i] = (float) Util.unsignedShortToInt((short) missing[i]);
                }
            }
        }

        metaStr = (String) metadata.get("valid_range");
        // attr name not supplied, so try the convention default
        if (metaStr == null) {
            metaStr = "valid_range";
        }

        valid_range = getAttributeAsDoubleArray(array_name, metaStr);
        if (valid_range != null) {
            if (!(valid_range[0] == valid_range[1])) { // don't allow this. Use default if equal
                valid_low = valid_range[0];
                valid_high = valid_range[1];
            }

            if (valid_range[0] > valid_range[1]) {
                valid_low = valid_range[1];
                valid_high = valid_range[0];
            }
        } else {
            metaStr = (String) metadata.get("valid_low");
            if (metaStr == null) { // attr name not supplied, so try the convention default
                metaStr = "valid_min";
            }
            double[] dblA = getAttributeAsDoubleArray(array_name, metaStr);
            if (dblA != null) {
                valid_low = dblA[0];
            }

            metaStr = (String) metadata.get("valid_high");
            if (metaStr == null) { // attr name not supplied, so try the convention default
                metaStr = "valid_max";
            }
            dblA = getAttributeAsDoubleArray(array_name, metaStr);
            if (dblA != null) {
                valid_high = dblA[0];
            }
        }

        if (rangeCheckBeforeScaling) {
            if (unsigned) {
                if (valid_low != -Double.MAX_VALUE) {
                    valid_low = (double) Util.unsignedShortToInt((short) valid_low);
                }
                if (valid_high != Double.MAX_VALUE) {
                    valid_high = (double) Util.unsignedShortToInt((short) valid_high);
                }
            }
        }

        String str = (String) metadata.get("multiScaleDimensionIndex");
        hasMultiDimensionScale = (str != null);
        multiScaleDimensionIndex = (str != null) ? Integer.parseInt(str) : 0;
    }

    void getFlags() {
        if (metadata.get("unpack") != null) {
            unpack = true;
        }

        if (metadata.get("unsigned") != null) {
            unsigned = true;
        }

        if (metadata.get("divideByScale") != null) {
            divideByScale = true;
        }

        if (metadata.get("range_check_after_scaling") != null) {
            String s = (String) metadata.get("range_check_after_scaling");
            //logger.debug("range_check_after_scaling: " + s);
            rangeCheckBeforeScaling = false;
        }

        String str = (String) metadata.get("multiScaleDimensionIndex");
        hasMultiDimensionScale = (str != null);
        multiScaleDimensionIndex = (str != null) ? Integer.parseInt(str) : 0;
    }

    void getScaleOffset() throws Exception {
        String array_name = (String) metadata.get("array_name");
        String metaStr = (String) metadata.get("scale_name");
        if (metaStr == null) {
            metaStr = "scale_factor";
        }
        scale = getAttributeAsFloatArray(array_name, metaStr);

        if (divideByScale && scale != null) {
            for (int k = 0; k < scale.length; k++) scale[k] = 1f / scale[k];
        }

        metaStr = (String) metadata.get("offset_name");
        if (metaStr == null) {
            metaStr = "add_offset";
        }
        offset = getAttributeAsFloatArray(array_name, metaStr);

        if (scale != null) {
            scaleOffsetLen = scale.length;

            if (offset != null) {
                if (scale.length != offset.length) {
                    throw new Exception("RangeProcessor: scale and offset array lengths must be equal");
                }
            } else {
                offset = new float[scaleOffsetLen];
                for (int i = 0; i < offset.length; i++) offset[i] = 0f;
            }

        }
    }

    void getValidRange(String array_name) throws Exception {
        String metaStr = (String) metadata.get("valid_range");
        // attr name not supplied, so try the convention default
        if (metaStr == null) {
            metaStr = "valid_range";
        }

        valid_range = getAttributeAsDoubleArray(array_name, metaStr);
        if (valid_range != null) {

            valid_low = valid_range[0];
            valid_high = valid_range[1];

            if (valid_range[0] > valid_range[1]) {
                valid_low = valid_range[1];
                valid_high = valid_range[0];
            }
        } else {
            metaStr = (String) metadata.get("valid_low");
            if (metaStr == null) { // attr name not supplied, so try the convention default
                metaStr = "valid_min";
            }
            double[] dblA = getAttributeAsDoubleArray(array_name, metaStr);
            if (dblA != null) {
                valid_low = dblA[0];
            }

            metaStr = (String) metadata.get("valid_high");
            if (metaStr == null) { // attr name not supplied, so try the convention default
                metaStr = "valid_max";
            }
            dblA = getAttributeAsDoubleArray(array_name, metaStr);
            if (dblA != null) {
                valid_high = dblA[0];
            }
        }
    }

    void getMissing(String array_name) throws Exception {
        String metaStr = (String) metadata.get("fill_value_name");
        if (metaStr == null) {
            metaStr = "_FillValue"; // Convention name
        }
        missing = getAttributeAsDoubleArray(array_name, metaStr);

        // if we are working with unsigned data, need to convert missing vals to unsigned too
        if (unsigned) {
            if (missing != null) {
                for (int i = 0; i < missing.length; i++) {
                    missing[i] = (float) Util.unsignedShortToInt((short) missing[i]);
                }
            }
        }
    }

    public float[] getAttributeAsFloatArray(String arrayName, String attrName)
            throws Exception {
        if (attrName == null) {
            return null;
        }

        float[] fltArray = null;
        HDFArray arrayAttr = null;
        try {
            arrayAttr = reader.getArrayAttribute(arrayName, attrName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (arrayAttr != null) {

            if (arrayAttr.getType().equals(Float.TYPE)) {
                float[] attr = (float[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++) fltArray[k] = attr[k];
            } else if (arrayAttr.getType().equals(Short.TYPE)) {
                short[] attr = (short[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++) fltArray[k] = (float) attr[k];
            } else if (arrayAttr.getType().equals(Integer.TYPE)) {
                int[] attr = (int[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++) fltArray[k] = (float) attr[k];
            } else if (arrayAttr.getType().equals(Double.TYPE)) {
                double[] attr = (double[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++) fltArray[k] = (float) attr[k];
            } else if (arrayAttr.getType().equals(Byte.TYPE)) {
                byte[] attr = (byte[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++) fltArray[k] = (float) attr[k];
            } else if (arrayAttr.getType().equals(String.class)) {
                String[] attr = (String[]) arrayAttr.getArray();
                StringTokenizer stok = new StringTokenizer(attr[0], ", ");
                int num = stok.countTokens();
                fltArray = new float[num];
                for (int k = 0; k < num; k++) {
                    String str = stok.nextToken();
                    fltArray[k] = Float.valueOf(str);
                }
            }


        }

        return fltArray;
    }

    public double[] getAttributeAsDoubleArray(String arrayName, String attrName)
            throws Exception {
        if (attrName == null) {
            return null;
        }

        double[] dblArray = null;
        HDFArray arrayAttr = null;
        try {
            arrayAttr = reader.getArrayAttribute(arrayName, attrName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (arrayAttr != null) {

            if (arrayAttr.getType().equals(Float.TYPE)) {
                float[] attr = (float[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++) dblArray[k] = attr[k];
            } else if (arrayAttr.getType().equals(Short.TYPE)) {
                short[] attr = (short[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++) dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(Integer.TYPE)) {
                int[] attr = (int[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++) dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(Double.TYPE)) {
                double[] attr = (double[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++) dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(Byte.TYPE)) {
                byte[] attr = (byte[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++) dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(String.class)) {
                String[] attr = (String[]) arrayAttr.getArray();
                StringTokenizer stok = new StringTokenizer(attr[0], ", ");
                int num = stok.countTokens();
                dblArray = new double[num];
                for (int k = 0; k < num; k++) {
                    String str = stok.nextToken();
                    dblArray[k] = Double.valueOf(str);
                }
            }
        }

        return dblArray;
    }

    /**
     * Process a range of data from a byte array
     *
     * @param values
     * @param subset
     * @return
     */

    public float[] processRange(byte[] values, HashMap subset) {

        int multiScaleDimLen = 1;

        if (subset != null) {
            if (subset.get(multiScaleDimName) != null) {
                double[] coords = (double[]) subset.get(multiScaleDimName);
                soIndex = (int) coords[0];
                multiScaleDimLen = (int) (coords[1] - coords[0] + 1.0);
            }
        }

        float[] new_values = new float[values.length];


        float val = 0f;
        int i = 0;
        boolean isMissing = false;

        for (int k = 0; k < values.length; k++) {

            val = (float) values[k];
            if (unsigned) {
                i = Util.unsignedByteToInt(values[k]);
                val = (float) i;
            }

            // first, check the (possibly multiple) missing values
            isMissing = false;
            if (missing != null) {
                for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                    if (val == missing[mvIdx]) {
                        isMissing = true;
                        break;
                    }
                }
            }

            if (isMissing) {
                new_values[k] = Float.NaN;
                continue;
            }

            if (rangeCheckBeforeScaling) {
                if ((val < valid_low) || (val > valid_high)) {
                    new_values[k] = Float.NaN;
                    continue;
                }
            }

            if (scale != null) {
                if (unpack) {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = (scale[soIndex] * val) + offset[soIndex];
                    } else {
                        new_values[k] = (scale[soIndex + k] * val) + offset[soIndex + k];
                    }
                } else {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = scale[soIndex] * (val - offset[soIndex]);
                    } else {
                        new_values[k] = scale[soIndex + k] * (val - offset[soIndex + k]);
                    }
                }
            } else {
                new_values[k] = val;
            }

            // do valid range check AFTER scaling?
            if (!rangeCheckBeforeScaling) {
                isMissing = false;
                if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
                    new_values[k] = Float.NaN;
                    isMissing = true;
                }
                if (missing != null && !isMissing) {
                    for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                        if (new_values[k] == missing[mvIdx]) {
                            new_values[k] = Float.NaN;
                            break;
                        }
                    }
                }
            }
        }
        return new_values;
    }

    /**
     * Process a range of data from a short array
     *
     * @param values
     * @param subset
     * @return
     */

    public float[] processRange(short[] values, HashMap subset) {

        int multiScaleDimLen = 1;

        if (subset != null) {
            if (subset.get(multiScaleDimName) != null) {
                double[] coords = (double[]) subset.get(multiScaleDimName);
                soIndex = (int) coords[0];
                multiScaleDimLen = (int) (coords[1] - coords[0] + 1.0);
            }
        }

        float[] new_values = new float[values.length];


        float val = 0f;
        int i = 0;
        boolean isMissing = false;

        for (int k = 0; k < values.length; k++) {

            val = (float) values[k];
            if (unsigned) {
                i = Util.unsignedShortToInt(values[k]);
                val = (float) i;
            }

            // first, check the (possibly multiple) missing values
            isMissing = false;
            if (missing != null) {
                for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                    if (val == missing[mvIdx]) {
                        isMissing = true;
                        break;
                    }
                }
            }

            if (isMissing) {
                new_values[k] = Float.NaN;
                continue;
            }

            if (rangeCheckBeforeScaling) {
                if ((val < valid_low) || (val > valid_high)) {
                    new_values[k] = Float.NaN;
                    continue;
                }
            }

            if (scale != null && scale.length > 1) {
                if (unpack) {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = (scale[soIndex] * val) + offset[soIndex];
                    } else {
                        new_values[k] = (scale[soIndex + k] * val) + offset[soIndex + k];
                    }
                } else {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = scale[soIndex] * (val - offset[soIndex]);
                    } else {

                        new_values[k] = scale[soIndex + k] * (val - offset[soIndex + k]);
                    }
                }
            } else if (scale != null && scale.length == 1) { // multiChannel dimension, but one scale/offset for all channels
                if (unpack) {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = (scale[0] * val) + offset[0];
                    } else {
                        new_values[k] = (scale[0] * val) + offset[0];
                    }
                } else {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = scale[0] * (val - offset[0]);
                    } else {

                        new_values[k] = scale[0] * (val - offset[0]);
                    }
                }
            } else {
                new_values[k] = val;
            }

            // do valid range check AFTER scaling?
            if (!rangeCheckBeforeScaling) {
                isMissing = false;
                if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
                    new_values[k] = Float.NaN;
                    isMissing = true;
                }
                if (missing != null && !isMissing) {
                    for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                        if (new_values[k] == missing[mvIdx]) {
                            new_values[k] = Float.NaN;
                            break;
                        }
                    }
                }
            }

        }
        return new_values;
    }

    /**
     * Process a range of data from a int array
     * An extending class should implement this
     *
     * @param values
     * @param subset
     * @return
     */

    public float[] processRange(int[] values, HashMap subset) {
        int multiScaleDimLen = 1;

        if (subset != null) {
            if (subset.get(multiScaleDimName) != null) {
                double[] coords = (double[]) subset.get(multiScaleDimName);
                soIndex = (int) coords[0];
                multiScaleDimLen = (int) (coords[1] - coords[0] + 1.0);
            }
        }

        float[] new_values = new float[values.length];
        float val = 0f;
        int i = 0;
        boolean isMissing = false;

        for (int k = 0; k < values.length; k++) {

            val = (float) values[k];

            // first, check the (possibly multiple) missing values
            isMissing = false;
            if (missing != null) {
                for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                    if (val == missing[mvIdx]) {
                        isMissing = true;
                        break;
                    }
                }
            }

            if (isMissing) {
                new_values[k] = Float.NaN;
                continue;
            }

            if (rangeCheckBeforeScaling) {
                if ((val < valid_low) || (val > valid_high)) {
                    new_values[k] = Float.NaN;
                    continue;
                }
            }

            if (scale != null && scale.length > 1) {
                if (unpack) {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = (scale[soIndex] * val) + offset[soIndex];
                    } else {
                        new_values[k] = (scale[soIndex + k] * val) + offset[soIndex + k];
                    }
                } else {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = scale[soIndex] * (val - offset[soIndex]);
                    } else {

                        new_values[k] = scale[soIndex + k] * (val - offset[soIndex + k]);
                    }
                }
            } else if (scale != null && scale.length == 1) { // multiScale dimension, but one scale/offset for all channels
                if (unpack) {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = (scale[0] * val) + offset[0];
                    } else {
                        new_values[k] = (scale[0] * val) + offset[0];
                    }
                } else {
                    if (multiScaleDimLen == 1) {
                        new_values[k] = scale[0] * (val - offset[0]);
                    } else {

                        new_values[k] = scale[0] * (val - offset[0]);
                    }
                }
            } else {
                new_values[k] = val;
            }

            // do valid range check AFTER scaling?
            if (!rangeCheckBeforeScaling) {
                if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
                    new_values[k] = Float.NaN;
                }
            }

        }
        return new_values;
    }

    /**
     * Process a range of data from a float array
     *
     * @param values
     * @param subset
     * @return
     */

    public float[] processRange(float[] values, HashMap subset) {

        float[] new_values = null;

        if ((missing != null) || (valid_range != null)) {
            new_values = new float[values.length];
        } else {
            return values;
        }

        float val;

        for (int k = 0; k < values.length; k++) {
            val = values[k];
            new_values[k] = val;

            // first, check the (possibly multiple) missing values
            if (missing != null) {
                for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                    if (val == missing[mvIdx]) {
                        new_values[k] = Float.NaN;
                        break;
                    }
                }
            }

            if ((valid_range != null) && ((val < valid_low) || (val > valid_high))) {
                new_values[k] = Float.NaN;
            }

        }

        return new_values;
    }

    /**
     * Process a range of data from a double array
     *
     * @param values
     * @param subset
     * @return
     */

    public double[] processRange(double[] values, HashMap subset) {

        double[] new_values = null;

        if ((missing != null) || (valid_range != null)) {
            new_values = new double[values.length];
        } else {
            return values;
        }

        double val;

        for (int k = 0; k < values.length; k++) {
            val = values[k];
            new_values[k] = val;

            // first, check the (possibly multiple) missing values
            if (missing != null) {
                for (int mvIdx = 0; mvIdx < missing.length; mvIdx++) {
                    if (val == missing[mvIdx]) {
                        new_values[k] = Float.NaN;
                        break;
                    }
                }
            }

            if ((valid_range != null) && ((val < valid_low) || (val > valid_high))) {
                new_values[k] = Double.NaN;
            }
        }

        return new_values;
    }

    public void setMultiScaleDimName(String multiScaleDimName) {
        this.multiScaleDimName = multiScaleDimName;
    }

    public int getMultiScaleDimensionIndex() {
        return multiScaleDimensionIndex;
    }

    public boolean hasMultiDimensionScale() {
        return hasMultiDimensionScale;
    }

    public void setHasMultiDimensionScale(boolean yesno) {
        hasMultiDimensionScale = yesno;
    }

    public void setMultiScaleIndex(int idx) {
        this.soIndex = idx;
    }

}



class IASI_NCDF_RangeProcessor extends RangeProcessor {
    float[] scaleFactors;

    public IASI_NCDF_RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
        super();
        try {
            scaleFactors = reader.getFloatArray("scale_factor", new int[]{0}, new int[]{8700}, new int[]{1});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float[] processRange(short[] values, HashMap subset) {
        int channelIndex = (int) ((double[]) subset.get(SpectrumAdapter.channelIndex_name))[0];
        float[] new_values = new float[values.length];
        for (int k = 0; k < new_values.length; k++) {
            new_values[k] = (100000f * values[k]) / (scaleFactors[channelIndex]);
        }

        double[] track_coords = (double[]) subset.get(SwathAdapter.track_name);
        double[] xtrack_coords = (double[]) subset.get(SwathAdapter.xtrack_name);

        int numElems = ((int) (xtrack_coords[1] - xtrack_coords[0]) + 1);
        int numLines = ((int) (track_coords[1] - track_coords[0]) + 1);

        new_values = IASI_L1C_Utility.psuedoScanReorder2(new_values, numElems / 2, numLines * 2);


        return new_values;
    }

}

class CrIS_RangeProcessor extends RangeProcessor {
    RangeProcessor rangeProcessor = null;
    public static int[][] ifov_order = new int[][]{new int[]{2, 0}, new int[]{2, 1}, new int[]{2, 2},
            new int[]{1, 0}, new int[]{1, 1}, new int[]{1, 2},
            new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2}};

    public CrIS_RangeProcessor() throws Exception {
        super();
    }

    public CrIS_RangeProcessor(RangeProcessor rangeProcessor) throws Exception {
        super();
        this.rangeProcessor = rangeProcessor;
    }

    public float[] processRange(float[] values, HashMap subset) {
        if (rangeProcessor != null) {
            values = rangeProcessor.processRange(values, null);
        }

        double[] track_coords = (double[]) subset.get(SwathAdapter.track_name);
        double[] xtrack_coords = (double[]) subset.get(SwathAdapter.xtrack_name);

        int numElems = ((int) (xtrack_coords[1] - xtrack_coords[0]) + 1);
        int numLines = ((int) (track_coords[1] - track_coords[0]) + 1);

        values = psuedoScanReorder(values, numElems * 3, numLines * 3);

        return values;
    }
    public static float[] psuedoScanReorder(float[] values, int numElems, int numLines) {
        float[] new_values = new float[values.length];
        for (int j = 0; j < numLines / 3; j++) { //- loop over EFOVs or FORs
            for (int i = 0; i < numElems / 3; i++) {
                int i2 = i * 3;
                int j2 = j * 3;
                for (int jj = 0; jj < 3; jj++) {  //- loop over IFOVs
                    for (int ii = 0; ii < 3; ii++) {
                        int k = jj * 3 + ii;
                        int idx_ma = j * (numElems / 3 * 9) + i * 9 + k;
                        int idx_a = (j2 + ifov_order[k][0]) * numElems + i2 + ifov_order[k][1];  // idx_a: aligned
                        new_values[idx_a] = values[idx_ma];
                    }
                }
            }
        }
        return new_values;
    }
}

class IASI_L1C_AAPP_RangeProcessor extends RangeProcessor {
    RangeProcessor rangeProcessor = null;

    public IASI_L1C_AAPP_RangeProcessor() throws Exception {
        super();
    }

    public IASI_L1C_AAPP_RangeProcessor(RangeProcessor rangeProcessor) throws Exception {
        super();
        this.rangeProcessor = rangeProcessor;
    }

    public float[] processRange(float[] fvalues, HashMap subset) {

        double[] track_coords = (double[]) subset.get(SwathAdapter.track_name);
        double[] xtrack_coords = (double[]) subset.get(SwathAdapter.xtrack_name);

        int numElems = ((int) (xtrack_coords[1] - xtrack_coords[0]) + 1);
        int numLines = ((int) (track_coords[1] - track_coords[0]) + 1);

        fvalues = IASI_L1C_Utility.psuedoScanReorder2(fvalues, numElems * 2, numLines * 2);

        return fvalues;
    }

}

class MODIS_CloudMask extends RangeProcessor {

    public MODIS_CloudMask() throws Exception {
        super();
    }

    public float[] processRange(byte[] values, HashMap subset) {
        int determined = 1;
        float[] new_values = new float[values.length];

        for (int k = 0; k < values.length; k++) {
            int val = (int) values[k];
            val = val & 7;
            determined = val & 1;
            //new_values[k] = (float) ((val >> 1) + determined);
            new_values[k] = (float) (val >> 1);
        }

        return new_values;
    }

}



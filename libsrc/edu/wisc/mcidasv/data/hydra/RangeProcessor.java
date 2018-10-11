/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.data.QualityFlag;
import visad.util.Util;

public class RangeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RangeProcessor.class);

    static RangeProcessor createRangeProcessor(MultiDimensionReader reader,
            Map<String, Object> metadata) throws Exception {
        if (reader instanceof GranuleAggregation) {
            return new AggregationRangeProcessor((GranuleAggregation) reader, metadata);
        }

        if (metadata.get("scale_name") == null) {
            String product_name = (String) metadata.get(SwathAdapter.product_name);
            if (Objects.equals(product_name, "IASI_L1C_xxx")) {
                return new IASI_RangeProcessor();
            }
            return null;
        } else {
            String product_name = (String) metadata.get(ProfileAlongTrack.product_name);
            if (Objects.equals(product_name, "2B-GEOPROF")) {
                return new CloudSat_2B_GEOPROF_RangeProcessor(reader, metadata);
            } else {
                return new RangeProcessor(reader, metadata);
            }
        }
    }

    MultiDimensionReader reader;
    Map<String, Object> metadata;

    float[] scale = null;
    float[] offset = null;
    double[] missing = null;
    double[] valid_range = null;
    double valid_low = -Double.MAX_VALUE;
    double valid_high = Double.MAX_VALUE;

    boolean unpack = false;
    boolean unsigned = false;
    boolean rangeCheckBeforeScaling = true;

    int scaleOffsetLen = 1;

    String multiScaleDimName = SpectrumAdapter.channelIndex_name;
    boolean hasMultiDimensionScale = false;

    int multiScaleDimensionIndex = 0;

    int soIndex = 0;

    public RangeProcessor() {
    }

    public RangeProcessor(float scale, float offset, float valid_low, float valid_high,
            float missing) {
        this.scale = new float[] { scale };
        this.offset = new float[] { offset };
        this.missing = new double[] { missing };
        this.valid_low = valid_low;
        this.valid_high = valid_high;
    }

    public RangeProcessor(MultiDimensionReader reader, Map<String, Object> metadata,
            String multiScaleDimName) throws Exception {
        this(reader, metadata);
        this.multiScaleDimName = multiScaleDimName;
    }

    public RangeProcessor(MultiDimensionReader reader, Map<String, Object> metadata)
            throws Exception {
        this.reader = reader;
        this.metadata = metadata;

        if (metadata.get("unpack") != null) {
            unpack = true;
        }

        if (metadata.get("unsigned") != null) {
            unsigned = true;
        }

        if (metadata.get("range_check_after_scaling") != null) {
            String s = (String) metadata.get("range_check_after_scaling");
            logger.debug("range_check_after_scaling: " + s);
            rangeCheckBeforeScaling = false;
        }

        String array_name = (String) metadata.get("array_name");

        scale = getAttributeAsFloatArray(array_name, (String) metadata.get("scale_name"));

        offset = getAttributeAsFloatArray(array_name, (String) metadata.get("offset_name"));

        if (scale != null) {
            scaleOffsetLen = scale.length;

            if (offset != null) {
                if (scale.length != offset.length) {
                    throw new Exception(
                            "RangeProcessor: scale and offset array lengths must be equal");
                }
            } else {
                offset = new float[scaleOffsetLen];
                for (int i = 0; i < offset.length; i++)
                    offset[i] = 0f;
            }

        }

        missing = getAttributeAsDoubleArray(array_name, (String) metadata.get("fill_value_name"));

        // if we are working with unsigned data, need to convert missing vals to
        // unsigned too
        if (unsigned) {
            if (missing != null) {
                for (int i = 0; i < missing.length; i++) {
                    missing[i] = (float) Util.unsignedShortToInt((short) missing[i]);
                }
            }
        }

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
            if (metaStr == null) { // attr name not supplied, so try the
                                   // convention default
                metaStr = "valid_min";
            }
            double[] dblA = getAttributeAsDoubleArray(array_name, metaStr);
            if (dblA != null) {
                valid_low = dblA[0];
            }

            metaStr = (String) metadata.get("valid_high");
            if (metaStr == null) { // attr name not supplied, so try the
                                   // convention default
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

    public float[] getAttributeAsFloatArray(String arrayName, String attrName) throws Exception {
        float[] fltArray = null;
        HDFArray arrayAttr = reader.getArrayAttribute(arrayName, attrName);

        if (arrayAttr != null) {

            if (arrayAttr.getType().equals(Float.TYPE)) {
                float[] attr = (float[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++)
                    fltArray[k] = attr[k];
            } else if (arrayAttr.getType().equals(Short.TYPE)) {
                short[] attr = (short[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++)
                    fltArray[k] = (float) attr[k];
            } else if (arrayAttr.getType().equals(Integer.TYPE)) {
                int[] attr = (int[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++)
                    fltArray[k] = (float) attr[k];
            } else if (arrayAttr.getType().equals(Double.TYPE)) {
                double[] attr = (double[]) arrayAttr.getArray();
                fltArray = new float[attr.length];
                for (int k = 0; k < attr.length; k++)
                    fltArray[k] = (float) attr[k];
            }

        }

        return fltArray;
    }

    public double[] getAttributeAsDoubleArray(String arrayName, String attrName) throws Exception {
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
                for (int k = 0; k < attr.length; k++)
                    dblArray[k] = attr[k];
            } else if (arrayAttr.getType().equals(Short.TYPE)) {
                short[] attr = (short[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++) {
                    if (unsigned) {
                        dblArray[k] = (double) Util.unsignedShortToInt((short) attr[k]);
                    } else {
                        dblArray[k] = (double) attr[k];
                    }
                }
            } else if (arrayAttr.getType().equals(Integer.TYPE)) {
                int[] attr = (int[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++)
                    dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(Double.TYPE)) {
                double[] attr = (double[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++)
                    dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(Byte.TYPE)) {
                byte[] attr = (byte[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++)
                    dblArray[k] = (double) attr[k];
            } else if (arrayAttr.getType().equals(String.class)) {
                String[] attr = (String[]) arrayAttr.getArray();
                dblArray = new double[attr.length];
                for (int k = 0; k < attr.length; k++)
                    dblArray[k] = Double.valueOf(attr[0]);
            }
        }

        return dblArray;
    }

    /**
     * Process a range of data from an array of {@code byte} values where bytes
     * are packed bit or multi-bit fields of quality flags. Based on info in a
     * {@link QualityFlag} object passed in, we extract and return values for
     * that flag.
     * 
     * @param values
     *            Input byte values. Cannot be {@code null}.
     * @param subset
     *            Optional subset.
     * @param qf
     *            Quality flag.
     *
     * @return Processed range.
     */

    public float[] processRangeQualityFlag(byte[] values, Map<String, double[]> subset, QualityFlag qf) {

        if (subset != null) {
            if (subset.get(multiScaleDimName) != null) {
                soIndex = (int) (subset.get(multiScaleDimName))[0];
            }
        }

        float[] newValues = new float[values.length];

        float val = 0f;
        int bitOffset = qf.getBitOffset();
        int divisor = -1;

        // map bit offset to a divisor
        switch (bitOffset) {
            case 1:
                divisor = 2;
                break;
            case 2:
                divisor = 4;
                break;
            case 3:
                divisor = 8;
                break;
            case 4:
                divisor = 16;
                break;
            case 5:
                divisor = 32;
                break;
            case 6:
                divisor = 64;
                break;
            case 7:
                divisor = 128;
                break;
            default:
                divisor = 1;
                break;
        }

        // now map bit width to a mask
        int numBits = qf.getNumBits();
        int mask = -1;
        switch (numBits) {
            case 1:
                mask = (int) 0x00000001;
                break;
            case 2:
                mask = (int) 0x00000003;
                break;
            case 3:
                mask = (int) 0x00000007;
                break;
            case 4:
                mask = (int) 0x0000000F;
                break;
            case 5:
                mask = (int) 0x0000001F;
                break;
            case 6:
                mask = (int) 0x0000003F;
                break;
            case 7:
                mask = (int) 0x0000007F;
                break;
            default:
                mask = (int) 0x00000000;
                break;
        }

        int i = 0;
        for (int k = 0; k < values.length; k++) {
            val = (float) values[k];
            i = Util.unsignedByteToInt(values[k]);
            val = (float) ((i / divisor) & mask);
            newValues[k] = val;
        }

        return newValues;
    }

    /**
     * Process a range of data from an array of {@code byte} values.
     * 
     * @param values
     *            Input {@code byte} values. Cannot be {@code null}.
     * @param subset
     *            Optional subset.
     *
     * @return Processed range.
     */
    
    public float[] processRange(byte[] values, Map<String, double[]> subset) {

        int multiScaleDimLen = 1;

        if (subset != null) {
            if (subset.get(multiScaleDimName) != null) {
                double[] coords = subset.get(multiScaleDimName);
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
                if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
                    new_values[k] = Float.NaN;
                }
            }
        }
        return new_values;
    }

    /**
     * Process a range of data from an array of {@code short} values.
     * 
     * @param values
     *            Input {@code short} values. Cannot be {@code null}.
     * @param subset
     *            Optional subset.
     *
     * @return Processed range.
     */
    
    public float[] processRange(short[] values, Map<String, double[]> subset) {

        int multiScaleDimLen = 1;

        if (subset != null) {
            if (subset.get(multiScaleDimName) != null) {
                double[] coords = subset.get(multiScaleDimName);
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
                if ((new_values[k] < valid_low) || (new_values[k] > valid_high)) {
                    new_values[k] = Float.NaN;
                }
            }

        }
        return new_values;
    }

    /**
     * Process a range of data from an array of {@code float} values.
     * 
     * @param values
     *            Input {@code float} values. Cannot be {@code null}.
     * @param subset
     *            Optional subset.
     *
     * @return Processed array.
     */
    
    public float[] processRange(float[] values, Map<String, double[]> subset) {

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
     * Process a range of data from an array of {@code double} value.
     * 
     * @param values
     *            Input {@code double} values. Cannot be {@code null}.
     * @param subset
     *            Optional subset.
     *
     * @return Processed array.
     */
    
    public double[] processRange(double[] values, Map<String, double[]> subset) {

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

    /**
     * Should be generalized. For now works for short to float conversions
     * 
     * @param values
     *            the set of input values to map
     * @param lut
     *            the lookup table for direct mapping input to output values
     * @return output array as primitive floats
     */

    public Object processRangeApplyLUT(short[] values, float[] lut) {

        float[] newValues = new float[values.length];

        int lutLen = lut.length;

        for (int i = 0; i < values.length; i++) {
            short tmpVal = values[i];
            if (tmpVal > 0) {
                if (tmpVal < lutLen) {
                    newValues[i] = lut[tmpVal];
                } else {
                    newValues[i] = Float.NaN;
                }
            } else {
                newValues[i] = Float.NaN;
            }
        }

        return newValues;
    }

}

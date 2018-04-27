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

import visad.*;

public class HistogramField {

    Linear2DSet histSet;
    Linear1DSet set0;
    Linear1DSet set1;
    int len0;
    int len1;
    int[] count;
    int[][] indexes;
    FlatField field_0;
    FlatField field_1;
    FlatField mask_field;
    float[][] maskRange;
    Class rangeType;
    byte[][] mask = new byte[3][];
    byte[] order = new byte[3];

    public FlatField scatterDensityField;

    public HistogramField(FlatField field_0, FlatField field_1,
            FlatField mask_field,
            int n_bins, int bin_size)
            throws Exception {
        this.field_0 = field_0;
        this.field_1 = field_1;
        this.mask_field = mask_field;
        maskRange = mask_field.getFloats(false);

        mask[0] = new byte[maskRange[0].length];
        mask[1] = new byte[maskRange[0].length];
        mask[2] = new byte[maskRange[0].length];
        java.util.Arrays.fill(mask[0], Byte.MAX_VALUE);
        java.util.Arrays.fill(mask[1], Byte.MAX_VALUE);
        java.util.Arrays.fill(mask[2], Byte.MAX_VALUE);
        java.util.Arrays.fill(order, Byte.MAX_VALUE);

        Set[] rangeSets = field_0.getRangeSets();
        Set rngSet = rangeSets[0];

        if (rngSet instanceof FloatSet) {
            rangeType = Float.TYPE;
        } else if (rngSet instanceof DoubleSet) {
            rangeType = Double.TYPE;
        } else if (rngSet instanceof IntegerSet) {
            rangeType = Integer.TYPE;
        }

        double[] minmax_0 = {Double.MAX_VALUE, -Double.MAX_VALUE};
        double[] minmax_1 = {Double.MAX_VALUE, -Double.MAX_VALUE};


        if (rangeType == Integer.TYPE) {
          //Ghansham: Dont do any allocation here. Do based on the individual ranges of fieldX and fieldY respectively
        } else {
            indexes = new int[n_bins * n_bins][1];
            count = new int[n_bins * n_bins];
        }


        double[][] val = new double[2][1];
        int[] histIdx = null;

        if (rangeType == Double.TYPE) {
            double[][] vals_0 = field_0.getValues(false);
            double[][] vals_1 = field_1.getValues(false);
            int n_samples = vals_0[0].length;
            for (int k = 0; k < n_samples; k++) {
                double v0 = vals_0[0][k];
                if (v0 < minmax_0[0]) {
                    minmax_0[0] = v0;
                } else if (v0 > minmax_0[1]) {
                    minmax_0[1] = v0;
                }

                double v1 = vals_1[0][k];
                if (v1 < minmax_1[0]) {
                    minmax_1[0] = v1;
                } else if (v1 > minmax_1[1]) {
                    minmax_1[1] = v1;
                }
            }

            histSet = new Linear2DSet(minmax_0[0], minmax_0[1], n_bins,
                    minmax_1[0], minmax_1[1], n_bins);

            for (int k = 0; k < n_samples; k++) {
                val[0][0] = vals_0[0][k];
                val[1][0] = vals_1[0][k];
                histIdx = histSet.doubleToIndex(val);
                if (histIdx[0] >= 0) {
                    int len = indexes[histIdx[0]].length;
                    if (count[histIdx[0]] > len - 1) { //-grow array
                        int[] tmp = new int[len + bin_size];
                        System.arraycopy(indexes[histIdx[0]], 0, tmp, 0, len);
                        indexes[histIdx[0]] = tmp;
                    }
                    indexes[histIdx[0]][count[histIdx[0]]++] = k;
                }
            }
        } else if (rangeType == Float.TYPE) {
            float[][] vals_0 = field_0.getFloats(false);
            float[][] vals_1 = field_1.getFloats(false);
            int n_samples = vals_0[0].length;
            for (int k = 0; k < n_samples; k++) {
                double v0 = vals_0[0][k];
                if (v0 < minmax_0[0]) {
                    minmax_0[0] = v0;
                } else if (v0 > minmax_0[1]) {
                    minmax_0[1] = v0;
                }
                double v1 = vals_1[0][k];
                if (v1 < minmax_1[0]) {
                    minmax_1[0] = v1;
                } else if (v1 > minmax_1[1]) {
                    minmax_1[1] = v1;
                }

            }
            histSet = new Linear2DSet(minmax_0[0], minmax_0[1], n_bins, minmax_1[0], minmax_1[1], n_bins);
            for (int k = 0; k < n_samples; k++) {
                val[0][0] = vals_0[0][k];
                val[1][0] = vals_1[0][k];
                histIdx = histSet.doubleToIndex(val);
                if (histIdx[0] >= 0) {
                    int len = indexes[histIdx[0]].length;
                    if (count[histIdx[0]] > len - 1) { //-grow array

                        int[] tmp = new int[len + bin_size];
                        System.arraycopy(indexes[histIdx[0]], 0, tmp, 0, len);
                        indexes[histIdx[0]] = tmp;
                    }
                    indexes[histIdx[0]][count[histIdx[0]]++] = k;
                }
            }
        } else if (rangeType == Integer.TYPE) {
            float[][] vals_0 = field_0.getFloats(false);
            float[][] vals_1 = field_1.getFloats(false);
            int n_samples = vals_0[0].length;
            for (int k = 0; k < n_samples; k++) {
                double v0 = vals_0[0][k];
                if (v0 < minmax_0[0]) {
                    minmax_0[0] = v0;
                } else if (v0 > minmax_0[1]) {
                    minmax_0[1] = v0;
                }
                double v1 = vals_1[0][k];
                if (v1 < minmax_1[0]) {
                    minmax_1[0] = v1;
                } else if (v1 > minmax_1[1]) {
                    minmax_1[1] = v1;
                }
            }


            int startX = (int) minmax_0[0];
            int endX = (int) minmax_0[1];
            int startY = (int) minmax_1[0];
            int endY = (int) minmax_1[1];
            int lenX = endX - startX + 1;
            int lenY = endY - startY + 1;
            histSet = new Linear2DSet(startX, endX, lenX, startY, endY, lenY);



            //Ghansham:Allocate here based on length of lenghts of XField and YField
            indexes = new int[lenY * lenX][]; //Ghansham:Dont allocate here if not required.
            count = new int[lenY * lenX];

            //First calculate frequency of each grey count.
            for (int k = 0; k < n_samples; k++) {
                val[0][0] = vals_0[0][k];
                val[1][0] = vals_1[0][k];
                histIdx = histSet.doubleToIndex(val);
                if (histIdx[0] >= 0) {
                    count[histIdx[0]]++;
                }
            }

            for (int k = 0; k < n_samples; k++) {
                val[0][0] = vals_0[0][k];
                val[1][0] = vals_1[0][k];
                histIdx = histSet.doubleToIndex(val);
                if (histIdx[0] >= 0) {
                    if (indexes[histIdx[0]] == null) { //Tricky stuff is here: encountering a particular grey count first time.
                        indexes[histIdx[0]] = new int[count[histIdx[0]]]; //Allocating the values based on the frequency of this grey count (calculate earlier). No extra allocation at all
                        count[histIdx[0]] = 0;  //Resetting the frequency to 0.
                    }
                    indexes[histIdx[0]][count[histIdx[0]]++] = k;
                }
            }
        }


        set0 = histSet.getLinear1DComponent(0);
        set1 = histSet.getLinear1DComponent(1);
        len0 = set0.getLength();
        len1 = set1.getLength();


        Linear2DSet dSet = (Linear2DSet) histSet.changeMathType(new RealTupleType(RealType.XAxis, RealType.YAxis));
        scatterDensityField = new FlatField(
            new FunctionType(((SetType)dSet.getType()).getDomain(), RealType.getRealType("ScatterDensity")), dSet);
        float[][] fltCount = new float[1][count.length];
        for (int i=0; i<count.length; i++) { 
            fltCount[0][i] = (float) count[i];
            if (count[i] == 0) {
               fltCount[0][i] = Float.NaN;
            }
            else {
               fltCount[0][i] = (float) java.lang.Math.log((double)fltCount[0][i]);
            }
        }
        scatterDensityField.setSamples(fltCount);
    }

    public FlatField getScatterDensityField() {
        return scatterDensityField;
    }

    public void markMaskFieldByRange(double[] lowhi_0, double[] lowhi_1, float maskVal)
            throws Exception {
        reorder((byte)maskVal);

        int[] hist0 = set0.doubleToIndex(new double[][] {{lowhi_0[0], lowhi_0[1]}});
        int[] hist1 = set1.doubleToIndex(new double[][] {{lowhi_1[0], lowhi_1[1]}});

        if (hist0[0] < 0) {
            if (lowhi_0[0] < lowhi_0[1]) {
                hist0[0] = 0;
            } else {
                hist0[0] = len0 - 1;
            }
        }
        if (hist0[1] < 0) {
            if (lowhi_0[0] < lowhi_0[1]) {
                hist0[1] = len0 - 1;
            } else {
                hist0[1] = 0;
            }
        }

        if (hist1[0] < 0) {
            if (lowhi_1[0] < lowhi_1[1]) {
                hist1[0] = 0;
            } else {
                hist1[0] = len1 - 1;
            }
        }
        if (hist1[1] < 0) {
            if (lowhi_1[0] < lowhi_1[1]) {
                hist1[1] = len1 - 1;
            } else {
                hist1[1] = 0;
            }
        }

        int h00, h01, h10, h11;


        h10 = hist1[1];
        h11 = hist1[0];
        if (hist1[0] < hist1[1]) {
            h10 = hist1[0];
            h11 = hist1[1];
        }

        h00 = hist0[1];
        h01 = hist0[0];
        if (hist0[0] < hist0[1]) {
            h00 = hist0[0];
            h01 = hist0[1];
        }

        for (int k = 0; k < maskRange[0].length; k++) {
            if (maskRange[0][k] == maskVal) {
                maskRange[0][k] = Float.NaN;
                mask[(byte)maskVal][k] = Byte.MAX_VALUE;
            }
        }

        int lenX = set0.getLengthX();

        for (int j = h10; j <= h11; j++) {
            int col_factor = j * lenX;
            for (int i = h00; i <= h01; i++) {
                int idx = col_factor + i;
                for (int k = 0; k < count[idx]; k++) {
                    maskRange[0][indexes[idx][k]] = maskVal;
                    mask[(byte)maskVal][indexes[idx][k]] = (byte)maskVal;
                }
            }
        }

        mask_field.setSamples(maskRange, false);
    }

    public void markMaskFieldByCurve(float[][] curve, float maskVal) throws Exception {
        reorder((byte)maskVal);
        float[][] samples0 = set0.getSamples();
        float[][] samples1 = set1.getSamples();

        boolean[][] checked = null;
        boolean[][] inside = null;


        if (rangeType == Integer.TYPE) { //Dealing with rangeSet constructed fields separately.
            float[][] vals_0 = field_0.getFloats(false);
            float[][] vals_1 = field_1.getFloats(false);
            int lenX = set0.getLength();
            int lenY = set1.getLength();
            checked = new boolean[lenX][lenY];
            inside = new boolean[lenX][lenY];
            for (int jj = 0; jj < lenX; jj++) {
                java.util.Arrays.fill(checked[jj], false);
                java.util.Arrays.fill(inside[jj], false);
            }
            for (int jj = 0; jj < lenY - 1; jj++) {
                for (int ii = 0; ii < lenX - 1; ii++) {
                    int idx = jj * lenX + ii; //Calclualting the index value in the start only.
                    if (count[idx] > 0) { //No need to do go further if the frequency of particular grey count is zero.
                        int inside_cnt = 0;
                        if (!checked[ii][jj]) {
                            float x = samples0[0][ii];
                            float y = samples1[0][jj];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii][jj] = true;
                            }
                            checked[ii][jj] = true;
                        } else if (inside[ii][jj]) {
                            inside_cnt++;
                        }

                        if (!checked[ii + 1][jj]) {
                            float x = samples0[0][ii + 1];
                            float y = samples1[0][jj];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii + 1][jj] = true;
                            }
                            checked[ii + 1][jj] = true;
                        } else if (inside[ii + 1][jj]) {
                            inside_cnt++;
                        }

                        if (!checked[ii][jj + 1]) {
                            float x = samples0[0][ii];
                            float y = samples1[0][jj + 1];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii][jj + 1] = true;
                            }
                            checked[ii][jj + 1] = true;
                        } else if (inside[ii][jj + 1]) {
                            inside_cnt++;
                        }

                        if (!checked[ii + 1][jj + 1]) {
                            float x = samples0[0][ii + 1];
                            float y = samples1[0][jj + 1];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii + 1][jj + 1] = true;
                            }
                            checked[ii + 1][jj + 1] = true;
                        } else if (inside[ii + 1][jj + 1]) {
                            inside_cnt++;
                        }

                        if (inside_cnt == 0) {
                            continue;
                        } else if (inside_cnt == 4) {
                            for (int k = 0; k < count[idx]; k++) {
                                maskRange[0][indexes[idx][k]] = maskVal;
                            }
                        } else if (inside_cnt > 0 && inside_cnt < 4) {
                            for (int k = 0; k < count[idx]; k++) {
                                float xx = vals_0[0][indexes[idx][k]];
                                float yy = vals_1[0][indexes[idx][k]];
                                if (DelaunayCustom.inside(curve, xx, yy)) {
                                    maskRange[0][indexes[idx][k]] = maskVal;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            int len = set0.getLength();
            checked = new boolean[len][len];
            inside = new boolean[len][len];
            for (int jj = 0; jj < len; jj++) {
                java.util.Arrays.fill(checked[jj], false);
                java.util.Arrays.fill(inside[jj], false);
            }
            for (int jj = 0; jj < len - 1; jj++) {
                for (int ii = 0; ii < len - 1; ii++) {
                    int idx = jj * set0.getLengthX() + ii; //Calclualting the index value in the start only.
                    if (count[idx] > 0) { //No need to do go further if the frequency of particular value is zero.
                        int inside_cnt = 0;
                        if (!checked[ii][jj]) {
                            float x = samples0[0][ii];
                            float y = samples1[0][jj];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii][jj] = true;
                            }
                            checked[ii][jj] = true;
                        } else if (inside[ii][jj]) {
                            inside_cnt++;
                        }

                        if (!checked[ii + 1][jj]) {
                            float x = samples0[0][ii + 1];
                            float y = samples1[0][jj];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii + 1][jj] = true;
                            }
                            checked[ii + 1][jj] = true;
                        } else if (inside[ii + 1][jj]) {
                            inside_cnt++;
                        }

                        if (!checked[ii][jj + 1]) {
                            float x = samples0[0][ii];
                            float y = samples1[0][jj + 1];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii][jj + 1] = true;
                            }
                            checked[ii][jj + 1] = true;
                        } else if (inside[ii][jj + 1]) {
                            inside_cnt++;
                        }

                        if (!checked[ii + 1][jj + 1]) {
                            float x = samples0[0][ii + 1];
                            float y = samples1[0][jj + 1];
                            if (DelaunayCustom.inside(curve, x, y)) {
                                inside_cnt++;
                                inside[ii + 1][jj + 1] = true;
                            }
                            checked[ii + 1][jj + 1] = true;
                        } else if (inside[ii + 1][jj + 1]) {
                            inside_cnt++;
                        }
                        if (inside_cnt == 0) {
                            continue;
                        }

                        if (rangeType == Float.TYPE) {
                            float[][] vals_0 = field_0.getFloats(false);
                            float[][] vals_1 = field_1.getFloats(false);
                            if (inside_cnt == 4) {

                                for (int k = 0; k < count[idx]; k++) {
                                    maskRange[0][indexes[idx][k]] = maskVal;
                                    mask[(byte)maskVal][indexes[idx][k]] = (byte)maskVal;
                                }
                            }
                            if (inside_cnt > 0 && inside_cnt < 4) {

                                for (int k = 0; k < count[idx]; k++) {
                                    float xx = vals_0[0][indexes[idx][k]];
                                    float yy = vals_1[0][indexes[idx][k]];
                                    if (DelaunayCustom.inside(curve, xx, yy)) {
                                        maskRange[0][indexes[idx][k]] = maskVal;
                                        mask[(byte)maskVal][indexes[idx][k]] = (byte)maskVal;
                                    }
                                }
                            }
                        } else if (rangeType == Double.TYPE) {
                            double[][] vals_0 = field_0.getValues(false);
                            double[][] vals_1 = field_1.getValues(false);
                            if (inside_cnt == 4) {
                                for (int k = 0; k < count[idx]; k++) {
                                    maskRange[0][indexes[idx][k]] = maskVal;
                                    mask[(byte)maskVal][indexes[idx][k]] = (byte)maskVal;
                                }
                            }
                            if (inside_cnt > 0 && inside_cnt < 4) {

                                for (int k = 0; k < count[idx]; k++) {
                                    double xx = vals_0[0][indexes[idx][k]];
                                    double yy = vals_1[0][indexes[idx][k]];
                                    if (DelaunayCustom.inside(curve, (float) xx, (float) yy)) {
                                        maskRange[0][indexes[idx][k]] = maskVal;
                                        mask[(byte)maskVal][indexes[idx][k]] = (byte)maskVal;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        mask_field.setSamples(maskRange, false);
    }

    private void reorder(byte maskVal) {
       order[2] = order[1];
       order[1] = order[0];
       order[0] = maskVal;
    }

    public void clearMaskField(float maskVal) {
        for (int k = 0; k < maskRange[0].length; k++) {
            maskRange[0][k] = Float.NaN;
            mask[(byte)maskVal][k] = Byte.MAX_VALUE;
        }

        for (int t=0; t<order.length; t++) {
           if (order[t] == (byte)maskVal) {
               order[t] = Byte.MAX_VALUE;
           }
        }

        for (int t=order.length-1; t >=0; t--) {
            if (order[t] != Byte.MAX_VALUE) {
               for (int k=0; k<maskRange[0].length; k++) {
                   if (mask[order[t]][k] != Byte.MAX_VALUE) {
                      maskRange[0][k] = (float) order[t];
                   }
               }
            }
        }
    }

    public void resetMaskField(float maskVal) throws Exception {
        clearMaskField(maskVal);
        mask_field.setSamples(maskRange, false);
    }
}

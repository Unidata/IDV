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

import java.rmi.RemoteException;
import visad.Set;
import visad.Gridded1DSet;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Gridded2DSet;
import visad.SampledSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;
import visad.QuickSort;
import visad.FlatField;
import visad.FieldImpl;
import java.util.HashMap;
import java.util.Map;
import visad.GriddedSet;


public abstract class ProfileAlongTrack extends MultiDimensionAdapter {

      private FunctionType mathtype;

      int TrackLen;
      int VertLen;

      private float[] vertLocs = null;
      private float[] trackTimes = null;
      private float[] trackLongitude = null;
      private float[] trackLatitude = null;

      public static String longitude_name = "Longitude";
      public static String latitude_name  = "Latitude";
      public static String trackDim_name  = "TrackDim";
      public static String vertDim_name  = "VertDim";
      public static String array_name = "array_name";
      public static String profileTime_name = "ProfileTime";
      public static String profileTime_unit = "ProfileTime_unit";
      public static String altitude_unit = "altitude_unit";
      public static String sfcElev_name = "SurfaceElev";
      public static String range_name = "range_name";
      public static String scale_name = "scale_name";
      public static String offset_name = "offset_name";
      public static String fill_value_name = "fill_value_name";
      public static String valid_range = "valid_range";
      public static String ancillary_file_name = "ancillary_file";
      static String product_name = "product_name";
      
      String[] rangeName_s  = null;
      Class[] arrayType_s = null;
      Unit[] rangeUnit_s  = new Unit[] {null};

      RealType track  = RealType.getRealType(trackDim_name);
      RealType vert = RealType.getRealType(vertDim_name);
      RealType[] domainRealTypes = new RealType[2];

      RealType vertLocType;
      RealType trackTimeType;

      int track_idx      = -1;
      int vert_idx       = -1;
      int range_rank     = -1;

      int track_tup_idx;
      int vert_tup_idx;

      boolean isVertDimAlt = true;

      CoordinateSystem cs = null;
      
      int medianFilterTrackWidth = 10;
      int medianFilterVertWidth = 10;

      public static Map<String, double[]> getEmptySubset() {
        Map<String, double[]> subset = new HashMap<>();
        subset.put(trackDim_name, new double[3]);
        subset.put(vertDim_name, new double[3]);
        return subset;
      }

      public static Map<String, Object> getEmptyMetadataTable() {
        Map<String, Object> metadata = new HashMap<>();
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

      public ProfileAlongTrack(MultiDimensionReader reader, Map<String, Object> metadata) {
        this(reader, metadata, true);
      }

      public ProfileAlongTrack(MultiDimensionReader reader, Map<String, Object> metadata, boolean isVertDimAlt) {
        super(reader, metadata);
        this.isVertDimAlt = isVertDimAlt;
        this.init();
      }


      private void init() {
        for (int k=0; k<array_rank;k++) {
          if ( ((String)metadata.get(trackDim_name)).equals(array_dim_names[k]) ) {
            track_idx = k;
          }
          if ( ((String)metadata.get(vertDim_name)).equals(array_dim_names[k]) ) {
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
        }
        else {
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
          rangeName = (String)metadata.get("range_name");
        } 
        else {
          rangeName = (String)metadata.get("array_name");
        }
        rangeType = RealType.getRealType(rangeName, rangeUnit_s[0]);

        try {
          rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
        } 
        catch (Exception e) {
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
        } 
        catch (Exception e) {
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
      
      public int getMedianFilterWindowWidth() {
        return medianFilterTrackWidth;
      }
      
      public int getMedianFilterWindowHeight() {
        return medianFilterVertWidth;
      }
                                                                                                                                                     
      public Set makeDomain(Map<String, double[]> subset) throws Exception {
        double[] first = new double[2];
        double[] last = new double[2];
        int[] length = new int[2];

        Map<String, double[]> domainSubset = new HashMap<>();
        domainSubset.put(trackDim_name, subset.get(trackDim_name));
        domainSubset.put(vertDim_name, subset.get(vertDim_name));

        for (int kk=0; kk<2; kk++) {
          RealType rtype = domainRealTypes[kk];
          double[] coords = subset.get(rtype.getName());
          first[kk] = coords[0];
          last[kk] = coords[1];
          length[kk] = (int) ((last[kk] - first[kk])/coords[2] + 1);
          last[kk] = first[kk]+coords[2]*(length[kk]-1);
        }
        Linear2DSet domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
        final Linear1DSet[] lin1DSet_s = new Linear1DSet[] {domainSet.getLinear1DComponent(0),
                                           domainSet.getLinear1DComponent(1)};

        float[] new_altitudes = new float[length[vert_tup_idx]];
        float[] new_times = new float[length[track_tup_idx]];

        int track_start = (int) first[track_tup_idx];
        int vert_start = (int) first[vert_tup_idx];
        int vert_skip = (int) (subset.get(vertDim_name))[2];
        int track_skip = (int) (subset.get(trackDim_name))[2];
        for (int k=0; k<new_altitudes.length; k++) {
          new_altitudes[k] = vertLocs[vert_start+(k*vert_skip)];
        }

        for (int k=0; k<new_times.length; k++) {
          new_times[k] = trackTimes[track_start+(k*track_skip)];
        }

        final Gridded1DSet alt_set = new Gridded1DSet(vertLocType, new float[][] {new_altitudes}, new_altitudes.length);
        final Gridded1DSet time_set = new Gridded1DSet(trackTimeType, new float[][] {new_times}, new_times.length);
        final float vert_offset = (float) first[vert_tup_idx];
        final float track_offset = (float) first[track_tup_idx];

        RealTupleType reference = new RealTupleType(vertLocType, trackTimeType);
        
        CoordinateSystem cs = null;

        try {
        cs = new CoordinateSystem(reference, null) {
          public float[][] toReference(float[][] vals) throws VisADException {
            int[] indexes = lin1DSet_s[0].valueToIndex(new float[][] {vals[0]});
            /* ?
            for (int k=0; k<vals[0].length;k++) {
               indexes[k] = (int) (vals[vert_tup_idx][k] - vert_offset);
            }
            */
            float[][] alts = alt_set.indexToValue(indexes);

            indexes = lin1DSet_s[1].valueToIndex(new float[][] {vals[1]});
            /* ?
            for (int k=0; k<vals[0].length;k++) {
               indexes[k] = (int) (vals[track_tup_idx][k] - track_offset);
            }
            */
            float[][] times = time_set.indexToValue(indexes);

            return new float[][] {alts[0], times[0]};
          }
          public float[][] fromReference(float[][] vals) throws VisADException {
            int[] indexes = alt_set.valueToIndex(new float[][] {vals[vert_tup_idx]});
            float[][] vert_coords = lin1DSet_s[vert_tup_idx].indexToValue(indexes);
            indexes = time_set.valueToIndex(new float[][] {vals[track_tup_idx]});
            float[][] track_coords = lin1DSet_s[track_tup_idx].indexToValue(indexes);
            return new float[][] {vert_coords[0], track_coords[0]};
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
        }
        catch (Exception e) {
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

      public Map<String, double[]> getDefaultSubset() {
        Map<String, double[]> subset = ProfileAlongTrack.getEmptySubset();

        double[] coords = subset.get("TrackDim");
        coords[0] = 20000.0;
        coords[1] = (TrackLen - 15000.0) - 1;
        //-coords[2] = 30.0;
        coords[2] = 5.0;
        subset.put("TrackDim", coords);

        coords = subset.get("VertDim");
        coords[0] = 98.0;
        coords[1] = (VertLen) - 1;
        coords[2] = 2.0;
        subset.put("VertDim", coords);
        return subset;
      }

      public int[] getTrackRangeInsideLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
        int nn = 100;
        int skip = TrackLen/nn;
        double lon;
        double lat;
        
        int idx = 0;
        while (idx < TrackLen) {
          lon = (double)trackLongitude[idx];
          lat = (double)trackLatitude[idx];
          if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat)&&(lat < maxLat))) break;
          idx += skip;
        }
        if (idx > TrackLen-1) idx = TrackLen-1;
        if (idx == TrackLen-1) return new int[] {-1,-1};

        int low_idx = idx;
        while (low_idx > 0) {
          lon = (double)trackLongitude[low_idx];
          lat = (double)trackLatitude[low_idx];
          if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat)&&(lat < maxLat))) {
            low_idx -= 1;
            continue;
          }
          else {
            break;
          }
        }

        int hi_idx = idx;
        while (hi_idx < TrackLen-1) {
          lon = (double)trackLongitude[hi_idx];
          lat = (double)trackLatitude[hi_idx];
          if (((lon > minLon) && (lon < maxLon)) && ((lat > minLat)&&(lat < maxLat))) {
            hi_idx += 1;
            continue;
          }
          else {
            break;
          }
        }
        return new int[] {low_idx, hi_idx};
      }

      public Map<String, double[]> getSubsetFromLonLatRect(Map<String, double[]> subset, double minLat, double maxLat, double minLon, double maxLon) {
        double[] coords = subset.get("TrackDim");
        int[] idxs = getTrackRangeInsideLonLatRect(minLat, maxLat, minLon, maxLon);
        coords[0] = (double) idxs[0];
        coords[1] = (double) idxs[1];
        if ((coords[0] == -1) || (coords[1] == -1)) return null;
        return subset;
      }

      public Map<String, double[]> getSubsetFromLonLatRect(Map<String, double[]> subset, double minLat, double maxLat, double minLon, double maxLon,
                                             int xStride, int yStride, int zStride) {

        double[] coords = subset.get("TrackDim");
        int[] idxs = getTrackRangeInsideLonLatRect(minLat, maxLat, minLon, maxLon);
        coords[0] = (double) idxs[0];
        coords[1] = (double) idxs[1];
        if ((coords[0] == -1) || (coords[1] == -1)) return null;
        if (xStride > 0) {
          coords[2] = xStride;
        }

        coords = subset.get("VertDim");
        if (yStride > 0) {
          coords[2] = yStride;
        }
        return subset;
      }

      public Map<String, double[]> getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
        return getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon);
      }

      public Map<String, double[]> getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon,
                                             int xStride, int yStride, int zStride) {

        return getSubsetFromLonLatRect(getDefaultSubset(), minLat, maxLat, minLon, maxLon, xStride, yStride, zStride);
      }

      public abstract float[] getVertBinAltitude() throws Exception;
      public abstract float[] getTrackTimes() throws Exception;
      public abstract RealType makeVertLocType() throws Exception;
      public abstract RealType makeTrackTimeType() throws Exception;
      public abstract float[] getTrackLongitude() throws Exception;
      public abstract float[] getTrackLatitude() throws Exception;
      
      public static FieldImpl medianFilter(FieldImpl field, int window_lenx, int window_leny) throws VisADException, RemoteException, CloneNotSupportedException  {
         Set dSet = field.getDomainSet();
         if (dSet.getManifoldDimension() != 1) {
            throw new VisADException("medianFilter: outer field domain must have manifoldDimension = 1");
         }
         int outerLen = dSet.getLength();
         
         FieldImpl filtField = (FieldImpl)field.clone();
         
         for (int t=0; t<outerLen; t++) {
            FlatField ff = (FlatField) filtField.getSample(t, false);
            medianFilter(ff, window_lenx, window_leny);
         }
         
         return filtField;
      }
      
      public static FlatField medianFilter(FlatField fltFld, int window_lenx, int window_leny) throws VisADException, RemoteException {
         GriddedSet domSet = (GriddedSet) fltFld.getDomainSet();
         FlatField filtFld = new FlatField((FunctionType)fltFld.getType(), domSet);
         
         int[] lens = domSet.getLengths();
         int manifoldDimension = domSet.getManifoldDimension();
         
         float[][] rngVals = fltFld.getFloats(false);
         int rngTupleDim = rngVals.length;
         float[][] filtVals = new float[rngTupleDim][];
         
         if (manifoldDimension == 2) {
            for (int t=0; t<rngTupleDim; t++) {
               filtVals[t] = medianFilter(rngVals[t], lens[0], lens[1], window_lenx, window_leny);
            }
         }
         else if (manifoldDimension == 3) {
            int outerDimLen = lens[0];
            filtVals = new float[rngTupleDim][lens[0]*lens[1]*lens[2]];
            float[] rngVals2D = new float[lens[1]*lens[2]];
            
            for (int k = 0; k<outerDimLen; k++) {
               int idx = k*lens[1]*lens[2];
               for (int t=0; t<rngTupleDim; t++) {
                  System.arraycopy(rngVals[t], idx, rngVals2D, 0, lens[1]*lens[2]);
                  
                  float[] fltA = medianFilter(rngVals2D, lens[1], lens[2], window_lenx, window_leny);
                  
                  System.arraycopy(fltA, 0, filtVals[t], idx, lens[1]*lens[2]);
               }
            }
         }
         
         filtFld.setSamples(filtVals, false);
         
         return filtFld;
      }
      
      public static float[] medianFilter(float[] A, int lenx, int leny, int window_lenx, int window_leny)
           throws VisADException {
        float[] result =  new float[A.length];
        float[] window =  new float[window_lenx*window_leny];
        float[] sortedWindow =  new float[window_lenx*window_leny];
        int[] sort_indexes = new int[window_lenx*window_leny];
        int[] indexes = new int[window_lenx*window_leny];
        int[] indexesB = new int[window_lenx*window_leny];
        
        int[] numToInsertAt = new int[window_lenx*window_leny];
        float[][] valsToInsert = new float[window_lenx*window_leny][window_lenx*window_leny];
        int[][] idxsToInsert = new int[window_lenx*window_leny][window_lenx*window_leny];
        
        int[] numBefore = new int[window_lenx*window_leny];
        float[][] valsBefore = new float[window_lenx*window_leny][window_lenx*window_leny];
        int[][] idxsBefore = new int[window_lenx*window_leny][window_lenx*window_leny];
        
        int[] numAfter = new int[window_lenx*window_leny];
        float[][] valsAfter = new float[window_lenx*window_leny][window_lenx*window_leny];
        int[][] idxsAfter = new int[window_lenx*window_leny][window_lenx*window_leny];                
        
        float[] sortedArray = new float[window_lenx*window_leny];
                                                                                                                                    
        int a_idx;
        int w_idx;
                                                                                                                                    
        int w_lenx = window_lenx/2;
        int w_leny = window_leny/2;
                                                                                                                                    
        int lo;
        int hi;
        int ww_jj;
        int ww_ii;
        int cnt=0;
        int ncnt;
        int midx;
        float median;
        
        int lenA = A.length;
        
        for (int i=0; i<lenx; i++) { // zig-zag better? Maybe, but more complicated
          for (int j=0; j<leny; j++) {             
            a_idx = j*lenx + i;
            
            if (j > 0) {
              ncnt = 0;
              for (int t=0; t<cnt; t++) {
                 // last window index in data coords: A[lenx,leny]
                 int k = indexes[sort_indexes[t]];
                 ww_jj = k/lenx;
                 ww_ii = k % lenx;
                 
                 // current window bounds in data coords
                 int ww_jj_lo = j - w_leny;
                 int ww_jj_hi = j + w_leny;
                 int ww_ii_lo = i - w_lenx;
                 int ww_ii_hi = i + w_lenx;
                 
                 if (ww_jj_lo < 0) ww_jj_lo = 0;
                 if (ww_ii_lo < 0) ww_ii_lo = 0;
                 if (ww_jj_hi > leny-1) ww_jj_hi = leny-1;
                 if (ww_ii_hi > lenx-1) ww_ii_hi = lenx-1;
                 
                 
                 // These are the points which overlap between the last and current window
                 if ((ww_jj >= ww_jj_lo && ww_jj < ww_jj_hi) && (ww_ii >= ww_ii_lo && ww_ii < ww_ii_hi)) {
                    window[ncnt] = sortedWindow[t];
                    indexesB[ncnt] = k;
                    ncnt++;
                 }
              }
              
              
              // Add the new points from sliding the window to the overlap points above
              java.util.Arrays.fill(numToInsertAt, 0);
              java.util.Arrays.fill(numBefore, 0);
              java.util.Arrays.fill(numAfter, 0);
              
              ww_jj = w_leny-1 + j;
              for (int w_i=-w_lenx; w_i<w_lenx; w_i++) {
                 ww_ii = w_i + i;
                 int k = ww_jj*lenx+ww_ii;
                  if (k >= 0 && k < lenA) {
                     float val = A[k];
                        if (ncnt > 0) {
                           int t = 0;
                           if (val < window[t]) {
                                 valsBefore[0][numBefore[0]] = val;
                                 idxsBefore[0][numBefore[0]] = k;
                                 numBefore[0]++;  
                                 continue;
                           }                     
                           t = ncnt-1;
                           if (val >= window[t]) {
                                 valsAfter[0][numAfter[0]] = val;
                                 idxsAfter[0][numAfter[0]] = k;
                                 numAfter[0]++;  
                                 continue;
                           }

                           for (t=0; t<ncnt-1; t++) {
                              if (val >= window[t] && val < window[t+1]) {
                                 valsToInsert[t][numToInsertAt[t]] = val;
                                 idxsToInsert[t][numToInsertAt[t]] = k;
                                 numToInsertAt[t]++;
                                 break;
                              }
                           } 
                        }
                        else if (!Float.isNaN(val)) {
                                 valsBefore[0][numBefore[0]] = val;
                                 idxsBefore[0][numBefore[0]] = k;
                                 numBefore[0]++;  
                                 continue;                           
                        }
                  }
              }

              // insert new unsorted values into the already sorted overlap window region
              int tcnt = 0;
              
              for (int it=0; it<numBefore[0]; it++) {
                 sortedArray[tcnt] = valsBefore[0][it];
                 indexes[tcnt] = idxsBefore[0][it];
                 tcnt++;
              }  
                       
              for (int t=0; t<ncnt; t++) {
                 sortedArray[tcnt] = window[t];
                 indexes[tcnt] = indexesB[t];
                 tcnt++;
                 if (numToInsertAt[t] > 0) {
                    if (numToInsertAt[t] == 2) { // two item sort here to save work for QuickSort
                       float val0 = valsToInsert[t][0];
                       float val1 = valsToInsert[t][1];
                       
                       if (val0 <= val1) {
                          sortedArray[tcnt] = val0;
                          indexes[tcnt] = idxsToInsert[t][0];
                          tcnt++;
                          
                          sortedArray[tcnt] = val1;
                          indexes[tcnt] = idxsToInsert[t][1];
                          tcnt++;
                       }
                       else {
                          sortedArray[tcnt] = val1;
                          indexes[tcnt] = idxsToInsert[t][1];  
                          tcnt++;
                          
                          sortedArray[tcnt] = val0;
                          indexes[tcnt] = idxsToInsert[t][0];      
                          tcnt++;
                       }
                    }
                    else if (numToInsertAt[t] == 1) {
                       sortedArray[tcnt] = valsToInsert[t][0];
                       indexes[tcnt] = idxsToInsert[t][0];
                       tcnt++;
                    }
                    else {
                       for (int it=0; it<numToInsertAt[t]; it++) {
                          sortedArray[tcnt] = valsToInsert[t][it];
                          indexes[tcnt] = idxsToInsert[t][it];
                          tcnt++;
                       }
                    }
                 }
              }
              
              for (int it=0; it<numAfter[0]; it++) {
                 sortedArray[tcnt] = valsAfter[0][it];
                 indexes[tcnt] = idxsAfter[0][it];
                 tcnt++;
              }  
              
              // Now sort the new unsorted and overlap sorted points together to get the new window median
              
              System.arraycopy(sortedArray, 0, sortedWindow, 0, tcnt);
              if (tcnt > 0) {
                 sort_indexes = QuickSort.sort(sortedWindow, 0, tcnt-1);
                 median = sortedWindow[tcnt/2];
              }
              else {
                 median = Float.NaN;
              }
              cnt = tcnt;

            }
            else { // full sort done once for each row (see note on zigzag above)
            
               cnt = 0;
               for (int w_j=-w_leny; w_j<w_leny; w_j++) {
                 for (int w_i=-w_lenx; w_i<w_lenx; w_i++) {
                   ww_jj = w_j + j;
                   ww_ii = w_i + i;
                   w_idx = (w_j+w_leny)*window_lenx + (w_i+w_lenx);
                   if ((ww_jj >= 0) && (ww_ii >=0) && (ww_jj < leny) && (ww_ii < lenx)) {
                     int k = ww_jj*lenx+ww_ii;
                     float val = A[k];
                     if (!Float.isNaN(val)) {
                       window[cnt] = val;
                       indexes[cnt] = k;
                       cnt++;
                     }
                   }
                 }
               }
            
            
               System.arraycopy(window, 0, sortedWindow, 0, cnt);
               if (cnt > 0) {
                  sort_indexes = QuickSort.sort(sortedWindow, 0, cnt-1);
                  midx = cnt/2;
                  median = sortedWindow[midx];
               }
               else {
                  median = Float.NaN;
               }
               
            }
            
            if (Float.isNaN(A[a_idx])) {
              result[a_idx] = Float.NaN;
            }
            else {
              result[a_idx] = median;
            }
            
          }
        }
        
        return result;
      }
      
      public static float[] medianFilterOrg(float[] A, int lenx, int leny, int window_lenx, int window_leny)
           throws VisADException {
        float[] result =  new float[A.length];
        float[] window =  new float[window_lenx*window_leny];
        float[] new_window =  new float[window_lenx*window_leny];
        int[] sort_indexes = new int[window_lenx*window_leny];
                                                                                                                                    
        int a_idx;
        int w_idx;
                                                                                                                                    
        int w_lenx = window_lenx/2;
        int w_leny = window_leny/2;
                                                                                                                                    
        int lo;
        int hi;
        int ww_jj;
        int ww_ii;
        int cnt;
                                                                                                                                    
        for (int j=0; j<leny; j++) {
          for (int i=0; i<lenx; i++) {
            a_idx = j*lenx + i;
            cnt = 0;
            for (int w_j=-w_leny; w_j<w_leny; w_j++) {
              for (int w_i=-w_lenx; w_i<w_lenx; w_i++) {
                ww_jj = w_j + j;
                ww_ii = w_i + i;
                w_idx = (w_j+w_leny)*window_lenx + (w_i+w_lenx);
                if ((ww_jj >= 0) && (ww_ii >=0) && (ww_jj < leny) && (ww_ii < lenx)) {
                  window[cnt] = A[ww_jj*lenx+ww_ii];
                  cnt++;
                }
              }
            }
            System.arraycopy(window, 0, new_window, 0, cnt);
            //-sort_indexes = QuickSort.sort(new_window, sort_indexes);
            sort_indexes = QuickSort.sort(new_window);
            result[a_idx] = new_window[cnt/2];
          }
        }
        return result;
      }
}

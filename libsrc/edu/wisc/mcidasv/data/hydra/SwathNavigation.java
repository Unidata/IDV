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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import visad.CoordinateSystem;
import visad.Gridded2DDoubleSet;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.RealTupleType;

public class SwathNavigation implements Navigation  {

  public static SwathNavigation createNavigation(SwathAdapter swathAdapter) throws Exception {
    String product_name = null;
    SwathNavigation swathNav = null;
    
    product_name = (String)swathAdapter.getMetadata().get(SwathAdapter.product_name);

    if (product_name == null) {
      swathNav = new SwathNavigation(swathAdapter);
    }
    else if (Objects.equals(product_name, "IASI_L1C_xxx")) {
      swathNav = new IASI_L1C_LonLatNavigation(swathAdapter);
    }
    else if (Objects.equals(product_name, "CrIS_SDR")) {
      swathNav = new CrIS_SDR_LonLatNavigation(swathAdapter);
    }
    else {
      swathNav = new SwathNavigation(swathAdapter);
    }
    
    return swathNav;
  }

  int geo_track_idx;
  int geo_xtrack_idx;
  int geoTrackLen;
  int geoXTrackLen;

  SwathAdapter swathAdapter;
  MultiDimensionReader reader;
  String lon_array_name;
  String lat_array_name;
  int[] idx_order = new int[2];
  float ratio = 1;
  float track_ratio = 1;
  float xtrack_ratio = 1;
  double track_offset = 0;
  double xtrack_offset = 0;
  int track_idx;
  int xtrack_idx;
  int[] geo_stride = new int[2];
  int[] geo_count = new int[2];
  int[] geo_start = new int[2];

  String scale_name = "SCALE_NAME";
  String offset_name = "OFFSET_NAME";
  String fillValue_name = "_FILLVALUE";

  int numDims = 2;

  Class type;

  public SwathNavigation(SwathAdapter swathAdapter) throws Exception {

    Map<String, Object> metadata = swathAdapter.getMetadata();
    reader = swathAdapter.getReader();
    this.swathAdapter = swathAdapter;
    track_idx = swathAdapter.track_idx;
    xtrack_idx = swathAdapter.xtrack_idx;

    lon_array_name = (String)metadata.get(SwathAdapter.lon_array_name);
    lat_array_name = (String)metadata.get(SwathAdapter.lat_array_name);

    String[] lon_dim_names = null;

    String[] lonDimNames = (String[])metadata.get(SwathAdapter.lon_array_dimension_names);

    if (lonDimNames != null) {
      lon_dim_names = lonDimNames;
    }
    else {
      lon_dim_names = reader.getDimensionNames(lon_array_name);
    }

    int[] lon_dim_lengths = reader.getDimensionLengths(lon_array_name);

    numDims = lon_dim_lengths.length;
    geo_stride = new int[numDims];
    geo_count = new int[numDims];
    geo_start = new int[numDims];


    String geo_track_name = (String)metadata.get(SwathAdapter.geo_track_name);
    String geo_xtrack_name = (String)metadata.get(SwathAdapter.geo_xtrack_name);

    for (int k=0; k<numDims;k++) {
      if ( geo_track_name.equals(lon_dim_names[k]) ) {
         geo_track_idx = k;
      }
      if ( geo_xtrack_name.equals(lon_dim_names[k]) ) {
         geo_xtrack_idx = k;
      }
    }

    if (geo_track_idx < geo_xtrack_idx) {
      idx_order[0] = geo_xtrack_idx;
      idx_order[1] = geo_track_idx;
    }
    else {
      idx_order[0] = geo_track_idx;
      idx_order[1] = geo_xtrack_idx;
    }

    geoTrackLen  = lon_dim_lengths[geo_track_idx];
    geoXTrackLen = lon_dim_lengths[geo_xtrack_idx];

    String str = (String)metadata.get(SwathAdapter.geo_track_skip_name);

    if (str != null) {
      track_ratio = (float) Double.parseDouble(str);
      ratio = track_ratio;
    }
    str = (String)metadata.get(SwathAdapter.geo_xtrack_skip_name);
    if (str != null) {
      xtrack_ratio = (float) Double.parseDouble(str);
    }
    str = (String)metadata.get(SwathAdapter.geo_track_offset_name);
    if (str != null) {
      track_offset = Double.parseDouble(str);
    }
    str = (String)metadata.get(SwathAdapter.geo_xtrack_offset_name);
    if (str != null) {
      xtrack_offset = Double.parseDouble(str);
    }

    str = (String)metadata.get(SwathAdapter.geo_scale_name);
    if (str != null) {
      scale_name = str;
    }

    str = (String)metadata.get(SwathAdapter.geo_offset_name);
    if (str != null) {
      offset_name = str;
    }

    str = (String)metadata.get(SwathAdapter.geo_fillValue_name);
    if (str != null) {
      fillValue_name = str;
    }
 
    type = reader.getArrayType(lon_array_name);
  }

  public CoordinateSystem getVisADCoordinateSystem(Linear2DSet domainSet, Map<String, double[]> domainSubset) throws Exception
  {
      Subset select = swathAdapter.getIndexes(domainSubset);

      double[] track_coords = domainSubset.get(SwathAdapter.track_name);
      double[] xtrack_coords = domainSubset.get(SwathAdapter.xtrack_name);
      
      int[] stride = new int[numDims];
      stride[geo_track_idx] = (int) track_coords[2];
      stride[geo_xtrack_idx] = (int) xtrack_coords[2]; 


      if (numDims > 2) { // initialize geo arrays, then recompute xtrack/track dimensions below
        if (numDims == select.getRank()) {
          int[] start = select.getStart();
          int[] count = select.getCount();
          stride = select.getStride();
          for (int i=0; i<numDims; i++) {
            geo_start[i] = start[i];
            geo_count[i] = count[i];
            geo_stride[i] = stride[i];
          }
        }
        else {
          geo_start[geo_track_idx] = (int) track_coords[0];
          geo_start[geo_xtrack_idx] = (int) xtrack_coords[0];
          geo_count[geo_track_idx] = (int) ((track_coords[1] - track_coords[0])/track_coords[2] + 1f);
          geo_count[geo_xtrack_idx] = (int) ((xtrack_coords[1] - xtrack_coords[0])/xtrack_coords[2] + 1f);
        }
      }


      if (ratio/(float)stride[0] <= 1) {
        geo_stride[geo_track_idx] = Math.round((1f/(track_ratio/((float)stride[geo_track_idx]))));
        geo_stride[geo_xtrack_idx] = Math.round((1f/(xtrack_ratio/((float)stride[geo_xtrack_idx]))));
      }
      else {
        geo_stride[geo_track_idx] = 1;
        geo_stride[geo_xtrack_idx] = 1;
      }

      int geo_track_start  = (int) Math.ceil((track_coords[0] - track_offset)/track_ratio);
      int geo_xtrack_start = (int) Math.ceil((xtrack_coords[0] - xtrack_offset)/xtrack_ratio);

      int geo_track_end  = (int) ((track_coords[1] - track_offset)/((double)track_ratio));
      int geo_xtrack_end = (int) ((xtrack_coords[1] - xtrack_offset)/((double)xtrack_ratio));

      geo_count[geo_track_idx]  = (int) ((geo_track_end - geo_track_start)/geo_stride[geo_track_idx]) + 1;
      geo_count[geo_xtrack_idx] = (int) ((geo_xtrack_end - geo_xtrack_start)/geo_stride[geo_xtrack_idx]) + 1;

      geo_track_end = geo_track_start + (geo_count[geo_track_idx]-1)*geo_stride[geo_track_idx];
      geo_xtrack_end = geo_xtrack_start + (geo_count[geo_xtrack_idx]-1)*geo_stride[geo_xtrack_idx];
     
      geo_start[geo_track_idx]  = geo_track_start;
      geo_start[geo_xtrack_idx] = geo_xtrack_start;

      //-- convert back track/xtrack coords:
      int new_track_start  = (int) (geo_track_start*track_ratio + (float)track_offset);
      int new_xtrack_start = (int) (geo_xtrack_start*xtrack_ratio + (float)xtrack_offset);
      int new_track_end  = (int) (geo_track_end*track_ratio + (float)track_offset);
      int new_xtrack_end = (int) (geo_xtrack_end*xtrack_ratio + (float)xtrack_offset);


      //- these must be only 2D (Swath dimensions)
      double[] first = new double[2];
      double[] last  = new double[2];
      int[] length   = new int[2];

      int track_idx;
      int xtrack_idx;
      if (geo_track_idx < geo_xtrack_idx) {
        track_idx = 1;
        xtrack_idx = 0;
      } else {
        track_idx = 0;
        xtrack_idx = 1;
      }

      first[track_idx]  = new_track_start;
      first[xtrack_idx] = new_xtrack_start;
      last[track_idx]   = new_track_end;
      last[xtrack_idx]  = new_xtrack_end;
      length[track_idx]  = (int) ((last[track_idx] - first[track_idx])/stride[geo_track_idx] + 1);
      length[xtrack_idx] = (int) ((last[xtrack_idx] - first[xtrack_idx])/stride[geo_xtrack_idx] + 1);

      domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
   
      Gridded2DSet gset = null;

      gset = createInterpSet();

      CoordinateSystem cs = new LongitudeLatitudeCoordinateSystem(domainSet, gset);

      return cs;
  }

  Gridded2DSet createInterpSet() throws Exception {
    Gridded2DSet gset = null;
    if (type == Float.TYPE) {
      float[] lonValues = reader.getFloatArray(lon_array_name, geo_start, geo_count, geo_stride);
      float[] latValues = reader.getFloatArray(lat_array_name, geo_start, geo_count, geo_stride);
                                                                                                                                             
      gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                     new float[][] {lonValues, latValues},
                         geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);
    }
    else if (type == Double.TYPE) {
      double[] lonValues = reader.getDoubleArray(lon_array_name, geo_start, geo_count, geo_stride);
      double[] latValues = reader.getDoubleArray(lat_array_name, geo_start, geo_count, geo_stride);
                                                                                                                                             
      gset = new Gridded2DDoubleSet(RealTupleType.SpatialEarth2DTuple,
                    new double[][] {lonValues, latValues},
                       geo_count[idx_order[0]], geo_count[idx_order[1]],
                           null, null, null, false);
    }
    else if (type == Short.TYPE) {
      short[] values = reader.getShortArray(lon_array_name, geo_start, geo_count, geo_stride);
      Map<String, Object> metadata = new HashMap<>();
      metadata.put(SwathAdapter.array_name, lon_array_name);
      metadata.put(SwathAdapter.scale_name, scale_name);
      metadata.put(SwathAdapter.offset_name, offset_name);
      metadata.put(SwathAdapter.fill_value_name, fillValue_name);
      RangeProcessor rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
      float[] lonValues = rangeProcessor.processRange(values, null);
      
      values = reader.getShortArray(lat_array_name, geo_start, geo_count, geo_stride);
      metadata = new HashMap<>();
      metadata.put(SwathAdapter.array_name, lat_array_name);
      metadata.put(SwathAdapter.scale_name, scale_name);
      metadata.put(SwathAdapter.offset_name, offset_name);
      metadata.put(SwathAdapter.fill_value_name, fillValue_name);
      rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
      float[] latValues = rangeProcessor.processRange(values, null);


      gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                     new float[][] {lonValues, latValues},
                         geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);

    }
    return gset;
  }



  public static Linear2DSet getNavigationDomain(double data_x_start, double data_x_stop, double data_x_stride,
                                         double data_y_start, double data_y_stop, double data_y_stride,
                                         double ratio_x, double ratio_y,
                                         double offset_x, double offset_y,
                                         int[] geo_start, int[] geo_count, int[] geo_stride)
      throws Exception {

      int geo_track_idx = 1;
      int geo_xtrack_idx = 0;
      double track_ratio = ratio_y;
      double xtrack_ratio = ratio_x;
      double track_offset = offset_y;
      double xtrack_offset = offset_x;
 
      double[] track_coords = new double[3];
      double[] xtrack_coords = new double[3];

      xtrack_coords[0] = data_x_start;
      xtrack_coords[1] = data_x_stop;
      track_coords[0] = data_y_start;
      track_coords[1] = data_y_stop;

      double[] stride =  new double[2];
      stride[geo_track_idx] = data_y_stride;
      stride[geo_xtrack_idx] = data_x_stride;

      if (track_ratio/(float)stride[0] <= 1) {
        geo_stride[geo_track_idx] = (int) Math.round((1/(track_ratio/(stride[1]))));
        geo_stride[geo_xtrack_idx] = (int) Math.round((1/(xtrack_ratio/(stride[0]))));
      }
      else {
        geo_stride[0] = 1;
        geo_stride[1] = 1;
      }

      int geo_track_start  = (int) Math.ceil((track_coords[0] - track_offset)/track_ratio);
      int geo_xtrack_start = (int) Math.ceil((xtrack_coords[0] - xtrack_offset)/xtrack_ratio);

      int geo_track_end  = (int) ((track_coords[1] - track_offset)/((double)track_ratio));
      int geo_xtrack_end = (int) ((xtrack_coords[1] - xtrack_offset)/((double)xtrack_ratio));

      geo_count[geo_track_idx]  = (int) ((geo_track_end - geo_track_start)/geo_stride[geo_track_idx]) + 1;
      geo_count[geo_xtrack_idx] = (int) ((geo_xtrack_end - geo_xtrack_start)/geo_stride[geo_xtrack_idx]) + 1;

      geo_track_end = geo_track_start + (geo_count[geo_track_idx]-1)*geo_stride[geo_track_idx];
      geo_xtrack_end = geo_xtrack_start + (geo_count[geo_xtrack_idx]-1)*geo_stride[geo_xtrack_idx];

      geo_start[geo_track_idx]  = geo_track_start;
      geo_start[geo_xtrack_idx] = geo_xtrack_start;

      //-- convert back track/xtrack coords:
      int new_track_start  = (int) (geo_track_start*track_ratio + (float)track_offset);
      int new_xtrack_start = (int) (geo_xtrack_start*xtrack_ratio + (float)xtrack_offset);
      int new_track_end  = (int) (geo_track_end*track_ratio + (float)track_offset);
      int new_xtrack_end = (int) (geo_xtrack_end*xtrack_ratio + (float)xtrack_offset);


      double[] first = new double[2];
      double[] last  = new double[2];
      int[] length   = new int[2];
      first[geo_track_idx]  = new_track_start;
      first[geo_xtrack_idx] = new_xtrack_start;
      last[geo_track_idx]   = new_track_end;
      last[geo_xtrack_idx]  = new_xtrack_end;
      length[geo_track_idx]  = (int) ((last[geo_track_idx] - first[geo_track_idx])/stride[geo_track_idx] + 1);
      length[geo_xtrack_idx] = (int) ((last[geo_xtrack_idx] - first[geo_xtrack_idx])/stride[geo_xtrack_idx] + 1);

      return new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);

  }


}

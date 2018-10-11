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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import visad.FunctionType;
import visad.Gridded1DSet;
import visad.QuickSort;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SingletonSet;

public class SpectrumAdapter extends MultiDimensionAdapter {

  public static String channels_name = "Channels";
  public static String channelIndex_name = "channelIndex";
  public static String FOVindex_name = "FOVindex";
  public static String channelUnit = "cm";
  public static String channelType = "wavenumber";
  public static String array_name  = "array_name";
  public static String array_dimension_names = "array_dimension_names";
  public static String range_name = "range_name";
  public static String x_dim_name  = "x_dim"; //- 2 spatial dimensions, x fastest varying
  public static String y_dim_name  = "y_dim"; //-----------------------------------------
  public static String time_dim_name = "time_dim";
  public static String ancillary_file_name = "ancillary_file";
  public static String channelValues = "channelValues";
  public static String bandNames = "bandNames";


  public static Map<String, Object> getEmptyMetadataTable() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put(array_name, null);
    metadata.put(range_name, null);
    metadata.put(channelIndex_name, null);
    metadata.put(ancillary_file_name, null);
    metadata.put(x_dim_name, null);
    metadata.put(y_dim_name, null);
    metadata.put(time_dim_name, null);
    metadata.put(channelUnit, null);
    metadata.put(channelType, "wavenumber");
    metadata.put(channelValues, null);
    metadata.put(bandNames, null);

    /*
    metadata.put(scale_name, null);
    metadata.put(offset_name, null);
    metadata.put(fill_value_name, null);
    metadata.put(range_unit, null);
    metadata.put(valid_range, null);
    */
    return metadata;
  }

  public static Map<String, double[]> getEmptySubset() {
    Map<String, double[]> subset = new HashMap<>();
    subset.put(x_dim_name, new double[3]);
    subset.put(y_dim_name, new double[3]);
    subset.put(channelIndex_name, new double[3]);
    return subset;
  }

  int numChannels;
  int channelIndex = -1;
  int[] channel_sort;
  SampledSet domainSet;
  RealType channelRealType;
  RealType spectrumRangeType;
  FunctionType spectrumType;

  List<String> bandNameList = new ArrayList<>();
  String[] bandNameArray = null;
  Map<String, Float> bandNameMap = null;
  boolean hasBandNames = false;

  public SpectrumAdapter(MultiDimensionReader reader, Map<String, Object> metadata) {
    super(reader, metadata);
    this.init();
  }

  private void init() {
    for (int k=0; k<array_rank;k++) {
      String name = (String) metadata.get(channelIndex_name);
      if (name != null) {
        if ( name.equals(array_dim_names[k]) ) {
          channelIndex = k;
        }
      }
    }

    numChannels = computeNumChannels();

    String[] names = (String[]) metadata.get(bandNames);
    if (names != null) {
      hasBandNames = true;
      bandNameArray = new String[names.length];
      for (int k=0; k<names.length;k++) {
        bandNameList.add(names[k]);
        bandNameArray[k] = names[k];
      }
    }

    try {
      domainSet = makeDomainSet();
      rangeType = makeSpectrumRangeType();
      spectrumType = new FunctionType(channelRealType, spectrumRangeType);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("cannot create spectrum domain");
    }
  
  }

  public boolean hasBandNames() {
     return hasBandNames;
  }

  public List<String> getBandNames() {
    return bandNameList;
  }

  public Map<String, Float> getBandNameMap() {
    return bandNameMap;
  }

  public int computeNumChannels() {
    if (channelIndex == -1) {
      return 1;
    } 
    else {
      return array_dim_lengths[channelIndex];
    }
  }

  public Set makeDomain(Map<String, double[]> subset) throws Exception {
    return domainSet;
  }

  public SampledSet getDomainSet() throws Exception {
    return domainSet;
  }

  private SampledSet makeDomainSet() throws Exception {
    RealType domainType = makeSpectrumDomainType();
    float[] channels = getChannels();
    channel_sort = QuickSort.sort(channels);
    if (numChannels == 1) {
      domainSet = new SingletonSet(new RealTupleType(domainType), new double[] {(double)channels[0]}, null, null, null);
    }
    else {
      domainSet = new Gridded1DSet(domainType, new float[][] {channels}, numChannels);
    }
    return domainSet;
  }

  public float[] getChannels() throws Exception {
    float[] channels = null;
    if (metadata.get(channelValues) == null) {
      channels = reader.getFloatArray((String)metadata.get(channels_name),
                                            new int[] {0}, new int[] {numChannels}, new int[] {1});
    } 
    else {
      channels = (float[]) metadata.get(channelValues);
    }

    if (hasBandNames) {
      bandNameMap = new HashMap<>();
      for (int k=0; k<numChannels; k++) {
        bandNameMap.put(bandNameArray[k], new Float(channels[k]));
      }
    }
    return channels;
  }

  public RealType makeSpectrumDomainType() throws Exception {
    /**
    if ( ((String)metadata.get(channelType)).equals("wavenumber") ) {
      ScaledUnit centimeter = new ScaledUnit(0.01, CommonUnit.meter, "cm");
      Unit tmp_unit = centimeter.pow(-1);
      ScaledUnit inv_centimeter = new ScaledUnit(1.0, tmp_unit, "cm^-1");
      channelRealType = RealType.getRealType("wavenumber", null);
    }
    **/
    channelRealType = RealType.getRealType((String)metadata.get(channelType), null);
    return channelRealType;
  }

  public RealType makeSpectrumRangeType() throws Exception {
    spectrumRangeType = RealType.getRealType("Radiance");
    return spectrumRangeType;
  }

  float[] sortRange(float[] range) {
    float[] sorted_range = new float[numChannels];
    for (int k=0; k<numChannels; k++) sorted_range[k] = range[channel_sort[k]];
    return sorted_range;
  }

  double[] sortRange(double[] range) {
    double[] sorted_range =  new double[numChannels];
    for (int k=0; k<numChannels; k++) sorted_range[k] = range[channel_sort[k]];
    return sorted_range;
  }

  public Map<String, double[]> getDefaultSubset() {
    Map<String, double[]> subset = SpectrumAdapter.getEmptySubset();
    
    double[] coords = (double[])subset.get(y_dim_name);
    coords[0] = 1.0;
    coords[1] = 1.0;
    coords[2] = 1.0;
    subset.put(y_dim_name, coords);
                                                                                                                                     
    coords = (double[])subset.get(x_dim_name);
    coords[0] = 1.0;
    coords[1] = 1.0;
    coords[2] = 1.0;
    subset.put(x_dim_name, coords);

    coords = (double[])subset.get(channelIndex_name);
    coords[0] = 0.0;
    coords[1] = (double) (numChannels - 1);
    coords[2] = 1.0;
    subset.put(channelIndex_name, coords);

    return subset;
  }

  public int getChannelIndexFromWavenumber(float wavenumber) throws Exception {
    int idx = (domainSet.valueToIndex(new float[][] {{wavenumber}}))[0];
    return channel_sort[idx];
  }

  public float getWavenumberFromChannelIndex(int index) throws Exception {
    int idx = channel_sort[index];
    return (domainSet.indexToValue(new int[] {idx}))[0][0];
  }

  public int getNumChannels() {
    return numChannels;
  }
}

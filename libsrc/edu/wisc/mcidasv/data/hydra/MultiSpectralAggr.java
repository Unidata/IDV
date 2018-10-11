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

import visad.FlatField;
import visad.SampledSet;
import visad.RealTuple;
import visad.SetType;
import visad.RealType;
import visad.RealTupleType;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.FunctionType;
import visad.Real;
import visad.Set;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Gridded1DSet;
import visad.Gridded2DSet;
import visad.QuickSort;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import visad.georef.MapProjection;
import visad.CachingCoordinateSystem;
import ucar.visad.ProjectionCoordinateSystem;

public class MultiSpectralAggr extends MultiSpectralData {

  Gridded1DSet aggrDomain = null;

  MultiSpectralData[] adapters = null;

  int[] sort_indexes = null;

  float[] aggrValues = null;

  float[] aggrSamples = null;

  int numAdapters;

  int numBands;

  int[] offset;

  public MultiSpectralAggr(MultiSpectralData[] adapters)
         throws Exception {
    super(adapters[0].swathAdapter, null);
    this.adapters = adapters;
    paramName = adapters[0].getParameter();

    numAdapters = adapters.length;
    int[] numBandsAdapter = new int[numAdapters];
    offset = new int[numAdapters];
    SampledSet[] spectrumDomains = new SampledSet[numAdapters];

    if (adapters[0].spectrumAdapter.hasBandNames()) {
      hasBandNames = true;
      bandNameList = new ArrayList<>();
      bandNameMap = new HashMap<>();
      for (int k=0; k<numAdapters; k++) {
        bandNameList.addAll(adapters[k].spectrumAdapter.getBandNames());
        bandNameMap.putAll(adapters[k].spectrumAdapter.getBandNameMap());
      }
    }

    numBands = 0;
    for (int k=0; k<numAdapters; k++) {
      SampledSet set = adapters[k].spectrumAdapter.getDomainSet();
      spectrumDomains[k] = set;
      numBandsAdapter[k] = set.getLength();
      offset[k] = numBands;
      numBands += numBandsAdapter[k];
    }
   
    aggrSamples = new float[numBands];
    aggrValues  = new float[numBands];

    for (int k=0; k<numAdapters; k++) {
      float[][] samples = spectrumDomains[k].getSamples(false);
      System.arraycopy(samples[0], 0, aggrSamples, offset[k], samples[0].length);
    }

    sort_indexes = QuickSort.sort(aggrSamples);
    SpectrumAdapter specAdapt = adapters[0].spectrumAdapter;
    aggrDomain = new Gridded1DSet(specAdapt.getDomainSet().getType(), 
                        new float[][] {aggrSamples}, aggrSamples.length); 

    init_wavenumber = getWavenumberFromChannelIndex(0);
  }

  public FlatField getSpectrum(int[] coords) throws Exception {
    FlatField spectrum = null;
    for (int k=0; k<numAdapters; k++) {
      spectrum = adapters[k].getSpectrum(coords);
      if (spectrum == null) {
        return null;
      }
      float[][] values = spectrum.getFloats(false);
      System.arraycopy(values[0], 0, aggrValues, offset[k], values[0].length);
    }

    for (int t=0; t<numBands; t++) {
      aggrValues[t] = aggrValues[sort_indexes[t]];
    }

    spectrum = new FlatField((FunctionType)spectrum.getType(), aggrDomain);
    spectrum.setSamples(new float[][] {aggrValues});

    return spectrum;
  }

  public FlatField getSpectrum(RealTuple location) throws Exception {
    FlatField spectrum = null;
    for (int k=0; k<numAdapters; k++) {
      spectrum = adapters[k].getSpectrum(location);
      if (spectrum == null) {
        return null;
      }
      float[][] values = spectrum.getFloats(false);
      System.arraycopy(values[0], 0, aggrValues, offset[k], values[0].length);
    }

    for (int t=0; t<numBands; t++) {
      aggrValues[t] = aggrValues[sort_indexes[t]];
    }

    spectrum = new FlatField((FunctionType)spectrum.getType(), aggrDomain);
    spectrum.setSamples(new float[][] {aggrValues});

    return spectrum;
  }

  public FlatField getImage(Map<String, double[]> subset) throws Exception {
    int channelIndex = (int) ((double[])subset.get(SpectrumAdapter.channelIndex_name))[0];
    
    int idx = sort_indexes[channelIndex];
    
    int swathAdapterIndex = numAdapters-1;
    for (int k=0; k<numAdapters-1;k++) {
      if (idx >= offset[k] && idx < offset[k+1]) swathAdapterIndex = k;
    }
    float channel = aggrSamples[channelIndex];
    FlatField image = adapters[swathAdapterIndex].getImage(channel, subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();
    for (int k=0; k<numAdapters;k++) {
      if (k != swathAdapterIndex) adapters[k].setCoordinateSystem(cs);
    }
    return image;
  }

  public FlatField getImage(float channel, Map<String, double[]> subset) throws Exception {
    int channelIndex = aggrDomain.valueToIndex(new float[][] {{channel}})[0];

    int idx = sort_indexes[channelIndex];

    int swathAdapterIndex = numAdapters-1;
    for (int k=0; k<numAdapters-1;k++) {
      if (idx >= offset[k] && idx < offset[k+1]) swathAdapterIndex = k;
    }
    channel = aggrSamples[channelIndex];
    FlatField image = adapters[swathAdapterIndex].getImage(channel, subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();
    for (int k=0; k<numAdapters;k++) {
      if (k != swathAdapterIndex) adapters[k].setCoordinateSystem(cs);
    }
    return image;
  }

  public int getChannelIndexFromWavenumber(float channel) throws VisADException, RemoteException {
    int idx = (aggrDomain.valueToIndex(new float[][] {{channel}}))[0];
    return idx;
  }

  public float getWavenumberFromChannelIndex(int index) throws Exception {
    return (aggrDomain.indexToValue(new int[] {index}))[0][0];
  }

  public Map<String, double[]> getDefaultSubset() {
    Map<String, double[]> subset = adapters[0].getDefaultSubset();
    double chanIdx = 0;
    try {
      chanIdx = getChannelIndexFromWavenumber(init_wavenumber);
    }
    catch (Exception e) {
      System.out.println("couldn't get chanIdx, using zero");
    }
    subset.put(SpectrumAdapter.channelIndex_name, new double[] {chanIdx, chanIdx, 1});
    return subset;
  }

}
